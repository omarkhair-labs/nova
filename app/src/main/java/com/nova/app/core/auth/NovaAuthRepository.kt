package com.nova.app.core.auth

import android.content.Context
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.AuthSession
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaUser


class NovaAuthRepository(
    context: Context,
    private val api: NovaApiClient = NovaApiClient(),
) {
    private val sessionStore = NovaSessionStore(context)

    suspend fun register(
        email: String,
        password: String,
        username: String,
        name: String,
    ): ApiResult<NovaUser> {
        return when (
            val result = api.register(
                email = email.trim().lowercase(),
                password = password,
                username = username.trim().lowercase(),
                name = name.trim(),
            )
        ) {
            is ApiResult.Success -> {
                sessionStore.save(result.value)
                ApiResult.Success(result.value.user)
            }

            is ApiResult.Failure -> result
        }
    }

    suspend fun login(email: String, password: String): ApiResult<NovaUser> {
        return when (val result = api.login(email.trim().lowercase(), password)) {
            is ApiResult.Success -> {
                sessionStore.save(result.value)
                ApiResult.Success(result.value.user)
            }

            is ApiResult.Failure -> result
        }
    }

    suspend fun restoreSession(): ApiResult<NovaUser?> {
        val stored = sessionStore.load() ?: return ApiResult.Success(null)

        return when (val me = api.me(stored.accessToken)) {
            is ApiResult.Success -> {
                sessionStore.updateUser(me.value)
                ApiResult.Success(me.value)
            }

            is ApiResult.Failure -> {
                if (me.statusCode != 401) {
                    stored.cachedUser?.let { ApiResult.Success(it) } ?: me
                } else {
                    restoreWithRefresh(stored)
                }
            }
        }
    }

    fun logout() {
        sessionStore.clear()
    }

    private suspend fun restoreWithRefresh(
        stored: NovaSessionStore.StoredSession,
    ): ApiResult<NovaUser?> {
        return when (val refreshed = api.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)

                when (val me = api.me(refreshed.value)) {
                    is ApiResult.Success -> {
                        sessionStore.save(
                            AuthSession(
                                accessToken = refreshed.value,
                                refreshToken = stored.refreshToken,
                                user = me.value,
                            ),
                        )
                        ApiResult.Success(me.value)
                    }

                    is ApiResult.Failure -> {
                        if (me.statusCode == 401) {
                            sessionStore.clear()
                            ApiResult.Success(null)
                        } else {
                            stored.cachedUser?.let { ApiResult.Success(it) } ?: me
                        }
                    }
                }
            }

            is ApiResult.Failure -> {
                if (refreshed.statusCode == 400 || refreshed.statusCode == 401) {
                    sessionStore.clear()
                    ApiResult.Success(null)
                } else {
                    stored.cachedUser?.let { ApiResult.Success(it) } ?: refreshed
                }
            }
        }
    }
}
