package com.nova.app.core.sharing

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import com.nova.app.feature.sharing.data.SharingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


data class NovaRepostState(
    val postId: Long,
    val repostsCount: Int,
    val isReposted: Boolean,
    val stillInFeed: Boolean,
    val feedRepostedBy: NovaPostAuthor?,
)


class NovaSharingRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) : SharingRepository {
    private val sessionStore = NovaSessionStore(context.applicationContext)
    private val authApi = NovaApiClient(baseUrl)

    suspend fun repostState(postId: Long): ApiResult<NovaRepostState> {
        return authenticatedCall { token ->
            when (val response = requestJson("posts/$postId/repost/", bearerToken = token)) {
                is ApiResult.Success -> ApiResult.Success(parseRepostState(response.value))
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun setReposted(postId: Long, reposted: Boolean): ApiResult<NovaRepostState> {
        return authenticatedCall { token ->
            val response = if (reposted) {
                requestJson(
                    path = "posts/$postId/repost/",
                    method = "POST",
                    body = JSONObject(),
                    bearerToken = token,
                )
            } else {
                requestJson(
                    path = "posts/$postId/repost/",
                    method = "DELETE",
                    bearerToken = token,
                )
            }
            when (response) {
                is ApiResult.Success -> ApiResult.Success(parseRepostState(response.value))
                is ApiResult.Failure -> response
            }
        }
    }

    override suspend fun sharePost(recipientUsername: String, postId: Long): ApiResult<Unit> {
        return share(
            JSONObject()
                .put("recipient_username", recipientUsername.trim().lowercase())
                .put("kind", "post")
                .put("post_id", postId)
        )
    }

    override suspend fun shareReel(recipientUsername: String, reelId: Long): ApiResult<Unit> {
        return share(
            JSONObject()
                .put("recipient_username", recipientUsername.trim().lowercase())
                .put("kind", "reel")
                .put("reel_id", reelId)
        )
    }

    override suspend fun shareProfile(recipientUsername: String, profileUsername: String): ApiResult<Unit> {
        return share(
            JSONObject()
                .put("recipient_username", recipientUsername.trim().lowercase())
                .put("kind", "profile")
                .put("profile_username", profileUsername.trim().lowercase())
        )
    }

    override suspend fun sharePostToConversation(conversationId: Long, postId: Long): ApiResult<Unit> {
        if (conversationId <= 0L) return ApiResult.Failure("Choose a group to share with.")
        return share(
            JSONObject()
                .put("conversation_id", conversationId)
                .put("kind", "post")
                .put("post_id", postId)
        )
    }

    override suspend fun shareReelToConversation(conversationId: Long, reelId: Long): ApiResult<Unit> {
        if (conversationId <= 0L) return ApiResult.Failure("Choose a group to share with.")
        return share(
            JSONObject()
                .put("conversation_id", conversationId)
                .put("kind", "reel")
                .put("reel_id", reelId)
        )
    }

    override suspend fun shareProfileToConversation(
        conversationId: Long,
        profileUsername: String,
    ): ApiResult<Unit> {
        if (conversationId <= 0L) return ApiResult.Failure("Choose a group to share with.")
        return share(
            JSONObject()
                .put("conversation_id", conversationId)
                .put("kind", "profile")
                .put("profile_username", profileUsername.trim().lowercase())
        )
    }

    override suspend fun addPostToStory(
        postId: Long,
        caption: String,
        audience: String,
    ): ApiResult<Unit> {
        return addContentToStory(
            targetKey = "shared_post_id",
            targetId = postId,
            caption = caption,
            audience = audience,
        )
    }

    override suspend fun addReelToStory(
        reelId: Long,
        caption: String,
        audience: String,
    ): ApiResult<Unit> {
        return addContentToStory(
            targetKey = "shared_reel_id",
            targetId = reelId,
            caption = caption,
            audience = audience,
        )
    }

    private suspend fun addContentToStory(
        targetKey: String,
        targetId: Long,
        caption: String,
        audience: String,
    ): ApiResult<Unit> {
        val cleanAudience = audience.takeIf { it == "followers" || it == "close_friends" }
            ?: return ApiResult.Failure("Choose a valid Story audience.")
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "stories/",
                    method = "POST",
                    body = JSONObject()
                        .put(targetKey, targetId)
                        .put("caption", caption.trim().take(240))
                        .put("audience", cleanAudience),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> response
            }
        }
    }

    private suspend fun share(body: JSONObject): ApiResult<Unit> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "shares/messages/",
                    method = "POST",
                    body = body,
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> response
            }
        }
    }

    private fun parseRepostState(json: JSONObject): NovaRepostState {
        val reposter = json.optJSONObject("feed_reposted_by")?.let { user ->
            NovaPostAuthor(
                id = user.optLong("id"),
                username = user.optString("username"),
                name = user.optString("name"),
                avatarUrl = resolveMediaUrl(user.optString("avatar_url")),
            )
        }
        return NovaRepostState(
            postId = json.optLong("post_id"),
            repostsCount = json.optInt("reposts_count", 0),
            isReposted = json.optBoolean("is_reposted", false),
            stillInFeed = json.optBoolean("still_in_feed", true),
            feedRepostedBy = reposter,
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
            val json = if (raw.isBlank()) JSONObject() else runCatching { JSONObject(raw) }
                .getOrElse { JSONObject().put("detail", raw) }
            if (statusCode in 200..299) {
                ApiResult.Success(json)
            } else {
                ApiResult.Failure(
                    message = when (statusCode) {
                        401 -> "Your session expired. Please log in again."
                        403 -> json.optString("detail").ifBlank { "That share isn't available." }
                        404 -> "That content is no longer available."
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

    private fun resolveMediaUrl(raw: String): String {
        if (raw.isBlank() || raw == "null") return ""
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        return runCatching {
            val apiUrl = URL(baseUrl)
            URL("${apiUrl.protocol}://${apiUrl.authority}$raw").toString()
        }.getOrDefault(raw)
    }

    private companion object {
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
    }
}
