package com.nova.app.core.messaging

import android.content.Context
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPostAuthor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit


enum class NovaRealtimeStatus {
    Connecting,
    Live,
    Reconnecting,
    Offline,
}


sealed interface NovaRealtimeEvent {
    data class MessageCreated(val message: NovaMessage) : NovaRealtimeEvent

    data class ConversationRead(
        val readerId: Long,
        val readAt: String,
        val messageIds: Set<Long>,
    ) : NovaRealtimeEvent
}


class NovaConversationRealtimeClient(
    context: Context,
    private val conversationId: Long,
    private val repository: NovaMessagingRepository = NovaMessagingRepository(context.applicationContext),
) {
    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var connectionJob: Job? = null
    private var stopped = true
    private var reconnectAttempt = 0
    private var scope: CoroutineScope? = null
    private var onEvent: ((NovaRealtimeEvent) -> Unit)? = null
    private var onStatus: ((NovaRealtimeStatus) -> Unit)? = null
    private var onSessionExpired: (() -> Unit)? = null

    fun start(
        scope: CoroutineScope,
        onEvent: (NovaRealtimeEvent) -> Unit,
        onStatus: (NovaRealtimeStatus) -> Unit,
        onSessionExpired: () -> Unit,
    ) {
        stop()
        stopped = false
        reconnectAttempt = 0
        this.scope = scope
        this.onEvent = onEvent
        this.onStatus = onStatus
        this.onSessionExpired = onSessionExpired
        connect(initial = true)
    }

    fun stop() {
        stopped = true
        reconnectJob?.cancel()
        reconnectJob = null
        connectionJob?.cancel()
        connectionJob = null
        socket?.close(1000, "Conversation closed")
        socket = null
    }

    private fun connect(initial: Boolean) {
        if (stopped) return
        emitStatus(if (initial) NovaRealtimeStatus.Connecting else NovaRealtimeStatus.Reconnecting)

        connectionJob?.cancel()
        connectionJob = scope?.launch {
            when (val token = repository.realtimeAccessToken()) {
                is ApiResult.Success -> openSocket(token.value)
                is ApiResult.Failure -> {
                    if (token.statusCode == 401) {
                        onSessionExpired?.invoke()
                    } else {
                        emitStatus(NovaRealtimeStatus.Offline)
                        scheduleReconnect()
                    }
                }
            }
        }
    }

    private fun openSocket(accessToken: String) {
        if (stopped) return

        val request = Request.Builder()
            .url("${NovaMessagingRepository.PRODUCTION_WS_URL}conversations/$conversationId/")
            .header("Authorization", "Bearer $accessToken")
            .build()

        socket = sharedClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    reconnectAttempt = 0
                    emitStatus(NovaRealtimeStatus.Live)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    parseEvent(text)?.let(::emitEvent)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (socket === webSocket) socket = null
                    if (!stopped) scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (socket === webSocket) socket = null
                    if (!stopped) {
                        emitStatus(NovaRealtimeStatus.Offline)
                        scheduleReconnect()
                    }
                }
            },
        )
    }

    private fun scheduleReconnect() {
        if (stopped || reconnectJob?.isActive == true) return
        val delayMs = when (reconnectAttempt.coerceAtMost(4)) {
            0 -> 1_000L
            1 -> 2_000L
            2 -> 4_000L
            3 -> 7_000L
            else -> 10_000L
        }
        reconnectAttempt += 1
        emitStatus(NovaRealtimeStatus.Reconnecting)
        reconnectJob = scope?.launch {
            delay(delayMs)
            reconnectJob = null
            connect(initial = false)
        }
    }

    private fun emitStatus(status: NovaRealtimeStatus) {
        scope?.launch {
            if (!stopped) onStatus?.invoke(status)
        }
    }

    private fun emitEvent(event: NovaRealtimeEvent) {
        scope?.launch {
            if (!stopped) onEvent?.invoke(event)
        }
    }

    private fun parseEvent(raw: String): NovaRealtimeEvent? {
        return runCatching {
            val json = JSONObject(raw)
            when (json.optString("type")) {
                "message.created" -> {
                    val message = json.optJSONObject("message") ?: return@runCatching null
                    NovaRealtimeEvent.MessageCreated(parseMessage(message))
                }

                "conversation.read" -> {
                    val array = json.optJSONArray("message_ids")
                    val messageIds = buildSet {
                        if (array != null) {
                            for (index in 0 until array.length()) {
                                val id = array.optLong(index, -1L)
                                if (id > 0L) add(id)
                            }
                        }
                    }
                    NovaRealtimeEvent.ConversationRead(
                        readerId = json.optLong("reader_id"),
                        readAt = json.optString("read_at"),
                        messageIds = messageIds,
                    )
                }

                else -> null
            }
        }.getOrNull()
    }

    private fun parseMessage(json: JSONObject): NovaMessage {
        val sender = json.optJSONObject("sender") ?: JSONObject()
        val rawReadAt = json.opt("read_at")
        return NovaMessage(
            id = json.optLong("id"),
            clientId = json.optString("client_id"),
            sender = NovaPostAuthor(
                id = sender.optLong("id"),
                username = sender.optString("username"),
                name = sender.optString("name"),
                avatarUrl = resolveMediaUrl(sender.optString("avatar_url")),
            ),
            body = json.optString("body"),
            createdAt = json.optString("created_at"),
            readAt = when (rawReadAt) {
                null, JSONObject.NULL -> null
                else -> rawReadAt.toString().takeIf { it.isNotBlank() && it != "null" }
            },
            isMine = json.optBoolean("is_mine", false),
        )
    }

    private fun resolveMediaUrl(raw: String): String {
        if (raw.isBlank() || raw == "null") return ""
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw

        return runCatching {
            val apiUrl = URL(NovaMessagingRepository.PRODUCTION_API_URL)
            URL("${apiUrl.protocol}://${apiUrl.authority}$raw").toString()
        }.getOrDefault(raw)
    }

    private companion object {
        val sharedClient: OkHttpClient = OkHttpClient.Builder()
            .pingInterval(25, TimeUnit.SECONDS)
            .build()
    }
}
