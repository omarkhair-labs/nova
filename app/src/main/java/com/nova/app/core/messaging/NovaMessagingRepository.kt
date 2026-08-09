package com.nova.app.core.messaging

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.core.network.UploadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID


data class NovaReplyPreview(
    val id: Long,
    val sender: NovaPostAuthor,
    val body: String,
    val imageUrl: String,
)


data class NovaMessageReaction(
    val emoji: String,
    val count: Int,
    val reactedByMe: Boolean,
)


data class NovaMessage(
    val id: Long,
    val clientId: String,
    val sender: NovaPostAuthor,
    val body: String,
    val imageUrl: String,
    val replyTo: NovaReplyPreview?,
    val reactions: List<NovaMessageReaction>,
    val createdAt: String,
    val deliveredAt: String?,
    val readAt: String?,
    val isMine: Boolean,
)


data class NovaConversation(
    val id: Long,
    val otherUser: NovaPostAuthor,
    val lastMessage: NovaMessage?,
    val unreadCount: Int,
    val createdAt: String,
    val updatedAt: String,
)


data class NovaConversationList(
    val conversations: List<NovaConversation>,
    val unreadCount: Int,
)


data class NovaMessagePage(
    val messages: List<NovaMessage>,
    val nextCursor: String?,
)


class NovaMessagingRepository(
    context: Context,
    private val api: NovaMessagingApiClient = NovaMessagingApiClient(PRODUCTION_API_URL),
    private val authApi: NovaApiClient = NovaApiClient(PRODUCTION_API_URL),
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

    suspend fun conversations(query: String = ""): ApiResult<NovaConversationList> {
        return authenticatedCall { accessToken ->
            api.conversations(accessToken, query)
        }
    }

    suspend fun openConversation(username: String): ApiResult<NovaConversation> {
        return authenticatedCall { accessToken ->
            api.openConversation(accessToken, username)
        }
    }

    suspend fun messages(
        conversationId: Long,
        cursor: String? = null,
    ): ApiResult<NovaMessagePage> {
        return authenticatedCall { accessToken ->
            api.messages(accessToken, conversationId, cursor)
        }
    }

    suspend fun sendMessage(
        conversationId: Long,
        body: String,
        clientId: String,
        replyToId: Long? = null,
        imageUri: Uri? = null,
    ): ApiResult<NovaMessage> {
        val image = if (imageUri != null) {
            when (val prepared = prepareImage(imageUri)) {
                is ApiResult.Success -> prepared.value
                is ApiResult.Failure -> return prepared
            }
        } else {
            null
        }

        return authenticatedCall { accessToken ->
            api.sendMessage(
                accessToken = accessToken,
                conversationId = conversationId,
                body = body,
                clientId = clientId,
                replyToId = replyToId,
                image = image,
            )
        }
    }

    suspend fun setReaction(
        messageId: Long,
        emoji: String?,
    ): ApiResult<List<NovaMessageReaction>> {
        return authenticatedCall { accessToken ->
            api.setReaction(accessToken, messageId, emoji)
        }
    }

    suspend fun markRead(conversationId: Long): ApiResult<Int> {
        return authenticatedCall { accessToken ->
            api.markRead(accessToken, conversationId)
        }
    }

    suspend fun realtimeAccessToken(): ApiResult<String> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        return when (val refreshed = authApi.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                ApiResult.Success(refreshed.value)
            }

            is ApiResult.Failure -> {
                if (refreshed.statusCode == 400 || refreshed.statusCode == 401) {
                    sessionStore.clear()
                    ApiResult.Failure("Your session expired. Please log in again.", 401)
                } else {
                    ApiResult.Success(stored.accessToken)
                }
            }
        }
    }

    private suspend fun prepareImage(uri: Uri): ApiResult<UploadFile> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val resolver = appContext.contentResolver
                val mimeType = resolver.getType(uri)?.takeIf { it.startsWith("image/") }
                    ?: "image/jpeg"
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType)
                    ?.takeIf { it.isNotBlank() }
                    ?: "jpg"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Couldn't read that photo.")

                if (bytes.size > 10 * 1024 * 1024) {
                    return@withContext ApiResult.Failure("Message photo must be 10 MB or smaller.")
                }

                ApiResult.Success(
                    UploadFile(
                        bytes = bytes,
                        fileName = "nova-message-${System.currentTimeMillis()}.$extension",
                        mimeType = mimeType,
                    )
                )
            }.getOrElse {
                ApiResult.Failure("Nova couldn't read that photo. Pick another image and try again.")
            }
        }
    }

    private suspend fun <T> authenticatedCall(
        call: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return first
            is ApiResult.Failure -> if (first.statusCode != 401) return first
        }

        return when (val refreshed = authApi.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                when (val retried = call(refreshed.value)) {
                    is ApiResult.Success -> retried
                    is ApiResult.Failure -> {
                        if (retried.statusCode == 401) sessionStore.clear()
                        retried
                    }
                }
            }

            is ApiResult.Failure -> {
                if (refreshed.statusCode == 400 || refreshed.statusCode == 401) {
                    sessionStore.clear()
                    ApiResult.Failure("Your session expired. Please log in again.", 401)
                } else {
                    refreshed
                }
            }
        }
    }

    companion object {
        const val PRODUCTION_API_URL = "https://nova-production-4f6b.up.railway.app/api/v1/"
        const val PRODUCTION_WS_URL = "wss://nova-production-4f6b.up.railway.app/ws/"
    }
}


