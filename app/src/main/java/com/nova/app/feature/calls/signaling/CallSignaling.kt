package com.nova.app.feature.calls.signaling

import com.nova.app.feature.calls.domain.model.NovaCallSession
import kotlinx.coroutines.CoroutineScope


enum class NovaCallSocketStatus {
    Connecting,
    Live,
    Reconnecting,
    Offline,
}


sealed interface NovaCallSignalEvent {
    data class Ready(val call: NovaCallSession) : NovaCallSignalEvent
    data class Offer(val sdp: String, val negotiationId: String?) : NovaCallSignalEvent
    data class Answer(val sdp: String, val negotiationId: String?) : NovaCallSignalEvent
    data class Ice(
        val candidate: String,
        val sdpMid: String,
        val sdpMLineIndex: Int,
    ) : NovaCallSignalEvent
    data object IceRestartRequested : NovaCallSignalEvent
    data class State(val call: NovaCallSession) : NovaCallSignalEvent
    data class Error(val detail: String) : NovaCallSignalEvent
}


interface CallSignaling {
    fun start(
        scope: CoroutineScope,
        onEvent: (NovaCallSignalEvent) -> Unit,
        onStatus: (NovaCallSocketStatus) -> Unit,
        onSessionExpired: () -> Unit,
    )

    fun stop()
    fun sendOffer(sdp: String, negotiationId: String? = null): Boolean
    fun sendAnswer(sdp: String, negotiationId: String? = null): Boolean
    fun sendIce(candidate: String, sdpMid: String?, sdpMLineIndex: Int): Boolean
    fun requestIceRestart(): Boolean
    fun accept(): Boolean
    fun decline(): Boolean
    fun cancel(): Boolean
    fun end(): Boolean
    fun timeout(): Boolean
    fun failed(): Boolean
}


fun interface CallSignalingFactory {
    fun create(callId: String): CallSignaling
}
