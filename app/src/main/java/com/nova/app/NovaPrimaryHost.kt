package com.nova.app

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nova.app.core.auth.NovaAuthRepository
import com.nova.app.core.push.NovaPushOpenSignal
import com.nova.app.feature.reels.ReelsScreen
import com.nova.app.navigation.NovaPrimaryDestination
import com.nova.app.navigation.NovaPrimaryNavigationDispatcher
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab


/**
 * Single MainActivity owner for Nova's five primary destinations.
 *
 * Home / People / You keep using their existing Nav3 social stack, which remains
 * composed underneath. Reels and Messages are promoted from separate root
 * Activities into MainActivity overlays, so primary-tab switching never creates
 * or races Android Activity stacks. Deep-link Activities remain available for
 * direct conversations and profile Reel viewers.
 */
@Composable
fun NovaPrimaryHost() {
    val context = LocalContext.current
    val authRepository = remember(context) {
        NovaAuthRepository(context.applicationContext)
    }

    var primaryOverlay by remember { mutableStateOf<NovaPrimaryDestination?>(null) }

    fun showSocialRoot(tab: NovaRootTab) {
        primaryOverlay = null
        NovaRootNavigationSignal.request(tab)
    }

    fun expireSession() {
        authRepository.logout()
        primaryOverlay = null
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
        showSocialRoot(NovaRootTab.Home)
    }

    val primaryHandler = remember {
        { destination: NovaPrimaryDestination ->
            when (destination) {
                NovaPrimaryDestination.Home -> showSocialRoot(NovaRootTab.Home)
                NovaPrimaryDestination.People -> showSocialRoot(NovaRootTab.People)
                NovaPrimaryDestination.Profile -> showSocialRoot(NovaRootTab.Profile)
                NovaPrimaryDestination.Reels -> primaryOverlay = NovaPrimaryDestination.Reels
                NovaPrimaryDestination.Messages -> primaryOverlay = NovaPrimaryDestination.Messages
            }
        }
    }

    DisposableEffect(primaryHandler) {
        NovaPrimaryNavigationDispatcher.attach(primaryHandler)
        onDispose {
            NovaPrimaryNavigationDispatcher.detach(primaryHandler)
        }
    }

    BackHandler(enabled = primaryOverlay == NovaPrimaryDestination.Reels) {
        primaryOverlay = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Keep the social Nav3 tree alive so tab switches do not reset Home/People/You state.
        NovaApp()

        // Stop empty areas in the active root from forwarding touches to the social tree below.
        if (primaryOverlay != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = {}),
            )
        }

        when (primaryOverlay) {
            NovaPrimaryDestination.Reels -> {
                ReelsScreen(
                    onFinish = ::expireSession,
                    onHomeClick = { showSocialRoot(NovaRootTab.Home) },
                    onPeopleClick = { showSocialRoot(NovaRootTab.People) },
                    onProfileClick = { showSocialRoot(NovaRootTab.Profile) },
                    onPersonClick = ::openReelAuthor,
                )
            }

            NovaPrimaryDestination.Messages -> {
                NovaMessagesRootContent(
                    onRootRequested = ::showSocialRoot,
                    onSessionExpired = ::expireSession,
                )
            }

            NovaPrimaryDestination.Home,
            NovaPrimaryDestination.People,
            NovaPrimaryDestination.Profile,
            null -> Unit
        }
    }
}
