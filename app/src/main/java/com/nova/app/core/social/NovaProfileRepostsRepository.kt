package com.nova.app.core.social

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPost
import com.nova.app.core.network.NovaPostAuthor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


class NovaProfileRepostsRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) {
    private val sessionStore = NovaSessionStore(context.applicationContext)
    private val authApi = NovaApiClient(baseUrl)

    suspend fun reposts(
        username: String,
        cursor: String? = null,
    ): ApiResult<NovaProfilePostPage> {
        val cleanUsername = username.trim().lowercase()
        if (cleanUsername.isBlank()) return ApiResult.Failure("Nova couldn't find that profile.")

        return authenticatedCall { token ->
            val params = buildList {
                add("kind=reposts")
                if (!cursor.isNullOrBlank()) add("cursor=${encode(cursor)}")
            }
            when (
                val response = requestJson(
                    path = "people/${encode(cleanUsername)}/posts/?${params.joinToString("&")}",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> {
                    val rows = response.value.optJSONArray("results") ?: JSONArray()
                    val posts = buildList {
                        for (index in 0 until rows.length()) {
                            rows.optJSONObject(index)?.let { add(parsePost(it)) }
                        }
                    }
                    ApiResult.Success(
                        NovaProfilePostPage(
                            posts = posts,
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
                if (refreshed.statusCode == 400 || refreshed.statusCode == 401) sessionStore.clear()
                refreshed
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
                    json.optString("detail").ifBlank {
                        when (statusCode) {
                            401 -> "Your session expired. Please log in again."
                            403 -> "Follow this private account to see this content."
                            404 -> "Nova couldn't find that profile."
                            else -> "Couldn't load reposts. Try again."
                        }
                    },
                    statusCode,
                )
            }
        } catch (_: Exception) {
            ApiResult.Failure("Can't reach Nova right now. Check your connection and try again.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun parsePost(json: JSONObject): NovaPost {
        val author = json.optJSONObject("author") ?: JSONObject()
        return NovaPost(
            id = json.optLong("id"),
            author = NovaPostAuthor(
                id = author.optLong("id"),
                username = author.optString("username"),
                name = author.optString("name"),
                avatarUrl = resolveMediaUrl(author.optString("avatar_url")),
            ),
            imageUrl = resolveMediaUrl(json.optString("image_url")),
            caption = json.optString("caption"),
            createdAt = json.optString("created_at"),
            isMine = json.optBoolean("is_mine", false),
            likesCount = json.optInt("likes_count", 0),
            commentsCount = json.optInt("comments_count", 0),
            isLiked = json.optBoolean("is_liked", false),
        )
    }

    private fun optionalString(json: JSONObject, key: String): String? {
        return json.optString(key).takeIf { it.isNotBlank() && it != "null" }
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
