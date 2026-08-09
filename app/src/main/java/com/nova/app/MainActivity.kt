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

        NovaPushOpenSignal.offer(intent)
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
