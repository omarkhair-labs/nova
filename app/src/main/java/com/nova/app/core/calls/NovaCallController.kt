package com.nova.app.core.calls

import android.content.Context
import android.telecom.DisconnectCause
import com.nova.app.core.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.VideoTrack


sealed interface NovaCallLaunchSpec {
    data class Outgoing(
        val conversationId: Long,
        val kind: NovaCallKind,
        val peer: NovaCallPerson,
    ) : NovaCallLaunchSpec

    data class Existing(
        val callId: String,
        val requestedAction: String,
        val fallbackPeer: NovaCallPerson? = null,
        val fallbackKind: NovaCallKind = NovaCallKind.Audio,
        val fallbackConversationId: Long = -1L,
    ) : NovaCallLaunchSpec
}


data class NovaCallUiState(
    val session: NovaCallSession? = null,
    val fallbackPeer: NovaCallPerson? = null,
    val fallbackKind: NovaCallKind = NovaCallKind.Audio,
    val stage: String = "Preparing call…",
    val error: String? = null,
    val socketStatus: NovaCallSocketStatus = NovaCallSocketStatus.Connecting,
    val microphoneEnabled: Boolean = true,
    val cameraEnabled: Boolean = true,
    val localVideoTrack: VideoTrack? = null,
    val remoteVideoTrack: VideoTrack? = null,
    val eglContext: EglBase.Context? = null,
    val permissionsPending: Boolean = false,
    val connected: Boolean = false,
) {
    val kind: NovaCallKind
        get() = session?.kind ?: fallbackKind
    val peer: NovaCallPerson?
        get() = session?.peer ?: fallbackPeer
    val isIncomingRinging: Boolean
        get() = session?.let { !it.isCaller && it.status == NovaCallStatus.Ringing } == true
    val isTerminal: Boolean
        get() = session?.status?.isTerminal == true
}


