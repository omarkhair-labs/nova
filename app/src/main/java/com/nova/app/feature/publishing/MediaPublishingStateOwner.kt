package com.nova.app.feature.publishing

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


data class MediaPublishItem(
    val workId: UUID,
    val target: MediaPublishTarget,
    val stage: String,
    val progress: Int?,
    val error: String?,
    val input: androidx.work.Data,
) {
    val canRetry: Boolean get() = stage == MediaPublishWorker.STAGE_FAILED
    val canCancel: Boolean get() = stage == MediaPublishWorker.STAGE_QUEUED ||
        stage == MediaPublishWorker.STAGE_PREPARING
}


data class MediaPublishingUiState(
    val userId: Long? = null,
    val items: List<MediaPublishItem> = emptyList(),
    val postPublishedVersion: Int = 0,
    val reelPublishedVersion: Int = 0,
    val storyPublishedVersion: Int = 0,
    val pulsePublishedVersion: Int = 0,
)


class MediaPublishingStateOwner(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private var pollingJob: Job? = null
    private val seenSuccesses = mutableSetOf<UUID>()
    private var observerStartedAtMs = 0L

    var state by mutableStateOf(MediaPublishingUiState())
        private set

    fun enter(userId: Long) {
        if (state.userId == userId && pollingJob?.isActive == true) return
        pollingJob?.cancel()
        seenSuccesses.clear()
        observerStartedAtMs = System.currentTimeMillis()
        state = MediaPublishingUiState(userId = userId)
        pollingJob = scope.launch {
            while (isActive && state.userId == userId) {
                refresh(userId)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun reset() {
        pollingJob?.cancel()
        pollingJob = null
        seenSuccesses.clear()
        observerStartedAtMs = 0L
        state = MediaPublishingUiState()
    }

    fun retry(item: MediaPublishItem) {
        if (item.canRetry) MediaPublishWorker.retry(appContext, item.input)
    }

    fun cancel(item: MediaPublishItem) {
        if (item.canCancel) workManager.cancelWorkById(item.workId)
    }

    private suspend fun refresh(userId: Long) {
        val infos = withContext(Dispatchers.IO) {
            workManager.getWorkInfosByTag(MediaPublishWorker.userTag(userId)).get()
        }
        if (state.userId != userId) return
        var postVersion = state.postPublishedVersion
        var reelVersion = state.reelPublishedVersion
        var storyVersion = state.storyPublishedVersion
        var pulseVersion = state.pulsePublishedVersion
        infos.filter { it.state == WorkInfo.State.SUCCEEDED }.forEach { info ->
            val finishedAt = info.outputData.getLong(MediaPublishWorker.KEY_FINISHED_AT, 0L)
            if (!isPublishSuccessFresh(finishedAt, observerStartedAtMs)) {
                seenSuccesses.add(info.id)
                return@forEach
            }
            if (seenSuccesses.add(info.id)) {
                when (MediaPublishTarget.fromWire(info.outputData.getString(MediaPublishWorker.KEY_TARGET).orEmpty())) {
                    MediaPublishTarget.POST -> postVersion += 1
                    MediaPublishTarget.REEL -> reelVersion += 1
                    MediaPublishTarget.STORY -> storyVersion += 1
                    MediaPublishTarget.PULSE -> pulseVersion += 1
                    null -> Unit
                }
            }
        }
        state = state.copy(
            items = infos.mapNotNull(::publishItem).sortedByDescending {
                it.input.getLong(MediaPublishWorker.KEY_ENQUEUED_AT, 0L)
            },
            postPublishedVersion = postVersion,
            reelPublishedVersion = reelVersion,
            storyPublishedVersion = storyVersion,
            pulsePublishedVersion = pulseVersion,
        )
    }

    private companion object {
        const val POLL_INTERVAL_MS = 2_500L
    }
}


internal fun isPublishSuccessFresh(
    finishedAtMs: Long,
    observerStartedAtMs: Long,
): Boolean = finishedAtMs > 0L && observerStartedAtMs > 0L && finishedAtMs >= observerStartedAtMs


internal fun publishItem(info: WorkInfo, nowMs: Long = System.currentTimeMillis()): MediaPublishItem? {
    if (info.state == WorkInfo.State.CANCELLED) return null
    val metadata = if (info.state.isFinished) info.outputData else info.progress
    val target = MediaPublishTarget.fromWire(
        metadata.getString(MediaPublishWorker.KEY_TARGET).orEmpty(),
    ) ?: info.tags.firstNotNullOfOrNull { tag ->
        MediaPublishTarget.entries.firstOrNull { tag == MediaPublishWorker.targetTag(it) }
    } ?: return null
    val stage = when (info.state) {
        WorkInfo.State.SUCCEEDED -> MediaPublishWorker.STAGE_PUBLISHED
        WorkInfo.State.FAILED -> MediaPublishWorker.STAGE_FAILED
        else -> info.progress.getString(MediaPublishWorker.KEY_STAGE)
            ?: MediaPublishWorker.STAGE_QUEUED
    }
    val finishedAt = info.outputData.getLong(MediaPublishWorker.KEY_FINISHED_AT, 0L)
    if (stage == MediaPublishWorker.STAGE_PUBLISHED && finishedAt > 0L && nowMs - finishedAt > 30_000L) {
        return null
    }
    return MediaPublishItem(
        workId = info.id,
        target = target,
        stage = stage,
        progress = info.progress.getInt(MediaPublishWorker.KEY_PROGRESS, -1).takeIf { it >= 0 },
        error = info.outputData.getString(MediaPublishWorker.KEY_ERROR),
        input = metadata,
    )
}
