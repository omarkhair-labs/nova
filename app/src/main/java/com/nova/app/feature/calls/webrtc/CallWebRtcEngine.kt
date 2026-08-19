package com.nova.app.feature.calls.webrtc

import com.nova.app.feature.calls.domain.model.NovaCallKind
import com.nova.app.feature.calls.domain.model.NovaIceConfig
import com.nova.app.feature.calls.webrtc.model.NovaCallAudioQualitySnapshot
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.VideoTrack


interface CallWebRtcListener {
    fun onLocalIceCandidate(candidate: IceCandidate)
    fun onConnectionState(state: PeerConnection.PeerConnectionState)
    fun onLocalVideoTrack(track: VideoTrack?)
    fun onRemoteVideoTrack(track: VideoTrack?)
    fun onError(message: String)
}


interface CallWebRtcEngine {
    val eglContext: EglBase.Context

    fun start(): Boolean
    fun createOffer(onReady: (String) -> Unit)
    fun createIceRestartOffer(onReady: (String) -> Unit)
    fun setRemoteOffer(sdp: String, onReady: () -> Unit)
    fun createAnswer(onReady: (String) -> Unit)
    fun setRemoteAnswer(sdp: String, onReady: () -> Unit = {})
    fun addRemoteIce(candidate: IceCandidate)
    fun collectAudioQualitySnapshot(onReady: (NovaCallAudioQualitySnapshot) -> Unit)
    fun setMicrophoneEnabled(enabled: Boolean)
    fun setCameraEnabled(enabled: Boolean)
    fun switchCamera()
    fun release()
}


fun interface CallWebRtcFactory {
    fun create(
        kind: NovaCallKind,
        iceConfig: NovaIceConfig,
        listener: CallWebRtcListener,
    ): CallWebRtcEngine
}
