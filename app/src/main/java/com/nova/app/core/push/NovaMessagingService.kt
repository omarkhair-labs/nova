package com.nova.app.core.push

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nova.app.MainActivity
import com.nova.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class NovaMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        NovaPushStore(applicationContext).saveInstallationId(installationId)

        serviceScope.launch {
            NovaPushRepository(applicationContext).register(installationId)
        }
    }

    override fun onUnregistered(installationId: String) {
        super.onUnregistered(installationId)

        serviceScope.launch {
            NovaPushRepository(applicationContext).remove(installationId)
        }
        NovaPushStore(applicationContext).clear()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        showForegroundNotification(message)
    }

    private fun showForegroundNotification(message: RemoteMessage) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NovaPushRegistration.ensureChannel(this)

        val title = message.notification?.title
            ?: titleForKind(message.data["kind"].orEmpty())
        val body = message.notification?.body
            ?: bodyForKind(
                kind = message.data["kind"].orEmpty(),
                actorUsername = message.data["actor_username"].orEmpty(),
            )

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            message.data.forEach { (key, value) -> putExtra(key, value) }
        }
        val requestCode = message.data["notification_id"]?.toIntOrNull()
            ?: message.messageId?.hashCode()
            ?: System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NovaPushRegistration.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nova_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(requestCode, notification)
    }

    private fun titleForKind(kind: String): String {
        return when (kind) {
            "follow" -> "New follower"
            "like" -> "New like"
            "comment" -> "New comment"
            else -> "Nova activity"
        }
    }

    private fun bodyForKind(kind: String, actorUsername: String): String {
        val actor = actorUsername.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Someone"
        return when (kind) {
            "follow" -> "$actor started following you"
            "like" -> "$actor liked your post"
            "comment" -> "$actor commented on your post"
            else -> "$actor interacted with you"
        }
    }
}
