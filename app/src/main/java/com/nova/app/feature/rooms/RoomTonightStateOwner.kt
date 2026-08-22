package com.nova.app.feature.rooms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.rooms.data.RoomRepository
import com.nova.app.feature.rooms.domain.model.RoomTonightSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class RoomTonightUiState(
    val snapshot: RoomTonightSnapshot? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
)


class RoomTonightStateOwner(
    private val repository: RoomRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(RoomTonightUiState())
        private set

    fun load(utcOffsetMinutes: Int, showSpinner: Boolean = false) {
        scope.launch { loadNow(utcOffsetMinutes, showSpinner) }
    }

    internal suspend fun loadNow(utcOffsetMinutes: Int, showSpinner: Boolean = false) {
        if (showSpinner) state = state.copy(loading = true)
        when (val result = repository.roomTonight(utcOffsetMinutes)) {
            is ApiResult.Success -> state = state.copy(
                snapshot = result.value,
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
