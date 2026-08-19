package com.nova.app.feature.calls

import android.content.Context
import android.os.SystemClock
import android.telecom.DisconnectCause
import android.util.Log
import com.nova.app.core.calls.NovaCallActionDispatcher
import com.nova.app.core.calls.NovaCallDuration
import com.nova.app.core.calls.NovaCallNotification
import com.nova.app.core.calls.NovaCallSignalEvent
import com.nova.app.core.calls.NovaCallSocketStatus
import com.nova.app.core.calls.NovaTelecomBridge
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.calls.data.CallRepository
import com.nova.app.feature.calls.domain.model.NovaCallKind
import com.nova.app.feature.calls.domain.model.NovaCallPerson
import com.nova.app.feature.calls.domain.model.NovaCallSession
import com.nova.app.feature.calls.domain.model.NovaCallStatus
import com.nova.app.feature.calls.signaling.CallSignaling
import com.nova.app.feature.calls.webrtc.CallWebRtcEngine
import com.nova.app.feature.calls.webrtc.CallWebRtcListener
import com.nova.app.feature.calls.webrtc.model.NovaCallAudioQualityDelta
import com.nova.app.feature.calls.webrtc.model.NovaCallAudioQualitySnapshot
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
import java.util.UUID


sealed interface CallLaunchSpec {
    data class Outgoing(
        val conversationId: Long,
        val kind: NovaCallKind,
        val peer: NovaCallPerson,
    ) : CallLaunchSpec

    data class Existing(
        val callId: String,
        val requestedAction: String,
        val fallbackPeer: NovaCallPerson? = null,
        val fallbackKind: NovaCallKind = NovaCallKind.Audio,
        val fallbackConversationId: Long = -1L,
    ) : CallLaunchSpec
}


enum class CallPhase {
    Preparing,
    RingingIncoming,
    RingingOutgoing,
    Connecting,
    Active,
    Reconnecting,
    Terminal,
}


data class CallUiState(
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
    val phase: CallPhase
        get() = when {
            isTerminal -> CallPhase.Terminal
            session?.status == NovaCallStatus.Ringing && session.isCaller -> CallPhase.RingingOutgoing
            session?.status == NovaCallStatus.Ringing -> CallPhase.RingingIncoming
            session?.status == NovaCallStatus.Active && connected -> CallPhase.Active
            session?.status == NovaCallStatus.Active && stage == "Reconnecting…" -> CallPhase.Reconnecting
            session?.status == NovaCallStatus.Active -> CallPhase.Connecting
            else -> CallPhase.Preparing
        }
}


