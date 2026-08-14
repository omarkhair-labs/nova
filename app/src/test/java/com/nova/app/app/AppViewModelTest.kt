package com.nova.app.app

import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaUser
import com.nova.app.navigation.AppDestination
import com.nova.app.navigation.NovaRootTab
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AppViewModelTest {
    @Test
    fun bootstrapOwnsRestoredSessionState() = runBlocking {
        val user = user("maya")
        val viewModel = viewModel(restore = { ApiResult.Success(user) })

        viewModel.bootstrapSession()

        assertFalse(viewModel.state.isBootstrapping)
        assertEquals(user, viewModel.state.currentUser)
    }

    @Test
    fun typedNavigationOwnsOverlayAndSocialRootState() {
        val requestedRoots = mutableListOf<NovaRootTab>()
        val viewModel = viewModel(requestRoot = requestedRoots::add)

        viewModel.navigate(AppDestination.Messages)
        assertEquals(AppDestination.Messages, viewModel.state.primaryOverlay)

        viewModel.navigate(AppDestination.Profile)
        assertNull(viewModel.state.primaryOverlay)
        assertEquals(listOf(NovaRootTab.Profile), requestedRoots)
    }

    @Test
    fun sessionExpiryLogsOutAndClearsSessionAndOverlay() {
        var logoutCount = 0
        val viewModel = viewModel(logout = { logoutCount += 1 })
        viewModel.onAuthenticated(user("maya"))
        viewModel.navigate(AppDestination.Reels)

        viewModel.expireSession()

        assertEquals(1, logoutCount)
        assertNull(viewModel.state.currentUser)
        assertNull(viewModel.state.primaryOverlay)
    }

    @Test
    fun unauthorizedRefreshUsesTheSingleExpiryCoordinator() = runBlocking {
        var logoutCount = 0
        val viewModel = viewModel(
            restore = { ApiResult.Failure("expired", statusCode = 401) },
            logout = { logoutCount += 1 },
        )
        viewModel.onAuthenticated(user("maya"))

        val sessionActive = viewModel.refreshSession()

        assertFalse(sessionActive)
        assertEquals(1, logoutCount)
        assertNull(viewModel.state.currentUser)
    }

    private fun viewModel(
        restore: suspend () -> ApiResult<NovaUser?> = { ApiResult.Success(null) },
        logout: () -> Unit = {},
        requestRoot: (NovaRootTab) -> Unit = {},
    ) = AppViewModel(
        restoreSession = restore,
        logout = logout,
        requestSocialRoot = requestRoot,
    )

    private fun user(username: String) = NovaUser(
        id = 1L,
        email = "$username@example.test",
        username = username,
        name = "Maya",
        avatarUrl = "",
    )
}
