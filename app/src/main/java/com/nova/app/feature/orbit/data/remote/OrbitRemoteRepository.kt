package com.nova.app.feature.orbit.data.remote

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.orbit.data.OrbitRepository
import com.nova.app.feature.orbit.data.parseOrbitPage
import com.nova.app.feature.orbit.domain.model.OrbitPage
import java.net.URLEncoder


class OrbitRemoteRepository(
    context: Context,
    private val api: NovaApiClient,
) : OrbitRepository {
    private val sessionStore = NovaSessionStore(context.applicationContext)

    override suspend fun orbit(cursor: String?): ApiResult<OrbitPage> = authenticatedCall { token ->
        val path = cursor?.takeIf { it.isNotBlank() }?.let {
            "orbit/?cursor=${URLEncoder.encode(it, Charsets.UTF_8.name())}"
        } ?: "orbit/"

        when (val response = api.requestJson(path, bearerToken = token)) {
            is ApiResult.Success -> ApiResult.Success(
                parseOrbitPage(response.value, api::resolveMediaUrl),
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
