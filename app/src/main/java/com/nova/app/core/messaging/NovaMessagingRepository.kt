package com.nova.app.core.messaging

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.core.network.UploadFile
import com.nova.app.feature.messages.data.MessagesRepository
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.messages.domain.model.NovaConversationList
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaMessagePage
import com.nova.app.feature.messages.domain.model.NovaMessageReaction
import com.nova.app.feature.messages.domain.model.NovaMessageShare
import com.nova.app.feature.messages.domain.model.NovaReplyPreview
import com.nova.app.feature.messages.domain.model.NovaSharedPost
import com.nova.app.feature.messages.domain.model.NovaSharedReel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID


class NovaMessagingRepository(
    context: Context,
    private val api: NovaMessagingApiClient = NovaMessagingApiClient(PRODUCTION_API_URL),
    private val authApi: NovaApiClient = NovaApiClient(PRODUCTION_API_URL),
) : MessagesRepository {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

    override suspend fun conversations(query: String): ApiResult<NovaConversationList> {
        return authenticatedCall { accessToken ->
            api.conversations(accessToken, query)
        }
    }

    override suspend fun openConversation(username: String): ApiResult<NovaConversation> {
        return authenticatedCall { accessToken ->
            api.openConversation(accessToken, username)
        }
    }

    override suspend fun messages(
        conversationId: Long,
        cursor: String?,
    ): ApiResult<NovaMessagePage> {
        return authenticatedCall { accessToken ->
            api.messages(accessToken, conversationId, cursor)
        }
    }

    override suspend fun sendMessage(
        conversationId: Long,
        body: String,
        clientId: String,
        replyToId: Long?,
        imageUri: Uri?,
        audioFile: File?,
        audioDurationMs: Long?,
    ): ApiResult<NovaMessage> {
        if (imageUri != null && audioFile != null) {
            return ApiResult.Failure("Send either a photo or a voice message, not both at once.")
        }

        val image = if (imageUri != null) {
            when (val prepared = prepareImage(imageUri)) {
                is ApiResult.Success -> prepared.value
                is ApiResult.Failure -> return prepared
            }
        } else {
            null
        }

        val audio = if (audioFile != null) {
            if (audioDurationMs == null || audioDurationMs < 1_000L || audioDurationMs > 5 * 60 * 1_000L) {
                return ApiResult.Failure("Voice message must be between 1 second and 5 minutes.")
            }
            when (val prepared = prepareAudio(audioFile)) {
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
                audio = audio,
                audioDurationMs = audioDurationMs,
            )
        }
    }

    override suspend fun editMessage(messageId: Long, body: String): ApiResult<NovaMessage> {
        return authenticatedCall { accessToken ->
            api.editMessage(accessToken, messageId, body)
        }
    }

    override suspend fun deleteMessage(messageId: Long): ApiResult<String> {
        return authenticatedCall { accessToken ->
            api.deleteMessage(accessToken, messageId)
        }
    }

    override suspend fun setReaction(
        messageId: Long,
        emoji: String?,
    ): ApiResult<List<NovaMessageReaction>> {
        return authenticatedCall { accessToken ->
            api.setReaction(accessToken, messageId, emoji)
        }
    }

    override suspend fun markRead(conversationId: Long): ApiResult<Int> {
        return authenticatedCall { accessToken ->
            api.markRead(accessToken, conversationId)
        }
    }

    override suspend fun realtimeAccessToken(): ApiResult<String> {
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

    private suspend fun prepareAudio(file: File): ApiResult<UploadFile> {
        return withContext(Dispatchers.IO) {
            runCatching {
                if (!file.exists() || !file.isFile) {
                    throw IllegalStateException("Voice recording is unavailable.")
                }
                val bytes = file.readBytes()
                if (bytes.size > 15 * 1024 * 1024) {
                    return@withContext ApiResult.Failure("Voice message must be 15 MB or smaller.")
                }
                ApiResult.Success(
                    UploadFile(
                        bytes = bytes,
                        fileName = file.name.ifBlank { "nova-voice-${System.currentTimeMillis()}.m4a" },
                        mimeType = "audio/mp4",
                    )
                )
            }.getOrElse {
                ApiResult.Failure("Nova couldn't read that voice message. Record it again and try once more.")
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
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
        const val PRODUCTION_WS_URL = "wss://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/ws/"
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
        audio: UploadFile? = null,
        audioDurationMs: Long? = null,
    ): ApiResult<NovaMessage> {
        val response = if (image != null || audio != null) {
            requestMultipartMessage(
                path = "conversations/$conversationId/messages/",
                bearerToken = accessToken,
                body = body.trim(),
                clientId = clientId,
                replyToId = replyToId,
                image = image,
                audio = audio,
                audioDurationMs = audioDurationMs,
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

    suspend fun editMessage(
        accessToken: String,
        messageId: Long,
        body: String,
    ): ApiResult<NovaMessage> {
        return when (
            val response = requestJson(
                path = "messages/$messageId/",
                method = "POST",
                body = JSONObject().put("body", body.trim()),
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseMessage(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun deleteMessage(
        accessToken: String,
        messageId: Long,
    ): ApiResult<String> {
        return when (
            val response = requestJson(
                path = "messages/$messageId/",
                method = "DELETE",
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> {
                val deletedAt = response.value.optString("deleted_at")
                if (deletedAt.isBlank()) ApiResult.Failure("Nova couldn't confirm that deletion.")
                else ApiResult.Success(deletedAt)
            }
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
        val kind = json.optString("kind", "direct")
        val title = json.optString("title")
        val membersArray = json.optJSONArray("members_preview") ?: JSONArray()
        val members = buildList {
            for (index in 0 until membersArray.length()) {
                val item = membersArray.optJSONObject(index) ?: continue
                add(
                    NovaPostAuthor(
                        id = item.optLong("id"),
                        username = item.optString("username"),
                        name = item.optString("name"),
                        avatarUrl = resolveMediaUrl(item.optString("avatar_url")),
                    )
                )
            }
        }
        val user = json.optJSONObject("other_user")
        val otherUser = if (user != null) {
            NovaPostAuthor(
                id = user.optLong("id"),
                username = user.optString("username"),
                name = user.optString("name"),
                avatarUrl = resolveMediaUrl(user.optString("avatar_url")),
            )
        } else {
            NovaPostAuthor(
                id = 0L,
                username = "group",
                name = title.ifBlank { "Nova group" },
                avatarUrl = "",
            )
        }
        val rawLastMessage = json.opt("last_message")
        val lastMessage = if (rawLastMessage is JSONObject) parseMessage(rawLastMessage) else null

        return NovaConversation(
            id = json.optLong("id"),
            otherUser = otherUser,
            lastMessage = lastMessage,
            unreadCount = json.optInt("unread_count", 0),
            createdAt = json.optString("created_at"),
            updatedAt = json.optString("updated_at"),
            kind = kind,
            title = title,
            membersPreview = members,
            membersCount = json.optInt("members_count", if (kind == "group") members.size + 1 else 2),
            currentUserRole = json.optString("current_user_role"),
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
            share = parseShare(json.optJSONObject("share")),
        )
    }

    private fun parseShare(json: JSONObject?): NovaMessageShare? {
        if (json == null) return null
        val kind = json.optString("kind")
        if (kind.isBlank()) return null
        val available = json.optBoolean("available", false)
        if (!available) {
            return NovaMessageShare(kind = kind, available = false)
        }

        val post = json.optJSONObject("post")?.let { item ->
            val author = item.optJSONObject("author") ?: JSONObject()
            NovaSharedPost(
                id = item.optLong("id"),
                author = NovaPostAuthor(
                    id = author.optLong("id"),
                    username = author.optString("username"),
                    name = author.optString("name"),
                    avatarUrl = resolveMediaUrl(author.optString("avatar_url")),
                ),
                imageUrl = resolveMediaUrl(item.optString("image_url")),
                caption = item.optString("caption"),
            )
        }
        val profile = json.optJSONObject("profile")?.let { item ->
            NovaPostAuthor(
                id = item.optLong("id"),
                username = item.optString("username"),
                name = item.optString("name"),
                avatarUrl = resolveMediaUrl(item.optString("avatar_url")),
            )
        }
        val reel = json.optJSONObject("reel")?.let { item ->
            val author = item.optJSONObject("author") ?: JSONObject()
            NovaSharedReel(
                id = item.optLong("id"),
                author = NovaPostAuthor(
                    id = author.optLong("id"),
                    username = author.optString("username"),
                    name = author.optString("name"),
                    avatarUrl = resolveMediaUrl(author.optString("avatar_url")),
                ),
                videoUrl = resolveMediaUrl(item.optString("video_url")),
                caption = item.optString("caption"),
            )
        }
        return NovaMessageShare(
            kind = kind,
            available = true,
            post = post,
            profile = profile,
            reel = reel,
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

    private suspend fun requestMultipartMessage(
        path: String,
        bearerToken: String,
        body: String,
        clientId: String,
        replyToId: Long?,
        image: UploadFile?,
        audio: UploadFile?,
        audioDurationMs: Long?,
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

                    fun fileField(name: String, upload: UploadFile) {
                        output.writeBytes("--$boundary\r\n")
                        output.writeBytes(
                            "Content-Disposition: form-data; name=\"$name\"; filename=\"${upload.fileName}\"\r\n"
                        )
                        output.writeBytes("Content-Type: ${upload.mimeType}\r\n\r\n")
                        output.write(upload.bytes)
                        output.writeBytes("\r\n")
                    }

                    field("body", body)
                    field("client_id", clientId)
                    if (replyToId != null) field("reply_to_id", replyToId.toString())
                    if (audio != null && audioDurationMs != null) {
                        field("audio_duration_ms", audioDurationMs.toString())
                    }
                    image?.let { fileField("image", it) }
                    audio?.let { fileField("audio", it) }
                    output.writeBytes("--$boundary--\r\n")
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
                    403 -> json.optString("detail").ifBlank { "You can't change that message." }
                    404 -> "That message or conversation is no longer available."
                    409 -> json.optString("detail").ifBlank { "That message request conflicted with another one." }
                    413 -> "That attachment is too large to send."
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
