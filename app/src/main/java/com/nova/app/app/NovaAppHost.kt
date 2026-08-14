package com.nova.app.app

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nova.app.NovaApp
import com.nova.app.MessagesRootRoute
import com.nova.app.core.push.NovaPushOpenSignal
import com.nova.app.feature.reels.ReelsRoute
import com.nova.app.feature.reels.ReelsRouteArgs
import com.nova.app.navigation.AppDestination
import com.nova.app.navigation.NovaRootTab


/**
 * Single MainActivity owner for Nova's five primary destinations and session shell.
 *
 * The social Nav3 tree stays composed beneath the Reels and Messages overlays,
 * preserving the established root-switching behavior.
 */
@Composable
fun NovaAppHost() {
    val context = LocalContext.current
    val container = context.appContainer
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.factory(container))
    val appState = appViewModel.state

    fun expireOverlaySession() {
        appViewModel.expireSession()
        (context as? Activity)?.recreate()
    }

    fun openReelAuthor(username: String) {
        val cleanUsername = username.trim().lowercase()
        if (cleanUsername.isBlank()) return

        NovaPushOpenSignal.offer(
            Intent()
                .putExtra("kind", "follow")
                .putExtra("actor_username", cleanUsername),
        )
        appViewModel.showSocialRoot(NovaRootTab.Home)
    }

    val primaryHandler = remember(appViewModel) {
        { destination: AppDestination ->
            appViewModel.navigate(destination)
            Unit
        }
    }

    DisposableEffect(container.appNavigator, primaryHandler) {
        container.appNavigator.attach(primaryHandler)
        onDispose {
            container.appNavigator.detach(primaryHandler)
        }
    }

    BackHandler(enabled = appState.primaryOverlay == AppDestination.Reels) {
        appViewModel.clearPrimaryOverlay()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NovaApp(
            appContainer = container,
            appViewModel = appViewModel,
        )

        // Stop empty areas in the active root from forwarding touches to the social tree below.
        if (appState.primaryOverlay != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = {}),
            )
        }

        when (appState.primaryOverlay) {
            AppDestination.Reels -> {
                ReelsRoute(
                    route = ReelsRouteArgs.Root,
                    onFinish = ::expireOverlaySession,
                    onHomeClick = { appViewModel.showSocialRoot(NovaRootTab.Home) },
                    onPeopleClick = { appViewModel.showSocialRoot(NovaRootTab.People) },
                    onProfileClick = { appViewModel.showSocialRoot(NovaRootTab.Profile) },
                    onPersonClick = ::openReelAuthor,
                )
            }

            AppDestination.Messages -> {
                MessagesRootRoute(
                    onRootRequested = appViewModel::showSocialRoot,
                    onSessionExpired = ::expireOverlaySession,
                )
            }

            AppDestination.Home,
            AppDestination.People,
            AppDestination.Profile,
            null -> Unit
        }
    }
}
