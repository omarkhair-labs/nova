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
import com.nova.app.navigation.DeepLinkRouter
import com.nova.app.navigation.NovaDeepLinkDecision
import com.nova.app.navigation.NovaPrimaryNavigationDispatcher
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
                    NovaPrimaryHost()
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
        NovaPrimaryNavigationDispatcher.setHostActive(true)
        syncMessageUnreadCount()
        if (::inAppUpdateController.isInitialized) {
            inAppUpdateController.onResume()
        }
    }

    override fun onPause() {
        NovaPrimaryNavigationDispatcher.setHostActive(false)
        super.onPause()
    }

    override fun onDestroy() {
        NovaPrimaryNavigationDispatcher.setHostActive(false)
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
        when (val decision = DeepLinkRouter.decide(intent)) {
            is NovaDeepLinkDecision.Conversation -> {
                NovaMessagingNavigator.openConversation(
                    context = this,
                    conversationId = decision.conversationId,
                    username = decision.username,
                    displayName = decision.displayName,
                    avatarUrl = decision.avatarUrl,
                    kind = decision.kind,
                    membersCount = decision.membersCount,
                )
            }

            is NovaDeepLinkDecision.ProfileReel -> {
                NovaReelsNavigator.openProfile(
                    context = this,
                    username = decision.username,
                    initialReelId = decision.initialReelId,
                )
            }

            NovaDeepLinkDecision.InAppSignal -> NovaPushOpenSignal.offer(intent)
        }
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
