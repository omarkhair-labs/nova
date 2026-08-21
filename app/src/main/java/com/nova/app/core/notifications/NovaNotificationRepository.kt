package com.nova.app.core.notifications

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.notifications.data.NotificationsRepository
import com.nova.app.feature.notifications.domain.model.NovaNotification
import com.nova.app.feature.notifications.domain.model.NovaNotificationPage
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


class NovaNotificationRepository(
    context: Context,
    private val api: NovaNotificationApiClient = NovaNotificationApiClient(PRODUCTION_API_URL),
    private val authApi: NovaApiClient = NovaApiClient(PRODUCTION_API_URL),
) : NotificationsRepository {
    private val sessionStore = NovaSessionStore(context.applicationContext)

    override suspend fun notifications(cursor: String?): ApiResult<NovaNotificationPage> {
        return authenticatedCall { accessToken ->
            api.notifications(accessToken, cursor)
        }
    }

    override suspend fun markAllRead(): ApiResult<Int> {
        return authenticatedCall { accessToken ->
            api.markAllRead(accessToken)
        }
    }

    private suspend fun <T> authenticatedCall(
        call: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return first
            is ApiResult.Failure -> {
                if (first.statusCode != 401) return first
            }
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

    private companion object {
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
    }
}


class NovaNotificationApiClient(
    private val baseUrl: String,
) {
    suspend fun notifications(
        accessToken: String,
        cursor: String? = null,
    ): ApiResult<NovaNotificationPage> {
        val path = if (cursor.isNullOrBlank()) {
            "notifications/"
        } else {
            "notifications/?cursor=${encode(cursor)}"
        }

        return when (val response = requestJson(path, bearerToken = accessToken)) {
            is ApiResult.Success -> {
                val array = response.value.optJSONArray("results") ?: JSONArray()
                val items = buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { add(parseNotification(it)) }
                    }
                }
                val nextCursor = response.value.optString("next_cursor")
                    .takeIf { it.isNotBlank() && it != "null" }
                ApiResult.Success(
                    NovaNotificationPage(
                        notifications = items,
                        nextCursor = nextCursor,
                        unreadCount = response.value.optInt("unread_count", 0),
                    )
                )
            }

            is ApiResult.Failure -> response
        }
    }

    suspend fun markAllRead(accessToken: String): ApiResult<Int> {
        return when (
            val response = requestJson(
                path = "notifications/read/",
                method = "POST",
                body = JSONObject(),
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(response.value.optInt("unread_count", 0))
            is ApiResult.Failure -> response
        }
    }

    private fun parseNotification(json: JSONObject): NovaNotification {
        val actor = json.optJSONObject("actor") ?: JSONObject()
        val rawPostId = json.opt("post_id")
        val postId = when (rawPostId) {
            null, JSONObject.NULL -> null
            is Number -> rawPostId.toLong()
            else -> rawPostId.toString().toLongOrNull()
        }
        val rawReelId = json.opt("reel_id")
        val reelId = when (rawReelId) {
            null, JSONObject.NULL -> null
            is Number -> rawReelId.toLong()
            else -> rawReelId.toString().toLongOrNull()
        }

        return NovaNotification(
            id = json.optLong("id"),
            kind = json.optString("kind"),
            actor = NovaPostAuthor(
                id = actor.optLong("id"),
                username = actor.optString("username"),
                name = actor.optString("name"),
                avatarUrl = resolveMediaUrl(actor.optString("avatar_url")),
            ),
            postId = postId,
            reelId = reelId,
            reelAuthorUsername = json.optString("reel_author_username"),
            commentPreview = json.optString("comment_preview"),
            createdAt = json.optString("created_at"),
            isRead = json.optBoolean("is_read", false),
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
                        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(body.toString())
                        }
                    }
                }

                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = raw.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()

                if (statusCode in 200..299) {
                    ApiResult.Success(json)
                } else {
                    ApiResult.Failure(
                        message = when (statusCode) {
                            400 -> json.optString("detail").ifBlank { "Nova couldn't load that activity page." }
                            401 -> "Your session expired. Please log in again."
                            429 -> "Too many requests. Give Nova a moment and try again."
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

    private fun encode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }
}
