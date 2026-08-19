package com.nova.app.core.calls

import android.content.Context
import android.util.Log
import com.nova.app.feature.calls.domain.model.NovaCallKind
import com.nova.app.feature.calls.domain.model.NovaIceConfig
import com.nova.app.feature.calls.webrtc.model.NovaCallAudioQualitySnapshot
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.atomic.AtomicBoolean


class NovaWebRtcEngine(
    context: Context,
    private val kind: NovaCallKind,
    private val iceConfig: NovaIceConfig,
    private val listener: Listener,
) {
    interface Listener {
        fun onLocalIceCandidate(candidate: IceCandidate)
        fun onConnectionState(state: PeerConnection.PeerConnectionState)
        fun onLocalVideoTrack(track: VideoTrack?)
        fun onRemoteVideoTrack(track: VideoTrack?)
        fun onError(message: String)
    }

    private val appContext = context.applicationContext
    private val released = AtomicBoolean(false)
    private val eglBase = EglBase.create()
    private val audioDeviceModule = JavaAudioDeviceModule.builder(appContext).createAudioDeviceModule()
    private val factory: PeerConnectionFactory

    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var cameraCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private val pendingRemoteIce = mutableListOf<IceCandidate>()
    private val appliedRemoteAnswers = ArrayDeque<String>()
    private val recoveryLock = Any()
    private var deferredIceRestart: ((String) -> Unit)? = null
    private var hasRemoteDescription = false

    var microphoneEnabled: Boolean = true
        private set
    var cameraEnabled: Boolean = kind == NovaCallKind.Video
        private set

    val eglContext: EglBase.Context
        get() = eglBase.eglBaseContext

    init {
        initializeWebRtcOnce(appContext)
        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            )
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun start(): Boolean {
        if (released.get()) return false
        if (peerConnection != null) return true

        val rtcServers = iceConfig.servers.flatMap { server ->
            if (server.urls.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    PeerConnection.IceServer.builder(server.urls)
                        .setUsername(server.username)
                        .setPassword(server.credential)
                        .createIceServer()
                )
            }
        }
        if (rtcServers.isEmpty()) {
            listener.onError("Nova couldn't load call network settings.")
            return false
        }

        Log.i(
            TAG,
            "Starting WebRTC: turnConfigured=${iceConfig.turnConfigured}, servers=${serverKinds(iceConfig)}",
        )

        val rtcConfig = PeerConnection.RTCConfiguration(rtcServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        }

        peerConnection = factory.createPeerConnection(rtcConfig, observer)
        if (peerConnection == null) {
            listener.onError("Nova couldn't start the call connection.")
            return false
        }

        audioSource = factory.createAudioSource(MediaConstraints())
        localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource).apply {
            setEnabled(true)
        }
        peerConnection?.addTrack(localAudioTrack, listOf(MEDIA_STREAM_ID))

        if (kind == NovaCallKind.Video) {
            if (!startCamera()) {
                listener.onError("Nova couldn't open the camera. You can continue with audio.")
                cameraEnabled = false
            }
        }
        return true
    }

    fun createOffer(onReady: (String) -> Unit) {
        createOfferInternal(onReady)
    }

    fun createIceRestartOffer(onReady: (String) -> Unit) {
        val peer = peerConnection ?: return listener.onError("Call connection is not ready.")
        val signalingState = peer.signalingState()
        if (signalingState != PeerConnection.SignalingState.STABLE) {
            synchronized(recoveryLock) {
                deferredIceRestart = onReady
            }
            Log.i(TAG, "Deferring ICE restart while signaling=$signalingState")
            return
        }
        synchronized(recoveryLock) {
            deferredIceRestart = null
        }
        performIceRestart(peer, onReady)
    }

    private fun performIceRestart(peer: PeerConnection, onReady: (String) -> Unit) {
        Log.i(TAG, "Requesting ICE restart")
        runCatching { peer.restartIce() }
            .onFailure {
                listener.onError("Nova couldn't restart the call connection.")
                return
            }
        createOfferInternal(onReady)
    }

    private fun createOfferInternal(onReady: (String) -> Unit) {
        val peer = peerConnection ?: return listener.onError("Call connection is not ready.")
        peer.createOffer(
            object : SimpleSdpObserver() {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp == null) return listener.onError("Nova couldn't create the call offer.")
                    peer.setLocalDescription(
                        object : SimpleSdpObserver() {
                            override fun onSetSuccess() = onReady(sdp.description)
                            override fun onSetFailure(error: String?) {
                                listener.onError(error ?: "Nova couldn't prepare the call offer.")
                            }
                        },
                        sdp,
                    )
                }

                override fun onCreateFailure(error: String?) {
                    listener.onError(error ?: "Nova couldn't create the call offer.")
                }
            },
            offerAnswerConstraints(),
        )
    }

    fun setRemoteOffer(sdp: String, onReady: () -> Unit) {
        setRemoteDescription(SessionDescription.Type.OFFER, sdp, onReady)
    }

    fun createAnswer(onReady: (String) -> Unit) {
        val peer = peerConnection ?: return listener.onError("Call connection is not ready.")
        peer.createAnswer(
            object : SimpleSdpObserver() {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp == null) return listener.onError("Nova couldn't create the call answer.")
                    peer.setLocalDescription(
                        object : SimpleSdpObserver() {
                            override fun onSetSuccess() = onReady(sdp.description)
                            override fun onSetFailure(error: String?) {
                                listener.onError(error ?: "Nova couldn't prepare the call answer.")
                            }
                        },
                        sdp,
                    )
                }

                override fun onCreateFailure(error: String?) {
                    listener.onError(error ?: "Nova couldn't create the call answer.")
                }
            },
            offerAnswerConstraints(),
        )
    }

    fun setRemoteAnswer(sdp: String, onReady: () -> Unit = {}) {
        val peer = peerConnection ?: return listener.onError("Call connection is not ready.")
        val alreadyApplied = synchronized(appliedRemoteAnswers) {
            appliedRemoteAnswers.contains(sdp)
        }
        if (alreadyApplied) {
            Log.i(TAG, "Ignoring replayed remote answer")
            return
        }

        val signalingState = peer.signalingState()
        if (signalingState != PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
            // A delayed/replayed answer can arrive after reconnect recovery has
            // already completed. Applying it in STABLE is invalid WebRTC SDP
            // sequencing, so treat it as harmless stale signaling instead of
            // surfacing the native "Called in wrong state: stable" error.
            Log.w(TAG, "Ignoring stale remote answer while signaling=$signalingState")
            return
        }

        setRemoteDescription(SessionDescription.Type.ANSWER, sdp) {
            synchronized(appliedRemoteAnswers) {
                if (!appliedRemoteAnswers.contains(sdp)) {
                    appliedRemoteAnswers.addLast(sdp)
                }
                while (appliedRemoteAnswers.size > MAX_APPLIED_REMOTE_ANSWERS) {
                    appliedRemoteAnswers.removeFirstOrNull()
                }
            }
            onReady()
        }
    }

    fun addRemoteIce(candidate: IceCandidate) {
        val peer = peerConnection ?: return
        synchronized(pendingRemoteIce) {
            if (!hasRemoteDescription) {
                pendingRemoteIce += candidate
                return
            }
        }
        peer.addIceCandidate(candidate)
    }

    fun collectAudioQualitySnapshot(onReady: (NovaCallAudioQualitySnapshot) -> Unit) {
        val peer = peerConnection ?: return
        if (released.get()) return
        peer.getStats { report ->
            if (!released.get()) {
                onReady(NovaCallAudioQualitySnapshot.from(report))
            }
        }
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        microphoneEnabled = enabled
        localAudioTrack?.setEnabled(enabled)
    }

    fun setCameraEnabled(enabled: Boolean) {
        if (kind != NovaCallKind.Video) return
        cameraEnabled = enabled
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        cameraCapturer?.switchCamera(
            object : CameraVideoCapturer.CameraSwitchHandler {
                override fun onCameraSwitchDone(isFrontCamera: Boolean) = Unit
                override fun onCameraSwitchError(errorDescription: String?) {
                    listener.onError(errorDescription ?: "Nova couldn't switch cameras.")
                }
            }
        )
    }

    fun release() {
        if (!released.compareAndSet(false, true)) return
        synchronized(pendingRemoteIce) {
            pendingRemoteIce.clear()
        }
        synchronized(appliedRemoteAnswers) {
            appliedRemoteAnswers.clear()
        }
        synchronized(recoveryLock) {
            deferredIceRestart = null
        }
        runCatching { cameraCapturer?.stopCapture() }
        runCatching { cameraCapturer?.dispose() }
        cameraCapturer = null
        runCatching { surfaceTextureHelper?.dispose() }
        surfaceTextureHelper = null
        remoteVideoTrack = null
        runCatching { localVideoTrack?.dispose() }
        localVideoTrack = null
        runCatching { videoSource?.dispose() }
        videoSource = null
        runCatching { localAudioTrack?.dispose() }
        localAudioTrack = null
        runCatching { audioSource?.dispose() }
        audioSource = null
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        peerConnection = null
        runCatching { factory.dispose() }
        runCatching { audioDeviceModule.release() }
        runCatching { eglBase.release() }
    }

    private fun startCamera(): Boolean {
        val capturer = createCameraCapturer() ?: return false
        val helper = SurfaceTextureHelper.create("NovaCameraCapture", eglBase.eglBaseContext)
        val source = factory.createVideoSource(false)
        capturer.initialize(helper, appContext, source.capturerObserver)

        return runCatching {
            capturer.startCapture(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS)
            val track = factory.createVideoTrack(VIDEO_TRACK_ID, source).apply { setEnabled(true) }
            peerConnection?.addTrack(track, listOf(MEDIA_STREAM_ID))
            cameraCapturer = capturer
            surfaceTextureHelper = helper
            videoSource = source
            localVideoTrack = track
            listener.onLocalVideoTrack(track)
            true
        }.getOrElse {
            runCatching { capturer.dispose() }
            runCatching { helper.dispose() }
            runCatching { source.dispose() }
            false
        }
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(appContext)) {
            Camera2Enumerator(appContext)
        } else {
            Camera1Enumerator(true)
        }

        val front = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        val back = enumerator.deviceNames.firstOrNull { enumerator.isBackFacing(it) }
        val name = front ?: back ?: enumerator.deviceNames.firstOrNull() ?: return null
        return enumerator.createCapturer(name, null) as? CameraVideoCapturer
    }

    private fun setRemoteDescription(
        type: SessionDescription.Type,
        sdp: String,
        onReady: () -> Unit,
    ) {
        val peer = peerConnection ?: return listener.onError("Call connection is not ready.")
        synchronized(pendingRemoteIce) {
            // During an ICE restart, candidates for the new ufrag can arrive
            // while setRemoteDescription is still applying. Queue them until
            // the corresponding SDP is installed instead of applying them to
            // the previous ICE generation.
            hasRemoteDescription = false
        }
        peer.setRemoteDescription(
            object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    val queued = synchronized(pendingRemoteIce) {
                        hasRemoteDescription = true
                        pendingRemoteIce.toList().also { pendingRemoteIce.clear() }
                    }
                    queued.forEach(peer::addIceCandidate)
                    onReady()
                }

                override fun onSetFailure(error: String?) {
                    synchronized(pendingRemoteIce) { hasRemoteDescription = true }
                    listener.onError(error ?: "Nova couldn't apply remote call settings.")
                }
            },
            SessionDescription(type, sdp),
        )
    }

    private fun offerAnswerConstraints(): MediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(
            MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo",
                if (kind == NovaCallKind.Video) "true" else "false",
            )
        )
    }

    private val observer = object : PeerConnection.Observer {
        override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
            Log.d(TAG, "signaling=${newState ?: "unknown"}")
            if (newState != PeerConnection.SignalingState.STABLE || released.get()) return

            val deferred = synchronized(recoveryLock) {
                deferredIceRestart.also { deferredIceRestart = null }
            } ?: return
            val peer = peerConnection ?: return
            if (peer.connectionState() == PeerConnection.PeerConnectionState.CONNECTED) {
                Log.i(TAG, "Dropping deferred ICE restart because media recovered")
                return
            }
            Log.i(TAG, "Running deferred ICE restart after signaling returned to STABLE")
            performIceRestart(peer, deferred)
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            Log.i(TAG, "iceConnection=${newState ?: "unknown"}")
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            Log.d(TAG, "iceReceiving=$receiving")
        }

        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
            Log.i(TAG, "iceGathering=${newState ?: "unknown"}")
        }

        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let {
                Log.d(TAG, "localCandidate=${candidateSummary(it)}")
                listener.onLocalIceCandidate(it)
            }
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit

        override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
            if (event == null) return
            Log.i(
                TAG,
                "selectedPair local=${candidateSummary(event.local)} remote=${candidateSummary(event.remote)} " +
                    "reason=${event.reason} estimatedDisconnectMs=${event.estimatedDisconnectedTimeMs}",
            )
        }

        override fun onAddStream(stream: MediaStream?) = Unit
        override fun onRemoveStream(stream: MediaStream?) = Unit
        override fun onDataChannel(dataChannel: DataChannel?) = Unit
        override fun onRenegotiationNeeded() {
            Log.d(TAG, "renegotiationNeeded")
        }

        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
            (receiver?.track() as? VideoTrack)?.let(::publishRemoteVideoTrack)
        }

        override fun onTrack(transceiver: RtpTransceiver?) {
            (transceiver?.receiver?.track() as? VideoTrack)?.let(::publishRemoteVideoTrack)
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
            if (newState != null) {
                Log.i(TAG, "peerConnection=$newState")
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    synchronized(recoveryLock) {
                        deferredIceRestart = null
                    }
                }
                listener.onConnectionState(newState)
            }
        }
    }

    private fun publishRemoteVideoTrack(track: VideoTrack) {
        if (track.kind() != MediaStreamTrack.VIDEO_TRACK_KIND) return
        if (remoteVideoTrack === track) return
        remoteVideoTrack = track
        track.setEnabled(true)
        listener.onRemoteVideoTrack(track)
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    private companion object {
        const val TAG = "NovaCallRTC"
        const val MEDIA_STREAM_ID = "nova-media"
        const val AUDIO_TRACK_ID = "nova-audio"
        const val VIDEO_TRACK_ID = "nova-video"
        const val VIDEO_WIDTH = 1280
        const val VIDEO_HEIGHT = 720
        const val VIDEO_FPS = 30
        const val MAX_APPLIED_REMOTE_ANSWERS = 4
        val initialized = AtomicBoolean(false)

        fun initializeWebRtcOnce(context: Context) {
            if (initialized.compareAndSet(false, true)) {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                )
            }
        }

        fun serverKinds(config: NovaIceConfig): String {
            return config.servers
                .flatMap { it.urls }
                .mapNotNull { raw -> raw.substringBefore(':').lowercase().takeIf { it.isNotBlank() } }
                .distinct()
                .joinToString(",")
                .ifBlank { "none" }
        }

        fun candidateSummary(candidate: IceCandidate?): String {
            if (candidate == null) return "none"
            val parts = candidate.sdp.trim().split(Regex("\\s+"))
            val protocol = parts.getOrNull(2)?.lowercase() ?: "unknown"
            val typeIndex = parts.indexOfFirst { it.equals("typ", ignoreCase = true) }
            val candidateType = parts.getOrNull(typeIndex + 1)?.lowercase() ?: "unknown"
            return "$candidateType/$protocol"
        }
    }
}
