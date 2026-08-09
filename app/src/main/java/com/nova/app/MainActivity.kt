package com.nova.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.push.NovaPushOpenSignal
import com.nova.app.core.push.NovaPushRegistration
import com.nova.app.ui.theme.NovaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        routePushIntent(intent)
        NovaPushRegistration.ensureChannel(this)
        NovaPushRegistration.syncForCurrentSession(this)
        requestNotificationPermissionIfNeeded()
        syncMessageUnreadCount()

        setContent {
            NovaTheme {
                NovaApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncMessageUnreadCount()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routePushIntent(intent)
    }

    private fun routePushIntent(intent: Intent?) {
        if (intent?.getStringExtra("kind") == "message") {
            val conversationId = intent.getStringExtra("conversation_id")?.toLongOrNull()
            val username = intent.getStringExtra("actor_username").orEmpty()
            if (conversationId != null && conversationId > 0L && username.isNotBlank()) {
                NovaMessagingNavigator.openConversation(
                    context = this,
                    conversationId = conversationId,
                    username = username,
                    displayName = intent.getStringExtra("actor_name").orEmpty(),
                    avatarUrl = intent.getStringExtra("actor_avatar_url").orEmpty(),
                )
                return
            }
        }

        NovaPushOpenSignal.offer(intent)
    }

    private fun syncMessageUnreadCount() {
        activityScope.launch {
            when (val result = NovaMessagingRepository(applicationContext).conversations()) {
                is ApiResult.Success -> NovaMessagesSignal.updateUnreadCount(result.value.unreadCount)
                is ApiResult.Failure -> Unit
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST,
            )
        }
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 4201
    }
}
