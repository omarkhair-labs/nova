package com.nova.app.feature.messages.details.data.remote

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.messages.details.data.ConversationToolsRepository
import com.nova.app.feature.messages.details.model.ConversationMediaPage
import com.nova.app.feature.messages.details.model.ConversationMessageContext
import com.nova.app.feature.messages.details.model.ConversationToolMessage
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


class ConversationToolsRemoteRepository(
    context: Context,
    private val baseUrl: String = NovaMessagingRepository.PRODUCTION_API_URL,
) : ConversationToolsRepository {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)
    private val authApi = NovaApiClient(baseUrl)

    override suspend fun searchMessages(
        conversationId: Long,
        query: String,
    ): ApiResult<List<ConversationToolMessage>> {
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

    override suspend fun messageContext(
        conversationId: Long,
        messageId: Long,
    ): ApiResult<ConversationMessageContext> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/messages/context/?message_id=$messageId",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(
                    ConversationMessageContext(
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

    override suspend fun sharedMedia(
        conversationId: Long,
        type: String,
        cursor: String?,
    ): ApiResult<ConversationMediaPage> {
        val cleanType = normalizeConversationMediaType(type)
        return authenticatedCall { token ->
            fetchMediaPages(
                token = token,
                conversationId = conversationId,
                type = cleanType,
                initialCursor = cursor,
            )
        }
    }

    override suspend fun isMuted(conversationId: Long): ApiResult<Boolean> {
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

    override suspend fun setMuted(conversationId: Long, muted: Boolean): ApiResult<Boolean> {
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

    private suspend fun fetchMediaPages(
        token: String,
        conversationId: Long,
        type: String,
        initialCursor: String?,
    ): ApiResult<ConversationMediaPage> {
        val collected = mutableListOf<ConversationToolMessage>()
        var cursor = initialCursor
        var pages = 0

        do {
            val cursorPart = cursor?.takeIf { it.isNotBlank() }
                ?.let { "&cursor=${encode(it)}" }
                .orEmpty()

            when (
                val response = requestJson(
                    path = "conversations/$conversationId/media/?type=$type$cursorPart",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> {
                    val page = parseItems(response.value.optJSONArray("results"))
                    val existingIds = collected.mapTo(mutableSetOf()) { it.id }
                    collected += page.filterNot { it.id in existingIds }
                    cursor = nullableString(response.value.opt("next_cursor"))
                    pages += 1
                }
                is ApiResult.Failure -> return response
            }
        } while (cursor != null && pages < MAX_AUTO_MEDIA_PAGES)

        return ApiResult.Success(
            ConversationMediaPage(
                items = collected,
                nextCursor = cursor,
            )
        )
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

    private fun parseItems(array: JSONArray?): List<ConversationToolMessage> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(parseItem(it)) }
            }
        }
    }

    private fun parseItem(json: JSONObject): ConversationToolMessage {
        val sender = json.optJSONObject("sender") ?: JSONObject()
        val reply = json.optJSONObject("reply_to")
        val replyPreview = if (reply == null) {
            ""
        } else {
            conversationReplyPreview(
                isDeleted = reply.optBoolean("is_deleted", false),
                body = reply.optString("body"),
                audioUrl = reply.optString("audio_url"),
                imageUrl = reply.optString("image_url"),
            )
        }

        return ConversationToolMessage(
            id = json.optLong("id"),
            sender = NovaPostAuthor(
                id = sender.optLong("id"),
                username = sender.optString("username"),
                name = sender.optString("name"),
                avatarUrl = resolveConversationMediaUrl(baseUrl, sender.optString("avatar_url")),
            ),
            body = json.optString("body"),
            imageUrl = resolveConversationMediaUrl(baseUrl, json.optString("image_url")),
            audioUrl = resolveConversationMediaUrl(baseUrl, json.optString("audio_url")),
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

    private companion object {
        const val MAX_AUTO_MEDIA_PAGES = 100
    }
}


internal fun normalizeConversationMediaType(type: String): String =
    type.takeIf { it in setOf("all", "image", "audio") } ?: "all"


internal fun conversationReplyPreview(
    isDeleted: Boolean,
    body: String,
    audioUrl: String,
    imageUrl: String,
): String = when {
    isDeleted -> "Message deleted"
    body.isNotBlank() -> body
    audioUrl.isNotBlank() -> "Voice message"
    imageUrl.isNotBlank() -> "Photo"
    else -> "Message"
}


internal fun resolveConversationMediaUrl(baseUrl: String, raw: String): String {
    if (raw.isBlank() || raw == "null") return ""
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
    return runCatching {
        val apiUrl = URL(baseUrl)
        URL("${apiUrl.protocol}://${apiUrl.authority}$raw").toString()
    }.getOrDefault(raw)
}
