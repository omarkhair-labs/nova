package com.nova.app.core.social

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource
import com.nova.app.feature.people.data.PeopleRepository
import com.nova.app.feature.people.domain.model.NovaPerson


class NovaSocialRepository(
    context: Context,
    private val api: NovaApiClient = NovaApiClient("https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"),
) : PeopleRepository {
    private val sessionStore = NovaSessionStore(context.applicationContext)
    private val authRemote = AuthRemoteDataSource(api)

    override suspend fun people(query: String): ApiResult<List<NovaPerson>> {
        return authenticatedCall { accessToken ->
            api.people(accessToken, query)
        }
    }

    override suspend fun person(username: String): ApiResult<NovaPerson> {
        return authenticatedCall { accessToken ->
            api.person(accessToken, username)
        }
    }

    override suspend fun setFollowing(
        username: String,
        follow: Boolean,
    ): ApiResult<NovaPerson> {
        return authenticatedCall { accessToken ->
            api.setFollowing(accessToken, username, follow)
        }
    }

    override suspend fun setBlocked(
        username: String,
        blocked: Boolean,
    ): ApiResult<Unit> {
        return authenticatedCall { accessToken ->
            api.setBlocked(accessToken, username, blocked)
        }
    }

    override suspend fun report(
        username: String,
        reason: String,
        details: String,
    ): ApiResult<String> {
        return authenticatedCall { accessToken ->
            api.reportPerson(accessToken, username, reason, details)
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

        return when (val refreshed = authRemote.refresh(stored.refreshToken)) {
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
