package com.nova.app.core.rooms

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nova.app.MainActivity
import com.nova.app.R
import com.nova.app.feature.rooms.domain.model.RoomItem
import java.time.OffsetDateTime


object NovaRoomReminderScheduler {
    private const val ACTION = "com.nova.app.rooms.REMINDER"
    private const val CHANNEL_ID = "nova_room_reminders"
    private const val LEAD_MILLIS = 15 * 60 * 1000L

    fun update(context: Context, item: RoomItem) {
        val pendingIntent = pendingIntent(context, item)
        val alarms = context.getSystemService(AlarmManager::class.java)
        if (!item.reminderSet) {
            alarms.cancel(pendingIntent)
            pendingIntent.cancel()
            return
        }
        val eventAt = runCatching { OffsetDateTime.parse(item.scheduledFor).toInstant().toEpochMilli() }.getOrNull()
            ?: return
        val triggerAt = (eventAt - LEAD_MILLIS).coerceAtLeast(System.currentTimeMillis() + 1_000L)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun pendingIntent(context: Context, item: RoomItem): PendingIntent = PendingIntent.getBroadcast(
        context,
        item.id.hashCode(),
        Intent(context, NovaRoomReminderReceiver::class.java).apply {
            action = ACTION
            putExtra("item_id", item.id)
            putExtra("title", item.title.ifBlank { "Room plan" })
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun show(context: Context, itemId: Long, title: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Room reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = PendingIntent.getActivity(
            context,
            itemId.hashCode(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            itemId.hashCode(),
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nova_notification)
                .setContentTitle(title)
                .setContentText("Starts in about 15 minutes in your Nova Room.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }
}


class NovaRoomReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NovaRoomReminderScheduler.show(
            context,
            intent.getLongExtra("item_id", 0L),
            intent.getStringExtra("title").orEmpty().ifBlank { "Room plan" },
        )
    }
}
