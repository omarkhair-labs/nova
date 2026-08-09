package com.nova.app.core.messaging

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPostAuthor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


data class NovaV9MessageItem(
    val id: Long,
    val sender: NovaPostAuthor,
    val body: String,
    val imageUrl: String,
    val audioUrl: String,
    val audioDurationMs: Long?,
    val replyToId: Long?,
    val replyPreview: String,
    val createdAt: String,
    val isMine: Boolean,
    val isDeleted: Boolean,
)


data class NovaV9MediaPage(
    val items: List<NovaV9MessageItem>,
    val nextCursor: String?,
)


data class NovaV9MessageContext(
    val items: List<NovaV9MessageItem>,
    val targetMessageId: Long,
    val hasEarlier: Boolean,
    val hasLater: Boolean,
)


class NovaMessagingV9ToolsRepository(
    context: Context,
    private val baseUrl: String = NovaMessagingRepository.PRODUCTION_API_URL,
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)
    private val authApi = NovaApiClient(baseUrl)

    suspend fun searchMessages(
        conversationId: Long,
        query: String,
    ): ApiResult<List<NovaV9MessageItem>> {
        val clean = query.trim()
        if (clean.isBlank()) return ApiResult.Success(emptyList())
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/messages/search/?q=${encode(clean)}",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseItems(response.value.optJSONArray("results")))
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun messageContext(
        conversationId: Long,
        messageId: Long,
    ): ApiResult<NovaV9MessageContext> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/messages/context/?message_id=$messageId",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(
                    NovaV9MessageContext(
                        items = parseItems(response.value.optJSONArray("results")),
                        targetMessageId = response.value.optLong("target_message_id", messageId),
                        hasEarlier = response.value.optBoolean("has_earlier", false),
                        hasLater = response.value.optBoolean("has_later", false),
                    )
                )
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun sharedMedia(
        conversationId: Long,
        type: String = "all",
        cursor: String? = null,
    ): ApiResult<NovaV9MediaPage> {
        val cleanType = type.takeIf { it in setOf("all", "image", "audio") } ?: "all"
        val cursorPart = cursor?.takeIf { it.isNotBlank() }?.let { "&cursor=${encode(it)}" }.orEmpty()
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/media/?type=$cleanType$cursorPart",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(
                    NovaV9MediaPage(
                        items = parseItems(response.value.optJSONArray("results")),
                        nextCursor = nullableString(response.value.opt("next_cursor")),
                    )
                )
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun isMuted(conversationId: Long): ApiResult<Boolean> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/preferences/",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(response.value.optBoolean("muted", false))
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun setMuted(conversationId: Long, muted: Boolean): ApiResult<Boolean> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/preferences/",
                    method = "POST",
                    body = JSONObject().put("muted", muted),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(response.value.optBoolean("muted", muted))
                is ApiResult.Failure -> response
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
                } else refreshed
            }
        }
    }

    private fun parseItems(array: JSONArray?): List<NovaV9MessageItem> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(parseItem(it)) }
            }
        }
    }

    private fun parseItem(json: JSONObject): NovaV9MessageItem {
        val sender = json.optJSONObject("sender") ?: JSONObject()
        val reply = json.optJSONObject("reply_to")
        val replyPreview = when {
            reply == null -> ""
            reply.optBoolean("is_deleted", false) -> "Message deleted"
            reply.optString("body").isNotBlank() -> reply.optString("body")
            reply.optString("audio_url").isNotBlank() -> "🎤 Voice message"
            reply.optString("image_url").isNotBlank() -> "📷 Photo"
            else -> "Message"
        }

        return NovaV9MessageItem(
            id = json.optLong("id"),
            sender = NovaPostAuthor(
                id = sender.optLong("id"),
                username = sender.optString("username"),
                name = sender.optString("name"),
                avatarUrl = resolveMediaUrl(sender.optString("avatar_url")),
            ),
            body = json.optString("body"),
            imageUrl = resolveMediaUrl(json.optString("image_url")),
            audioUrl = resolveMediaUrl(json.optString("audio_url")),
            audioDurationMs = nullableLong(json.opt("audio_duration_ms")),
            replyToId = reply?.optLong("id")?.takeIf { it > 0L },
            replyPreview = replyPreview,
            createdAt = json.optString("created_at"),
            isMine = json.optBoolean("is_mine", false),
            isDeleted = json.optBoolean("is_deleted", false),
        )
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
                        outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
                    }
                }

                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = raw.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()

                if (statusCode in 200..299) {
                    ApiResult.Success(json)
                } else {
                    ApiResult.Failure(
                        message = when (statusCode) {
                            400 -> json.optString("detail").ifBlank { "Nova couldn't complete that request." }
                            401 -> "Your session expired. Please log in again."
                            403 -> "You can't access that conversation."
                            404 -> json.optString("detail").ifBlank { "That message or conversation is no longer available." }
                            in 500..599 -> "Nova's server had a problem. Try again in a moment."
                            else -> json.optString("detail").ifBlank { "Something went wrong. Please try again." }
                        },
                        statusCode = statusCode,
                    )
                }
            } catch (_: Exception) {
                ApiResult.Failure("Can't reach Nova right now. Check your connection and try again.")
            } finally {
                connection?.disconnect()
            }
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

    private fun nullableLong(value: Any?): Long? = when (value) {
        null, JSONObject.NULL -> null
        is Number -> value.toLong()
        else -> value.toString().toLongOrNull()
    }

    private fun nullableString(value: Any?): String? = when (value) {
        null, JSONObject.NULL -> null
        else -> value.toString().takeIf { it.isNotBlank() && it != "null" }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
