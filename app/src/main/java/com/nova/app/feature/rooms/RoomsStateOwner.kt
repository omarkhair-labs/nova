package com.nova.app.feature.rooms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.rooms.data.RoomRepository
import com.nova.app.feature.rooms.domain.model.RoomSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class RoomsUiState(
    val rooms: List<RoomSummary> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
)


class RoomsStateOwner(
    private val repository: RoomRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(RoomsUiState())
        private set

    fun load(showSpinner: Boolean = false) {
        scope.launch { loadNow(showSpinner) }
    }

    internal suspend fun loadNow(showSpinner: Boolean = false) {
        if (showSpinner) state = state.copy(loading = true)
        when (val result = repository.rooms()) {
            is ApiResult.Success -> state = state.copy(
                rooms = result.value,
                loading = false,
                error = null,
            )
            is ApiResult.Failure -> state = if (result.statusCode == 401) {
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
