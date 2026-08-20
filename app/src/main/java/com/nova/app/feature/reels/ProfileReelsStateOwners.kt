package com.nova.app.feature.reels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.reels.data.ProfileReelsRepository
import com.nova.app.feature.reels.data.ReelsRepository
import com.nova.app.feature.reels.domain.model.NovaReel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class ProfileReelsViewerUiState(
    val reels: List<NovaReel> = emptyList(),
    val nextCursor: String? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val likingId: Long? = null,
    val repostingId: Long? = null,
    val deletingId: Long? = null,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
    val deletedVersion: Int = 0,
)


/** Owns profile-Reel paging and mutations; ExoPlayer, overlays and sharing stay in UI. */
class ProfileReelsViewerStateOwner(
    private val username: String,
    private val initialReelId: Long,
    private val profileRepository: ProfileReelsRepository,
    private val interactionRepository: ReelsRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(ProfileReelsViewerUiState())
        private set

    fun clearError() {
        state = state.copy(error = null)
    }

    fun replaceReel(updated: NovaReel) {
        state = state.copy(
            reels = state.reels.map { existing -> if (existing.id == updated.id) updated else existing },
        )
    }

    fun loadInitial() {
        scope.launch { loadInitialNow() }
    }

    internal suspend fun loadInitialNow() {
        state = state.copy(
            reels = emptyList(),
            nextCursor = null,
            loading = true,
            loadingMore = false,
            error = null,
        )

        var cursor: String? = null
        var loadedPages = 0
        var aggregate = emptyList<NovaReel>()

        while (loadedPages < MAX_INITIAL_LOOKUP_PAGES) {
            when (val result = profileRepository.reels(username, cursor)) {
                is ApiResult.Success -> {
                    aggregate = appendPagePreservingIncomingDuplicates(aggregate, result.value.reels)
                    state = state.copy(
                        reels = aggregate,
                        nextCursor = result.value.nextCursor,
                    )
                    cursor = result.value.nextCursor
                    loadedPages += 1
                    if (aggregate.any { it.id == initialReelId } || cursor == null) break
                }

                is ApiResult.Failure -> {
                    recordFailure(result)
                    break
                }
            }
        }

        state = state.copy(loading = false)
    }

    fun loadMore() {
        scope.launch { loadMoreNow() }
    }

    internal suspend fun loadMoreNow() {
        val cursor = state.nextCursor ?: return
        if (state.loadingMore) return

        state = state.copy(loadingMore = true, error = null)
        when (val result = profileRepository.reels(username, cursor)) {
            is ApiResult.Success -> {
                state = state.copy(
                    reels = appendPagePreservingIncomingDuplicates(state.reels, result.value.reels),
                    nextCursor = result.value.nextCursor,
                )
            }

            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(loadingMore = false)
    }

    fun toggleLike(reel: NovaReel) {
        scope.launch { toggleLikeNow(reel) }
    }

    internal suspend fun toggleLikeNow(reel: NovaReel) {
        if (state.likingId != null) return
        state = state.copy(likingId = reel.id)
        when (val result = interactionRepository.setLiked(reel.id, !reel.isLiked)) {
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
        when (val result = interactionRepository.setReposted(reel.id, !reel.isReposted)) {
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
        when (val result = interactionRepository.deleteReel(reel.id)) {
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

    private fun recordFailure(result: ApiResult.Failure) {
        state = if (result.statusCode == 401) {
            state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
        } else {
            state.copy(error = result.message)
        }
    }

    private companion object {
        const val MAX_INITIAL_LOOKUP_PAGES = 20
    }
}


enum class ProfileReelsSource {
    Authored,
    Reposted,
}


data class ProfileReelsGridUiState(
    val reels: List<NovaReel> = emptyList(),
    val nextCursor: String? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
)


/** Owns authored/reposted profile-grid paging. All failures, including 401, stay inline by contract. */
class ProfileReelsGridStateOwner(
    private val username: String,
    private val source: ProfileReelsSource,
    private val repository: ProfileReelsRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(ProfileReelsGridUiState())
        private set

    fun clearError() {
        state = state.copy(error = null)
    }

    fun loadFirstPage() {
        scope.launch { loadFirstPageNow() }
    }

    internal suspend fun loadFirstPageNow() {
        if (username.isBlank()) {
            state = state.copy(loading = false)
            return
        }

        state = state.copy(loading = true, error = null)
        when (val result = page(cursor = null)) {
            is ApiResult.Success -> {
                state = state.copy(
                    reels = result.value.reels,
                    nextCursor = result.value.nextCursor,
                )
            }

            is ApiResult.Failure -> state = state.copy(error = result.message)
        }
        state = state.copy(loading = false)
    }

    fun loadMore() {
        scope.launch { loadMoreNow() }
    }

    internal suspend fun loadMoreNow() {
        val cursor = state.nextCursor ?: return
        if (state.loadingMore || username.isBlank()) return

        state = state.copy(loadingMore = true, error = null)
        when (val result = page(cursor)) {
            is ApiResult.Success -> {
                state = state.copy(
                    reels = appendPagePreservingIncomingDuplicates(state.reels, result.value.reels),
                    nextCursor = result.value.nextCursor,
                )
            }

            is ApiResult.Failure -> state = state.copy(error = result.message)
        }
        state = state.copy(loadingMore = false)
    }

    private suspend fun page(cursor: String?) = when (source) {
        ProfileReelsSource.Authored -> repository.reels(username, cursor)
        ProfileReelsSource.Reposted -> repository.repostedReels(username, cursor)
    }
}
