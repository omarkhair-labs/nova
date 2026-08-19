package com.nova.app.core.calls

import com.nova.app.feature.calls.domain.model.NovaCallKind
import com.nova.app.feature.calls.domain.model.NovaCallSession
import com.nova.app.feature.calls.domain.model.NovaCallStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.OffsetDateTime


data class NovaActiveCallSummary(
    val callId: String,
    val kind: NovaCallKind,
    val peerName: String,
    val peerUsername: String,
    val status: NovaCallStatus,
    val answeredAtEpochMs: Long?,
)


object NovaActiveCallSignal {
    private val mutableState = MutableStateFlow<NovaActiveCallSummary?>(null)
    val state = mutableState.asStateFlow()

    fun publish(call: NovaCallSession): NovaActiveCallSummary? {
        if (call.status.isTerminal) {
            clear(call.id)
            return null
        }

        val summary = NovaActiveCallSummary(
            callId = call.id,
            kind = call.kind,
            peerName = call.peer.displayName,
            peerUsername = call.peer.username,
            status = call.status,
            answeredAtEpochMs = parseEpochMillis(call.answeredAt),
        )
        mutableState.value = summary
        return summary
    }

    fun clear(callId: String) {
        if (mutableState.value?.callId == callId) {
            mutableState.value = null
        }
    }

    private fun parseEpochMillis(value: String?): Long? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        return runCatching { OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
            .recoverCatching { Instant.parse(raw).toEpochMilli() }
            .getOrNull()
    }
}
