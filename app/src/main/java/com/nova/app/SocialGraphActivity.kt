package com.nova.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nova.app.feature.people.MODE_FOLLOWERS
import com.nova.app.feature.people.MODE_FOLLOWING
import com.nova.app.feature.people.SocialConnectionsScreen
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

        setContent {
            NovaTheme {
                SocialConnectionsScreen(
                    username = username,
                    mode = mode,
                    onBack = { finish() },
                    onSessionExpired = {
                        startActivity(
                            Intent(this, MainActivity::class.java).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK,
                            )
                        )
                        finish()
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_USERNAME = "social_graph_username"
        const val EXTRA_MODE = "social_graph_mode"
    }
}
