package com.nova.app.core.auth

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.core.push.NovaPushRegistration
import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource
import com.nova.app.feature.auth.domain.model.AuthSession
import com.nova.app.feature.auth.domain.model.NovaUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class NovaAuthRepository(
    context: Context,
    private val remote: AuthRemoteDataSource = AuthRemoteDataSource(
        NovaApiClient("https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"),
    ),
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

    /**
     * Returns only the user attached to the locally persisted token session.
     * Remote session validation remains owned by [restoreSession].
     */
    fun cachedUser(): NovaUser? = sessionStore.load()?.cachedUser

    suspend fun register(
        email: String,
        password: String,
        username: String,
        name: String,
    ): ApiResult<NovaUser> {
        return when (
            val result = remote.register(
                email = email.trim().lowercase(),
                password = password,
                username = username.trim().lowercase(),
                name = name.trim(),
            )
        ) {
            is ApiResult.Success -> {
                sessionStore.save(result.value)
                NovaPushRegistration.activate(appContext)

                val pendingAvatarUri = NovaPendingRegistrationPhoto.consume()
                if (pendingAvatarUri == null) {
                    ApiResult.Success(result.value.user)
                } else {
                    val prepared = prepareAvatar(pendingAvatarUri)
                    if (prepared is ApiResult.Success && prepared.value != null) {
                        when (
                            val updated = remote.updateProfile(
                                accessToken = result.value.accessToken,
                                name = name.trim(),
                                username = username.trim().lowercase(),
                                avatar = prepared.value,
                            )
                        ) {
                            is ApiResult.Success -> {
                                sessionStore.updateUser(updated.value)
                                ApiResult.Success(updated.value)
                            }

                            is ApiResult.Failure -> {
                                // The account is already valid. Avatar upload is optional, so
                                // do not strand a new user in onboarding if media upload fails.
                                ApiResult.Success(result.value.user)
                            }
                        }
                    } else {
                        // The selected image is optional. Registration remains successful even
                        // if the local picker URI becomes unreadable before the upload begins.
                        ApiResult.Success(result.value.user)
                    }
                }
            }

            is ApiResult.Failure -> result
        }
    }

    suspend fun login(email: String, password: String): ApiResult<NovaUser> {
        NovaPendingRegistrationPhoto.clear()
        return when (val result = remote.login(email.trim().lowercase(), password)) {
            is ApiResult.Success -> {
                sessionStore.save(result.value)
                NovaPushRegistration.activate(appContext)
                ApiResult.Success(result.value.user)
            }

            is ApiResult.Failure -> result
        }
    }

    suspend fun restoreSession(): ApiResult<NovaUser?> {
        val stored = sessionStore.load() ?: return ApiResult.Success(null)

        return when (val me = remote.me(stored.accessToken)) {
            is ApiResult.Success -> {
                sessionStore.updateUser(me.value)
                NovaPushRegistration.activate(appContext)
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
        bio: String,
        location: String,
        link: String,
        interests: List<String>,
        profileTheme: String,
        showOrbit: Boolean,
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
            bio = bio.trim(),
            location = location.trim(),
            link = link.trim(),
            interests = interests.map(String::trim).filter(String::isNotBlank).distinct().take(8),
            profileTheme = profileTheme,
            showOrbit = showOrbit,
        )
    }

    fun logout() {
        NovaPendingRegistrationPhoto.clear()
        val accessToken = sessionStore.load()?.accessToken
        NovaPushRegistration.logout(
            context = appContext,
            accessToken = accessToken,
        )
        sessionStore.clear()
    }

    private suspend fun updateProfileWithSession(
        stored: NovaSessionStore.StoredSession,
        name: String,
        username: String,
        avatar: UploadFile?,
        bio: String,
        location: String,
        link: String,
        interests: List<String>,
        profileTheme: String,
        showOrbit: Boolean,
    ): ApiResult<NovaUser> {
        when (
            val first = remote.updateProfile(
                accessToken = stored.accessToken,
                name = name,
                username = username,
                avatar = avatar,
                bio = bio,
                location = location,
                link = link,
                interests = interests,
                profileTheme = profileTheme,
                showOrbit = showOrbit,
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

        return when (val refreshed = remote.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                when (
                    val retried = remote.updateProfile(
                        accessToken = refreshed.value,
                        name = name,
                        username = username,
                        avatar = avatar,
                        bio = bio,
                        location = location,
                        link = link,
                        interests = interests,
                        profileTheme = profileTheme,
                        showOrbit = showOrbit,
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
        return when (val refreshed = remote.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)

                when (val me = remote.me(refreshed.value)) {
                    is ApiResult.Success -> {
                        sessionStore.save(
                            AuthSession(
                                accessToken = refreshed.value,
                                refreshToken = stored.refreshToken,
                                user = me.value,
                            ),
                        )
                        NovaPushRegistration.activate(appContext)
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
