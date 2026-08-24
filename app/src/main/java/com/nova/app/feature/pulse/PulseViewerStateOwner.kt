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


data class PulseViewerUiState(
    val chain: List<NovaPulse>,
    val loading: Boolean = false,
    val replying: Boolean = false,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
    val replyCreatedVersion: Int = 0,
)


class PulseViewerStateOwner(
    initialPulse: NovaPulse,
    private val repository: PulseRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(PulseViewerUiState(chain = listOf(initialPulse)))
        private set

    fun clearError() {
        state = state.copy(error = null)
    }

    fun loadChain(pulseId: Long) {
        scope.launch { loadChainNow(pulseId) }
    }

    internal suspend fun loadChainNow(pulseId: Long) {
        state = state.copy(loading = true, error = null)
        when (val result = repository.pulseChain(pulseId)) {
            is ApiResult.Success -> state = state.copy(
                chain = result.value.ifEmpty { state.chain },
                loading = false,
                error = null,
            )

            is ApiResult.Failure -> {
                recordFailure(result)
                state = state.copy(loading = false)
            }
        }
    }

    fun replyText(pulseId: Long, note: String, audience: String, category: String = "vibes") {
        scope.launch { replyTextNow(pulseId, note, audience, category) }
    }

    internal suspend fun replyTextNow(pulseId: Long, note: String, audience: String, category: String = "vibes") {
        state = state.copy(replying = true, error = null)
        when (val result = repository.replyTextPulse(pulseId, note, audience, category)) {
            is ApiResult.Success -> acceptReply(result.value)
            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(replying = false)
    }

    fun replyMedia(pulseId: Long, mediaUri: Uri, note: String, audience: String, category: String = "vibes") {
        scope.launch { replyMediaNow(pulseId, mediaUri, note, audience, category) }
    }

    internal suspend fun replyMediaNow(
        pulseId: Long,
        mediaUri: Uri,
        note: String,
        audience: String,
        category: String = "vibes",
    ) {
        state = state.copy(replying = true, error = null)
        when (val result = repository.replyMediaPulse(pulseId, mediaUri, note, audience, category)) {
            is ApiResult.Success -> acceptReply(result.value)
            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(replying = false)
    }

    private fun acceptReply(pulse: NovaPulse) {
        state = state.copy(
            chain = (state.chain + pulse).distinctBy { it.id },
            replyCreatedVersion = state.replyCreatedVersion + 1,
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
