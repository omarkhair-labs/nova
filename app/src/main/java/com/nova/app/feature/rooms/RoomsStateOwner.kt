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
    val selectedList: String = "mine",
    val loading: Boolean = true,
    val busyRoomId: Long? = null,
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
        when (val result = repository.rooms(state.selectedList)) {
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

    fun selectList(value: String) {
        val normalized = value.trim().lowercase().takeIf { it in setOf("mine", "discover", "following") }
            ?: return
        if (normalized == state.selectedList && state.rooms.isNotEmpty()) return
        state = state.copy(selectedList = normalized, rooms = emptyList(), loading = true, error = null)
        load(showSpinner = true)
    }

    fun join(room: RoomSummary, onJoined: (Long) -> Unit) {
        if (state.busyRoomId != null) return
        scope.launch {
            state = state.copy(busyRoomId = room.conversation.id, error = null)
            when (val result = repository.joinRoom(room.conversation.id)) {
                is ApiResult.Success -> {
                    state = state.copy(
                        rooms = state.rooms.filterNot { it.conversation.id == room.conversation.id },
                        busyRoomId = null,
                    )
                    onJoined(room.conversation.id)
                }
                is ApiResult.Failure -> handleActionFailure(result)
            }
        }
    }

    fun toggleFollow(room: RoomSummary) {
        if (state.busyRoomId != null) return
        scope.launch {
            state = state.copy(busyRoomId = room.conversation.id, error = null)
            when (val result = repository.followRoom(room.conversation.id, !room.isFollowing)) {
                is ApiResult.Success -> state = state.copy(
                    rooms = if (state.selectedList == "following" && !result.value.isFollowing) {
                        state.rooms.filterNot { it.conversation.id == room.conversation.id }
                    } else {
                        state.rooms.map { if (it.conversation.id == room.conversation.id) result.value else it }
                    },
                    busyRoomId = null,
                )
                is ApiResult.Failure -> handleActionFailure(result)
            }
        }
    }

    private fun handleActionFailure(result: ApiResult.Failure) {
        state = if (result.statusCode == 401) {
            state.copy(
                busyRoomId = null,
                sessionExpiryVersion = state.sessionExpiryVersion + 1,
            )
        } else {
            state.copy(busyRoomId = null, error = result.message)
        }
    }
}
