package com.nova.app.feature.tonight.data.remote

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.auth.authenticatedRequest
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.tonight.data.TonightRepository
import com.nova.app.feature.tonight.data.parseTonightSnapshot
import com.nova.app.feature.tonight.domain.model.TonightSnapshot


class TonightRemoteRepository(
    context: Context,
    private val api: NovaApiClient,
) : TonightRepository {
    private val sessionStore = NovaSessionStore(context.applicationContext)

    override suspend fun tonight(utcOffsetMinutes: Int): ApiResult<TonightSnapshot> =
        authenticatedCall { token ->
            when (
                val response = api.requestJson(
                    path = "tonight/?utc_offset_minutes=$utcOffsetMinutes",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(
                    parseTonightSnapshot(response.value, api::resolveMediaUrl),
                )
                is ApiResult.Failure -> response
            }
        }

    private suspend fun <T> authenticatedCall(
        call: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)
        return sessionStore.authenticatedRequest(
            stored = stored,
            refresh = { api.refresh(stored.refreshToken) },
            request = call,
        )
    }
}
