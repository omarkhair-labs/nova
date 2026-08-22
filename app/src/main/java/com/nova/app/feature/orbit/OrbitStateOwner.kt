package com.nova.app.feature.orbit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.orbit.data.OrbitRepository
import com.nova.app.feature.orbit.domain.model.OrbitEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class OrbitUiState(
    val events: List<OrbitEvent> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val nextCursor: String? = null,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
)


class OrbitStateOwner(
    private val repository: OrbitRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(OrbitUiState())
        private set

    fun clearError() {
        state = state.copy(error = null)
    }

    fun load(showSpinner: Boolean = false) {
        scope.launch { loadNow(showSpinner) }
    }

    internal suspend fun loadNow(showSpinner: Boolean = false) {
        if (showSpinner) state = state.copy(loading = true)
        when (val result = repository.orbit()) {
            is ApiResult.Success -> state = state.copy(
                events = result.value.events,
                loading = false,
                loadingMore = false,
                nextCursor = result.value.nextCursor,
                error = null,
            )
            is ApiResult.Failure -> {
                recordFailure(result)
                state = state.copy(loading = false, loadingMore = false)
            }
        }
    }

    fun loadMore() {
        val cursor = state.nextCursor ?: return
        if (state.loading || state.loadingMore) return
        scope.launch { loadMoreNow(cursor) }
    }

    internal suspend fun loadMoreNow(cursor: String) {
        state = state.copy(loadingMore = true, error = null)
        when (val result = repository.orbit(cursor)) {
            is ApiResult.Success -> state = state.copy(
                events = (state.events + result.value.events).distinctBy { it.id },
                loadingMore = false,
                nextCursor = result.value.nextCursor,
                error = null,
            )
            is ApiResult.Failure -> {
                recordFailure(result)
                state = state.copy(loadingMore = false)
            }
        }
    }

    private fun recordFailure(result: ApiResult.Failure) {
        state = if (result.statusCode == 401) {
            state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
        } else {
            state.copy(error = result.message)
        }
    }
}
