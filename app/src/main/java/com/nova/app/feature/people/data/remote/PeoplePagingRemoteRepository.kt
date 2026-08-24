package com.nova.app.feature.people.data.remote

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource
import com.nova.app.feature.people.data.PeoplePagingRepository
import com.nova.app.feature.people.data.parseNovaPerson
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.people.domain.model.NovaPersonPage
import com.nova.app.feature.people.domain.model.NovaProfilePostPage
import com.nova.app.feature.posts.data.parseNovaPost
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


class PeoplePagingRemoteRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) : PeoplePagingRepository {
    private val sessionStore = NovaSessionStore(context.applicationContext)
    private val authRemote = AuthRemoteDataSource(NovaApiClient(baseUrl))

    override suspend fun people(
        query: String,
        cursor: String?,
    ): ApiResult<NovaPersonPage> {
        return authenticatedCall { token ->
            requestPeoplePage(
                path = "people/",
                bearerToken = token,
                query = query,
                cursor = cursor,
            )
        }
    }

    override suspend fun discover(
        query: String,
        cursor: String?,
        filter: String,
    ): ApiResult<NovaPersonPage> {
        return authenticatedCall { token ->
            requestPeoplePage(
                path = "people/",
                bearerToken = token,
                query = query,
                cursor = cursor,
                filter = filter,
            )
        }
    }

    override suspend fun followers(
        username: String,
        query: String,
        cursor: String?,
    ): ApiResult<NovaPersonPage> {
        return authenticatedCall { token ->
            requestPeoplePage(
                path = "people/${encode(username.trim().lowercase())}/followers/",
                bearerToken = token,
                query = query,
                cursor = cursor,
            )
        }
    }

    override suspend fun following(
        username: String,
        query: String,
        cursor: String?,
    ): ApiResult<NovaPersonPage> {
        return authenticatedCall { token ->
            requestPeoplePage(
                path = "people/${encode(username.trim().lowercase())}/following/",
                bearerToken = token,
                query = query,
                cursor = cursor,
            )
        }
    }

    override suspend fun profilePosts(
        username: String,
        cursor: String?,
    ): ApiResult<NovaProfilePostPage> {
        return authenticatedCall { token ->
            requestProfilePostPage(
                path = "people/${encode(username.trim().lowercase())}/posts/",
                bearerToken = token,
                cursor = cursor,
            )
        }
    }

    override suspend fun profileReposts(
        username: String,
        cursor: String?,
    ): ApiResult<NovaProfilePostPage> {
        return authenticatedCall { token ->
            requestProfilePostPage(
                path = "people/${encode(username.trim().lowercase())}/reposts/",
                bearerToken = token,
                cursor = cursor,
            )
        }
    }

    private suspend fun requestProfilePostPage(
        path: String,
        bearerToken: String,
        cursor: String?,
    ): ApiResult<NovaProfilePostPage> {
        val resolvedPath = if (cursor.isNullOrBlank()) {
            path
        } else {
            "$path?cursor=${encode(cursor)}"
        }
        return when (val response = requestJson(resolvedPath, bearerToken = bearerToken)) {
            is ApiResult.Success -> {
                val array = response.value.optJSONArray("results") ?: JSONArray()
                val posts = buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { add(parseNovaPost(it, ::resolveMediaUrl)) }
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

    private suspend fun requestPeoplePage(
        path: String,
        bearerToken: String,
        query: String,
        cursor: String?,
        filter: String = "people",
    ): ApiResult<NovaPersonPage> {
        val parameters = buildList {
            val cleanQuery = query.trim()
            if (cleanQuery.isNotBlank()) add("q=${encode(cleanQuery)}")
            if (!cursor.isNullOrBlank()) add("cursor=${encode(cursor)}")
            if (filter.isNotBlank() && filter != "people") add("filter=${encode(filter)}")
        }
        val resolvedPath = if (parameters.isEmpty()) path else "$path?${parameters.joinToString("&")}"

        return when (val response = requestJson(resolvedPath, bearerToken = bearerToken)) {
            is ApiResult.Success -> {
                val array = response.value.optJSONArray("results") ?: JSONArray()
                val people = mutableListOf<NovaPerson>()
                val privacy = mutableMapOf<Long, NovaPersonPrivacyState>()
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val person = parseNovaPerson(item, ::resolveMediaUrl)
                    people += person
                    privacy[person.id] = parsePrivacyState(item)
                }
                ApiResult.Success(
                    NovaPersonPage(
                        people = people,
                        nextCursor = optionalString(response.value, "next_cursor"),
                        privacyByUserId = privacy,
                    )
                )
            }
            is ApiResult.Failure -> response
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

        return when (val refreshed = authRemote.refresh(stored.refreshToken)) {
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
                            403 -> "Follow this private account to see this content."
                            404 -> "Nova couldn't find that profile."
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

    private fun parsePrivacyState(json: JSONObject): NovaPersonPrivacyState {
        return NovaPersonPrivacyState(
            isPrivate = json.optBoolean("is_private", false),
            followRequested = json.optBoolean("follow_requested", false),
            canViewContent = json.optBoolean("can_view_content", true),
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

    private fun encode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    private companion object {
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
    }
}
