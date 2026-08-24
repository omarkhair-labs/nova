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
    private val restoreSession: suspend () -> ApiResult<NovaUser?>,
    private val logout: () -> Unit,
    private val requestSocialRoot: (NovaRootTab) -> Unit,
) : ViewModel(), AppNavigator {
    var state by mutableStateOf(AppState())
        private set

    suspend fun bootstrapSession() {
        if (!state.isBootstrapping) return

        state = when (val restored = restoreSession()) {
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
        return when (val refreshed = restoreSession()) {
            is ApiResult.Success -> {
                if (refreshed.value == null) {
                    expireSession()
                    false
                } else {
                    state = state.copy(currentUser = refreshed.value)
                    true
                }
            }

            is ApiResult.Failure -> {
                if (refreshed.statusCode == 401) {
                    expireSession()
                    false
                } else {
                    true
                }
            }
        }
    }

    fun onAuthenticated(user: NovaUser) {
        state = state.copy(currentUser = user, isBootstrapping = false)
    }

    fun expireSession() {
        logout()
        state = state.copy(primaryOverlay = null, currentUser = null)
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
                        restoreSession = container.authRepository::restoreSession,
                        logout = container.authRepository::logout,
                        requestSocialRoot = NovaRootNavigationSignal::request,
                    ) as T
                }
            }
    }
}
