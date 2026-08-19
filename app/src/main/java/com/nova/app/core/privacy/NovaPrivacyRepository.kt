package com.nova.app.core.privacy

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPerson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


data class NovaPersonPrivacyState(
    val isPrivate: Boolean,
    val followRequested: Boolean,
    val canViewContent: Boolean,
)


data class NovaPrivacySummary(
    val isPrivate: Boolean,
    val pendingFollowRequests: Int,
    val closeFriendsCount: Int,
    val acceptedPendingRequests: Int = 0,
)


data class NovaFollowRequest(
    val id: Long,
    val requester: NovaPerson,
    val createdAt: String,
)


class NovaPrivacyRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) {
    private val sessionStore = NovaSessionStore(context.applicationContext)
    private val authApi = NovaApiClient(baseUrl)

    suspend fun summary(): ApiResult<NovaPrivacySummary> = authenticatedCall { token ->
        when (val response = requestJson("privacy/", bearerToken = token)) {
            is ApiResult.Success -> ApiResult.Success(parseSummary(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun setPrivate(isPrivate: Boolean): ApiResult<NovaPrivacySummary> = authenticatedCall { token ->
        when (
            val response = requestJson(
                path = "privacy/",
                method = "POST",
                body = JSONObject().put("is_private", isPrivate),
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseSummary(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun personState(username: String): ApiResult<NovaPersonPrivacyState> = authenticatedCall { token ->
        when (
            val response = requestJson(
                path = "people/${encode(username.trim().lowercase())}/",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parsePrivacyState(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun followRequests(): ApiResult<List<NovaFollowRequest>> = authenticatedCall { token ->
        when (val response = requestJson("follow-requests/", bearerToken = token)) {
            is ApiResult.Success -> {
                val rows = response.value.optJSONArray("results") ?: JSONArray()
                ApiResult.Success(
                    buildList {
                        for (index in 0 until rows.length()) {
                            val item = rows.optJSONObject(index) ?: continue
                            val requester = item.optJSONObject("requester") ?: continue
                            add(
                                NovaFollowRequest(
                                    id = item.optLong("id"),
                                    requester = parsePerson(requester),
                                    createdAt = item.optString("created_at"),
                                )
                            )
                        }
                    }
                )
            }
            is ApiResult.Failure -> response
        }
    }

    suspend fun acceptFollowRequest(requestId: Long): ApiResult<Unit> = requestDecision(requestId, "accept")

    suspend fun declineFollowRequest(requestId: Long): ApiResult<Unit> = requestDecision(requestId, "decline")

    suspend fun closeFriends(): ApiResult<List<NovaPerson>> = authenticatedCall { token ->
        when (val response = requestJson("close-friends/", bearerToken = token)) {
            is ApiResult.Success -> {
                val rows = response.value.optJSONArray("results") ?: JSONArray()
                ApiResult.Success(
                    buildList {
                        for (index in 0 until rows.length()) {
                            rows.optJSONObject(index)?.let { add(parsePerson(it)) }
                        }
                    }
                )
            }
            is ApiResult.Failure -> response
        }
    }

    suspend fun setCloseFriend(username: String, enabled: Boolean): ApiResult<Unit> = authenticatedCall { token ->
        val clean = username.trim().lowercase()
        val response = if (enabled) {
            requestJson(
                path = "close-friends/",
                method = "POST",
                body = JSONObject().put("username", clean),
                bearerToken = token,
            )
        } else {
            requestJson(
                path = "close-friends/${encode(clean)}/",
                method = "DELETE",
                bearerToken = token,
            )
        }
        when (response) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> response
        }
    }

    private suspend fun requestDecision(requestId: Long, action: String): ApiResult<Unit> = authenticatedCall { token ->
        when (
            val response = requestJson(
                path = "follow-requests/$requestId/$action/",
                method = "POST",
                body = JSONObject(),
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> response
        }
    }

    private fun parseSummary(json: JSONObject): NovaPrivacySummary {
        return NovaPrivacySummary(
            isPrivate = json.optBoolean("is_private", false),
            pendingFollowRequests = json.optInt("pending_follow_requests", 0),
            closeFriendsCount = json.optInt("close_friends_count", 0),
            acceptedPendingRequests = json.optInt("accepted_pending_requests", 0),
        )
    }

    private fun parsePrivacyState(json: JSONObject): NovaPersonPrivacyState {
        return NovaPersonPrivacyState(
            isPrivate = json.optBoolean("is_private", false),
            followRequested = json.optBoolean("follow_requested", false),
            canViewContent = json.optBoolean("can_view_content", true),
        )
    }

    private fun parsePerson(json: JSONObject): NovaPerson {
        return NovaPerson(
            id = json.optLong("id"),
            username = json.optString("username"),
            name = json.optString("name"),
            avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
            followersCount = json.optInt("followers_count", 0),
            followingCount = json.optInt("following_count", 0),
            postsCount = json.optInt("posts_count", 0),
            isFollowing = json.optBoolean("is_following", false),
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
                    message = json.optString("detail").ifBlank {
                        when (statusCode) {
                            401 -> "Your session expired. Please log in again."
                            403 -> "You don't have access to that private content."
                            404 -> "That account is no longer available."
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
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
    }
}
