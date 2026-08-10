package com.nova.app.core.calls

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.nova.app.CallActivity
import com.nova.app.R


object NovaCallNotification {
    const val CHANNEL_ID = "nova_calls_incoming_v2"
    const val CHANNEL_NAME = "Nova incoming calls"
    const val ONGOING_CHANNEL_ID = "nova_calls_ongoing_v1"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val incoming = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Incoming Nova voice and video calls"
            setSound(
                ringtone,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val ongoing = NotificationChannel(
            ONGOING_CHANNEL_ID,
            "Ongoing Nova calls",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing Nova voice and video calls"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannels(listOf(incoming, ongoing))
    }

    fun showIncoming(
        context: Context,
        callId: String,
        callKind: NovaCallKind,
        conversationId: Long,
        callerUsername: String,
        callerName: String,
        callerAvatarUrl: String,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(context)
        val displayName = callerName.ifBlank { callerUsername.ifBlank { "Nova caller" } }
        val person = Person.Builder().setName(displayName).setImportant(true).build()

        val openIntent = freshIncomingIntent(
            CallActivity.incomingIntent(
                context = context,
                callId = callId,
                conversationId = conversationId,
                callKind = callKind,
                callerUsername = callerUsername,
                callerName = displayName,
                callerAvatarUrl = callerAvatarUrl,
                action = CallActivity.ACTION_OPEN_CALL,
            ),
            callId = callId,
            actionTag = "open",
        )
        val answerIntent = freshIncomingIntent(
            CallActivity.incomingIntent(
                context = context,
                callId = callId,
                conversationId = conversationId,
                callKind = callKind,
                callerUsername = callerUsername,
                callerName = displayName,
                callerAvatarUrl = callerAvatarUrl,
                action = CallActivity.ACTION_ANSWER_CALL,
            ),
            callId = callId,
            actionTag = "answer",
        )
        val declineIntent = freshIncomingIntent(
            CallActivity.incomingIntent(
                context = context,
                callId = callId,
                conversationId = conversationId,
                callKind = callKind,
                callerUsername = callerUsername,
                callerName = displayName,
                callerAvatarUrl = callerAvatarUrl,
                action = CallActivity.ACTION_DECLINE_CALL,
            ),
            callId = callId,
            actionTag = "decline",
        )

        val openPending = pendingActivity(context, openIntent, requestCode(callId, 1))
        val answerPending = pendingActivity(context, answerIntent, requestCode(callId, 2))
        val declinePending = pendingActivity(context, declineIntent, requestCode(callId, 3))

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nova_notification)
            .setContentTitle(displayName)
            .setContentText(
                if (callKind == NovaCallKind.Video) "Incoming Nova video call" else "Incoming Nova voice call"
            )
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openPending)
            .setFullScreenIntent(openPending, true)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, declinePending, answerPending))
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(callId), notification)
    }

    fun showOngoing(context: Context, call: NovaCallSession) {
        val activeSummary = NovaActiveCallSignal.publish(call)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(context)
        val person = Person.Builder().setName(call.peer.displayName).setImportant(true).build()
        val openIntent = CallActivity.existingCallIntent(context, call.id, CallActivity.ACTION_OPEN_CALL)
        val hangupIntent = CallActivity.existingCallIntent(context, call.id, CallActivity.ACTION_END_CALL)
        val openPending = pendingActivity(context, openIntent, requestCode(call.id, 4))
        val hangupPending = pendingActivity(context, hangupIntent, requestCode(call.id, 5))

        val builder = NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nova_notification)
            .setContentTitle(call.peer.displayName)
            .setContentText(
                when (call.status) {
                    NovaCallStatus.Ringing -> if (call.isCaller) "Calling…" else "Incoming call"
                    NovaCallStatus.Active -> if (call.kind == NovaCallKind.Video) "Nova video call" else "Nova voice call"
                    else -> "Nova call"
                }
            )
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openPending)
            .setFullScreenIntent(openPending, false)
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangupPending))

        if (call.status == NovaCallStatus.Active) {
            activeSummary?.answeredAtEpochMs?.let { startedAt ->
                builder
                    .setWhen(startedAt)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
            }
        }

        NotificationManagerCompat.from(context).notify(notificationId(call.id), builder.build())
    }

    fun cancel(context: Context, callId: String) {
        NovaActiveCallSignal.clear(callId)
        NotificationManagerCompat.from(context).cancel(notificationId(callId))
    }

    private fun freshIncomingIntent(intent: Intent, callId: String, actionTag: String): Intent {
        return intent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("nova://call/$callId/$actionTag")
        }
    }

    private fun pendingActivity(context: Context, intent: Intent, requestCode: Int): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(callId: String): Int = (callId.hashCode() and 0x7fffffff) or 0x10000000
    private fun requestCode(callId: String, salt: Int): Int = callId.hashCode() xor (salt shl 20)
}
