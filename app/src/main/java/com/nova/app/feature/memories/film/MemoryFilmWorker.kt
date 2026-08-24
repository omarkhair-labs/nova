package com.nova.app.feature.memories.film

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nova.app.MainActivity
import com.nova.app.R
import com.nova.app.app.appContainer
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.memories.domain.model.MemoryFilmPlan
import java.util.UUID


class MemoryFilmWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val weeksAgo = inputData.getInt(KEY_WEEKS_AGO, 0)
        val offset = inputData.getInt(KEY_UTC_OFFSET, 0)
        setForeground(foregroundInfo(0))
        val plan = when (val result = applicationContext.appContainer.memoryRepository.filmPlan(offset, weeksAgo)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return Result.failure(workDataOf(KEY_ERROR to result.message))
        }
        if (!plan.filmReady) {
            return Result.failure(workDataOf(KEY_ERROR to "This week does not have enough media for a film."))
        }
        val exported = applicationContext.appContainer.memoryFilmExporter.export(plan) { progress ->
            setProgressAsync(workDataOf(KEY_PROGRESS to progress.coerceIn(0, 99)))
            setForegroundAsync(foregroundInfo(progress.coerceIn(0, 99)))
        }
        return exported.fold(
            onSuccess = { output ->
                Result.success(
                    workDataOf(
                        KEY_PROGRESS to 100,
                        KEY_OUTPUT_PATH to output.filePath,
                    )
                )
            },
            onFailure = { error ->
                Result.failure(workDataOf(KEY_ERROR to (error.message ?: "Nova couldn't render this film.")))
            },
        )
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(0)

    private fun foregroundInfo(progress: Int): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Memory film rendering", NotificationManager.IMPORTANCE_LOW)
        )
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nova_notification)
            .setContentTitle("Rendering your Nova Memory")
            .setContentText(if (progress > 0) "$progress% complete" else "Preparing your moments…")
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    companion object {
        const val KEY_WEEKS_AGO = "weeks_ago"
        const val KEY_UTC_OFFSET = "utc_offset"
        const val KEY_PROGRESS = "progress"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_ERROR = "error"
        private const val CHANNEL_ID = "nova_memory_render"
        private const val NOTIFICATION_ID = 4207

        fun uniqueName(plan: MemoryFilmPlan): String = "memory-film-${plan.selectionVersion}"

        fun enqueue(context: Context, plan: MemoryFilmPlan): UUID {
            val request = OneTimeWorkRequestBuilder<MemoryFilmWorker>()
                .setInputData(
                    Data.Builder()
                        .putInt(KEY_WEEKS_AGO, plan.weeksAgo)
                        .putInt(KEY_UTC_OFFSET, plan.utcOffsetMinutes)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueName(plan),
                ExistingWorkPolicy.KEEP,
                request,
            )
            return request.id
        }
    }
}
