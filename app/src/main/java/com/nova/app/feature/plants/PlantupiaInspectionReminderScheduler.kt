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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nova.app.MainActivity
import com.nova.app.R
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "plantupia-approved-inspection-check"
private const val PREFS = "plantupia_opt_in_approved_checks"
private const val CHANNEL = "plantupia_reviewed_care_v2"

/** Device delivery is inexact; times are grounded in a real reviewed plan. */
internal fun approvedInspectionDelay(date: String, zone: String, now: ZonedDateTime): Long? =
    runCatching {
        Duration.between(
            now.toInstant(), LocalDate.parse(date).atTime(9, 0).atZone(ZoneId.of(zone)).toInstant(),
        ).toMillis().takeIf { it > 0L }
    }.getOrNull()

internal fun correctInspectionOccurrence(
    sourceId: String, targetId: String, enabled: Boolean, valid: Boolean,
    serverRevision: Int, expectedRevision: Int,
    serverDue: String, expectedDue: String,
    serverPlant: String, expectedPlant: String,
): Boolean = sourceId == targetId && enabled && valid &&
    serverRevision == expectedRevision && serverDue == expectedDue && serverPlant == expectedPlant

/**
 * Explicit consent stored per session and plan. Server remains authoritative;
 * the Worker verifies the same approved occurrence immediately before delivery.
 */
internal object PlantupiaInspectionReminderScheduler {
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun prefix(user: Long, plan: String) = "$user:$plan:"
    private fun work(user: Long, plan: String) = "$TAG:$user:$plan"
    private fun permission(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    fun isOptedIn(context: Context, plan: String): Boolean {
        val session = NovaSessionStore(context).load() ?: return false
        val user = session.cachedUser?.id ?: return false
        val p = prefix(user, plan)
        return prefs(context).getBoolean(p + "consent", false) &&
            prefs(context).getString(p + "generation", "") == session.identity.generation
    }

    fun optIn(context: Context, plan: String, plant: String, date: String, zone: String, revision: Int): Boolean {
        val app = context.applicationContext
        val session = NovaSessionStore(app).load() ?: return false
        val user = session.cachedUser?.id?.takeIf { it > 0L } ?: return false
        if (!permission(app) || revision < 0 ||
            runCatching { UUID.fromString(plan); UUID.fromString(plant) }.isFailure) return false
        if (approvedInspectionDelay(date, zone, ZonedDateTime.now()) == null) return false
        val p = prefix(user, plan)
        if (!prefs(app).edit().putBoolean(p + "consent", true)
            .putString(p + "generation", session.identity.generation)
            .putString(p + "plant", plant).commit()) return false
        val ok = schedule(app, user, plan, plant, date, zone, revision, session.identity.generation)
        if (!ok) cancel(app, plan)
        return ok
    }

    private fun schedule(app: Context, user: Long, plan: String, plant: String,
                         date: String, zone: String, revision: Int, generation: String): Boolean {
        val delay = approvedInspectionDelay(date, zone, ZonedDateTime.now()) ?: return false
        val p = prefix(user, plan)
        val prefs = prefs(app)
        if (!prefs.getBoolean(p + "consent", false) ||
            prefs.getString(p + "generation", "") != generation ||
            prefs.getInt(p + "delivered_revision", -1) == revision) return false
        if (prefs.getInt(p + "scheduled_revision", -1) == revision &&
            prefs.getString(p + "scheduled_date", "") == date) return true
        val data = Data.Builder().putString("plan", plan).putString("plant", plant)
            .putString("date", date).putString("zone", zone)
            .putInt("revision", revision).putLong("user", user)
            .putString("generation", generation).build()
        val request = OneTimeWorkRequestBuilder<PlantupiaApprovedInspectionWorker>()
            .addTag(TAG).setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data).build()
        return runCatching {
            WorkManager.getInstance(app).enqueueUniqueWork(work(user, plan), ExistingWorkPolicy.REPLACE, request)
            prefs.edit().putInt(p + "scheduled_revision", revision)
                .putString(p + "scheduled_date", date).commit()
        }.getOrDefault(false)
    }

    /** Only called from a fresh, authorized server plan response. No implicit consent. */
    fun reconcile(context: Context, plan: String, plant: String, date: String, zone: String,
                  revision: Int, enabled: Boolean, valid: Boolean) {
        if (!isOptedIn(context, plan)) return
        if (!enabled || !valid) { cancel(context, plan); return }
        val app = context.applicationContext
        val session = NovaSessionStore(app).load() ?: return
        val user = session.cachedUser?.id ?: return
        schedule(app, user, plan, plant, date, zone, revision, session.identity.generation)
    }

