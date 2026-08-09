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
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit


enum class NovaRealtimeStatus {
    Connecting,
    Live,
    Reconnecting,
    Offline,
}


data class NovaConversationPresence(
    val userId: Long,
    val username: String,
    val isOnline: Boolean,
    val lastSeenAt: String?,
)


data class NovaMessageReactionEvent(
    val messageId: Long,
    val emoji: String,
    val count: Int,
    val active: Boolean,
    val isMine: Boolean,
)


data class NovaMessageUpdatedEvent(
    val messageId: Long,
    val body: String,
    val editedAt: String,
)


data class NovaMessageDeletedEvent(
    val messageId: Long,
    val deletedAt: String,
)


sealed interface NovaRealtimeEvent {
    data class MessageCreated(val message: NovaMessage) : NovaRealtimeEvent

    data class MessagesDelivered(
        val recipientId: Long,
        val deliveredAt: String,
        val messageIds: Set<Long>,
    ) : NovaRealtimeEvent

    data class ConversationRead(
        val readerId: Long,
        val readAt: String,
        val messageIds: Set<Long>,
    ) : NovaRealtimeEvent

    data class Typing(
        val userId: Long,
        val username: String,
        val isTyping: Boolean,
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
    private var onPresence: ((NovaConversationPresence) -> Unit)? = null
    private var onReaction: ((NovaMessageReactionEvent) -> Unit)? = null
    private var onMessageUpdated: ((NovaMessageUpdatedEvent) -> Unit)? = null
    private var onMessageDeleted: ((NovaMessageDeletedEvent) -> Unit)? = null
    private var onSessionExpired: (() -> Unit)? = null

    fun start(
        scope: CoroutineScope,
        onEvent: (NovaRealtimeEvent) -> Unit,
        onStatus: (NovaRealtimeStatus) -> Unit,
        onSessionExpired: () -> Unit,
        onPresence: (NovaConversationPresence) -> Unit = {},
        onReaction: (NovaMessageReactionEvent) -> Unit = {},
        onMessageUpdated: (NovaMessageUpdatedEvent) -> Unit = {},
        onMessageDeleted: (NovaMessageDeletedEvent) -> Unit = {},
    ) {
        stop()
        stopped = false
        reconnectAttempt = 0
        this.scope = scope
        this.onEvent = onEvent
        this.onStatus = onStatus
        this.onPresence = onPresence
        this.onReaction = onReaction
        this.onMessageUpdated = onMessageUpdated
        this.onMessageDeleted = onMessageDeleted
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

    fun sendTyping(isTyping: Boolean) {
        if (stopped) return
        socket?.send(
            JSONObject()
                .put("type", "typing")
                .put("is_typing", isTyping)
                .toString()
        )
    }

    private fun acknowledgeDelivered(messageId: Long) {
        if (stopped || messageId <= 0L) return
        socket?.send(
            JSONObject()
                .put("type", "message.delivered")
                .put("message_id", messageId)
                .toString()
        )
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
                    val json = runCatching { JSONObject(text) }.getOrNull() ?: return
                    parsePresence(json)?.let {
                        emitPresence(it)
                        return
                    }
                    parseReaction(json)?.let {
                        emitReaction(it)
                        return
                    }
                    parseMessageUpdated(json)?.let {
                        emitMessageUpdated(it)
                        return
                    }
                    parseMessageDeleted(json)?.let {
                        emitMessageDeleted(it)
                        return
                    }

                    val event = parseEvent(json) ?: return
                    if (event is NovaRealtimeEvent.MessageCreated && !event.message.isMine) {
                        acknowledgeDelivered(event.message.id)
                    }
                    emitEvent(event)
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

    private fun emitPresence(presence: NovaConversationPresence) {
        scope?.launch {
            if (!stopped) onPresence?.invoke(presence)
        }
    }

    private fun emitReaction(reaction: NovaMessageReactionEvent) {
        scope?.launch {
            if (!stopped) onReaction?.invoke(reaction)
        }
    }

    private fun emitMessageUpdated(event: NovaMessageUpdatedEvent) {
        scope?.launch {
            if (!stopped) onMessageUpdated?.invoke(event)
        }
    }

    private fun emitMessageDeleted(event: NovaMessageDeletedEvent) {
        scope?.launch {
            if (!stopped) onMessageDeleted?.invoke(event)
        }
    }

    private fun parsePresence(json: JSONObject): NovaConversationPresence? {
        val payload = when (json.optString("type")) {
            "ready" -> json.optJSONObject("presence")
            "presence" -> json
            else -> null
        } ?: return null

        val userId = payload.optLong("user_id", -1L)
        if (userId <= 0L) return null

        return NovaConversationPresence(
            userId = userId,
            username = payload.optString("username"),
            isOnline = payload.optBoolean("is_online", false),
            lastSeenAt = nullableString(payload.opt("last_seen_at")),
        )
    }

    private fun parseReaction(json: JSONObject): NovaMessageReactionEvent? {
        if (json.optString("type") != "message.reaction") return null
        val messageId = json.optLong("message_id", -1L)
        val emoji = json.optString("emoji")
        if (messageId <= 0L || emoji.isBlank()) return null
        return NovaMessageReactionEvent(
            messageId = messageId,
            emoji = emoji,
            count = json.optInt("count", 0).coerceAtLeast(0),
            active = json.optBoolean("active", false),
            isMine = json.optBoolean("is_mine", false),
        )
    }

    private fun parseMessageUpdated(json: JSONObject): NovaMessageUpdatedEvent? {
        if (json.optString("type") != "message.updated") return null
        val messageId = json.optLong("message_id", -1L)
        val editedAt = json.optString("edited_at")
        if (messageId <= 0L || editedAt.isBlank()) return null
        return NovaMessageUpdatedEvent(
            messageId = messageId,
            body = json.optString("body"),
            editedAt = editedAt,
        )
    }

    private fun parseMessageDeleted(json: JSONObject): NovaMessageDeletedEvent? {
        if (json.optString("type") != "message.deleted") return null
        val messageId = json.optLong("message_id", -1L)
        val deletedAt = json.optString("deleted_at")
        if (messageId <= 0L || deletedAt.isBlank()) return null
        return NovaMessageDeletedEvent(
            messageId = messageId,
            deletedAt = deletedAt,
        )
    }

    private fun parseEvent(json: JSONObject): NovaRealtimeEvent? {
        return runCatching {
            when (json.optString("type")) {
                "message.created" -> {
                    val message = json.optJSONObject("message") ?: return@runCatching null
                    NovaRealtimeEvent.MessageCreated(parseMessage(message))
                }

                "messages.delivered" -> NovaRealtimeEvent.MessagesDelivered(
                    recipientId = json.optLong("recipient_id"),
                    deliveredAt = json.optString("delivered_at"),
                    messageIds = parseIds(json),
                )

                "conversation.read" -> NovaRealtimeEvent.ConversationRead(
                    readerId = json.optLong("reader_id"),
                    readAt = json.optString("read_at"),
                    messageIds = parseIds(json),
                )

                "typing" -> NovaRealtimeEvent.Typing(
                    userId = json.optLong("user_id"),
                    username = json.optString("username"),
                    isTyping = json.optBoolean("is_typing", false),
                )

                else -> null
            }
        }.getOrNull()
    }

    private fun parseIds(json: JSONObject): Set<Long> {
        val array = json.optJSONArray("message_ids")
        return buildSet {
            if (array != null) {
                for (index in 0 until array.length()) {
                    val id = array.optLong(index, -1L)
                    if (id > 0L) add(id)
                }
            }
        }
    }

    private fun parseMessage(json: JSONObject): NovaMessage {
        val sender = json.optJSONObject("sender") ?: JSONObject()
        val replyJson = json.optJSONObject("reply_to")
        val reply = replyJson?.let {
            val replySender = it.optJSONObject("sender") ?: JSONObject()
            NovaReplyPreview(
                id = it.optLong("id"),
                sender = NovaPostAuthor(
                    id = replySender.optLong("id"),
                    username = replySender.optString("username"),
                    name = replySender.optString("name"),
                    avatarUrl = resolveMediaUrl(replySender.optString("avatar_url")),
                ),
                body = it.optString("body"),
                imageUrl = resolveMediaUrl(it.optString("image_url")),
                audioUrl = resolveMediaUrl(it.optString("audio_url")),
                audioDurationMs = nullableLong(it.opt("audio_duration_ms")),
                isDeleted = it.optBoolean("is_deleted", false),
            )
        }

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
            imageUrl = resolveMediaUrl(json.optString("image_url")),
            replyTo = reply,
            reactions = parseReactions(json.optJSONArray("reactions")),
            createdAt = json.optString("created_at"),
            deliveredAt = nullableString(json.opt("delivered_at")),
            readAt = nullableString(json.opt("read_at")),
            isMine = json.optBoolean("is_mine", false),
            audioUrl = resolveMediaUrl(json.optString("audio_url")),
            audioDurationMs = nullableLong(json.opt("audio_duration_ms")),
            editedAt = nullableString(json.opt("edited_at")),
            deletedAt = nullableString(json.opt("deleted_at")),
        )
    }

    private fun parseReactions(array: JSONArray?): List<NovaMessageReaction> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val emoji = item.optString("emoji")
                val count = item.optInt("count", 0)
                if (emoji.isNotBlank() && count > 0) {
                    add(
                        NovaMessageReaction(
                            emoji = emoji,
                            count = count,
                            reactedByMe = item.optBoolean("reacted_by_me", false),
                        )
                    )
                }
            }
        }
    }

    private fun nullableString(value: Any?): String? {
        return when (value) {
            null, JSONObject.NULL -> null
            else -> value.toString().takeIf { it.isNotBlank() && it != "null" }
        }
    }

    private fun nullableLong(value: Any?): Long? {
        return when (value) {
            null, JSONObject.NULL -> null
            is Number -> value.toLong()
            else -> value.toString().toLongOrNull()
        }
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
