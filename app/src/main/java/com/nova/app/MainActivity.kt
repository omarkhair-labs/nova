package com.nova.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.push.NovaPushOpenSignal
import com.nova.app.core.push.NovaPushRegistration
import com.nova.app.core.reels.NovaReelsNavigator
import com.nova.app.core.update.NovaInAppUpdateController
import com.nova.app.ui.components.NovaActiveCallPill
import com.nova.app.ui.components.NovaUpdateReadyBanner
import com.nova.app.ui.theme.NovaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updateReadyToInstall = mutableStateOf(false)
    private lateinit var inAppUpdateController: NovaInAppUpdateController

    private val inAppUpdateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (::inAppUpdateController.isInitialized) {
            inAppUpdateController.onActivityResult(result.resultCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        inAppUpdateController = NovaInAppUpdateController(
            activity = this,
            launcher = inAppUpdateLauncher,
            onReadyToInstall = { ready -> updateReadyToInstall.value = ready },
        )
        inAppUpdateController.start()

        routePushIntent(intent)
        NovaPushRegistration.ensureChannel(this)
        NovaPushRegistration.syncForCurrentSession(this)
        requestNotificationPermissionIfNeeded()
        syncMessageUnreadCount()

        setContent {
            NovaTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    NovaApp()
                    NovaActiveCallPill(
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                    if (updateReadyToInstall.value) {
                        NovaUpdateReadyBanner(
                            onRestart = inAppUpdateController::completeUpdate,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncMessageUnreadCount()
        if (::inAppUpdateController.isInitialized) {
            inAppUpdateController.onResume()
        }
    }

    override fun onDestroy() {
        if (::inAppUpdateController.isInitialized) {
            inAppUpdateController.onDestroy()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routePushIntent(intent)
    }

    private fun routePushIntent(intent: Intent?) {
        val kind = intent?.getStringExtra("kind").orEmpty()

        if (kind == "message") {
            val conversationId = intent?.getStringExtra("conversation_id")?.toLongOrNull()
            val conversationKind = intent?.getStringExtra("conversation_kind").orEmpty().ifBlank { "direct" }
            if (conversationId != null && conversationId > 0L) {
                if (conversationKind == "group") {
                    NovaMessagingNavigator.openConversation(
                        context = this,
                        conversationId = conversationId,
                        username = "group",
                        displayName = intent?.getStringExtra("group_title").orEmpty().ifBlank { "Nova group" },
                        avatarUrl = "",
                        kind = "group",
                        membersCount = 0,
                    )
                    return
                }

                val username = intent?.getStringExtra("actor_username").orEmpty()
                if (username.isNotBlank()) {
                    NovaMessagingNavigator.openConversation(
                        context = this,
                        conversationId = conversationId,
                        username = username,
                        displayName = intent?.getStringExtra("actor_name").orEmpty(),
                        avatarUrl = intent?.getStringExtra("actor_avatar_url").orEmpty(),
                    )
                    return
                }
            }
        }

        if (kind in REEL_ACTIVITY_KINDS) {
            val reelId = intent?.getStringExtra("reel_id")?.toLongOrNull()
            val reelAuthorUsername = intent?.getStringExtra("reel_author_username")
                .orEmpty()
                .trim()
                .lowercase()
            if (reelId != null && reelId > 0L && reelAuthorUsername.isNotBlank()) {
                NovaReelsNavigator.openProfile(
                    context = this,
                    username = reelAuthorUsername,
                    initialReelId = reelId,
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
        val REEL_ACTIVITY_KINDS = setOf("reel_like", "reel_comment", "reel_repost", "reel_reply")
    }
}
