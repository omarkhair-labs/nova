package com.nova.app.feature.memories

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.memories.data.MemoryRepository
import com.nova.app.feature.memories.domain.model.WeeklyMemory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class MemoryUiState(
    val memory: WeeklyMemory? = null,
    val weeksAgo: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
)


class MemoryStateOwner(
    private val repository: MemoryRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(MemoryUiState())
        private set

    fun load(
        utcOffsetMinutes: Int,
        weeksAgo: Int = state.weeksAgo,
        showSpinner: Boolean = false,
    ) {
        scope.launch { loadNow(utcOffsetMinutes, weeksAgo, showSpinner) }
    }

    internal suspend fun loadNow(
        utcOffsetMinutes: Int,
        weeksAgo: Int = state.weeksAgo,
        showSpinner: Boolean = false,
    ) {
        val boundedWeek = weeksAgo.coerceIn(0, 51)
        if (showSpinner) state = state.copy(loading = true)
        when (val result = repository.week(utcOffsetMinutes, boundedWeek)) {
            is ApiResult.Success -> state = state.copy(
                memory = result.value,
                weeksAgo = boundedWeek,
                loading = false,
                error = null,
            )
            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        loading = false,
                        error = null,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
