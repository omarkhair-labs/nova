package com.nova.app.feature.publishing

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nova.app.core.network.ApiResult
import com.nova.app.app.appContainer
import java.util.UUID


enum class MediaPublishTarget(val wireValue: String) {
    POST("post"),
    REEL("reel"),
    STORY("story");

    companion object {
        fun fromWire(value: String): MediaPublishTarget? = entries.firstOrNull { it.wireValue == value }
    }
}


class MediaPublishWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val target = MediaPublishTarget.fromWire(inputData.getString(KEY_TARGET).orEmpty())
            ?: return failed("Nova lost the publishing destination. Please retry from Create.")
        val userId = inputData.getLong(KEY_USER_ID, 0L)
        if (!publishAccountMatches(userId, applicationContext.appContainer.currentCachedUserId())) {
            return failed("This publish belongs to a different signed-in account.")
        }
        val source = inputData.getString(KEY_SOURCE_URI).orEmpty()
        val caption = inputData.getString(KEY_CAPTION).orEmpty()
        val audience = inputData.getString(KEY_AUDIENCE).orEmpty().ifBlank { "followers" }
        val clientPublishId = inputData.getString(KEY_CLIENT_PUBLISH_ID).orEmpty()
        if (source.isBlank() || clientPublishId.isBlank()) {
            return failed("Nova lost access to the selected media. Pick it again and retry.")
        }

        setProgress(progressData(STAGE_PREPARING))
        val isVideo = applicationContext.contentResolver.getType(Uri.parse(source))
            ?.startsWith("video/") == true
        if (!isVideo) setProgress(progressData(STAGE_UPLOADING))

        val progress: (Int?) -> Unit = { value ->
            val stage = if (value == 100) STAGE_UPLOADING else STAGE_PREPARING
            setProgressAsync(progressData(stage, value.takeUnless { stage == STAGE_UPLOADING }))
        }

        val response: ApiResult<Long> = when (target) {
            MediaPublishTarget.POST -> when (
                val result = applicationContext.appContainer.postDataRepository.createPost(
                    caption = caption,
                    mediaUri = Uri.parse(source),
                    clientPublishId = clientPublishId,
                    onProgress = progress,
                    expectedUserId = userId,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(result.value.id)
                is ApiResult.Failure -> result
            }
            MediaPublishTarget.REEL -> when (
                val result = applicationContext.appContainer.reelsRepository.createReel(
                    videoUri = Uri.parse(source),
                    caption = caption,
                    clientPublishId = clientPublishId,
                    onProgress = progress,
                    expectedUserId = userId,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(result.value.id)
                is ApiResult.Failure -> result
            }
            MediaPublishTarget.STORY -> when (
                val result = applicationContext.appContainer.storiesRepository.createStory(
                    mediaUri = Uri.parse(source),
                    caption = caption,
                    audience = audience,
                    clientPublishId = clientPublishId,
                    onProgress = progress,
                    expectedUserId = userId,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(result.value.id)
                is ApiResult.Failure -> result
            }
        }

        return when (response) {
            is ApiResult.Success -> Result.success(
                workDataOf(
                    KEY_STAGE to STAGE_PUBLISHED,
                    KEY_RESULT_ID to response.value,
                    KEY_FINISHED_AT to System.currentTimeMillis(),
                    KEY_TARGET to target.wireValue,
                    KEY_ENQUEUED_AT to inputData.getLong(KEY_ENQUEUED_AT, 0L),
                ),
            )
            is ApiResult.Failure -> if (shouldRetryPublish(response.statusCode, runAttemptCount)) {
                Result.retry()
            } else {
                failed(response.message)
            }
        }
    }

    private fun failed(message: String): Result = Result.failure(
        workDataOf(
            KEY_STAGE to STAGE_FAILED,
            KEY_ERROR to message,
            KEY_FINISHED_AT to System.currentTimeMillis(),
            KEY_TARGET to inputData.getString(KEY_TARGET),
            KEY_USER_ID to inputData.getLong(KEY_USER_ID, 0L),
            KEY_SOURCE_URI to inputData.getString(KEY_SOURCE_URI),
            KEY_CAPTION to inputData.getString(KEY_CAPTION),
            KEY_AUDIENCE to inputData.getString(KEY_AUDIENCE),
            KEY_CLIENT_PUBLISH_ID to inputData.getString(KEY_CLIENT_PUBLISH_ID),
            KEY_ENQUEUED_AT to inputData.getLong(KEY_ENQUEUED_AT, 0L),
        ),
    )

    private fun progressData(stage: String, progress: Int? = null): Data = workDataOf(
        KEY_STAGE to stage,
        KEY_PROGRESS to (progress?.coerceIn(0, 100) ?: -1),
        KEY_TARGET to inputData.getString(KEY_TARGET),
    )

    companion object {
        const val KEY_TARGET = "target"
        const val KEY_USER_ID = "user_id"
        const val KEY_SOURCE_URI = "source_uri"
        const val KEY_CAPTION = "caption"
        const val KEY_AUDIENCE = "audience"
        const val KEY_CLIENT_PUBLISH_ID = "client_publish_id"
        const val KEY_ENQUEUED_AT = "enqueued_at"
        const val KEY_STAGE = "stage"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val KEY_RESULT_ID = "result_id"
        const val KEY_FINISHED_AT = "finished_at"

        const val STAGE_PREPARING = "preparing"
        const val STAGE_QUEUED = "queued"
        const val STAGE_UPLOADING = "uploading"
        const val STAGE_PUBLISHED = "published"
        const val STAGE_FAILED = "failed"

        fun enqueue(
            context: Context,
            target: MediaPublishTarget,
            userId: Long,
            sourceUri: Uri,
            caption: String,
            audience: String = "followers",
            clientPublishId: String = UUID.randomUUID().toString(),
            replace: Boolean = false,
        ): UUID {
            val input = inputData(target, userId, sourceUri, caption, audience, clientPublishId)
            val request = OneTimeWorkRequestBuilder<MediaPublishWorker>()
                .setInputData(input)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .addTag(userTag(userId))
                .addTag(targetTag(target))
                .addTag(ALL_PUBLISHES_TAG)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                uniqueName(target, userId, clientPublishId),
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
            return request.id
        }

        fun retry(context: Context, input: Data): UUID {
            return enqueue(
                context = context,
                target = requireNotNull(MediaPublishTarget.fromWire(input.getString(KEY_TARGET).orEmpty())),
                userId = input.getLong(KEY_USER_ID, 0L),
                sourceUri = Uri.parse(input.getString(KEY_SOURCE_URI).orEmpty()),
                caption = input.getString(KEY_CAPTION).orEmpty(),
                audience = input.getString(KEY_AUDIENCE).orEmpty().ifBlank { "followers" },
                clientPublishId = input.getString(KEY_CLIENT_PUBLISH_ID).orEmpty(),
                replace = true,
            )
        }

        fun uniqueName(target: MediaPublishTarget, userId: Long, clientPublishId: String): String =
            "nova-publish-${target.wireValue}-$userId-$clientPublishId"

        fun userTag(userId: Long): String = "nova-media-publish-user-$userId"

        fun targetTag(target: MediaPublishTarget): String =
            "nova-media-publish-target-${target.wireValue}"

        private fun inputData(
            target: MediaPublishTarget,
            userId: Long,
            sourceUri: Uri,
            caption: String,
            audience: String,
            clientPublishId: String,
        ): Data = workDataOf(
            KEY_TARGET to target.wireValue,
            KEY_USER_ID to userId,
            KEY_SOURCE_URI to sourceUri.toString(),
            KEY_CAPTION to caption.take(500),
            KEY_AUDIENCE to audience,
            KEY_CLIENT_PUBLISH_ID to clientPublishId,
            KEY_ENQUEUED_AT to System.currentTimeMillis(),
        )

        private const val ALL_PUBLISHES_TAG = "nova-media-publish"
    }
}


internal fun shouldRetryPublish(statusCode: Int?, runAttemptCount: Int): Boolean {
    if (runAttemptCount >= 2) return false
    return statusCode == null || statusCode == 408 || statusCode == 429 || statusCode in 500..599
}


internal fun publishAccountMatches(expectedUserId: Long, activeUserId: Long?): Boolean =
    expectedUserId > 0L && activeUserId == expectedUserId
