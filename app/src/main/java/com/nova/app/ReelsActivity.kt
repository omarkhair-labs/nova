package com.nova.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nova.app.feature.reels.ReelsScreen
import com.nova.app.navigation.NovaPersonOpenSignal
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import com.nova.app.ui.components.NovaActiveCallPill
import com.nova.app.ui.theme.NovaTheme


class ReelsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NovaTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    ReelsScreen(
                        onFinish = { finish() },
                        onHomeClick = { finishToRoot(NovaRootTab.Home) },
                        onPeopleClick = { finishToRoot(NovaRootTab.People) },
                        onProfileClick = { finishToRoot(NovaRootTab.Profile) },
                        onPersonClick = { username ->
                            NovaPersonOpenSignal.request(username)
                            finish()
                        },
                    )
                    NovaActiveCallPill(
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }

    private fun finishToRoot(tab: NovaRootTab) {
        NovaRootNavigationSignal.request(tab)
        finish()
    }
}