class CallStateOwner(
    context: Context,
    private val scope: CoroutineScope,
    private val launchSpec: CallLaunchSpec,
    private val repository: CallRepository,
    private val signalingFactory: (String) -> CallSignaling,
    private val webRtcFactory: (NovaCallKind, com.nova.app.feature.calls.domain.model.NovaIceConfig, CallWebRtcListener) -> CallWebRtcEngine,
    private val requestPermissions: (NovaCallKind) -> Unit,
    private val onFinished: () -> Unit,
    private val onSessionExpired: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val telecom = NovaTelecomBridge(appContext)
    private val mutableState = MutableStateFlow(
        CallUiState(
            fallbackPeer = when (launchSpec) {
                is CallLaunchSpec.Outgoing -> launchSpec.peer
                is CallLaunchSpec.Existing -> launchSpec.fallbackPeer
            },
            fallbackKind = when (launchSpec) {
                is CallLaunchSpec.Outgoing -> launchSpec.kind
                is CallLaunchSpec.Existing -> launchSpec.fallbackKind
            },
            stage = when (launchSpec) {
                is CallLaunchSpec.Outgoing -> "Preparing call…"
                is CallLaunchSpec.Existing -> "Loading call…"
            },
        )
    )
    val state = mutableState.asStateFlow()

    private var signaling: CallSignaling? = null
    private var engine: CallWebRtcEngine? = null
    private var signalingReady = false
    private var permissionsRequested = false
    private var audioPermissionGranted = false
    private var cameraPermissionGranted = false
    private var pendingAccept = false
    private var acceptedLocally = false
    private var pendingOffer: String? = null
    private var pendingOfferNegotiationId: String? = null
    private var awaitingAnswerNegotiationId: String? = null
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
    private var durationJob: Job? = null
    private var callStartedAtEpochMs: Long? = null
    private var recoveryJob: Job? = null
    private var recoveryOfferInFlight = false
    private var qualityMonitorJob: Job? = null
    private var lastAudioQualitySnapshot: NovaCallAudioQualitySnapshot? = null
    private var inboundAudioStallStartedAtMs: Long? = null
    private var lastMediaQualityRecoveryAtMs = 0L
    private var queuedAction: String? = when (launchSpec) {
        is CallLaunchSpec.Existing -> launchSpec.requestedAction.takeIf { it.isNotBlank() }
        is CallLaunchSpec.Outgoing -> null
    }

    fun start() {
        when (launchSpec) {
            is CallLaunchSpec.Outgoing -> askForPermissions(launchSpec.kind)
            is CallLaunchSpec.Existing -> loadExistingCall(launchSpec.callId)
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
                val call = mutableState.value.session
                pendingAccept = false
                if (call != null) finishLocally(call, "decline", "Call declined", DisconnectCause.REJECTED)
            } else if (launchSpec is CallLaunchSpec.Outgoing) {
                scope.launch { delay(900); onFinished() }
            }
            return
        }
        when (launchSpec) {
            is CallLaunchSpec.Outgoing -> if (mutableState.value.session == null) createOutgoingCall(launchSpec)
            is CallLaunchSpec.Existing -> if (pendingAccept) performAccept()
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
        if (call.isCaller || call.status.isTerminal || ending) return
        pendingAccept = true
        if (!audioPermissionGranted) { askForPermissions(call.kind); return }
        performAccept()
    }

    fun decline() {
        val call = mutableState.value.session ?: return
        if (call.isCaller || call.status != NovaCallStatus.Ringing || ending) return
        ending = true
        finishLocally(call, "decline", "Call declined", DisconnectCause.REJECTED)
    }

    fun hangUp() {
        if (ending) return
        val call = mutableState.value.session ?: run { onFinished(); return }
        ending = true
        val action = when {
            call.status == NovaCallStatus.Active -> "end"
            call.status == NovaCallStatus.Ringing && call.isCaller -> "cancel"
            call.status == NovaCallStatus.Ringing -> "decline"
            else -> null
        }
        if (action == null) {
            NovaCallNotification.cancel(appContext, call.id)
            onFinished()
            return
        }
        finishLocally(call, action, "Call ended", DisconnectCause.LOCAL)
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

    fun switchCamera() = engine?.switchCamera() ?: Unit

    fun release() {
        if (released) return
        released = true
        ringTimeoutJob?.cancel(); ringTimeoutJob = null
        stopCallDurationTicker()
        recoveryJob?.cancel(); recoveryJob = null
        qualityMonitorJob?.cancel(); qualityMonitorJob = null
        lastAudioQualitySnapshot = null
        inboundAudioStallStartedAtMs = null
        recoveryOfferInFlight = false
        awaitingAnswerNegotiationId = null
        pendingOfferNegotiationId = null
        signaling?.stop(); signaling = null
        engine?.release(); engine = null
        telecom.release()
    }

    private fun askForPermissions(kind: NovaCallKind) {
        if (mutableState.value.permissionsPending) return
        update { it.copy(permissionsPending = true) }
        requestPermissions(kind)
    }

    private fun createOutgoingCall(spec: CallLaunchSpec.Outgoing) {
        update { it.copy(stage = "Starting ${if (spec.kind == NovaCallKind.Video) "video" else "voice"} call…", error = null) }
        scope.launch {
            when (val result = repository.createCall(spec.conversationId, spec.kind)) {
                is ApiResult.Success -> onSessionLoaded(result.value)
                is ApiResult.Failure -> if (result.statusCode == 401) onSessionExpired() else update { it.copy(stage = "Call failed", error = result.message) }
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
                    } else onSessionLoaded(call)
                }
                is ApiResult.Failure -> {
                    NovaCallNotification.cancel(appContext, callId)
                    if (result.statusCode == 401) onSessionExpired() else update { it.copy(stage = "Call unavailable", error = result.message) }
                }
            }
        }
    }

    private fun onSessionLoaded(call: NovaCallSession) {
        if (call.status == NovaCallStatus.Active) ensureCallDurationStart(call)
        update { it.copy(session = call, fallbackPeer = call.peer, fallbackKind = call.kind, stage = stageFor(call), cameraEnabled = call.kind == NovaCallKind.Video && cameraPermissionGranted) }
        if (call.isCaller) NovaCallNotification.showOngoing(appContext, call)
        startTelecom(call)
        startSignaling(call)
        if (call.isCaller) { initializeEngineIfPossible(call); scheduleRingTimeout(call) }
    }

    private fun startTelecom(call: NovaCallSession) {
        if (telecomStarted) return
        telecomStarted = true
        telecom.start(
            scope = scope,
            call = call,
            onSystemAnswer = { accept() },
            onSystemDisconnect = { if (!ending) hangUp() },
            onSystemSetActive = { engine?.setMicrophoneEnabled(mutableState.value.microphoneEnabled) },
            onSystemSetInactive = { engine?.setMicrophoneEnabled(false) },
            onFailure = { },
        )
    }

    private fun startSignaling(call: NovaCallSession) {
        signaling?.stop()
        signalingReady = false
        signaling = signalingFactory(call.id).also { client ->
            client.start(scope, ::handleSignal, { update { state -> state.copy(socketStatus = it) } }, onSessionExpired)
        }
    }

    private fun initializeEngineIfPossible(call: NovaCallSession) {
        if (engine != null || !audioPermissionGranted) return
        scope.launch {
            when (val result = repository.iceConfig()) {
                is ApiResult.Success -> {
                    if (released || engine != null) return@launch
                    val created = webRtcFactory(call.kind, result.value, webRtcListener)
                    engine = created
                    if (!created.start()) { engine = null; return@launch }
                    update { it.copy(eglContext = created.eglContext, microphoneEnabled = true, cameraEnabled = call.kind == NovaCallKind.Video && cameraPermissionGranted) }
                    pendingRemoteIce.toList().also { pendingRemoteIce.clear() }.forEach(created::addRemoteIce)
                    if (call.isCaller) maybeCreateOffer() else maybeCreateAnswer()
                }
                is ApiResult.Failure -> if (result.statusCode == 401) onSessionExpired() else update { it.copy(error = result.message) }
            }
        }
    }

    private fun handleSignal(event: NovaCallSignalEvent) {
        when (event) {
            is NovaCallSignalEvent.Ready -> {
                signalingReady = true
                if (event.call.status == NovaCallStatus.Active) ensureCallDurationStart(event.call)
                update { it.copy(session = event.call, stage = stageFor(event.call)) }
                flushLocalIce()
                queuedAction?.let { action -> queuedAction = null; sendAction(action) }
                if (event.call.isCaller) maybeCreateOffer() else if (pendingAccept || acceptedLocally) {
                    if (audioPermissionGranted) performAccept() else askForPermissions(event.call.kind)
                }
            }
            is NovaCallSignalEvent.Offer -> {
                answerCreating = false; answerSent = false
                pendingOffer = event.sdp; pendingOfferNegotiationId = event.negotiationId
                maybeCreateAnswer()
            }
            is NovaCallSignalEvent.Answer -> {
                val incomingId = event.negotiationId
                if (incomingId != null && incomingId != awaitingAnswerNegotiationId) return
                engine?.setRemoteAnswer(event.sdp) { awaitingAnswerNegotiationId = null; recoveryOfferInFlight = false }
            }
            is NovaCallSignalEvent.Ice -> {
                val candidate = IceCandidate(event.sdpMid, event.sdpMLineIndex, event.candidate)
                engine?.addRemoteIce(candidate) ?: run { pendingRemoteIce += candidate }
            }
            NovaCallSignalEvent.IceRestartRequested -> {
                val call = mutableState.value.session
                if (call?.status == NovaCallStatus.Active && call.isCaller && !ending) beginIceRestart()
            }
            is NovaCallSignalEvent.State -> {
                if (event.call.status == NovaCallStatus.Active) ensureCallDurationStart(event.call)
                update { it.copy(session = event.call, stage = stageFor(event.call)) }
                if (event.call.status == NovaCallStatus.Active) {
                    ringTimeoutJob?.cancel(); ringTimeoutJob = null
                    NovaCallNotification.showOngoing(appContext, event.call)
                    scope.launch { telecom.setActive() }
                    if (!event.call.isCaller && acceptedLocally) { initializeEngineIfPossible(event.call); maybeCreateAnswer() }
                } else if (event.call.status.isTerminal) finishTerminal(event.call)
            }
            is NovaCallSignalEvent.Error -> update { it.copy(error = event.detail) }
        }
    }

    private fun performAccept() {
        val call = mutableState.value.session ?: return
        if (call.isCaller || call.status.isTerminal || ending) return
        pendingAccept = true
        initializeEngineIfPossible(call)
        if (!acceptedLocally) {
            acceptedLocally = true
            NovaCallNotification.showOngoing(appContext, call.copy(status = NovaCallStatus.Active))
            NovaCallActionDispatcher.dispatch(appContext, call.id, "accept")
            scope.launch { telecom.answer() }
            update { it.copy(stage = "Connecting…") }
        }
        if (!signalingReady) return
        pendingAccept = false
        signaling?.accept()
        maybeCreateAnswer()
    }

    private fun maybeCreateOffer() {
        val call = mutableState.value.session ?: return
        val currentEngine = engine ?: return
        if (!call.isCaller || !signalingReady || offerCreating || offerSent || ending) return
        val negotiationId = UUID.randomUUID().toString()
        offerCreating = true
        currentEngine.createOffer { sdp -> scope.launch {
            offerCreating = false; awaitingAnswerNegotiationId = negotiationId
            offerSent = signaling?.sendOffer(sdp, negotiationId) == true
            if (!offerSent) { awaitingAnswerNegotiationId = null; update { it.copy(error = "Call signaling is reconnecting…") } }
        } }
    }

    private fun beginIceRestart() {
        val call = mutableState.value.session ?: return
        val currentEngine = engine ?: return
        if (!call.isCaller || call.status != NovaCallStatus.Active || !signalingReady || ending || released) return
        if (recoveryOfferInFlight || awaitingAnswerNegotiationId != null) return
        val negotiationId = UUID.randomUUID().toString()
        recoveryOfferInFlight = true
        currentEngine.createIceRestartOffer { sdp -> scope.launch {
            recoveryOfferInFlight = false; awaitingAnswerNegotiationId = negotiationId
            val queued = signaling?.sendOffer(sdp, negotiationId) == true
            if (!queued) { awaitingAnswerNegotiationId = null; update { it.copy(error = "Call recovery is waiting for signaling…") } }
        } }
    }

    private fun maybeCreateAnswer() {
        val call = mutableState.value.session ?: return
        val currentEngine = engine ?: return
        val offer = pendingOffer ?: return
        if (call.isCaller || !acceptedLocally || answerCreating || answerSent || ending) return
        val negotiationId = pendingOfferNegotiationId
        answerCreating = true; pendingOffer = null; pendingOfferNegotiationId = null
        currentEngine.setRemoteOffer(offer) { currentEngine.createAnswer { sdp -> scope.launch {
            answerCreating = false
            answerSent = signaling?.sendAnswer(sdp, negotiationId) == true
            if (!answerSent) update { it.copy(error = "Call signaling is reconnecting…") }
        } } }
    }

    private fun scheduleIceRecovery(immediate: Boolean) {
        val call = mutableState.value.session ?: return
        if (call.status != NovaCallStatus.Active || ending || released) return
        update { it.copy(stage = "Reconnecting…", connected = false) }
        if (recoveryJob?.isActive == true) return
        recoveryJob = scope.launch {
            if (!immediate) delay(1_200L)
            repeat(2) { attempt ->
                val current = mutableState.value.session
                if (released || ending || mutableState.value.connected || current?.status != NovaCallStatus.Active) return@launch
                if (current.isCaller) beginIceRestart() else signaling?.requestIceRestart()
                delay(if (attempt == 0) 6_000L else 8_000L)
                if (mutableState.value.connected) return@launch
            }
            val current = mutableState.value.session
            if (!released && !ending && !mutableState.value.connected && current?.status == NovaCallStatus.Active) {
                ending = true
                finishLocally(current, "failed", "Connection failed", DisconnectCause.ERROR)
            }
        }
    }

    private fun startAudioQualityMonitor() {
        if (qualityMonitorJob?.isActive == true || released || ending) return
        lastAudioQualitySnapshot = null; inboundAudioStallStartedAtMs = null
        qualityMonitorJob = scope.launch {
            while (!released && !ending) {
                val currentCall = mutableState.value.session
                val currentEngine = engine
                if (currentCall?.status == NovaCallStatus.Active && currentEngine != null) currentEngine.collectAudioQualitySnapshot { snapshot -> scope.launch { handleAudioQualitySnapshot(snapshot) } }
                delay(QUALITY_POLL_INTERVAL_MS)
            }
        }
    }

    private fun handleAudioQualitySnapshot(snapshot: NovaCallAudioQualitySnapshot) {
        val previous = lastAudioQualitySnapshot
        lastAudioQualitySnapshot = snapshot
        if (previous == null) return
        val delta = NovaCallAudioQualityDelta.between(previous, snapshot)
        Log.i(QUALITY_TAG, "inKbps=${delta.inboundKbps} outKbps=${delta.outboundKbps} lossPct=${delta.packetLossPercent} jitterMs=${delta.jitterMs} rttMs=${delta.roundTripTimeMs} concealedDelta=${delta.concealedSamplesDelta}")
        val call = mutableState.value.session
        if (call?.status != NovaCallStatus.Active || !mutableState.value.connected || recoveryJob?.isActive == true || ending || released) { inboundAudioStallStartedAtMs = null; return }
        val inboundStalled = delta.inboundPacketsDelta == 0L
        val outboundAlive = delta.outboundPacketsDelta?.let { it > 0L } == true
        if (!inboundStalled || !outboundAlive) { inboundAudioStallStartedAtMs = null; return }
        val now = SystemClock.elapsedRealtime()
        val startedAt = inboundAudioStallStartedAtMs ?: now.also { inboundAudioStallStartedAtMs = it }
        if (now - startedAt < INBOUND_AUDIO_STALL_MS || now - lastMediaQualityRecoveryAtMs < MEDIA_RECOVERY_COOLDOWN_MS || recoveryOfferInFlight || awaitingAnswerNegotiationId != null) return
        lastMediaQualityRecoveryAtMs = now; inboundAudioStallStartedAtMs = null
        Log.w(QUALITY_TAG, "Inbound audio RTP stalled while outbound RTP is alive; requesting one ICE recovery")
        requestMediaQualityRecovery(call)
    }

    private fun requestMediaQualityRecovery(call: NovaCallSession) {
        if (call.status != NovaCallStatus.Active || ending || released || !signalingReady) return
        if (call.isCaller) beginIceRestart() else signaling?.requestIceRestart()
    }

    private fun stopAudioQualityMonitor() { qualityMonitorJob?.cancel(); qualityMonitorJob = null; lastAudioQualitySnapshot = null; inboundAudioStallStartedAtMs = null }

    private fun ensureCallDurationStart(call: NovaCallSession): Long {
        callStartedAtEpochMs?.let { return it }
        val startedAt = NovaCallDuration.answeredAtEpochMs(call.answeredAt) ?: System.currentTimeMillis()
        callStartedAtEpochMs = startedAt
        return startedAt
    }

    private fun startCallDurationTicker() {
        val call = mutableState.value.session ?: return
        if (call.status != NovaCallStatus.Active || released || ending) return
        ensureCallDurationStart(call)
        if (durationJob?.isActive == true) return
        durationJob = scope.launch {
            while (!released && !ending) {
                val currentState = mutableState.value
                val currentCall = currentState.session
                if (currentCall?.status != NovaCallStatus.Active) break
                if (currentState.connected) {
                    val startedAt = ensureCallDurationStart(currentCall)
                    update { latest -> if (latest.connected && latest.session?.status == NovaCallStatus.Active) latest.copy(stage = NovaCallDuration.label(startedAt)) else latest }
                }
                delay(1_000L)
            }
        }
    }

    private fun stopCallDurationTicker() { durationJob?.cancel(); durationJob = null; callStartedAtEpochMs = null }
    private fun activeCallStage(call: NovaCallSession): String = NovaCallDuration.label(ensureCallDurationStart(call))
    private fun queueOrSendAction(action: String) { if (!signalingReady) queuedAction = action else sendAction(action) }
    private fun sendAction(action: String) { when (action) { "answer" -> accept(); "accept" -> signaling?.accept(); "decline" -> signaling?.decline(); "cancel" -> signaling?.cancel(); "end" -> signaling?.end(); "timeout" -> signaling?.timeout(); "failed" -> signaling?.failed() } }

    private fun finishLocally(call: NovaCallSession, action: String, stage: String, disconnectCause: Int) {
        ringTimeoutJob?.cancel(); ringTimeoutJob = null
        recoveryJob?.cancel(); recoveryJob = null
        stopAudioQualityMonitor(); stopCallDurationTicker()
        recoveryOfferInFlight = false; awaitingAnswerNegotiationId = null; pendingOfferNegotiationId = null
        NovaCallNotification.cancel(appContext, call.id)
        queueOrSendAction(action)
        NovaCallActionDispatcher.dispatch(appContext, call.id, action)
        update { it.copy(stage = stage, connected = false) }
        scope.launch { telecom.disconnect(disconnectCause) }
        onFinished()
    }

    private fun flushLocalIce() {
        if (!signalingReady) return
        val client = signaling ?: return
        val iterator = pendingLocalIce.iterator()
        while (iterator.hasNext()) {
            val candidate = iterator.next()
            if (client.sendIce(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex)) iterator.remove() else break
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
                NovaCallNotification.cancel(appContext, current.id)
                queueOrSendAction("timeout")
                NovaCallActionDispatcher.dispatch(appContext, current.id, "timeout")
                update { it.copy(stage = "No answer", connected = false) }
                telecom.disconnect(DisconnectCause.LOCAL)
                delay(650)
                onFinished()
            }
        }
    }

    private fun finishTerminal(call: NovaCallSession) {
        if (released) return
        ending = true
        ringTimeoutJob?.cancel(); recoveryJob?.cancel(); recoveryJob = null
        stopAudioQualityMonitor(); stopCallDurationTicker()
        recoveryOfferInFlight = false; awaitingAnswerNegotiationId = null; pendingOfferNegotiationId = null
        NovaCallNotification.cancel(appContext, call.id)
        update { it.copy(session = call, stage = terminalLabel(call), connected = false) }
        scope.launch {
            telecom.disconnect(if (call.status == NovaCallStatus.Declined) DisconnectCause.REJECTED else DisconnectCause.REMOTE)
            delay(650)
            onFinished()
        }
    }

    private fun stageFor(call: NovaCallSession): String = when (call.status) {
        NovaCallStatus.Ringing -> if (call.isCaller) "Calling…" else "Incoming ${if (call.kind == NovaCallKind.Video) "video" else "voice"} call"
        NovaCallStatus.Active -> when { mutableState.value.connected -> activeCallStage(call); recoveryJob?.isActive == true -> "Reconnecting…"; else -> "Connecting…" }
        else -> terminalLabel(call)
    }

    private fun terminalLabel(call: NovaCallSession): String = when (call.status) {
        NovaCallStatus.Declined -> "Call declined"
        NovaCallStatus.Canceled -> "Call canceled"
        NovaCallStatus.Missed -> "No answer"
        NovaCallStatus.Failed -> "Call failed"
        NovaCallStatus.Ended -> "Call ended"
        else -> "Call ended"
    }

    private fun update(transform: (CallUiState) -> CallUiState) { mutableState.value = transform(mutableState.value) }

    private val webRtcListener = object : CallWebRtcListener {
        override fun onLocalIceCandidate(candidate: IceCandidate) {
            scope.launch {
                val client = signaling
                if (!signalingReady || client == null || !client.sendIce(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex)) pendingLocalIce += candidate
            }
        }
        override fun onConnectionState(state: PeerConnection.PeerConnectionState) {
            scope.launch {
                when (state) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        recoveryJob?.cancel(); recoveryJob = null; recoveryOfferInFlight = false; awaitingAnswerNegotiationId = null
                        val call = mutableState.value.session
                        val connectedStage = if (call?.status == NovaCallStatus.Active) activeCallStage(call) else "Connected"
                        update { it.copy(stage = connectedStage, connected = true, error = null) }
                        startCallDurationTicker(); startAudioQualityMonitor()
                    }
                    PeerConnection.PeerConnectionState.DISCONNECTED -> { inboundAudioStallStartedAtMs = null; scheduleIceRecovery(false) }
                    PeerConnection.PeerConnectionState.FAILED -> { inboundAudioStallStartedAtMs = null; scheduleIceRecovery(true) }
                    PeerConnection.PeerConnectionState.CLOSED -> { stopAudioQualityMonitor(); update { it.copy(connected = false) } }
                    else -> Unit
                }
            }
        }
        override fun onLocalVideoTrack(track: VideoTrack?) { scope.launch { update { it.copy(localVideoTrack = track, cameraEnabled = track != null) } } }
        override fun onRemoteVideoTrack(track: VideoTrack?) { scope.launch { update { it.copy(remoteVideoTrack = track) } } }
        override fun onError(message: String) { scope.launch { update { it.copy(error = message) } } }
    }

    companion object {
        const val ACTION_OPEN = "open"
        const val ACTION_ANSWER = "answer"
        const val ACTION_DECLINE = "decline"
        const val ACTION_END = "end"
        private const val QUALITY_TAG = "NovaCallQuality"
        private const val QUALITY_POLL_INTERVAL_MS = 2_500L
        private const val INBOUND_AUDIO_STALL_MS = 8_000L
        private const val MEDIA_RECOVERY_COOLDOWN_MS = 20_000L
    }
}
