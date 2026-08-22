package com.nova.app.feature.memories.data.remote

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.memories.data.MemoryRepository
import com.nova.app.feature.memories.data.parseWeeklyMemory
import com.nova.app.feature.memories.domain.model.WeeklyMemory


class MemoryRemoteRepository(
    context: Context,
    private val api: NovaApiClient,
) : MemoryRepository {
    private val sessionStore = NovaSessionStore(context.applicationContext)

    override suspend fun week(
        utcOffsetMinutes: Int,
        weeksAgo: Int,
    ): ApiResult<WeeklyMemory> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "memories/week/?utc_offset_minutes=$utcOffsetMinutes&weeks_ago=$weeksAgo",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                parseWeeklyMemory(response.value, api::resolveMediaUrl),
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
