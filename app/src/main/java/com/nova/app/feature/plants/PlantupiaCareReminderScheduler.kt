package com.nova.app.feature.plants

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.nova.app.MainActivity
import com.nova.app.R
import com.nova.app.core.auth.NovaSessionStore
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val REMINDER_TAG = "plantupia-consented-checkin"
private const val REMINDER_CHANNEL = "plantupia_care"
private const val REMINDER_PREFS = "plantupia_local_checkins"
private const val EVENT_KEY = "event_id"
private const val USER_KEY = "user_id"

/**
 * Approximate WorkManager reminder for a user's *chosen review date*.
 * It never predicts watering and cannot promise exact delivery if Android
 * defers background work. Only event id/user id are persisted; no plant photos,
 * private room names, notes or diagnoses enter the notification.
 */
internal fun checkInDelayMillis(
    dateText: String,
    now: ZonedDateTime,
): Long? = runCatching {
    val date = LocalDate.parse(dateText)
    val target = date.atTime(9, 0).atZone(now.zone)
    java.time.Duration.between(now, target).toMillis().takeIf { it > 0L }
}.getOrNull()

object PlantupiaCareReminderScheduler {
    fun schedule(context: Context, eventId: String, checkInDate: String): Boolean {
        if (eventId.isBlank() || !hasNotificationPermission(context)) return false
        val userId = NovaSessionStore(context).load()?.cachedUser?.id?.takeIf { it > 0L }
            ?: return false
        val delay = checkInDelayMillis(checkInDate, ZonedDateTime.now()) ?: return false
        val app = context.applicationContext
        val input = Data.Builder()
            .putString(EVENT_KEY, eventId)
            .putLong(USER_KEY, userId)
            .build()
        val task = OneTimeWorkRequestBuilder<PlantupiaCareReminderWorker>()
            .addTag(REMINDER_TAG)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()
        return runCatching {
            app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
                .edit().putLong(eventId, userId).apply()
            WorkManager.getInstance(app).enqueueUniqueWork(
                REMINDER_TAG + ":" + eventId, ExistingWorkPolicy.REPLACE, task,
            )
            true
        }.getOrElse {
            app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
                .edit().remove(eventId).apply()
            false
        }
    }

    fun cancel(context: Context, eventId: String) {
        if (eventId.isBlank()) return
        val app = context.applicationContext
        app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
            .edit().remove(eventId).apply()
        WorkManager.getInstance(app).cancelUniqueWork(REMINDER_TAG + ":" + eventId)
    }

    fun isScheduled(context: Context, eventId: String): Boolean {
        val userId = NovaSessionStore(context).load()?.cachedUser?.id ?: return false
        return context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
            .getLong(eventId, -1L) == userId
    }

    fun clearForLogout(context: Context) {
        val app = context.applicationContext
        WorkManager.getInstance(app).cancelAllWorkByTag(REMINDER_TAG)
        app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    internal fun consumeIfAuthorized(context: Context, eventId: String, userId: Long): Boolean {
        val app = context.applicationContext
        val saved = app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
        if (saved.getLong(eventId, -1L) != userId) return false
        if (NovaSessionStore(app).load()?.cachedUser?.id != userId) return false
        // Once-only delivery, also prevents cross-account notification leakage.
        saved.edit().remove(eventId).commit()
        return hasNotificationPermission(app)
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}

class PlantupiaCareReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val eventId = inputData.getString(EVENT_KEY).orEmpty()
        val userId = inputData.getLong(USER_KEY, -1L)
        if (eventId.isBlank() || userId <= 0L) return Result.success()
        if (!PlantupiaCareReminderScheduler.consumeIfAuthorized(applicationContext, eventId, userId)) {
            return Result.success()
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
            ?: return Result.success()
        manager.createNotificationChannel(
            NotificationChannel(
                REMINDER_CHANNEL,
                "Plantupia · متابعة نباتاتك",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "مواعيد الفحص اللي اخترتها بنفسك، مش تنبؤات ري" }
        )
        val open = PendingIntent.getActivity(
            applicationContext,
            eventId.hashCode(),
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return runCatching {
            manager.notify(
                eventId.hashCode(),
                NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL)
                    .setSmallIcon(R.drawable.ic_nova_notification)
                    .setContentTitle("Plantupia · موعد مراجعة")
                    .setContentText("ده موعد متابعة اخترته بنفسك. افتح نباتك وشوف إيه اللي اتغيّر.")
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build(),
            )
            Result.success()
        }.getOrElse { Result.success() }
    }
}
