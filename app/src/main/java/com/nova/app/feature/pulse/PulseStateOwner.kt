package com.nova.app.feature.pulse

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.pulse.data.PulseRepository
import com.nova.app.feature.pulse.domain.model.NovaPulse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class PulseUiState(
    val pulses: List<NovaPulse> = emptyList(),
    val loading: Boolean = true,
    val uploading: Boolean = false,
    val deletingPulseId: Long? = null,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
    val createdVersion: Int = 0,
)


class PulseStateOwner(
    private val repository: PulseRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(PulseUiState())
        private set

    fun clearError() {
        state = state.copy(error = null)
    }

    fun load(showSpinner: Boolean = false) {
        scope.launch { loadNow(showSpinner) }
    }

    internal suspend fun loadNow(showSpinner: Boolean = false) {
        if (showSpinner) state = state.copy(loading = true)
        when (val result = repository.pulses()) {
            is ApiResult.Success -> state = state.copy(
                pulses = result.value,
                loading = false,
                error = null,
            )

            is ApiResult.Failure -> {
                recordFailure(result)
                state = state.copy(loading = false)
            }
        }
    }

    fun createText(note: String, audience: String, category: String = "vibes") {
        scope.launch { createTextNow(note, audience, category) }
    }

    internal suspend fun createTextNow(note: String, audience: String, category: String = "vibes") {
        state = state.copy(uploading = true, error = null)
        when (val result = repository.createTextPulse(note, audience, category)) {
            is ApiResult.Success -> acceptCreated(result.value)
            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(uploading = false)
    }

    fun createMedia(mediaUri: Uri, note: String, audience: String, category: String = "vibes") {
        scope.launch { createMediaNow(mediaUri, note, audience, category) }
    }

    internal suspend fun createMediaNow(mediaUri: Uri, note: String, audience: String, category: String = "vibes") {
        state = state.copy(uploading = true, error = null)
        when (val result = repository.createMediaPulse(mediaUri, note, audience, category)) {
            is ApiResult.Success -> acceptCreated(result.value)
            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(uploading = false)
    }

    fun delete(pulseId: Long) {
        scope.launch { deleteNow(pulseId) }
    }

    internal suspend fun deleteNow(pulseId: Long) {
        state = state.copy(deletingPulseId = pulseId, error = null)
        when (val result = repository.deletePulse(pulseId)) {
            is ApiResult.Success -> state = state.copy(
                pulses = state.pulses.filterNot { it.id == pulseId },
            )

            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(deletingPulseId = null)
    }

    private fun acceptCreated(pulse: NovaPulse) {
        state = state.copy(
            pulses = listOf(pulse) + state.pulses.filterNot { it.id == pulse.id },
            createdVersion = state.createdVersion + 1,
            error = null,
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
