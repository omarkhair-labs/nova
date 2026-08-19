package com.nova.app.feature.calls.signaling

import com.nova.app.core.calls.NovaCallSignalEvent
import com.nova.app.core.calls.NovaCallSocketStatus
import kotlinx.coroutines.CoroutineScope


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
