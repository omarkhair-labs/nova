package com.nova.app.core.reels

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


class NovaProfileReelsRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) {
    private val sessionStore = NovaSessionStore(context.applicationContext)
    private val authApi = NovaApiClient(baseUrl)

    suspend fun reels(
        username: String,
        cursor: String? = null,
    ): ApiResult<NovaReelPage> = profileReels(username, cursor, source = "authored")

    suspend fun repostedReels(
        username: String,
        cursor: String? = null,
    ): ApiResult<NovaReelPage> = profileReels(username, cursor, source = "reposted")

    private suspend fun profileReels(
        username: String,
        cursor: String?,
        source: String,
    ): ApiResult<NovaReelPage> {
        val cleanUsername = username.trim().lowercase()
        if (cleanUsername.isBlank()) return ApiResult.Failure("Choose a profile first.")

        val encodedUsername = encode(cleanUsername)
        val query = buildList {
            if (source != "authored") add("source=${encode(source)}")
            if (!cursor.isNullOrBlank()) add("cursor=${encode(cursor.trim())}")
        }.joinToString("&")
        val path = "reels/profile/$encodedUsername/" + if (query.isBlank()) "" else "?$query"

        return authenticatedCall { token ->
            when (val result = requestJson(path, token)) {
                is ApiResult.Success -> {
                    val rows = result.value.optJSONArray("results") ?: JSONArray()
                    val nextCursor = result.value.optString("next_cursor")
                        .takeIf { it.isNotBlank() && it != "null" }
                    ApiResult.Success(
                        NovaReelPage(
                            reels = buildList {
                                for (index in 0 until rows.length()) {
                                    rows.optJSONObject(index)?.let { add(parseReel(it)) }
                                }
                            },
                            nextCursor = nextCursor,
                        )
                    )
                }
                is ApiResult.Failure -> result
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
                readTimeout = 30_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = if (raw.isBlank()) JSONObject() else runCatching { JSONObject(raw) }
                .getOrElse { JSONObject().put("detail", raw) }
            if (statusCode in 200..299) {
                ApiResult.Success(json)
            } else {
                ApiResult.Failure(
                    message = when (statusCode) {
                        400 -> json.optString("detail").ifBlank { "Nova couldn't load those Reels." }
                        401 -> "Your session expired. Please log in again."
                        403 -> json.optString("detail").ifBlank { "You can't view these Reels." }
                        404 -> "That profile is no longer available."
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

    private fun parseReel(json: JSONObject): NovaReel {
        return NovaReel(
            id = json.optLong("id"),
            author = parseAuthor(json.optJSONObject("author") ?: JSONObject()),
            videoUrl = resolveMediaUrl(json.optString("video_url")),
            caption = json.optString("caption"),
            createdAt = json.optString("created_at"),
            isMine = json.optBoolean("is_mine"),
            likesCount = json.optInt("likes_count", 0),
            commentsCount = json.optInt("comments_count", 0),
            repostsCount = json.optInt("reposts_count", 0),
            isLiked = json.optBoolean("is_liked", false),
            isReposted = json.optBoolean("is_reposted", false),
            repostedBy = json.optJSONObject("reposted_by")?.let(::parseAuthor),
        )
    }

    private fun parseAuthor(json: JSONObject): NovaReelAuthor {
        return NovaReelAuthor(
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

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        const val PRODUCTION_API_URL = "https://nova-production-4f6b.up.railway.app/api/v1/"
    }
}
