package com.nova.app.core.calls

import android.content.Context
import com.nova.app.core.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit


enum class NovaCallSocketStatus {
    Connecting,
    Live,
    Reconnecting,
    Offline,
}


sealed interface NovaCallSignalEvent {
    data class Ready(val call: NovaCallSession) : NovaCallSignalEvent
    data class Offer(val sdp: String) : NovaCallSignalEvent
    data class Answer(val sdp: String) : NovaCallSignalEvent
    data class Ice(
        val candidate: String,
        val sdpMid: String,
        val sdpMLineIndex: Int,
    ) : NovaCallSignalEvent
    data object IceRestartRequested : NovaCallSignalEvent
    data class State(val call: NovaCallSession) : NovaCallSignalEvent
    data class Error(val detail: String) : NovaCallSignalEvent
}


class NovaCallSignalingClient(
    context: Context,
    private val callId: String,
    private val repository: NovaCallRepository = NovaCallRepository(context.applicationContext),
) {
    private var socket: WebSocket? = null
    private var scope: CoroutineScope? = null
    private var reconnectJob: Job? = null
    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null
    private var stopped = true
    private var reconnectAttempt = 0
    private var peerReady = false
    private val pendingPeerSignals = ArrayDeque<String>()
    private var onEvent: ((NovaCallSignalEvent) -> Unit)? = null
    private var onStatus: ((NovaCallSocketStatus) -> Unit)? = null
    private var onSessionExpired: (() -> Unit)? = null

    fun start(
        scope: CoroutineScope,
        onEvent: (NovaCallSignalEvent) -> Unit,
        onStatus: (NovaCallSocketStatus) -> Unit,
        onSessionExpired: () -> Unit,
    ) {
        stop()
        stopped = false
        reconnectAttempt = 0
        peerReady = false
        synchronized(pendingPeerSignals) { pendingPeerSignals.clear() }
        this.scope = scope
        this.onEvent = onEvent
        this.onStatus = onStatus
        this.onSessionExpired = onSessionExpired
        connect(initial = true)
    }

    fun stop() {
        stopped = true
        peerReady = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        reconnectJob?.cancel()
        reconnectJob = null
        connectionJob?.cancel()
        connectionJob = null
        socket?.close(1000, "Call closed")
        socket = null
        synchronized(pendingPeerSignals) { pendingPeerSignals.clear() }
    }

    fun sendOffer(sdp: String) = sendPeerSignal(JSONObject().put("type", "call.offer").put("sdp", sdp))
    fun sendAnswer(sdp: String) = sendPeerSignal(JSONObject().put("type", "call.answer").put("sdp", sdp))

    fun sendIce(candidate: String, sdpMid: String?, sdpMLineIndex: Int) = sendPeerSignal(
        JSONObject()
            .put("type", "call.ice")
            .put("candidate", candidate)
            .put("sdp_mid", sdpMid.orEmpty())
            .put("sdp_mline_index", sdpMLineIndex)
    )

    fun requestIceRestart() = sendPeerSignal(JSONObject().put("type", "call.ice_restart"))

    fun accept() = sendType("call.accept")
    fun decline() = sendType("call.decline")
    fun cancel() = sendType("call.cancel")
    fun end() = sendType("call.end")
    fun timeout() = sendType("call.timeout")
    fun failed() = sendType("call.failed")

    private fun sendType(type: String) = send(JSONObject().put("type", type))

    private fun sendPeerSignal(json: JSONObject): Boolean {
        val encoded = json.toString()
        if (peerReady && !stopped && socket?.send(encoded) == true) {
            return true
        }
        synchronized(pendingPeerSignals) {
            if (pendingPeerSignals.size >= MAX_PENDING_PEER_SIGNALS) {
                pendingPeerSignals.removeFirstOrNull()
            }
            pendingPeerSignals.addLast(encoded)
        }
        return true
    }

    private fun flushPeerSignals() {
        if (!peerReady || stopped) return
        synchronized(pendingPeerSignals) {
            while (pendingPeerSignals.isNotEmpty()) {
                val next = pendingPeerSignals.first()
                if (socket?.send(next) != true) return
                pendingPeerSignals.removeFirst()
            }
        }
    }

    private fun send(json: JSONObject): Boolean {
        if (stopped) return false
        return socket?.send(json.toString()) == true
    }

    private fun connect(initial: Boolean) {
        if (stopped) return
        peerReady = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        emitStatus(if (initial) NovaCallSocketStatus.Connecting else NovaCallSocketStatus.Reconnecting)
        connectionJob?.cancel()
        connectionJob = scope?.launch {
            when (val token = repository.realtimeAccessToken()) {
                is ApiResult.Success -> openSocket(token.value)
                is ApiResult.Failure -> {
                    if (token.statusCode == 401) {
                        onSessionExpired?.invoke()
                    } else {
                        emitStatus(NovaCallSocketStatus.Offline)
                        scheduleReconnect()
                    }
                }
            }
        }
    }

    private fun openSocket(accessToken: String) {
        if (stopped) return
        val request = Request.Builder()
            .url("${NovaCallRepository.PRODUCTION_WS_URL}calls/$callId/")
            .header("Authorization", "Bearer $accessToken")
            .build()

        socket = sharedClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    reconnectAttempt = 0
                    peerReady = false
                    emitStatus(NovaCallSocketStatus.Live)
                    startHeartbeat(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val json = runCatching { JSONObject(text) }.getOrNull() ?: return
                    parseEvent(json)?.let(::emitEvent)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (socket === webSocket) socket = null
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                    peerReady = false
                    if (stopped) return
                    if (code == 4401) {
                        onSessionExpired?.invoke()
                    } else if (code == 4403) {
                        emitEvent(NovaCallSignalEvent.Error("This call is no longer available."))
                    } else {
                        scheduleReconnect()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (socket === webSocket) socket = null
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                    peerReady = false
                    if (!stopped) {
                        emitStatus(NovaCallSocketStatus.Offline)
                        scheduleReconnect()
                    }
                }
            }
        )
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope?.launch {
            while (isActive && !stopped && socket === webSocket) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (stopped || socket !== webSocket) break
                if (!webSocket.send(HEARTBEAT_MESSAGE)) {
                    break
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (stopped || reconnectJob?.isActive == true) return
        val waitMs = when (reconnectAttempt.coerceAtMost(4)) {
            0 -> 700L
            1 -> 1_500L
            2 -> 3_000L
            3 -> 5_000L
            else -> 8_000L
        }
        reconnectAttempt += 1
        emitStatus(NovaCallSocketStatus.Reconnecting)
        reconnectJob = scope?.launch {
            delay(waitMs)
            reconnectJob = null
            connect(initial = false)
        }
    }

    private fun parseEvent(json: JSONObject): NovaCallSignalEvent? {
        return when (json.optString("type")) {
            "call.ready" -> NovaCallSignalEvent.Ready(parseCall(json.optJSONObject("call") ?: return null))
            "call.peer_ready" -> {
                peerReady = true
                flushPeerSignals()
                null
            }
            "call.state" -> NovaCallSignalEvent.State(parseCall(json.optJSONObject("call") ?: return null))
            "call.offer" -> NovaCallSignalEvent.Offer(json.optString("sdp")).takeIf { it.sdp.isNotBlank() }
            "call.answer" -> NovaCallSignalEvent.Answer(json.optString("sdp")).takeIf { it.sdp.isNotBlank() }
            "call.ice" -> {
                val candidate = json.optString("candidate")
                if (candidate.isBlank()) null else NovaCallSignalEvent.Ice(
                    candidate = candidate,
                    sdpMid = json.optString("sdp_mid"),
                    sdpMLineIndex = json.optInt("sdp_mline_index"),
                )
            }
            "call.ice_restart" -> NovaCallSignalEvent.IceRestartRequested
            "call.error" -> NovaCallSignalEvent.Error(json.optString("detail").ifBlank { "Call signaling failed." })
            else -> null
        }
    }

    private fun parseCall(json: JSONObject): NovaCallSession {
        return NovaCallSession(
            id = json.optString("id"),
            conversationId = json.optLong("conversation_id"),
            kind = NovaCallKind.fromWire(json.optString("kind")),
            status = NovaCallStatus.fromWire(json.optString("status")),
            caller = parsePerson(json.optJSONObject("caller") ?: JSONObject()),
            callee = parsePerson(json.optJSONObject("callee") ?: JSONObject()),
            peer = parsePerson(json.optJSONObject("peer") ?: JSONObject()),
            isCaller = json.optBoolean("is_caller"),
            createdAt = json.optString("created_at"),
            answeredAt = json.optString("answered_at").takeIf { it.isNotBlank() && it != "null" },
            endedAt = json.optString("ended_at").takeIf { it.isNotBlank() && it != "null" },
            endReason = json.optString("end_reason"),
            ringTimeoutSeconds = json.optInt("ring_timeout_seconds", 45),
        )
    }

    private fun parsePerson(json: JSONObject): NovaCallPerson = NovaCallPerson(
        id = json.optLong("id"),
        username = json.optString("username"),
        name = json.optString("name"),
        avatarUrl = json.optString("avatar_url"),
    )

    private fun emitEvent(event: NovaCallSignalEvent) {
        scope?.launch { if (!stopped) onEvent?.invoke(event) }
    }

    private fun emitStatus(status: NovaCallSocketStatus) {
        scope?.launch { if (!stopped) onStatus?.invoke(status) }
    }

    private companion object {
        const val MAX_PENDING_PEER_SIGNALS = 512
        const val HEARTBEAT_INTERVAL_MS = 20_000L
        const val HEARTBEAT_MESSAGE = "{\"type\":\"ping\"}"
        val sharedClient: OkHttpClient = OkHttpClient.Builder()
            .pingInterval(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }
}
