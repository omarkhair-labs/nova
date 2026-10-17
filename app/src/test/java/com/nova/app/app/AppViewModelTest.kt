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
    fun confirmedSessionExpiryClearsUiAndOverlay() {
        val viewModel = viewModel()
        viewModel.onAuthenticated(user("maya"))
        viewModel.navigate(AppDestination.Reels)

        viewModel.expireSession()

        assertNull(viewModel.state.currentUser)
        assertNull(viewModel.state.primaryOverlay)
    }

    @Test
    fun unauthorizedRefreshUpdatesUiAfterTransportHasInvalidatedSession() = runBlocking {
        val viewModel = viewModel(
            restore = { ApiResult.Failure("expired", statusCode = 401) },
        )
        viewModel.onAuthenticated(user("maya"))

        val sessionActive = viewModel.refreshSession()

        assertFalse(sessionActive)
        assertNull(viewModel.state.currentUser)
    }

    @Test
    fun refreshCompletionCannotReplaceOrExpireALaterAuthenticatedUser() = runBlocking {
        for (response in listOf(ApiResult.Success(user("old")), ApiResult.Success(null), ApiResult.Failure("old", 401))) {
            val reply = CompletableDeferred<ApiResult<NovaUser?>>()
            val model = viewModel(restore = { reply.await() })
            model.onAuthenticated(user("old"))
            val refresh = launch(start = CoroutineStart.UNDISPATCHED) { model.refreshSession() }
            val current = user("new")
            model.onAuthenticated(current)
            reply.complete(response)
            refresh.join()
            assertEquals(current, model.state.currentUser)
        }
    }

    @Test
    fun delayedFeatureExpiryCannotClearANewStoredLoginOrItsUi() {
        val model = viewModel(hasStoredSession = { true })
        val current = user("new")
        model.onAuthenticated(current)
        model.navigate(AppDestination.Inbox)
        assertFalse(model.expireSession())
        assertEquals(current, model.state.currentUser)
        assertEquals(AppDestination.Inbox, model.state.primaryOverlay)
    }

    @Test
    fun loginInstalledBetweenExpiryCheckAndCleanupPreventsUiExpiry() {
        val model = viewModel(cleanupExpiredSession = { false })
        val current = user("new")
        model.onAuthenticated(current)
        assertFalse(model.expireSession())
        assertEquals(current, model.state.currentUser)
    }

    private fun viewModel(
        cachedUser: () -> NovaUser? = { null },
        restore: suspend () -> ApiResult<NovaUser?> = { ApiResult.Success(null) },
        hasStoredSession: () -> Boolean = { false },
        requestRoot: (NovaRootTab) -> Unit = {},
        cleanupExpiredSession: () -> Boolean = { true },
    ) = AppViewModel(
        restoreCachedUser = cachedUser,
        restoreSession = restore,
        hasStoredSession = hasStoredSession,
        requestSocialRoot = requestRoot,
        cleanupExpiredSession = cleanupExpiredSession,
    )

    private fun user(username: String) = NovaUser(
        id = 1L,
        email = "$username@example.test",
        username = username,
        name = "Maya",
        avatarUrl = "",
    )
}
