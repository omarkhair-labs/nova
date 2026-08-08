package com.nova.app.core.social

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPerson


class NovaSocialRepository(
    context: Context,
    private val api: NovaApiClient = NovaApiClient("https://nova-production-4f6b.up.railway.app/api/v1/"),
) {
    private val sessionStore = NovaSessionStore(context.applicationContext)

    suspend fun people(query: String = ""): ApiResult<List<NovaPerson>> {
        return authenticatedCall { accessToken ->
            api.people(accessToken, query)
        }
    }

    suspend fun person(username: String): ApiResult<NovaPerson> {
        return authenticatedCall { accessToken ->
            api.person(accessToken, username)
        }
    }

    suspend fun setFollowing(
        username: String,
        follow: Boolean,
    ): ApiResult<NovaPerson> {
        return authenticatedCall { accessToken ->
            api.setFollowing(accessToken, username, follow)
        }
    }

    private suspend fun <T> authenticatedCall(
        call: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return first
            is ApiResult.Failure -> {
                if (first.statusCode != 401) return first
            }
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
