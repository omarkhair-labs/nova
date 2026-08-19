package com.nova.app.core.messaging

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.messages.data.InboxRepository
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.messages.domain.model.NovaConversationPage
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaReplyPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


class NovaInboxPagingRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) : InboxRepository {
    private val sessionStore = NovaSessionStore(context.applicationContext)
    private val authApi = NovaApiClient(baseUrl)

    override suspend fun conversations(
        query: String,
        cursor: String?,
    ): ApiResult<NovaConversationPage> {
        return authenticatedCall { token ->
            val parameters = buildList {
                val cleanQuery = query.trim()
                if (cleanQuery.isNotBlank()) add("q=${encode(cleanQuery)}")
                if (!cursor.isNullOrBlank()) add("cursor=${encode(cursor)}")
            }
            val path = if (parameters.isEmpty()) {
                "conversations/"
            } else {
                "conversations/?${parameters.joinToString("&")}"
            }
            when (val response = requestJson(path, token)) {
                is ApiResult.Success -> {
                    val array = response.value.optJSONArray("results") ?: JSONArray()
                    val items = buildList {
                        for (index in 0 until array.length()) {
                            array.optJSONObject(index)?.let { add(parseConversation(it)) }
                        }
                    }
                    ApiResult.Success(
                        NovaConversationPage(
                            conversations = items,
                            unreadCount = response.value.optInt("unread_count", 0),
                            nextCursor = optionalString(response.value, "next_cursor"),
                        )
                    )
                }
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
                } else {
                    refreshed
                }
            }
        }
    }

    private suspend fun requestJson(
        path: String,
        bearerToken: String,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = raw.takeIf { it.isNotBlank() }?.let {
                runCatching { JSONObject(it) }.getOrElse { JSONObject().put("detail", raw) }
            } ?: JSONObject()

            if (statusCode in 200..299) {
                ApiResult.Success(json)
            } else {
                ApiResult.Failure(
                    message = json.optString("detail").ifBlank {
                        when (statusCode) {
                            401 -> "Your session expired. Please log in again."
                            in 500..599 -> "Nova's server had a problem. Try again in a moment."
                            else -> "Something went wrong. Please try again."
                        }
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

    private fun parseConversation(json: JSONObject): NovaConversation {
        val kind = json.optString("kind", "direct")
        val title = json.optString("title")
        val groupAvatarUrl = resolveMediaUrl(json.optString("group_avatar_url"))
        val membersArray = json.optJSONArray("members_preview") ?: JSONArray()
        val members = buildList {
            for (index in 0 until membersArray.length()) {
                membersArray.optJSONObject(index)?.let { add(parseAuthor(it)) }
            }
        }
        val otherJson = json.optJSONObject("other_user")
        val other = if (otherJson != null) {
            parseAuthor(otherJson)
        } else {
            NovaPostAuthor(
                id = 0L,
                username = "group",
                name = title.ifBlank { "Nova group" },
                avatarUrl = groupAvatarUrl,
            )
        }
        val lastJson = json.optJSONObject("last_message")
        return NovaConversation(
            id = json.optLong("id"),
            otherUser = other,
            lastMessage = lastJson?.let(::parseMessage),
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
        val sender = parseAuthor(json.optJSONObject("sender") ?: JSONObject())
        val replyJson = json.optJSONObject("reply_to")
        val reply = replyJson?.let {
            NovaReplyPreview(
                id = it.optLong("id"),
                sender = parseAuthor(it.optJSONObject("sender") ?: JSONObject()),
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
            sender = sender,
            body = json.optString("body"),
            imageUrl = resolveMediaUrl(json.optString("image_url")),
            replyTo = reply,
            reactions = emptyList(),
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

    private fun parseAuthor(json: JSONObject): NovaPostAuthor {
        return NovaPostAuthor(
            id = json.optLong("id"),
            username = json.optString("username"),
            name = json.optString("name"),
            avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
        )
    }

    private fun optionalString(json: JSONObject, key: String): String? {
        return json.optString(key).takeIf { it.isNotBlank() && it != "null" }
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
            val apiUrl = URL(baseUrl)
            URL("${apiUrl.protocol}://${apiUrl.authority}$raw").toString()
        }.getOrDefault(raw)
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    private companion object {
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
    }
}
