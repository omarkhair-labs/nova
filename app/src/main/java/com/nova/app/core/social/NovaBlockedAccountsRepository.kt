package com.nova.app.core.social

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


class NovaBlockedAccountsRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) {
    private val sessionStore = NovaSessionStore(context.applicationContext)
    private val authApi = NovaApiClient(baseUrl)

    suspend fun blockedAccounts(): ApiResult<List<NovaPerson>> {
        return authenticatedCall { token ->
            when (val response = requestJson("auth/blocks/", bearerToken = token)) {
                is ApiResult.Success -> {
                    val rows = response.value.optJSONArray("results") ?: JSONArray()
                    ApiResult.Success(
                        buildList {
                            for (index in 0 until rows.length()) {
                                val item = rows.optJSONObject(index) ?: continue
                                add(parsePerson(item))
                            }
                        }
                    )
                }
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun unblock(username: String): ApiResult<Unit> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "people/${encode(username.trim().lowercase())}/block/",
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
        bearerToken: String,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 10_000
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
                        401 -> "Your session expired. Please log in again."
                        404 -> "That account is no longer available."
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

    private fun parsePerson(json: JSONObject): NovaPerson {
        return NovaPerson(
            id = json.optLong("id"),
            username = json.optString("username"),
            name = json.optString("name"),
            avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
            followersCount = json.optInt("followers_count", 0),
            followingCount = json.optInt("following_count", 0),
            postsCount = json.optInt("posts_count", 0),
            isFollowing = false,
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
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
    }
}