class NovaMessagingApiClient(
    private val baseUrl: String,
) {
    suspend fun conversations(
        accessToken: String,
        query: String,
    ): ApiResult<NovaConversationList> {
        val path = if (query.isBlank()) {
            "conversations/"
        } else {
            "conversations/?q=${encode(query.trim())}"
        }

        return when (val response = requestJson(path, bearerToken = accessToken)) {
            is ApiResult.Success -> {
                val array = response.value.optJSONArray("results") ?: JSONArray()
                val items = buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { add(parseConversation(it)) }
                    }
                }
                ApiResult.Success(
                    NovaConversationList(
                        conversations = items,
                        unreadCount = response.value.optInt("unread_count", 0),
                    )
                )
            }

            is ApiResult.Failure -> response
        }
    }

    suspend fun openConversation(
        accessToken: String,
        username: String,
    ): ApiResult<NovaConversation> {
        return when (
            val response = requestJson(
                path = "conversations/",
                method = "POST",
                body = JSONObject().put("username", username.trim().lowercase()),
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseConversation(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun messages(
        accessToken: String,
        conversationId: Long,
        cursor: String? = null,
    ): ApiResult<NovaMessagePage> {
        val path = if (cursor.isNullOrBlank()) {
            "conversations/$conversationId/messages/"
        } else {
            "conversations/$conversationId/messages/?cursor=${encode(cursor)}"
        }

        return when (val response = requestJson(path, bearerToken = accessToken)) {
            is ApiResult.Success -> {
                val array = response.value.optJSONArray("results") ?: JSONArray()
                val items = buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { add(parseMessage(it)) }
                    }
                }
                ApiResult.Success(
                    NovaMessagePage(
                        messages = items,
                        nextCursor = response.value.optString("next_cursor")
                            .takeIf { it.isNotBlank() && it != "null" },
                    )
                )
            }

            is ApiResult.Failure -> response
        }
    }

    suspend fun sendMessage(
        accessToken: String,
        conversationId: Long,
        body: String,
        clientId: String,
        replyToId: Long? = null,
        image: UploadFile? = null,
    ): ApiResult<NovaMessage> {
        val response = if (image != null) {
            requestMultipartMessage(
                path = "conversations/$conversationId/messages/",
                bearerToken = accessToken,
                body = body.trim(),
                clientId = clientId,
                replyToId = replyToId,
                image = image,
            )
        } else {
            val json = JSONObject()
                .put("body", body.trim())
                .put("client_id", clientId)
            if (replyToId != null) json.put("reply_to_id", replyToId)
            requestJson(
                path = "conversations/$conversationId/messages/",
                method = "POST",
                body = json,
                bearerToken = accessToken,
            )
        }

        return when (response) {
            is ApiResult.Success -> ApiResult.Success(parseMessage(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun setReaction(
        accessToken: String,
        messageId: Long,
        emoji: String?,
    ): ApiResult<List<NovaMessageReaction>> {
        val response = if (emoji == null) {
            requestJson(
                path = "messages/$messageId/reaction/",
                method = "DELETE",
                bearerToken = accessToken,
            )
        } else {
            requestJson(
                path = "messages/$messageId/reaction/",
                method = "POST",
                body = JSONObject().put("emoji", emoji),
                bearerToken = accessToken,
            )
        }
        return when (response) {
            is ApiResult.Success -> ApiResult.Success(parseReactions(response.value.optJSONArray("reactions")))
            is ApiResult.Failure -> response
        }
    }

    suspend fun markRead(
        accessToken: String,
        conversationId: Long,
    ): ApiResult<Int> {
        return when (
            val response = requestJson(
                path = "conversations/$conversationId/read/",
                method = "POST",
                body = JSONObject(),
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(response.value.optInt("marked_read", 0))
            is ApiResult.Failure -> response
        }
    }

    private fun parseConversation(json: JSONObject): NovaConversation {
        val user = json.optJSONObject("other_user") ?: JSONObject()
        val rawLastMessage = json.opt("last_message")
        val lastMessage = if (rawLastMessage is JSONObject) parseMessage(rawLastMessage) else null

        return NovaConversation(
            id = json.optLong("id"),
            otherUser = NovaPostAuthor(
                id = user.optLong("id"),
                username = user.optString("username"),
                name = user.optString("name"),
                avatarUrl = resolveMediaUrl(user.optString("avatar_url")),
            ),
            lastMessage = lastMessage,
            unreadCount = json.optInt("unread_count", 0),
            createdAt = json.optString("created_at"),
            updatedAt = json.optString("updated_at"),
        )
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

    private suspend fun requestMultipartMessage(
        path: String,
        bearerToken: String,
        body: String,
        clientId: String,
        replyToId: Long?,
        image: UploadFile,
    ): ApiResult<JSONObject> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            val boundary = "NovaBoundary${UUID.randomUUID()}"
            try {
                connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    doOutput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $bearerToken")
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                DataOutputStream(connection.outputStream).use { output ->
                    fun field(name: String, value: String) {
                        output.writeBytes("--$boundary\r\n")
                        output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                        output.write(value.toByteArray(Charsets.UTF_8))
                        output.writeBytes("\r\n")
                    }

                    field("body", body)
                    field("client_id", clientId)
                    if (replyToId != null) field("reply_to_id", replyToId.toString())

                    output.writeBytes("--$boundary\r\n")
                    output.writeBytes(
                        "Content-Disposition: form-data; name=\"image\"; filename=\"${image.fileName}\"\r\n"
                    )
                    output.writeBytes("Content-Type: ${image.mimeType}\r\n\r\n")
                    output.write(image.bytes)
                    output.writeBytes("\r\n--$boundary--\r\n")
                    output.flush()
                }

                parseConnectionResponse(connection)
            } catch (_: Exception) {
                ApiResult.Failure("Can't reach Nova right now. Check your connection and try again.")
            } finally {
                connection?.disconnect()
            }
        }
    }

    private suspend fun requestJson(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        bearerToken: String,
    ): ApiResult<JSONObject> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $bearerToken")

                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(body.toString())
                        }
                    }
                }
                parseConnectionResponse(connection)
            } catch (_: Exception) {
                ApiResult.Failure("Can't reach Nova right now. Check your connection and try again.")
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun parseConnectionResponse(connection: HttpURLConnection): ApiResult<JSONObject> {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val json = raw.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()

        return if (statusCode in 200..299) {
            ApiResult.Success(json)
        } else {
            ApiResult.Failure(
                message = when (statusCode) {
                    400 -> json.optString("detail").ifBlank { "Nova couldn't complete that message request." }
                    401 -> "Your session expired. Please log in again."
                    404 -> "That message or conversation is no longer available."
                    409 -> json.optString("detail").ifBlank { "That message request conflicted with another one." }
                    413 -> "That photo is too large to send."
                    429 -> "Too many requests. Give Nova a moment and try again."
                    in 500..599 -> "Nova's server had a problem. Try again in a moment."
                    else -> json.optString("detail").ifBlank { "Something went wrong. Please try again." }
                },
                statusCode = statusCode,
            )
        }
    }

    private fun resolveMediaUrl(raw: String): String {
        if (raw.isBlank() || raw == "null") return ""
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw

        return runCatching {
            val apiUrl = URL(baseUrl)
            URL("${apiUrl.protocol}://${apiUrl.authority}$raw").toString()
        }.getOrDefault(raw)
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }
}
