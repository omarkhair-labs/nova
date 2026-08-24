package com.nova.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nova.app.core.push.NovaPushOpenSignal
import com.nova.app.feature.reels.ReelPlaybackSafety
import com.nova.app.feature.reels.ReelsRoute
import com.nova.app.feature.reels.ReelsRouteFactory
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import com.nova.app.ui.components.NovaActiveCallPill
import com.nova.app.ui.theme.NovaTheme


class ReelsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val route = ReelsRouteFactory.fromIntent(intent)

        setContent {
            NovaTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    ReelsRoute(
                        route = route,
                        onFinish = { finish() },
                        onHomeClick = { finishToRoot(NovaRootTab.Home) },
                        onOrbitClick = { finishToRoot(NovaRootTab.Orbit) },
                        onCreateClick = { finishToRoot(NovaRootTab.Create) },
                        onProfileClick = { finishToRoot(NovaRootTab.Profile) },
                        onPersonClick = { username -> openPerson(username) },
                    )
                    NovaActiveCallPill(
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }

    override fun onPause() {
        // Pause pooled Reel players before another Activity/root tab becomes visible.
        // Compose disposal can happen a little later, so relying on onDispose alone
        // allowed audio to leak after leaving Reels on some navigation paths.
        ReelPlaybackSafety.pauseAll()
        super.onPause()
    }

    private fun openPerson(username: String) {
        NovaPushOpenSignal.offer(
            Intent()
                .putExtra("kind", "follow")
                .putExtra("actor_username", username),
        )
        NovaRootNavigationSignal.request(NovaRootTab.Home)
        finish()
    }

    private fun finishToRoot(tab: NovaRootTab) {
        NovaRootNavigationSignal.request(tab)
        finish()
    }

    companion object {
        const val EXTRA_PROFILE_USERNAME = ReelsRouteFactory.EXTRA_PROFILE_USERNAME
        const val EXTRA_INITIAL_REEL_ID = ReelsRouteFactory.EXTRA_INITIAL_REEL_ID
    }
}
