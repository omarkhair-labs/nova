package com.nova.app.app

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.auth.domain.model.NovaUser
import com.nova.app.navigation.AppDestination
import com.nova.app.navigation.NovaRootTab
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
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
    fun cachedSessionHydratesBeforeRemoteValidationAndRemoteResultRemainsAuthoritative() = runBlocking {
        val cached = user("cached")
        val refreshed = user("refreshed")
        val viewModel = viewModel(
            cachedUser = { cached },
            restore = { ApiResult.Success(refreshed) },
        )

        assertEquals(true, viewModel.hydrateCachedSession())
        assertFalse(viewModel.state.isBootstrapping)
        assertEquals(cached, viewModel.state.currentUser)

        viewModel.bootstrapSession()

        assertEquals(refreshed, viewModel.state.currentUser)
    }

    @Test
    fun invalidRemoteSessionClearsAnImmediatelyHydratedCachedUser() = runBlocking {
        val cached = user("cached")
        val viewModel = viewModel(
            cachedUser = { cached },
            restore = { ApiResult.Success(null) },
        )

        assertEquals(true, viewModel.hydrateCachedSession())
        assertEquals(cached, viewModel.state.currentUser)

        viewModel.bootstrapSession()

        assertNull(viewModel.state.currentUser)
        assertFalse(viewModel.state.isBootstrapping)
    }

    @Test
    fun logoutWhileCachedSessionValidationIsInFlightCannotRestoreTheOldAccount() = runBlocking {
        val cached = user("cached")
        val validation = CompletableDeferred<ApiResult<NovaUser?>>()
        val viewModel = viewModel(
            cachedUser = { cached },
            restore = { validation.await() },
        )
        viewModel.hydrateCachedSession()

        val bootstrap = launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.bootstrapSession()
        }
        viewModel.expireSession()
        validation.complete(ApiResult.Success(cached))
        bootstrap.join()

        assertNull(viewModel.state.currentUser)
    }

    @Test
    fun typedNavigationOwnsOverlayAndSocialRootState() {
        val requestedRoots = mutableListOf<NovaRootTab>()
        val viewModel = viewModel(requestRoot = requestedRoots::add)

        viewModel.navigate(AppDestination.Inbox)
        assertEquals(AppDestination.Inbox, viewModel.state.primaryOverlay)

        viewModel.navigate(AppDestination.Orbit)
        assertNull(viewModel.state.primaryOverlay)

        viewModel.navigate(AppDestination.Create)
        viewModel.navigate(AppDestination.Profile)
        assertNull(viewModel.state.primaryOverlay)
        assertEquals(
            listOf(NovaRootTab.Orbit, NovaRootTab.Create, NovaRootTab.Profile),
            requestedRoots,
        )
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
        cachedUser: () -> NovaUser? = { null },
        restore: suspend () -> ApiResult<NovaUser?> = { ApiResult.Success(null) },
        logout: () -> Unit = {},
        requestRoot: (NovaRootTab) -> Unit = {},
    ) = AppViewModel(
        restoreCachedUser = cachedUser,
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
