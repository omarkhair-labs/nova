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


class NovaAccountSecurityRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)
    private val authApi = NovaApiClient(baseUrl)

    suspend fun requestPasswordReset(email: String): ApiResult<String> {
        val body = JSONObject().put("email", email.trim().lowercase())
        return when (
            val result = requestJson(
                path = "auth/password/reset/request/",
                method = "POST",
                body = body,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                result.value.optString("detail").ifBlank {
                    "If an account exists for that email, a reset code has been sent."
                }
            )
            is ApiResult.Failure -> result
        }
    }

    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): ApiResult<String> {
        val body = JSONObject()
            .put("email", email.trim().lowercase())
            .put("code", code.trim())
            .put("new_password", newPassword)
        return when (
            val result = requestJson(
                path = "auth/password/reset/confirm/",
                method = "POST",
                body = body,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                result.value.optString("detail").ifBlank {
                    "Password reset. Log in with your new password."
                }
            )
            is ApiResult.Failure -> result
        }
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): ApiResult<NovaUser> {
        return authenticatedSessionCall(
            path = "auth/password/change/",
            body = JSONObject()
                .put("current_password", currentPassword)
                .put("new_password", newPassword),
        )
    }

    suspend fun revokeOtherSessions(currentPassword: String): ApiResult<NovaUser> {
        return authenticatedSessionCall(
            path = "auth/sessions/revoke-others/",
            body = JSONObject().put("current_password", currentPassword),
        )
    }

    suspend fun deleteAccount(currentPassword: String): ApiResult<String> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)
        val body = JSONObject().put("current_password", currentPassword)

        var accessToken = stored.accessToken
        var response = requestJson(
            path = "auth/account/delete/",
            method = "POST",
            body = body,
            bearerToken = accessToken,
        )

        if (response is ApiResult.Failure && response.statusCode == 401) {
            when (val refreshed = authApi.refresh(stored.refreshToken)) {
                is ApiResult.Success -> {
                    accessToken = refreshed.value
                    sessionStore.updateAccessToken(accessToken)
                    response = requestJson(
                        path = "auth/account/delete/",
                        method = "POST",
                        body = body,
                        bearerToken = accessToken,
                    )
                }
                is ApiResult.Failure -> {
                    if (refreshed.statusCode == 400 || refreshed.statusCode == 401) {
                        sessionStore.clear()
                        return ApiResult.Failure("Your session expired. Please log in again.", 401)
                    }
                    return refreshed
                }
            }
        }

        return when (response) {
            is ApiResult.Success -> {
                sessionStore.clear()
                ApiResult.Success(
                    response.value.optString("detail").ifBlank { "Your Nova account was deleted." }
                )
            }
            is ApiResult.Failure -> {
                if (response.statusCode == 401) sessionStore.clear()
                response
            }
        }
    }

    private suspend fun authenticatedSessionCall(
        path: String,
        body: JSONObject,
    ): ApiResult<NovaUser> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        var accessToken = stored.accessToken
        var response = requestJson(
            path = path,
            method = "POST",
            body = body,
            bearerToken = accessToken,
        )

        if (response is ApiResult.Failure && response.statusCode == 401) {
            when (val refreshed = authApi.refresh(stored.refreshToken)) {
                is ApiResult.Success -> {
                    accessToken = refreshed.value
                    sessionStore.updateAccessToken(accessToken)
                    response = requestJson(
                        path = path,
                        method = "POST",
                        body = body,
                        bearerToken = accessToken,
                    )
                }
                is ApiResult.Failure -> {
                    if (refreshed.statusCode == 400 || refreshed.statusCode == 401) {
                        sessionStore.clear()
                        return ApiResult.Failure("Your session expired. Please log in again.", 401)
                    }
                    return refreshed
                }
            }
        }

        return when (response) {
            is ApiResult.Success -> {
                val session = parseSession(response.value)
                sessionStore.save(session)
                NovaPushRegistration.activate(appContext)
                ApiResult.Success(session.user)
            }
            is ApiResult.Failure -> {
                if (response.statusCode == 401) sessionStore.clear()
                response
            }
        }
    }

    private fun parseSession(json: JSONObject): AuthSession {
        val user = json.optJSONObject("user") ?: JSONObject()
        return AuthSession(
            accessToken = json.optString("access"),
            refreshToken = json.optString("refresh"),
            user = NovaUser(
                id = user.optLong("id"),
                email = user.optString("email"),
                username = user.optString("username"),
                name = user.optString("name"),
                avatarUrl = resolveMediaUrl(user.optString("avatar_url")),
                followersCount = user.optInt("followers_count", 0),
                followingCount = user.optInt("following_count", 0),
                postsCount = user.optInt("posts_count", 0),
            ),
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
                    connectTimeout = 12_000
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
                            400 -> json.optString("detail").ifBlank { "Check those details and try again." }
                            401 -> "Your session expired. Please log in again."
                            403 -> json.optString("detail").ifBlank { "That action isn't available." }
                            429 -> "Too many attempts. Try again shortly."
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

    private companion object {
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
    }
}
