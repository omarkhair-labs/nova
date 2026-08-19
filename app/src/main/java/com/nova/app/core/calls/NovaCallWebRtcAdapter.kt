package com.nova.app.core.calls

import android.content.Context
import com.nova.app.feature.calls.domain.model.NovaCallKind
import com.nova.app.feature.calls.domain.model.NovaIceConfig
import com.nova.app.feature.calls.webrtc.CallWebRtcEngine
import com.nova.app.feature.calls.webrtc.CallWebRtcListener
import com.nova.app.feature.calls.webrtc.model.NovaCallAudioQualitySnapshot
import org.webrtc.EglBase
import org.webrtc.IceCandidate


/** Thin ownership adapter; all WebRTC algorithms remain in NovaWebRtcEngine unchanged. */
class NovaCallWebRtcAdapter(
    context: Context,
    kind: NovaCallKind,
    iceConfig: NovaIceConfig,
    listener: CallWebRtcListener,
) : CallWebRtcEngine {
    private val delegate = NovaWebRtcEngine(
        context = context,
        kind = kind,
        iceConfig = iceConfig,
        listener = object : NovaWebRtcEngine.Listener {
            override fun onLocalIceCandidate(candidate: IceCandidate) = listener.onLocalIceCandidate(candidate)
            override fun onConnectionState(state: org.webrtc.PeerConnection.PeerConnectionState) =
                listener.onConnectionState(state)
            override fun onLocalVideoTrack(track: org.webrtc.VideoTrack?) = listener.onLocalVideoTrack(track)
            override fun onRemoteVideoTrack(track: org.webrtc.VideoTrack?) = listener.onRemoteVideoTrack(track)
            override fun onError(message: String) = listener.onError(message)
        },
    )

    override val eglContext: EglBase.Context
        get() = delegate.eglContext

    override fun start(): Boolean = delegate.start()
    override fun createOffer(onReady: (String) -> Unit) = delegate.createOffer(onReady)
    override fun createIceRestartOffer(onReady: (String) -> Unit) = delegate.createIceRestartOffer(onReady)
    override fun setRemoteOffer(sdp: String, onReady: () -> Unit) = delegate.setRemoteOffer(sdp, onReady)
    override fun createAnswer(onReady: (String) -> Unit) = delegate.createAnswer(onReady)
    override fun setRemoteAnswer(sdp: String, onReady: () -> Unit) = delegate.setRemoteAnswer(sdp, onReady)
    override fun addRemoteIce(candidate: IceCandidate) = delegate.addRemoteIce(candidate)
    override fun collectAudioQualitySnapshot(onReady: (NovaCallAudioQualitySnapshot) -> Unit) =
        delegate.collectAudioQualitySnapshot(onReady)
    override fun setMicrophoneEnabled(enabled: Boolean) = delegate.setMicrophoneEnabled(enabled)
    override fun setCameraEnabled(enabled: Boolean) = delegate.setCameraEnabled(enabled)
    override fun switchCamera() = delegate.switchCamera()
    override fun release() = delegate.release()
}