    fun cancel(context: Context, plan: String) {
        val app = context.applicationContext
        val session = NovaSessionStore(app).load() ?: return
        val user = session.cachedUser?.id ?: return
        val p = prefix(user, plan)
        prefs(app).edit().remove(p + "consent").remove(p + "generation").remove(p + "plant")
            .remove(p + "scheduled_revision").remove(p + "scheduled_date")
            .remove(p + "delivered_revision").commit()
        runCatching { WorkManager.getInstance(app).cancelUniqueWork(work(user, plan)) }
    }

    fun cancelForPlant(context: Context, plant: String) {
        val app = context.applicationContext
        val user = NovaSessionStore(app).load()?.cachedUser?.id ?: return
        val base = "$user:"
        prefs(app).all.filter { (key, value) ->
            key.startsWith(base) && key.endsWith(":plant") && value == plant
        }.keys.map { it.removePrefix(base).removeSuffix(":plant") }.forEach { cancel(app, it) }
    }

    fun clearForLogout(context: Context) {
        val app = context.applicationContext
        prefs(app).edit().clear().commit()
        runCatching { WorkManager.getInstance(app).cancelAllWorkByTag(TAG) }
    }

    internal fun authorized(context: Context, user: Long, plan: String,
                            generation: String, revision: Int, date: String): Boolean {
        val session = NovaSessionStore(context).load() ?: return false
        if (!permission(context) || session.cachedUser?.id != user ||
            session.identity.generation != generation) return false
        val p = prefix(user, plan)
        val prefs = prefs(context)
        return prefs.getBoolean(p + "consent", false) &&
            prefs.getString(p + "generation", "") == generation &&
            prefs.getInt(p + "scheduled_revision", -1) == revision &&
            prefs.getString(p + "scheduled_date", "") == date &&
            prefs.getInt(p + "delivered_revision", -1) != revision
    }

    internal fun delivered(context: Context, user: Long, plan: String, revision: Int) {
        prefs(context).edit().putInt(prefix(user, plan) + "delivered_revision", revision).commit()
    }
}

class PlantupiaApprovedInspectionWorker(app: Context, params: WorkerParameters) : CoroutineWorker(app, params) {
    override suspend fun doWork(): Result {
        val plan = inputData.getString("plan").orEmpty()
        val plant = inputData.getString("plant").orEmpty()
        val date = inputData.getString("date").orEmpty()
        val zone = inputData.getString("zone").orEmpty()
        val generation = inputData.getString("generation").orEmpty()
        val revision = inputData.getInt("revision", -1)
        val user = inputData.getLong("user", -1L)
        if (user <= 0 || revision < 0 || generation.isBlank() ||
            runCatching { UUID.fromString(plan); UUID.fromString(plant) }.isFailure
        ) return Result.success()
        val app = applicationContext
        if (!PlantupiaInspectionReminderScheduler.authorized(app, user, plan, generation, revision, date)) return Result.success()
        val today = runCatching { LocalDate.now(ZoneId.of(zone)) }.getOrNull() ?: return Result.success()
        val due = runCatching { LocalDate.parse(date) }.getOrNull() ?: return Result.success()
        if (today.isBefore(due)) return Result.retry()
        if (today.isAfter(due.plusDays(1))) return Result.success()
        val repository = PlantupiaRepository(app)
        val result = repository.demoGet("care-plans/?plant_id=$plant")
        val record = when (result) {
            is ApiResult.Failure -> {
                if (result.statusCode in listOf(401, 403, 404, 410)) return Result.success()
                return if (runAttemptCount < 3) Result.retry() else Result.success()
            }
            is ApiResult.Success -> runCatching {
                result.value.demoRows().firstOrNull { it.optString("id") == plan }
            }.getOrNull() ?: return Result.success()
        }
        if (!correctInspectionOccurrence(
                record.optString("id"), plan, record.optBoolean("enabled"),
                record.optBoolean("valid"), record.optInt("revision", -1), revision,
                record.optString("next_due_on"), date, record.optString("plant_id"), plant,
            )) return Result.success()
        if (!repository.isCurrentSession() ||
            !PlantupiaInspectionReminderScheduler.authorized(app, user, plan, generation, revision, date)
        ) return Result.success()
        val manager = app.getSystemService(NotificationManager::class.java) ?: return Result.success()
        return runCatching {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Plantupia · Care checks",
                NotificationManager.IMPORTANCE_DEFAULT))
            val intent = Intent(app, MainActivity::class.java)
                .putExtra("kind", "plantupia_inspection_due")
                .putExtra("recipient_id", user.toString())
                .putExtra("plant_id", plant).putExtra("plan_id", plan)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pending = PendingIntent.getActivity(app, plan.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.notify(plan.hashCode(), NotificationCompat.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_nova_notification)
                .setContentTitle("Plantupia · Care check")
                .setContentText("A check you chose is due. Open your plant to record what you notice.")
                .setContentIntent(pending).setAutoCancel(true).build())
            PlantupiaInspectionReminderScheduler.delivered(app, user, plan, revision)
            Result.success()
        }.getOrDefault(Result.success())
    }
}