class NovaCallController(
    context: Context,
    private val scope: CoroutineScope,
    private val launchSpec: NovaCallLaunchSpec,
    private val requestPermissions: (NovaCallKind) -> Unit,
    private val onFinished: () -> Unit,
    private val onSessionExpired: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val repository = NovaCallRepository(appContext)
    private val telecom = NovaTelecomBridge(appContext)
    private val mutableState = MutableStateFlow(
        NovaCallUiState(
            fallbackPeer = when (launchSpec) {
                is NovaCallLaunchSpec.Outgoing -> launchSpec.peer
                is NovaCallLaunchSpec.Existing -> launchSpec.fallbackPeer
            },
            fallbackKind = when (launchSpec) {
                is NovaCallLaunchSpec.Outgoing -> launchSpec.kind
                is NovaCallLaunchSpec.Existing -> launchSpec.fallbackKind
            },
            stage = when (launchSpec) {
                is NovaCallLaunchSpec.Outgoing -> "Preparing call…"
                is NovaCallLaunchSpec.Existing -> "Loading call…"
            },
        )
    )
    val state = mutableState.asStateFlow()

    private var signaling: NovaCallSignalingClient? = null
    private var engine: NovaWebRtcEngine? = null
    private var signalingReady = false
    private var permissionsRequested = false
    private var audioPermissionGranted = false
    private var cameraPermissionGranted = false
    private var pendingAccept = false
    private var acceptedLocally = false
    private var pendingOffer: String? = null
    private val pendingRemoteIce = mutableListOf<IceCandidate>()
    private val pendingLocalIce = mutableListOf<IceCandidate>()
    private var offerCreating = false
    private var offerSent = false
    private var answerCreating = false
    private var answerSent = false
    private var telecomStarted = false
    private var ending = false
    private var released = false
    private var ringTimeoutJob: Job? = null
    private var queuedAction: String? = when (launchSpec) {
        is NovaCallLaunchSpec.Existing -> launchSpec.requestedAction.takeIf { it.isNotBlank() }
        is NovaCallLaunchSpec.Outgoing -> null
    }

    fun start() {
        when (launchSpec) {
            is NovaCallLaunchSpec.Outgoing -> askForPermissions(launchSpec.kind)
            is NovaCallLaunchSpec.Existing -> loadExistingCall(launchSpec.callId)
        }
    }

    fun onPermissionsResult(audioGranted: Boolean, cameraGranted: Boolean) {
        permissionsRequested = true
        audioPermissionGranted = audioGranted
        cameraPermissionGranted = cameraGranted
        update { it.copy(permissionsPending = false) }

        if (!audioGranted) {
            update { it.copy(error = "Microphone permission is required for Nova calls.") }
            if (pendingAccept) {
                pendingAccept = false
                queueOrSendAction("decline")
            } else if (launchSpec is NovaCallLaunchSpec.Outgoing) {
                scope.launch {
                    delay(900)
                    onFinished()
                }
            }
            return
        }

        when (launchSpec) {
            is NovaCallLaunchSpec.Outgoing -> {
                if (mutableState.value.session == null) createOutgoingCall(launchSpec)
            }
            is NovaCallLaunchSpec.Existing -> {
                if (pendingAccept) performAccept()
            }
        }
    }

    fun handleExternalAction(action: String) {
        when (action) {
            ACTION_ANSWER -> accept()
            ACTION_DECLINE -> decline()
            ACTION_END -> hangUp()
        }
    }

    fun accept() {
        val call = mutableState.value.session ?: return
        if (call.isCaller || call.status != NovaCallStatus.Ringing || ending) return
        pendingAccept = true
        if (!audioPermissionGranted) {
            askForPermissions(call.kind)
            return
        }
        performAccept()
    }

    fun decline() {
        val call = mutableState.value.session
        if (call != null && !call.isCaller && call.status == NovaCallStatus.Ringing) {
            ending = true
            queueOrSendAction("decline")
            update { it.copy(stage = "Declining…") }
        }
    }

    fun hangUp() {
        if (ending) return
        val call = mutableState.value.session ?: run {
            onFinished()
            return
        }
        ending = true
        val action = when {
            call.status == NovaCallStatus.Active -> "end"
            call.status == NovaCallStatus.Ringing && call.isCaller -> "cancel"
            call.status == NovaCallStatus.Ringing -> "decline"
            else -> null
        }
        if (action == null) {
            finishTerminal(call)
            return
        }
        queueOrSendAction(action)
        update { it.copy(stage = "Ending call…") }
        scope.launch { telecom.disconnect(DisconnectCause.LOCAL) }
    }

    fun toggleMicrophone() {
        val current = !mutableState.value.microphoneEnabled
        engine?.setMicrophoneEnabled(current)
        update { it.copy(microphoneEnabled = current) }
    }

    fun toggleCamera() {
        if (mutableState.value.kind != NovaCallKind.Video) return
        val current = !mutableState.value.cameraEnabled
        engine?.setCameraEnabled(current)
        update { it.copy(cameraEnabled = current) }
    }

    fun switchCamera() {
        engine?.switchCamera()
    }

    fun release() {
        if (released) return
        released = true
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
        signaling?.stop()
        signaling = null
        engine?.release()
        engine = null
        telecom.release()
    }

    private fun askForPermissions(kind: NovaCallKind) {
        if (mutableState.value.permissionsPending) return
        update { it.copy(permissionsPending = true) }
        requestPermissions(kind)
    }

    private fun createOutgoingCall(spec: NovaCallLaunchSpec.Outgoing) {
        update { it.copy(stage = "Starting ${if (spec.kind == NovaCallKind.Video) "video" else "voice"} call…", error = null) }
        scope.launch {
            when (val result = repository.createCall(spec.conversationId, spec.kind)) {
                is ApiResult.Success -> onSessionLoaded(result.value)
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) {
                        onSessionExpired()
                    } else {
                        update { it.copy(stage = "Call failed", error = result.message) }
                    }
                }
            }
        }
    }

    private fun loadExistingCall(callId: String) {
        scope.launch {
            when (val result = repository.call(callId)) {
                is ApiResult.Success -> {
                    val call = result.value
                    if (call.status.isTerminal) {
                        NovaCallNotification.cancel(appContext, call.id)
                        update { it.copy(session = call, stage = terminalLabel(call)) }
                        delay(650)
                        onFinished()
                    } else {
                        onSessionLoaded(call)
                    }
                }
                is ApiResult.Failure -> {
                    NovaCallNotification.cancel(appContext, callId)
                    if (result.statusCode == 401) onSessionExpired()
                    else update { it.copy(stage = "Call unavailable", error = result.message) }
                }
            }
        }
    }

    private fun onSessionLoaded(call: NovaCallSession) {
        update {
            it.copy(
                session = call,
                fallbackPeer = call.peer,
                fallbackKind = call.kind,
                stage = stageFor(call),
                cameraEnabled = call.kind == NovaCallKind.Video && cameraPermissionGranted,
            )
        }

        if (call.isCaller) {
            NovaCallNotification.showOngoing(appContext, call)
        }
        startTelecom(call)
        startSignaling(call)

        if (call.isCaller) {
            initializeEngineIfPossible(call)
            scheduleRingTimeout(call)
        }
    }

    private fun startTelecom(call: NovaCallSession) {
        if (telecomStarted) return
        telecomStarted = true
        telecom.start(
            scope = scope,
            call = call,
            onSystemAnswer = { accept() },
            onSystemDisconnect = {
                if (!ending) hangUp()
            },
            onSystemSetActive = {
                engine?.setMicrophoneEnabled(mutableState.value.microphoneEnabled)
            },
            onSystemSetInactive = {
                engine?.setMicrophoneEnabled(false)
            },
            onFailure = {
                // Calls still continue inside Nova if a device-specific Telecom integration fails.
            },
        )
    }

    private fun startSignaling(call: NovaCallSession) {
        signaling?.stop()
        signalingReady = false
        signaling = NovaCallSignalingClient(appContext, call.id).also { client ->
            client.start(
                scope = scope,
                onEvent = ::handleSignal,
                onStatus = { socketStatus ->
                    update { it.copy(socketStatus = socketStatus) }
                },
                onSessionExpired = onSessionExpired,
            )
        }
    }

    private fun initializeEngineIfPossible(call: NovaCallSession) {
        if (engine != null || !audioPermissionGranted) return
        scope.launch {
            when (val result = repository.iceConfig()) {
                is ApiResult.Success -> {
                    if (released || engine != null) return@launch
                    val created = NovaWebRtcEngine(
                        context = appContext,
                        kind = call.kind,
                        iceConfig = result.value,
                        listener = webRtcListener,
                    )
                    engine = created
                    if (!created.start()) {
                        engine = null
                        return@launch
                    }
                    update {
                        it.copy(
                            eglContext = created.eglContext,
                            microphoneEnabled = true,
                            cameraEnabled = call.kind == NovaCallKind.Video && cameraPermissionGranted,
                        )
                    }
                    pendingRemoteIce.toList().also { pendingRemoteIce.clear() }
                        .forEach(created::addRemoteIce)
                    if (call.isCaller) maybeCreateOffer()
                    else maybeCreateAnswer()
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired()
                    else update { it.copy(error = result.message) }
                }
            }
        }
    }

    private fun handleSignal(event: NovaCallSignalEvent) {
        when (event) {
            is NovaCallSignalEvent.Ready -> {
                signalingReady = true
                update { it.copy(session = event.call, stage = stageFor(event.call)) }
                flushLocalIce()
                queuedAction?.let { action ->
                    queuedAction = null
                    sendAction(action)
                }
                if (event.call.isCaller) {
                    maybeCreateOffer()
                } else {
                    if (queuedAction == null && pendingAccept) {
                        if (audioPermissionGranted) performAccept() else askForPermissions(event.call.kind)
                    }
                }
            }

            is NovaCallSignalEvent.Offer -> {
                pendingOffer = event.sdp
                maybeCreateAnswer()
            }

            is NovaCallSignalEvent.Answer -> {
                engine?.setRemoteAnswer(event.sdp)
            }

            is NovaCallSignalEvent.Ice -> {
                val candidate = IceCandidate(event.sdpMid, event.sdpMLineIndex, event.candidate)
                val currentEngine = engine
                if (currentEngine == null) pendingRemoteIce += candidate
                else currentEngine.addRemoteIce(candidate)
            }

            is NovaCallSignalEvent.State -> {
                update {
                    it.copy(
                        session = event.call,
                        stage = stageFor(event.call),
                    )
                }
                if (event.call.status == NovaCallStatus.Active) {
                    ringTimeoutJob?.cancel()
                    ringTimeoutJob = null
                    NovaCallNotification.showOngoing(appContext, event.call)
                    scope.launch { telecom.setActive() }
                } else if (event.call.status.isTerminal) {
                    finishTerminal(event.call)
                }
            }

            is NovaCallSignalEvent.Error -> {
                update { it.copy(error = event.detail) }
            }
        }
    }

    private fun performAccept() {
        val call = mutableState.value.session ?: return
        if (call.isCaller || call.status != NovaCallStatus.Ringing || acceptedLocally || ending) return
        pendingAccept = true
        initializeEngineIfPossible(call)
        if (!signalingReady) return
        acceptedLocally = true
        pendingAccept = false
        signaling?.accept()
        scope.launch { telecom.answer() }
        update { it.copy(stage = "Connecting…") }
        maybeCreateAnswer()
    }

    private fun maybeCreateOffer() {
        val call = mutableState.value.session ?: return
        val currentEngine = engine ?: return
        if (!call.isCaller || !signalingReady || offerCreating || offerSent || ending) return
        offerCreating = true
        currentEngine.createOffer { sdp ->
            scope.launch {
                offerCreating = false
                offerSent = signaling?.sendOffer(sdp) == true
                if (!offerSent) update { it.copy(error = "Call signaling is reconnecting…") }
            }
        }
    }

    private fun maybeCreateAnswer() {
        val call = mutableState.value.session ?: return
        val currentEngine = engine ?: return
        val offer = pendingOffer ?: return
        if (call.isCaller || !acceptedLocally || answerCreating || answerSent || ending) return
        answerCreating = true
        pendingOffer = null
        currentEngine.setRemoteOffer(offer) {
            currentEngine.createAnswer { sdp ->
                scope.launch {
                    answerCreating = false
                    answerSent = signaling?.sendAnswer(sdp) == true
                    if (!answerSent) update { it.copy(error = "Call signaling is reconnecting…") }
                }
            }
        }
    }

    private fun queueOrSendAction(action: String) {
        if (!signalingReady) {
            queuedAction = action
        } else {
            sendAction(action)
        }
    }

    private fun sendAction(action: String) {
        when (action) {
            "accept" -> signaling?.accept()
            "decline" -> signaling?.decline()
            "cancel" -> signaling?.cancel()
            "end" -> signaling?.end()
            "timeout" -> signaling?.timeout()
            "failed" -> signaling?.failed()
        }
    }

    private fun flushLocalIce() {
        if (!signalingReady) return
        val client = signaling ?: return
        val iterator = pendingLocalIce.iterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            if (client.sendIce(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex)) {
                iterator.remove()
            } else {
                break
            }
        }
    }

    private fun scheduleRingTimeout(call: NovaCallSession) {
        if (!call.isCaller) return
        ringTimeoutJob?.cancel()
        ringTimeoutJob = scope.launch {
            delay(call.ringTimeoutSeconds.coerceAtLeast(10) * 1_000L)
            val current = mutableState.value.session
            if (current?.status == NovaCallStatus.Ringing && !ending) {
                ending = true
                queueOrSendAction("timeout")
                update { it.copy(stage = "No answer") }
            }
        }
    }

    private fun finishTerminal(call: NovaCallSession) {
        if (released) return
        ending = true
        ringTimeoutJob?.cancel()
        NovaCallNotification.cancel(appContext, call.id)
        update { it.copy(session = call, stage = terminalLabel(call), connected = false) }
        scope.launch {
            telecom.disconnect(
                if (call.status == NovaCallStatus.Declined) DisconnectCause.REJECTED else DisconnectCause.REMOTE
            )
            delay(650)
            onFinished()
        }
    }

    private fun stageFor(call: NovaCallSession): String {
        return when (call.status) {
            NovaCallStatus.Ringing -> if (call.isCaller) "Calling…" else "Incoming ${if (call.kind == NovaCallKind.Video) "video" else "voice"} call"
            NovaCallStatus.Active -> if (mutableState.value.connected) "Connected" else "Connecting…"
            else -> terminalLabel(call)
        }
    }

    private fun terminalLabel(call: NovaCallSession): String {
        return when (call.status) {
            NovaCallStatus.Declined -> "Call declined"
            NovaCallStatus.Canceled -> "Call canceled"
            NovaCallStatus.Missed -> "No answer"
            NovaCallStatus.Failed -> "Call failed"
            NovaCallStatus.Ended -> "Call ended"
            else -> "Call ended"
        }
    }

    private fun update(transform: (NovaCallUiState) -> NovaCallUiState) {
        mutableState.value = transform(mutableState.value)
    }

    private val webRtcListener = object : NovaWebRtcEngine.Listener {
        override fun onLocalIceCandidate(candidate: IceCandidate) {
            scope.launch {
                val client = signaling
                if (!signalingReady || client == null || !client.sendIce(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex)) {
                    pendingLocalIce += candidate
                }
            }
        }

        override fun onConnectionState(state: PeerConnection.PeerConnectionState) {
            scope.launch {
                when (state) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        update { it.copy(stage = "Connected", connected = true, error = null) }
                    }
                    PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        update { it.copy(stage = "Reconnecting…", connected = false) }
                    }
                    PeerConnection.PeerConnectionState.FAILED -> {
                        if (!ending) {
                            ending = true
                            queueOrSendAction("failed")
                            update { it.copy(stage = "Connection failed", connected = false) }
                        }
                    }
                    PeerConnection.PeerConnectionState.CLOSED -> {
                        update { it.copy(connected = false) }
                    }
                    else -> Unit
                }
            }
        }

        override fun onLocalVideoTrack(track: VideoTrack?) {
            scope.launch { update { it.copy(localVideoTrack = track, cameraEnabled = track != null) } }
        }

        override fun onRemoteVideoTrack(track: VideoTrack?) {
            scope.launch { update { it.copy(remoteVideoTrack = track) } }
        }

        override fun onError(message: String) {
            scope.launch { update { it.copy(error = message) } }
        }
    }

    companion object {
        const val ACTION_OPEN = "open"
        const val ACTION_ANSWER = "answer"
        const val ACTION_DECLINE = "decline"
        const val ACTION_END = "end"
    }
}
