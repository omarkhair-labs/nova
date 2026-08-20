package com.nova.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.nova.app.app.appContainer
import com.nova.app.feature.people.MODE_FOLLOWERS
import com.nova.app.feature.people.MODE_FOLLOWING
import com.nova.app.feature.people.SocialConnectionsScreen
import com.nova.app.feature.people.SocialConnectionsStateOwner
import com.nova.app.ui.theme.NovaTheme


class SocialGraphActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val username = intent.getStringExtra(EXTRA_USERNAME).orEmpty().trim().lowercase()
        val mode = intent.getStringExtra(EXTRA_MODE)
            .takeIf { it == MODE_FOLLOWING }
            ?: MODE_FOLLOWERS

        if (username.isBlank()) {
            finish()
            return
        }

        val container = appContainer
        setContent {
            NovaTheme {
                val scope = rememberCoroutineScope()
                val owner = remember(username, mode, scope) {
                    SocialConnectionsStateOwner(
                        username = username,
                        mode = mode,
                        currentUserId = container.currentCachedUserId(),
                        pagingRepository = container.peoplePagingRepository,
                        peopleRepository = container.peopleRepository,
                        scope = scope,
                    )
                }
                val state = owner.state
                var handledSessionExpiry by remember(owner) { mutableStateOf(0) }

                LaunchedEffect(owner) {
                    owner.enter()
                }
                LaunchedEffect(state.sessionExpiryVersion) {
                    if (state.sessionExpiryVersion > handledSessionExpiry) {
                        handledSessionExpiry = state.sessionExpiryVersion
                        startActivity(
                            Intent(this@SocialGraphActivity, MainActivity::class.java).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
                            )
                        )
                        finish()
                    }
                }

                SocialConnectionsScreen(
                    username = username,
                    mode = owner.mode,
                    state = state,
                    onQueryChange = owner::setQuery,
                    onRetry = owner::retry,
                    onLoadMore = owner::loadMore,
                    onFollowToggle = owner::toggleFollow,
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_USERNAME = "social_graph_username"
        const val EXTRA_MODE = "social_graph_mode"
    }
}
