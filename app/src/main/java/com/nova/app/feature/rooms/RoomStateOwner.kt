package com.nova.app.feature.rooms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.rooms.data.RoomRepository
import com.nova.app.feature.rooms.domain.model.RoomDetail
import com.nova.app.feature.rooms.domain.model.RoomItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class RoomUiState(
    val detail: RoomDetail? = null,
    val selectedKind: String? = null,
    val pinned: List<RoomItem> = emptyList(),
    val items: List<RoomItem> = emptyList(),
    val nextBefore: Long? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val savingDescription: Boolean = false,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
)


class RoomStateOwner(
    private val conversationId: Long,
    private val repository: RoomRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(RoomUiState())
        private set

    fun load(showSpinner: Boolean = false) {
        scope.launch { loadNow(showSpinner) }
    }

    internal suspend fun loadNow(showSpinner: Boolean = false) {
        if (showSpinner) state = state.copy(loading = true)
        when (val detailResult = repository.room(conversationId)) {
            is ApiResult.Failure -> {
                handleFailure(detailResult, releaseMain = true)
                return
            }
            is ApiResult.Success -> state = state.copy(detail = detailResult.value)
        }
        loadItemsNow(state.selectedKind, showSpinner = false)
    }

    fun selectKind(kind: String?) {
        val normalized = kind?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        if (state.selectedKind == normalized && state.items.isNotEmpty()) return
        state = state.copy(selectedKind = normalized, items = emptyList(), pinned = emptyList(), nextBefore = null)
        scope.launch { loadItemsNow(normalized, showSpinner = true) }
    }

    private suspend fun loadItemsNow(kind: String?, showSpinner: Boolean) {
        if (showSpinner) state = state.copy(loading = true)
        when (val result = repository.items(conversationId, kind = kind)) {
            is ApiResult.Success -> state = state.copy(
                pinned = result.value.pinned,
                items = result.value.items,
                nextBefore = result.value.nextBefore,
                loading = false,
                loadingMore = false,
                error = null,
            )
            is ApiResult.Failure -> handleFailure(result, releaseMain = true)
        }
    }

    fun loadMore() {
        val before = state.nextBefore ?: return
        if (state.loading || state.loadingMore) return
        scope.launch { loadMoreNow(before) }
    }

    internal suspend fun loadMoreNow(before: Long) {
        state = state.copy(loadingMore = true)
        when (
            val result = repository.items(
                conversationId = conversationId,
                kind = state.selectedKind,
                before = before,
            )
        ) {
            is ApiResult.Success -> {
                val existing = state.items.mapTo(mutableSetOf()) { it.id }
                val appended = result.value.items.filter { existing.add(it.id) }
                state = state.copy(
                    pinned = result.value.pinned,
                    items = state.items + appended,
                    nextBefore = result.value.nextBefore,
                    loadingMore = false,
                    error = null,
                )
            }
            is ApiResult.Failure -> handleFailure(result, releaseMain = false)
        }
    }

    fun updateDescription(description: String) {
        if (state.savingDescription) return
        scope.launch { updateDescriptionNow(description) }
    }

    internal suspend fun updateDescriptionNow(description: String) {
        state = state.copy(savingDescription = true)
        when (val result = repository.updateDescription(conversationId, description)) {
            is ApiResult.Success -> state = state.copy(
                detail = result.value,
                savingDescription = false,
                error = null,
            )
            is ApiResult.Failure -> {
                if (result.statusCode == 401) {
                    state = state.copy(
                        savingDescription = false,
                        error = null,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state = state.copy(savingDescription = false, error = result.message)
                }
            }
        }
    }

    private fun handleFailure(result: ApiResult.Failure, releaseMain: Boolean) {
        state = if (result.statusCode == 401) {
            state.copy(
                loading = if (releaseMain) false else state.loading,
                loadingMore = false,
                error = null,
                sessionExpiryVersion = state.sessionExpiryVersion + 1,
            )
        } else {
            state.copy(
                loading = if (releaseMain) false else state.loading,
                loadingMore = false,
                error = result.message,
            )
        }
    }
}
