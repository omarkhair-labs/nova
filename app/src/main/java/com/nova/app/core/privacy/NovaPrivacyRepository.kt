package com.nova.app.core.privacy

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.data.FollowRequestRepository
import com.nova.app.feature.privacy.data.PrivacyRepository
import com.nova.app.feature.privacy.domain.model.NovaFollowRequest
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import com.nova.app.feature.privacy.domain.model.NovaPrivacySummary
import com.nova.app.feature.privacy.domain.model.NovaNotificationPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


class NovaPrivacyRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) : PrivacyRepository, FollowRequestRepository {
    private val sessionStore = NovaSessionStore(context.applicationContext)
    private val authApi = NovaApiClient(baseUrl)

    override suspend fun summary(): ApiResult<NovaPrivacySummary> = authenticatedCall { token ->
        when (val response = requestJson("privacy/", bearerToken = token)) {
            is ApiResult.Success -> ApiResult.Success(parseSummary(response.value))
            is ApiResult.Failure -> response
        }
    }

    override suspend fun setPrivate(isPrivate: Boolean): ApiResult<NovaPrivacySummary> = authenticatedCall { token ->
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

    override suspend fun updateSettings(
        showActivityStatus: Boolean?,
        sendReadReceipts: Boolean?,
        storyAudience: String?,
    ): ApiResult<NovaPrivacySummary> = authenticatedCall { token ->
        val body = JSONObject()
        showActivityStatus?.let { body.put("show_activity_status", it) }
        sendReadReceipts?.let { body.put("send_read_receipts", it) }
        storyAudience?.let { body.put("story_audience", it) }
        when (
            val response = requestJson(
                path = "privacy/",
                method = "POST",
                body = body,
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseSummary(response.value))
            is ApiResult.Failure -> response
        }
    }

    override suspend fun personState(username: String): ApiResult<NovaPersonPrivacyState> = authenticatedCall { token ->
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

    override suspend fun followRequests(): ApiResult<List<NovaFollowRequest>> = authenticatedCall { token ->
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

    override suspend fun acceptFollowRequest(requestId: Long): ApiResult<Unit> =
        requestDecision(requestId, "accept")

    override suspend fun sentFollowRequests(): ApiResult<List<NovaFollowRequest>> = authenticatedCall { token ->
        when (val response = requestJson("follow-requests/sent/", bearerToken = token)) {
            is ApiResult.Success -> {
                val rows = response.value.optJSONArray("results") ?: JSONArray()
                ApiResult.Success(buildList {
                    for (index in 0 until rows.length()) {
                        val item = rows.optJSONObject(index) ?: continue
                        val person = item.optJSONObject("target")?.let(::parsePerson) ?: continue
                        add(
                            NovaFollowRequest(
                                id = item.optLong("id"),
                                requester = person,
                                target = person,
                                createdAt = item.optString("created_at"),
                            )
                        )
                    }
                })
            }
            is ApiResult.Failure -> response
        }
    }

    override suspend fun declineFollowRequest(requestId: Long): ApiResult<Unit> =
        requestDecision(requestId, "decline")

    override suspend fun closeFriends(): ApiResult<List<NovaPerson>> = authenticatedCall { token ->
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

    override suspend fun setCloseFriend(username: String, enabled: Boolean): ApiResult<Unit> = authenticatedCall { token ->
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

    override suspend fun notificationPreferences(): ApiResult<NovaNotificationPreferences> = authenticatedCall { token ->
        when (val response = requestJson("notification-preferences/", bearerToken = token)) {
            is ApiResult.Success -> ApiResult.Success(parseNotificationPreferences(response.value))
            is ApiResult.Failure -> response
        }
    }

    override suspend fun updateNotificationPreference(
        key: String,
        enabled: Boolean,
    ): ApiResult<NovaNotificationPreferences> = authenticatedCall { token ->
        when (
            val response = requestJson(
                path = "notification-preferences/",
                method = "POST",
                body = JSONObject().put(key, enabled),
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseNotificationPreferences(response.value))
            is ApiResult.Failure -> response
        }
    }

    private fun parseNotificationPreferences(json: JSONObject) = NovaNotificationPreferences(
        likesCommentsShares = json.optBoolean("likes_comments_shares", true),
        mentionsTags = json.optBoolean("mentions_tags", true),
        followers = json.optBoolean("followers", true),
        messages = json.optBoolean("messages", true),
        liveSessions = json.optBoolean("live_sessions", true),
        reelsStories = json.optBoolean("reels_stories", true),
        eventsSpaces = json.optBoolean("events_spaces", true),
        productUpdates = json.optBoolean("product_updates", true),
    )

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
            showActivityStatus = json.optBoolean("show_activity_status", true),
            sendReadReceipts = json.optBoolean("send_read_receipts", true),
            storyAudience = json.optString("story_audience", "followers"),
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
            bio = json.optString("bio"),
            location = json.optString("location"),
            link = json.optString("link"),
            interests = buildList {
                val values = json.optJSONArray("interests")
                if (values != null) {
                    for (index in 0 until values.length()) {
                        values.optString(index).takeIf(String::isNotBlank)?.let(::add)
                    }
                }
            },
            profileTheme = json.optString("profile_theme", "violet"),
            showOrbit = json.optBoolean("show_orbit", true),
            isVerified = json.optBoolean("is_verified", false),
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
