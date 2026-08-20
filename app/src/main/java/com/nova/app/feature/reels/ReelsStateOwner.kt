package com.nova.app.feature.reels

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.reels.data.ReelWatchRepository
import com.nova.app.feature.reels.data.ReelsRepository
import com.nova.app.feature.reels.domain.model.NovaReel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class ReelsUiState(
    val reels: List<NovaReel> = emptyList(),
    val nextCursor: String? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val likingId: Long? = null,
    val repostingId: Long? = null,
    val deletingId: Long? = null,
    val error: String? = null,
    val uploading: Boolean = false,
    val sessionExpiryVersion: Int = 0,
    val createdVersion: Int = 0,
    val deletedVersion: Int = 0,
)


/** Owns root Reels feed/create/mutation transport state; playback and overlays stay in UI. */
class ReelsStateOwner(
    private val repository: ReelsRepository,
    private val watchRepository: ReelWatchRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(ReelsUiState())
        private set

    fun clearError() {
        state = state.copy(error = null)
    }

    fun replaceReel(updated: NovaReel) {
        state = state.copy(
            reels = state.reels.map { existing ->
                if (existing.id == updated.id) updated else existing
            },
        )
    }

    fun load(reset: Boolean) {
        scope.launch { loadNow(reset) }
    }

    internal suspend fun loadNow(reset: Boolean) {
        if (reset) {
            if (state.loading && state.reels.isNotEmpty()) return
        } else if (state.loadingMore || state.nextCursor == null) {
            return
        }

        val cursor = if (reset) null else state.nextCursor
        state = if (reset) {
            state.copy(loading = true, error = null)
        } else {
            state.copy(loadingMore = true, error = null)
        }

        when (val result = repository.reels(cursor)) {
            is ApiResult.Success -> {
                val merged = if (reset) {
                    result.value.reels
                } else {
                    appendPagePreservingIncomingDuplicates(state.reels, result.value.reels)
                }
                state = state.copy(
                    reels = merged,
                    nextCursor = result.value.nextCursor,
                )
            }

            is ApiResult.Failure -> recordFailure(result)
        }

        state = state.copy(loading = false, loadingMore = false)
    }

    fun createReel(videoUri: Uri, caption: String) {
        scope.launch { createReelNow(videoUri, caption) }
    }

    internal suspend fun createReelNow(videoUri: Uri, caption: String) {
        state = state.copy(uploading = true, error = null)
        when (val result = repository.createReel(videoUri, caption)) {
            is ApiResult.Success -> {
                state = state.copy(
                    reels = listOf(result.value) + state.reels.filterNot { it.id == result.value.id },
                    createdVersion = state.createdVersion + 1,
                )
            }

            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(uploading = false)
    }

    fun toggleLike(reel: NovaReel) {
        scope.launch { toggleLikeNow(reel) }
    }

    internal suspend fun toggleLikeNow(reel: NovaReel) {
        if (state.likingId != null) return
        state = state.copy(likingId = reel.id)
        when (val result = repository.setLiked(reel.id, !reel.isLiked)) {
            is ApiResult.Success -> replaceReel(result.value)
            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(likingId = null)
    }

    fun toggleRepost(reel: NovaReel) {
        scope.launch { toggleRepostNow(reel) }
    }

    internal suspend fun toggleRepostNow(reel: NovaReel) {
        if (state.repostingId != null) return
        state = state.copy(repostingId = reel.id)
        when (val result = repository.setReposted(reel.id, !reel.isReposted)) {
            is ApiResult.Success -> replaceReel(result.value)
            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(repostingId = null)
    }

    fun deleteReel(reel: NovaReel) {
        scope.launch { deleteReelNow(reel) }
    }

    internal suspend fun deleteReelNow(reel: NovaReel) {
        if (!reel.isMine || state.deletingId != null) return
        state = state.copy(deletingId = reel.id, error = null)
        when (val result = repository.deleteReel(reel.id)) {
            is ApiResult.Success -> {
                state = state.copy(
                    reels = state.reels.filterNot { it.id == reel.id },
                    deletedVersion = state.deletedVersion + 1,
                )
            }

            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(deletingId = null)
    }

    fun recordWatch(
        reel: NovaReel,
        snapshot: ReelWatchSnapshot,
    ) {
        scope.launch {
            recordWatchNow(
                reel = reel,
                sessionId = snapshot.sessionId,
                watchedMs = snapshot.watchedMs,
                durationMs = snapshot.durationMs,
                maxPositionMs = snapshot.maxPositionMs,
            )
        }
    }

    internal suspend fun recordWatchNow(
        reel: NovaReel,
        sessionId: String,
        watchedMs: Long,
        durationMs: Long,
        maxPositionMs: Long,
    ) {
        if (reel.isMine || watchedMs < 250L) return
        // Preserve the live best-effort telemetry contract: all results, including 401, are ignored.
        watchRepository.record(
            reelId = reel.id,
            sessionId = sessionId,
            watchedMs = watchedMs,
            durationMs = durationMs,
            maxPositionMs = maxPositionMs,
        )
    }

    private fun recordFailure(result: ApiResult.Failure) {
        state = if (result.statusCode == 401) {
            state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
        } else {
            state.copy(error = result.message)
        }
    }
}


internal fun appendPagePreservingIncomingDuplicates(
    existing: List<NovaReel>,
    incoming: List<NovaReel>,
): List<NovaReel> {
    val existingIds = existing.mapTo(mutableSetOf()) { it.id }
    return existing + incoming.filterNot { it.id in existingIds }
}
