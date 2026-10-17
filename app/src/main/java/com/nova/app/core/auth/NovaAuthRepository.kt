package com.nova.app.core.auth

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.core.push.NovaPushRegistration
import com.nova.app.feature.plants.PlantupiaCareReminderScheduler
import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource
import com.nova.app.feature.auth.domain.model.AuthSession
import com.nova.app.feature.auth.domain.model.NovaUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive


class NovaAuthRepository(
    context: Context,
    private val remote: AuthRemoteDataSource = AuthRemoteDataSource(
        NovaApiClient(com.nova.app.core.network.NovaServerConfig.API_URL),
    ),
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

    /**
     * Returns only the user attached to the locally persisted token session.
     * Remote session validation remains owned by [restoreSession].
     */
    fun cachedUser(): NovaUser? = sessionStore.load()?.cachedUser

    suspend fun register(email: String, password: String, username: String, name: String): ApiResult<NovaUser> {
        val pendingAvatarUri = NovaPendingRegistrationPhoto.consume()
        val owner = sessionStore.beginLogin()
        val result = remote.register(email.trim().lowercase(), password, username.trim().lowercase(), name.trim())
        currentCoroutineContext().ensureActive()
        if (!sessionStore.isCurrent(owner)) return changedSession()
        if (result is ApiResult.Failure) return result
        val session = (result as ApiResult.Success).value
        if (!sessionStore.save(owner, session)) return changedSession()
        NovaPushRegistration.activate(appContext)
        if (pendingAvatarUri == null) return ApiResult.Success(session.user)
        val prepared = prepareAvatar(pendingAvatarUri)
        if (!sessionStore.isCurrent(owner)) return changedSession()
        if (prepared !is ApiResult.Success || prepared.value == null) return ApiResult.Success(session.user)
        val stored = sessionStore.current(owner) ?: return changedSession()
        val updated = sessionStore.authenticatedRequest(
            stored, refresh = { remote.refresh(stored.refreshToken) },
        ) { token ->
            remote.updateProfile(token, name.trim(), username.trim().lowercase(), avatar = prepared.value)
        }
        currentCoroutineContext().ensureActive()
        if (!sessionStore.isCurrent(owner)) return changedSession()
        return when (updated) {
            is ApiResult.Success -> if (sessionStore.updateUser(owner, updated.value)) updated else changedSession()
            // Registration succeeded; an optional failed avatar upload must not strand onboarding.
            is ApiResult.Failure -> ApiResult.Success(session.user)
        }
    }

    suspend fun login(email: String, password: String): ApiResult<NovaUser> {
        NovaPendingRegistrationPhoto.clear()
        val owner = sessionStore.beginLogin()
        val result = remote.login(email.trim().lowercase(), password)
        currentCoroutineContext().ensureActive()
        if (!sessionStore.isCurrent(owner)) return changedSession()
        return when (result) {
            is ApiResult.Success -> {
                if (!sessionStore.save(owner, result.value)) return changedSession()
                NovaPushRegistration.activate(appContext)
                ApiResult.Success(result.value.user)
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun restoreSession(): ApiResult<NovaUser?> {
        val stored = sessionStore.load() ?: return ApiResult.Success(null)
        val result = sessionStore.authenticatedRequest(
            stored, refresh = { remote.refresh(stored.refreshToken) }, request = { token -> remote.me(token) },
        )
        currentCoroutineContext().ensureActive()
        return when (result) {
            is ApiResult.Success -> {
                if (!sessionStore.updateUser(stored.identity, result.value)) return changedSession()
                NovaPushRegistration.activate(appContext)
                ApiResult.Success(result.value)
            }
            is ApiResult.Failure -> when {
                result.statusCode == 409 -> result
                result.statusCode == 401 -> ApiResult.Success(null)
                else -> sessionStore.current(stored.identity)?.cachedUser?.let { ApiResult.Success(it) }
                    ?: if (sessionStore.isCurrent(stored.identity)) result else changedSession()
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
        plantupiaAvatarVariant: Int = -1,
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
            plantupiaAvatarVariant = plantupiaAvatarVariant,
        )
    }

    fun logout() {
        // Explicit local check-in reminders belong to the signed-in person.
        // Remove pending work before another account can use this device.
        PlantupiaCareReminderScheduler.clearForLogout(appContext)
        NovaPendingRegistrationPhoto.clear()
        val (previous, signedOut) = sessionStore.logoutSession()
        NovaPushRegistration.logout(
            context = appContext,
            accessToken = previous?.accessToken,
            signedOut = signedOut,
        )
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
        plantupiaAvatarVariant: Int = -1,
    ): ApiResult<NovaUser> {
        if (!sessionStore.isCurrent(stored.identity)) return changedSession()
        val result = sessionStore.authenticatedRequest(
            stored, refresh = { remote.refresh(stored.refreshToken) },
        ) { token ->
            remote.updateProfile(
                accessToken = token, name = name, username = username, avatar = avatar,
                bio = bio, location = location, link = link, interests = interests,
                profileTheme = profileTheme, showOrbit = showOrbit, plantupiaAvatarVariant = plantupiaAvatarVariant,
            )
        }
        currentCoroutineContext().ensureActive()
        return when (result) {
            is ApiResult.Success -> if (sessionStore.updateUser(stored.identity, result.value)) result else changedSession()
            is ApiResult.Failure -> result
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

}
