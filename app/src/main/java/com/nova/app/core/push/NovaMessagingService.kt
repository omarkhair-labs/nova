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
import com.nova.app.core.calls.NovaCallKind
import com.nova.app.core.calls.NovaCallNotification
import com.nova.app.core.messaging.NovaActiveConversation
import com.nova.app.core.messaging.NovaMessagesSignal
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
        val kind = message.data["kind"].orEmpty()

        if (kind == "incoming_call") {
            handleIncomingCall(message)
            return
        }

        if (kind == "call_state") {
            val callId = message.data["call_id"].orEmpty()
            val status = message.data["call_status"].orEmpty()
            if (callId.isNotBlank() && status in TERMINAL_CALL_STATUSES) {
                NovaCallNotification.cancel(this, callId)
            }
            return
        }

        val conversationId = message.data["conversation_id"]?.toLongOrNull()
        if (kind == "message" && !NovaActiveConversation.isActive(conversationId)) {
            NovaMessagesSignal.incrementUnreadCount()
            NovaMessagesSignal.requestInboxRefresh()
        }

        showForegroundNotification(message)
    }

    private fun handleIncomingCall(message: RemoteMessage) {
        val callId = message.data["call_id"].orEmpty()
        if (callId.isBlank()) return
        NovaCallNotification.showIncoming(
            context = this,
            callId = callId,
            callKind = NovaCallKind.fromWire(message.data["call_kind"].orEmpty()),
            conversationId = message.data["conversation_id"]?.toLongOrNull() ?: -1L,
            callerUsername = message.data["caller_username"].orEmpty(),
            callerName = message.data["caller_name"].orEmpty(),
            callerAvatarUrl = message.data["caller_avatar_url"].orEmpty(),
        )
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

        val kind = message.data["kind"].orEmpty()
        val conversationId = message.data["conversation_id"]?.toLongOrNull()
        if (kind == "message" && NovaActiveConversation.isActive(conversationId)) {
            return
        }

        NovaPushRegistration.ensureChannel(this)

        val actorUsername = message.data["actor_username"].orEmpty()
        val actorName = message.data["actor_name"].orEmpty()
        val preview = message.data["message_preview"].orEmpty()
        val title = message.notification?.title
            ?: titleForKind(kind, actorUsername, actorName)
        val body = message.notification?.body
            ?: bodyForKind(
                kind = kind,
                actorUsername = actorUsername,
                messagePreview = preview,
            )

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            message.data.forEach { (key, value) -> putExtra(key, value) }
        }
        val requestCode = message.data["message_id"]?.toIntOrNull()
            ?: message.data["notification_id"]?.toIntOrNull()
            ?: message.messageId?.hashCode()
            ?: System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = if (kind == "message") {
            NovaPushRegistration.MESSAGE_CHANNEL_ID
        } else {
            NovaPushRegistration.CHANNEL_ID
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_nova_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (kind == "message") {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .build()

        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(requestCode, notification)
    }

    private fun titleForKind(kind: String, actorUsername: String, actorName: String): String {
        val actor = actorName.takeIf { it.isNotBlank() }
            ?: actorUsername.takeIf { it.isNotBlank() }?.let { "@$it" }
            ?: "Someone"
        return when (kind) {
            "follow" -> "New follower"
            "like" -> "New like"
            "comment" -> "New comment"
            "reel_like" -> "New Reel like"
            "reel_comment" -> "New Reel comment"
            "reel_repost" -> "New Reel repost"
            "message" -> actor
            else -> "Nova activity"
        }
    }

    private fun bodyForKind(
        kind: String,
        actorUsername: String,
        messagePreview: String,
    ): String {
        val actor = actorUsername.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Someone"
        return when (kind) {
            "follow" -> "$actor started following you"
            "like" -> "$actor liked your post"
            "comment" -> "$actor commented on your post"
            "reel_like" -> "$actor liked your Reel"
            "reel_comment" -> "$actor commented on your Reel"
            "reel_repost" -> "$actor reposted your Reel"
            "message" -> messagePreview.ifBlank { "$actor sent you a message" }
            else -> "$actor interacted with you"
        }
    }

    private companion object {
        val TERMINAL_CALL_STATUSES = setOf(
            "declined",
            "canceled",
            "ended",
            "missed",
            "failed",
        )
    }
}
