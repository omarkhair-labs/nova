package com.nova.app.core.auth

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.AuthSession
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaUser
import com.nova.app.core.network.UploadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class NovaAuthRepository(
    context: Context,
    private val api: NovaApiClient = NovaApiClient(),
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

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

    suspend fun updateProfile(
        name: String,
        username: String,
        avatarUri: Uri?,
    ): ApiResult<NovaUser> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        val avatar = when (val prepared = prepareAvatar(avatarUri)) {
            is ApiResult.Success -> prepared.value
            is ApiResult.Failure -> return prepared
        }

        return updateProfileWithSession(
            stored = stored,
            name = name.trim(),
            username = username.trim().lowercase(),
            avatar = avatar,
        )
    }

    fun logout() {
        sessionStore.clear()
    }

    private suspend fun updateProfileWithSession(
        stored: NovaSessionStore.StoredSession,
        name: String,
        username: String,
        avatar: UploadFile?,
    ): ApiResult<NovaUser> {
        when (
            val first = api.updateProfile(
                accessToken = stored.accessToken,
                name = name,
                username = username,
                avatar = avatar,
            )
        ) {
            is ApiResult.Success -> {
                sessionStore.updateUser(first.value)
                return first
            }

            is ApiResult.Failure -> {
                if (first.statusCode != 401) return first
            }
        }

        return when (val refreshed = api.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                when (
                    val retried = api.updateProfile(
                        accessToken = refreshed.value,
                        name = name,
                        username = username,
                        avatar = avatar,
                    )
                ) {
                    is ApiResult.Success -> {
                        sessionStore.updateUser(retried.value)
                        retried
                    }

                    is ApiResult.Failure -> {
                        if (retried.statusCode == 401) sessionStore.clear()
                        retried
                    }
                }
            }

            is ApiResult.Failure -> {
                if (refreshed.statusCode == 400 || refreshed.statusCode == 401) {
                    sessionStore.clear()
                }
                refreshed
            }
        }
    }

    private suspend fun prepareAvatar(uri: Uri?): ApiResult<UploadFile?> {
        if (uri == null) return ApiResult.Success(null)

        return withContext(Dispatchers.IO) {
            runCatching {
                val resolver = appContext.contentResolver
                val mimeType = resolver.getType(uri)?.takeIf { it.startsWith("image/") }
                    ?: "image/jpeg"
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType)
                    ?.takeIf { it.isNotBlank() }
                    ?: "jpg"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Couldn't read that photo.")

                if (bytes.size > 5 * 1024 * 1024) {
                    return@withContext ApiResult.Failure("Photo must be 5 MB or smaller.")
                }

                ApiResult.Success(
                    UploadFile(
                        bytes = bytes,
                        fileName = "nova-avatar-${System.currentTimeMillis()}.$extension",
                        mimeType = mimeType,
                    ),
                )
            }.getOrElse {
                ApiResult.Failure("Nova couldn't read that photo. Pick another image and try again.")
            }
        }
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
