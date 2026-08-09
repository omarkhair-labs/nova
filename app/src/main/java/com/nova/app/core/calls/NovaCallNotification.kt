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
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.nova.app.CallActivity
import com.nova.app.R


object NovaCallNotification {
    const val CHANNEL_ID = "nova_calls"
    const val CHANNEL_NAME = "Nova calls"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Incoming and ongoing Nova calls"
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
        manager.createNotificationChannel(channel)
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

        val openIntent = CallActivity.incomingIntent(
            context = context,
            callId = callId,
            conversationId = conversationId,
            callKind = callKind,
            callerUsername = callerUsername,
            callerName = displayName,
            callerAvatarUrl = callerAvatarUrl,
            action = CallActivity.ACTION_OPEN_CALL,
        )
        val answerIntent = CallActivity.incomingIntent(
            context = context,
            callId = callId,
            conversationId = conversationId,
            callKind = callKind,
            callerUsername = callerUsername,
            callerName = displayName,
            callerAvatarUrl = callerAvatarUrl,
            action = CallActivity.ACTION_ANSWER_CALL,
        )
        val declineIntent = CallActivity.incomingIntent(
            context = context,
            callId = callId,
            conversationId = conversationId,
            callKind = callKind,
            callerUsername = callerUsername,
            callerName = displayName,
            callerAvatarUrl = callerAvatarUrl,
            action = CallActivity.ACTION_DECLINE_CALL,
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
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannel(context)
        val person = Person.Builder().setName(call.peer.displayName).setImportant(true).build()
        val openIntent = CallActivity.existingCallIntent(context, call.id, CallActivity.ACTION_OPEN_CALL)
        val hangupIntent = CallActivity.existingCallIntent(context, call.id, CallActivity.ACTION_END_CALL)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pendingActivity(context, openIntent, requestCode(call.id, 4)))
            .setStyle(
                NotificationCompat.CallStyle.forOngoingCall(
                    person,
                    pendingActivity(context, hangupIntent, requestCode(call.id, 5)),
                )
            )
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(call.id), notification)
    }

    fun cancel(context: Context, callId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(callId))
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
