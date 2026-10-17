package com.nova.app.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.auth.domain.model.NovaUser
import com.nova.app.navigation.AppDestination
import com.nova.app.navigation.AppNavigator
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab


/** Lifecycle-aware owner for global session bootstrap and primary route state. */
class AppViewModel internal constructor(
    private val restoreCachedUser: () -> NovaUser?,
    private val restoreSession: suspend () -> ApiResult<NovaUser?>,
    private val hasStoredSession: () -> Boolean,
    private val requestSocialRoot: (NovaRootTab) -> Unit,
    private val cleanupExpiredSession: () -> Boolean = { true },
) : ViewModel(), AppNavigator {
    var state by mutableStateOf(AppState())
        private set

    private var bootstrapStarted = false
    private var sessionGeneration = 0

    /**
     * Makes a persisted returning session available to the shell synchronously.
     * [bootstrapSession] still validates it remotely and remains authoritative.
     */
    fun hydrateCachedSession(): Boolean {
        if (!state.isBootstrapping) return state.currentUser != null
        val cachedUser = restoreCachedUser() ?: return false
        state = state.copy(currentUser = cachedUser, isBootstrapping = false)
        return true
    }

    suspend fun bootstrapSession() {
        if (bootstrapStarted) return
        bootstrapStarted = true
        val expectedGeneration = sessionGeneration

        val restored = restoreSession()
        if (expectedGeneration != sessionGeneration) return
        state = when (restored) {
            is ApiResult.Success -> state.copy(
                currentUser = restored.value,
                isBootstrapping = false,
            )

            is ApiResult.Failure -> state.copy(
                currentUser = null,
                isBootstrapping = false,
            )
        }
    }

    /** Returns false only when refresh expired and cleared the active session. */
    suspend fun refreshSession(): Boolean {
        val expectedGeneration = sessionGeneration
        val refreshed = restoreSession()
        if (expectedGeneration != sessionGeneration) return state.currentUser != null
        return when (refreshed) {
            is ApiResult.Success -> {
                if (refreshed.value == null) {
                    !expireSession()
                } else {
                    sessionGeneration += 1
                    state = state.copy(currentUser = refreshed.value)
                    true
                }
            }

            is ApiResult.Failure -> {
                if (refreshed.statusCode == 401) {
                    !expireSession()
                } else {
                    true
                }
            }
        }
    }

    fun onAuthenticated(user: NovaUser) {
        sessionGeneration += 1
        state = state.copy(currentUser = user, isBootstrapping = false)
    }

    /** Transport already invalidated its originating login. An old UI event cannot log out B. */
    fun expireSession(): Boolean {
        if (hasStoredSession()) return false
        if (!cleanupExpiredSession()) return false
        sessionGeneration += 1
        state = state.copy(primaryOverlay = null, currentUser = null)
        return true
    }

    fun clearPrimaryOverlay() {
        state = state.copy(primaryOverlay = null)
    }

    override fun navigate(destination: AppDestination): Boolean {
        when (destination) {
            AppDestination.Home -> showSocialRoot(NovaRootTab.Home)
            AppDestination.Orbit -> showSocialRoot(NovaRootTab.Orbit)
            AppDestination.Create -> showSocialRoot(NovaRootTab.Create)
            AppDestination.Profile -> showSocialRoot(NovaRootTab.Profile)
            AppDestination.Reels,
            AppDestination.Inbox -> state = state.copy(primaryOverlay = destination)
        }
        return true
    }

    fun showSocialRoot(tab: NovaRootTab) {
        state = state.copy(primaryOverlay = null)
        requestSocialRoot(tab)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AppViewModel::class.java))
                    return AppViewModel(
                        restoreCachedUser = container.authRepository::cachedUser,
                        restoreSession = container.authRepository::restoreSession,
                        hasStoredSession = container.authRepository::hasStoredSession,
                        cleanupExpiredSession = container.authRepository::cleanupExpiredSession,
                        requestSocialRoot = NovaRootNavigationSignal::request,
                    ) as T
                }
            }
    }
}
