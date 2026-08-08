package com.nova.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


data class NovaUser(
    val id: Long,
    val email: String,
    val username: String,
    val name: String,
    val avatarUrl: String,
)


data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: NovaUser,
)


sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(
        val message: String,
        val statusCode: Int? = null,
    ) : ApiResult<Nothing>
}


class NovaApiClient(
    private val baseUrl: String = "http://127.0.0.1:8000/api/v1/",
) {
    suspend fun register(
        email: String,
        password: String,
        username: String,
        name: String,
    ): ApiResult<AuthSession> {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("username", username)
            .put("name", name)

        return when (val response = requestJson("auth/register/", "POST", body)) {
            is ApiResult.Success -> parseSession(response.value)
            is ApiResult.Failure -> response
        }
    }

    suspend fun login(email: String, password: String): ApiResult<AuthSession> {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)

        return when (val response = requestJson("auth/login/", "POST", body)) {
            is ApiResult.Success -> parseSession(response.value)
            is ApiResult.Failure -> response
        }
    }

    suspend fun me(accessToken: String): ApiResult<NovaUser> {
        return when (val response = requestJson("me/", bearerToken = accessToken)) {
            is ApiResult.Success -> ApiResult.Success(parseUser(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun refresh(refreshToken: String): ApiResult<String> {
        val body = JSONObject().put("refresh", refreshToken)

        return when (val response = requestJson("auth/refresh/", "POST", body)) {
            is ApiResult.Success -> {
                val access = response.value.optString("access")
                if (access.isBlank()) {
                    ApiResult.Failure("Nova returned an invalid session response.")
                } else {
                    ApiResult.Success(access)
                }
            }

            is ApiResult.Failure -> response
        }
    }

    private fun parseSession(json: JSONObject): ApiResult<AuthSession> {
        val access = json.optString("access")
        val refresh = json.optString("refresh")
        val userJson = json.optJSONObject("user")

        if (access.isBlank() || refresh.isBlank() || userJson == null) {
            return ApiResult.Failure("Nova returned an invalid authentication response.")
        }

        return ApiResult.Success(
            AuthSession(
                accessToken = access,
                refreshToken = refresh,
                user = parseUser(userJson),
            ),
        )
    }

    private fun parseUser(json: JSONObject): NovaUser {
        return NovaUser(
            id = json.optLong("id"),
            email = json.optString("email"),
            username = json.optString("username"),
            name = json.optString("name"),
            avatarUrl = json.optString("avatar_url"),
        )
    }

    private suspend fun requestJson(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        bearerToken: String? = null,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                bearerToken?.let { setRequestProperty("Authorization", "Bearer $it") }

                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write(body.toString())
                    }
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = if (raw.isBlank()) JSONObject() else runCatching { JSONObject(raw) }
                .getOrElse { JSONObject().put("detail", raw) }

            if (status in 200..299) {
                ApiResult.Success(json)
            } else {
                ApiResult.Failure(
                    message = parseError(json, status),
                    statusCode = status,
                )
            }
        } catch (_: Exception) {
            ApiResult.Failure(
                message = "Can't reach Nova right now. Make sure the local server is running.",
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseError(json: JSONObject, statusCode: Int): String {
        val detail = json.optString("detail")
        if (detail.isNotBlank()) {
            return if (statusCode == 401) "Email or password is incorrect." else detail
        }

        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.opt(key)
            val message = when (value) {
                is JSONArray -> if (value.length() > 0) value.optString(0) else ""
                is String -> value
                else -> ""
            }

            if (message.isNotBlank()) {
                return when (key) {
                    "email" -> "Email: $message"
                    "username" -> "Username: $message"
                    "password" -> "Password: $message"
                    else -> message
                }
            }
        }

        return when (statusCode) {
            400 -> "Check your details and try again."
            401 -> "Email or password is incorrect."
            404 -> "Nova couldn't find that resource."
            else -> "Something went wrong. Please try again."
        }
    }
}
