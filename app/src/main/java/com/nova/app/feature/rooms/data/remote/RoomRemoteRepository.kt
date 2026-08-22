package com.nova.app.feature.rooms.data.remote

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.rooms.data.RoomRepository
import com.nova.app.feature.rooms.data.parseRoomDetail
import com.nova.app.feature.rooms.data.parseRoomItemPage
import com.nova.app.feature.rooms.data.parseRooms
import com.nova.app.feature.rooms.domain.model.RoomDetail
import com.nova.app.feature.rooms.domain.model.RoomItemPage
import com.nova.app.feature.rooms.domain.model.RoomSummary
import org.json.JSONObject


class RoomRemoteRepository(
    context: Context,
    private val api: NovaApiClient,
) : RoomRepository {
    private val sessionStore = NovaSessionStore(context.applicationContext)

    override suspend fun rooms(): ApiResult<List<RoomSummary>> = authenticatedCall { token ->
        when (val response = api.requestJson("rooms/", bearerToken = token)) {
            is ApiResult.Success -> ApiResult.Success(
                parseRooms(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    override suspend fun room(conversationId: Long): ApiResult<RoomDetail> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "rooms/$conversationId/",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                parseRoomDetail(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    override suspend fun items(
        conversationId: Long,
        kind: String?,
        before: Long?,
        limit: Int,
    ): ApiResult<RoomItemPage> = authenticatedCall { token ->
        val params = buildList {
            kind?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { add("kind=$it") }
            before?.takeIf { it > 0L }?.let { add("before=$it") }
            add("limit=${limit.coerceIn(1, 50)}")
        }.joinToString("&")
        when (
            val response = api.requestJson(
                path = "rooms/$conversationId/items/?$params",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                parseRoomItemPage(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    override suspend fun updateDescription(
        conversationId: Long,
        description: String,
    ): ApiResult<RoomDetail> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "rooms/$conversationId/",
                method = "PATCH",
                body = JSONObject().put("description", description.trim()),
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                parseRoomDetail(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    private suspend fun <T> authenticatedCall(
        call: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return first
            is ApiResult.Failure -> if (first.statusCode != 401) return first
        }

        return when (val refreshed = api.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                when (val retried = call(refreshed.value)) {
                    is ApiResult.Success -> retried
                    is ApiResult.Failure -> {
                        if (retried.statusCode == 401) sessionStore.clear()
                        retried
                    }
                }
            }

            is ApiResult.Failure -> {
                if (refreshed.statusCode == 400 || refreshed.statusCode == 401) {
                    sessionStore.clear()
                    ApiResult.Failure("Your session expired. Please log in again.", 401)
                } else {
                    refreshed
                }
            }
        }
    }
}
