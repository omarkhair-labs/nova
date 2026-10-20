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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val REMINDER_TAG = "plantupia-consented-checkin"
private const val REMINDER_CHANNEL = "plantupia_care"
private const val REMINDER_PREFS = "plantupia_local_checkins"
private const val EVENT_KEY = "event_id"
private const val PLANT_PREFIX = "plant:"
private const val USER_KEY = "user_id"
private const val SESSION_KEY = "login_generation"

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

internal fun reminderIdsForPlant(prefs: Map<String, *>, plantId: String): List<String> {
    if (plantId.isBlank()) return emptyList()
    return prefs.entries.mapNotNull { (key, value) ->
        if (key.startsWith(PLANT_PREFIX) && value == plantId) key.removePrefix(PLANT_PREFIX)
        else null
    }
}

object PlantupiaCareReminderScheduler {
    private val submissionReminderLock = Any()

    /** Creation replay cannot re-enable or duplicate an already scheduled/cancelled reminder. */
    suspend fun scheduleOnce(context: Context, eventId: String, checkInDate: String, plantId: String): Boolean {
        if (eventId.isBlank() || plantId.isBlank()) return false
        val app = context.applicationContext
        val session = NovaSessionStore(app).load() ?: return false
        val userId = session.cachedUser?.id?.takeIf { it > 0 } ?: return false
        return withContext(Dispatchers.IO) {
            synchronized(submissionReminderLock) {
                if (NovaSessionStore(app).current(session.identity) == null) return@synchronized false
                val receipts = app.getSharedPreferences("plantupia_submission_reminders", Context.MODE_PRIVATE)
                val key = "$userId:$eventId"
                if (receipts.getBoolean(key, false)) return@synchronized true
                runCatching {
                    val manager = WorkManager.getInstance(app)
                    val name = REMINDER_TAG + ":" + eventId
                    if (manager.getWorkInfosForUniqueWork(name).get().isNotEmpty()) {
                        check(receipts.edit().putBoolean(key, true).commit())
                        return@runCatching true
                    }
                    if (!hasNotificationPermission(app)) return@runCatching false
                    val delay = checkInDelayMillis(checkInDate, ZonedDateTime.now()) ?: return@runCatching false
                    val task = OneTimeWorkRequestBuilder<PlantupiaCareReminderWorker>()
                        .addTag(REMINDER_TAG).setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(Data.Builder().putString(EVENT_KEY, eventId).putLong(USER_KEY, userId)
                            .putString(SESSION_KEY, session.identity.generation).build()).build()
                    // Recheck the login after waiting for WorkManager's query.
                    if (NovaSessionStore(app).current(session.identity) == null) return@runCatching false
                    app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE).edit()
                        .putLong(eventId, userId).putString(PLANT_PREFIX + eventId, plantId).commit()
                    manager.enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, task).result.get()
                    check(receipts.edit().putBoolean(key, true).commit())
                    true
                }.getOrDefault(false)
            }
        }
    }

    fun schedule(context: Context, eventId: String, checkInDate: String, plantId: String): Boolean {
        if (eventId.isBlank() || plantId.isBlank() || !hasNotificationPermission(context)) return false
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
                .edit().putLong(eventId, userId).putString(PLANT_PREFIX + eventId, plantId).apply()
            WorkManager.getInstance(app).enqueueUniqueWork(
                REMINDER_TAG + ":" + eventId, ExistingWorkPolicy.REPLACE, task,
            )
            true
        }.getOrElse {
            app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
                .edit().remove(eventId).remove(PLANT_PREFIX + eventId).apply()
            false
        }
    }

    fun cancel(context: Context, eventId: String) {
        if (eventId.isBlank()) return
        val app = context.applicationContext
        app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
            .edit().remove(eventId).remove(PLANT_PREFIX + eventId).apply()
        // Preference removal alone prevents the worker from displaying a
        // notification, even if WorkManager is unavailable at this moment.
        runCatching {
            WorkManager.getInstance(app).cancelUniqueWork(REMINDER_TAG + ":" + eventId)
        }
    }

    /** Cancel only the archived plant's consented reminders, never other spaces'. */
    fun cancelForPlant(context: Context, plantId: String) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
        reminderIdsForPlant(prefs.all, plantId).forEach { eventId -> cancel(app, eventId) }
    }

    fun isScheduled(context: Context, eventId: String): Boolean {
        val userId = NovaSessionStore(context).load()?.cachedUser?.id ?: return false
        return context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
            .getLong(eventId, -1L) == userId
    }

    fun clearForLogout(context: Context) {
        val app = context.applicationContext
        // Clear the account authorization gate synchronously before stopping
        // workers. A pending callback may never surface private app context.
        app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        runCatching { WorkManager.getInstance(app).cancelAllWorkByTag(REMINDER_TAG) }
    }

    internal fun consumeIfAuthorized(context: Context, eventId: String, userId: Long, generation: String? = null): Boolean {
        val app = context.applicationContext
        val saved = app.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
        if (saved.getLong(eventId, -1L) != userId) return false
        val session = NovaSessionStore(app).load() ?: return false
        if (session.cachedUser?.id != userId || generation != null && session.identity.generation != generation) return false
        // Once-only delivery, also prevents cross-account notification leakage.
        saved.edit().remove(eventId).remove(PLANT_PREFIX + eventId).commit()
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
        if (!PlantupiaCareReminderScheduler.consumeIfAuthorized(applicationContext, eventId, userId, inputData.getString(SESSION_KEY))) {
            return Result.success()
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
            ?: return Result.success()
        val open = PendingIntent.getActivity(
            applicationContext,
            eventId.hashCode(),
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return runCatching {
            manager.createNotificationChannel(
                NotificationChannel(
                    REMINDER_CHANNEL,
                    "Plantupia · Plant check-ins",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Check-in dates you chose, not predicted watering times" }
            )
            manager.notify(
                eventId.hashCode(),
                NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL)
                    .setSmallIcon(R.drawable.ic_nova_notification)
                    .setContentTitle("Plantupia · Check-in reminder")
                    .setContentText("It's time for a check-in you chose. Open your plant and see what has changed.")
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build(),
            )
            Result.success()
        }.getOrElse { Result.success() }
    }
}
