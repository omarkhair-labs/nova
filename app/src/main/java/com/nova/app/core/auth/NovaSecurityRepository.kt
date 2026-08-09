package com.nova.app.core.auth

import android.content.Context
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.AuthSession
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaUser
import com.nova.app.core.push.NovaPushRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class NovaSecurityRepository(
    context: Context,
    private val api: NovaSecurityApiClient = NovaSecurityApiClient(PRODUCTION_API_URL),
    private val authApi: NovaApiClient = NovaApiClient(PRODUCTION_API_URL),
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

    suspend fun requestPasswordReset(email: String): ApiResult<String> {
        return api.requestPasswordReset(email.trim().lowercase())
    }

    suspend fun confirmPasswordReset(
        email: String,
        code: String,
        newPassword: String,
    ): ApiResult<String> {
        return api.confirmPasswordReset(
            email = email.trim().lowercase(),
            code = code.trim(),
            newPassword = newPassword,
        )
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): ApiResult<NovaUser> {
        return authenticatedSecurityCall { accessToken ->
            api.changePassword(accessToken, currentPassword, newPassword)
        }
    }

    suspend fun revokeOtherSessions(currentPassword: String): ApiResult<NovaUser> {
        return authenticatedSecurityCall { accessToken ->
            api.revokeOtherSessions(accessToken, currentPassword)
        }
    }

    private suspend fun authenticatedSecurityCall(
        call: suspend (String) -> ApiResult<AuthSession>,
    ): ApiResult<NovaUser> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return acceptRotatedSession(first.value)
            is ApiResult.Failure -> if (first.statusCode != 401) return first
        }

        return when (val refreshed = authApi.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                when (val retried = call(refreshed.value)) {
                    is ApiResult.Success -> acceptRotatedSession(retried.value)
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

    private fun acceptRotatedSession(session: AuthSession): ApiResult<NovaUser> {
        sessionStore.save(session)
        // The backend deactivates every old push token when security state
        // changes. Re-register only this freshly authenticated device.
        NovaPushRegistration.activate(appContext)
        return ApiResult.Success(session.user)
    }

    companion object {
        const val PRODUCTION_API_URL = "https://nova-production-4f6b.up.railway.app/api/v1/"
    }
}


class NovaSecurityApiClient(
    private val baseUrl: String,
) {
    suspend fun requestPasswordReset(email: String): ApiResult<String> {
        return when (
            val response = requestJson(
                path = "auth/password/reset/request/",
                method = "POST",
                body = JSONObject().put("email", email),
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                response.value.optString("detail").ifBlank {
                    "If an account exists for that email, a reset code has been sent."
                }
            )
            is ApiResult.Failure -> response
        }
    }

    suspend fun confirmPasswordReset(
        email: String,
        code: String,
        newPassword: String,
    ): ApiResult<String> {
        val body = JSONObject()
            .put("email", email)
            .put("code", code)
            .put("new_password", newPassword)
        return when (
            val response = requestJson(
                path = "auth/password/reset/confirm/",
                method = "POST",
                body = body,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                response.value.optString("detail").ifBlank { "Password reset." }
            )
            is ApiResult.Failure -> response
        }
    }

    suspend fun changePassword(
        accessToken: String,
        currentPassword: String,
        newPassword: String,
    ): ApiResult<AuthSession> {
        return when (
            val response = requestJson(
                path = "auth/password/change/",
                method = "POST",
                body = JSONObject()
                    .put("current_password", currentPassword)
                    .put("new_password", newPassword),
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> parseSession(response.value)
            is ApiResult.Failure -> response
        }
    }

    suspend fun revokeOtherSessions(
        accessToken: String,
        currentPassword: String,
    ): ApiResult<AuthSession> {
        return when (
            val response = requestJson(
                path = "auth/sessions/revoke-others/",
                method = "POST",
                body = JSONObject().put("current_password", currentPassword),
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> parseSession(response.value)
            is ApiResult.Failure -> response
        }
    }

    private fun parseSession(json: JSONObject): ApiResult<AuthSession> {
        val access = json.optString("access")
        val refresh = json.optString("refresh")
        val userJson = json.optJSONObject("user")
        if (access.isBlank() || refresh.isBlank() || userJson == null) {
            return ApiResult.Failure("Nova returned an invalid security session response.")
        }
        return ApiResult.Success(
            AuthSession(
                accessToken = access,
                refreshToken = refresh,
                user = parseUser(userJson),
            )
        )
    }

    private fun parseUser(json: JSONObject): NovaUser {
        return NovaUser(
            id = json.optLong("id"),
            email = json.optString("email"),
            username = json.optString("username"),
            name = json.optString("name"),
            avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
            followersCount = json.optInt("followers_count", 0),
            followingCount = json.optInt("following_count", 0),
            postsCount = json.optInt("posts_count", 0),
        )
    }

    private suspend fun requestJson(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        bearerToken: String? = null,
    ): ApiResult<JSONObject> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/json")
                    if (!bearerToken.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer $bearerToken")
                    }
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(body.toString())
                        }
                    }
                }

                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = raw.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()

                if (statusCode in 200..299) {
                    ApiResult.Success(json)
                } else {
                    ApiResult.Failure(
                        message = when (statusCode) {
                            400 -> json.optString("detail").ifBlank { "Check the information and try again." }
                            401 -> "Your session expired. Please log in again."
                            429 -> "Too many attempts. Wait a moment and try again."
                            503 -> json.optString("detail").ifBlank { "Password recovery is temporarily unavailable." }
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
}
