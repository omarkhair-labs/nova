package com.nova.app.core.messaging

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.messages.domain.model.NovaConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


data class NovaGroupMember(
    val user: NovaPostAuthor,
    val role: String,
    val joinedAt: String,
)


data class NovaGroupDetail(
    val conversation: NovaConversation,
    val members: List<NovaGroupMember>,
)


class NovaGroupMessagingRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) {
    private val sessionStore = NovaSessionStore(context.applicationContext)
    private val authApi = NovaApiClient(baseUrl)

    suspend fun createGroup(
        title: String,
        usernames: List<String>,
    ): ApiResult<NovaConversation> {
        val cleanTitle = title.trim()
        val cleanUsers = usernames.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        if (cleanTitle.isBlank()) return ApiResult.Failure("Give the group a name.")
        if (cleanTitle.length > 80) return ApiResult.Failure("Group name must be 80 characters or fewer.")
        if (cleanUsers.size < 2) return ApiResult.Failure("Choose at least two people for the group.")

        return authenticatedCall { token ->
            val users = JSONArray().apply { cleanUsers.forEach(::put) }
            when (
                val response = requestJson(
                    path = "conversations/groups/",
                    method = "POST",
                    body = JSONObject().put("title", cleanTitle).put("usernames", users),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseConversation(response.value))
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun detail(conversationId: Long): ApiResult<NovaGroupDetail> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/group/",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseDetail(response.value))
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun addMembers(
        conversationId: Long,
        usernames: List<String>,
    ): ApiResult<NovaGroupDetail> {
        val cleanUsers = usernames.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        if (cleanUsers.isEmpty()) return ApiResult.Failure("Choose someone to add.")
        return authenticatedCall { token ->
            val users = JSONArray().apply { cleanUsers.forEach(::put) }
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/group/members/",
                    method = "POST",
                    body = JSONObject().put("usernames", users),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseDetail(response.value))
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun removeMember(
        conversationId: Long,
        username: String,
    ): ApiResult<NovaGroupDetail?> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/group/members/${encode(username.trim().lowercase())}/",
                    method = "DELETE",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> {
                    if (response.value.optBoolean("deleted", false)) {
                        ApiResult.Success(null)
                    } else {
                        ApiResult.Success(parseDetail(response.value))
                    }
                }
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun leaveGroup(conversationId: Long): ApiResult<Boolean> {
        val username = sessionStore.load()?.cachedUser?.username
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)
        return when (val result = removeMember(conversationId, username)) {
            is ApiResult.Success -> ApiResult.Success(true)
            is ApiResult.Failure -> result
        }
    }

    suspend fun deleteGroup(conversationId: Long): ApiResult<Unit> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/group/",
                    method = "DELETE",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(Unit)
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
        method: String = "GET",
        body: JSONObject? = null,
        bearerToken: String,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 12_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $bearerToken")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
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
                            403 -> "You can't manage this group."
                            404 -> "That group is no longer available."
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

    private fun parseDetail(json: JSONObject): NovaGroupDetail {
        val conversationJson = json.optJSONObject("conversation") ?: JSONObject()
        val rows = json.optJSONArray("members") ?: JSONArray()
        val members = buildList {
            for (index in 0 until rows.length()) {
                val item = rows.optJSONObject(index) ?: continue
                val user = item.optJSONObject("user") ?: continue
                add(
                    NovaGroupMember(
                        user = parseAuthor(user),
                        role = item.optString("role"),
                        joinedAt = item.optString("joined_at"),
                    )
                )
            }
        }
        return NovaGroupDetail(
            conversation = parseConversation(conversationJson),
            members = members,
        )
    }

    private fun parseConversation(json: JSONObject): NovaConversation {
        val title = json.optString("title")
        val previewRows = json.optJSONArray("members_preview") ?: JSONArray()
        val preview = buildList {
            for (index in 0 until previewRows.length()) {
                previewRows.optJSONObject(index)?.let { add(parseAuthor(it)) }
            }
        }
        return NovaConversation(
            id = json.optLong("id"),
            otherUser = NovaPostAuthor(
                id = 0L,
                username = "group",
                name = title.ifBlank { "Nova group" },
                avatarUrl = "",
            ),
            lastMessage = null,
            unreadCount = json.optInt("unread_count", 0),
            createdAt = json.optString("created_at"),
            updatedAt = json.optString("updated_at"),
            kind = json.optString("kind", "group"),
            title = title,
            membersPreview = preview,
            membersCount = json.optInt("members_count", preview.size + 1),
            currentUserRole = json.optString("current_user_role"),
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
        const val PRODUCTION_API_URL = "https://nova-production-4f6b.up.railway.app/api/v1/"
    }
}
