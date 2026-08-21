package com.nova.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID


data class UploadFile(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
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

    internal fun resolveMediaUrl(raw: String): String {
        if (raw.isBlank() || raw == "null") return ""
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw

        return runCatching {
            val apiUrl = URL(baseUrl)
            URL("${apiUrl.protocol}://${apiUrl.authority}$raw").toString()
        }.getOrDefault(raw)
    }

    internal suspend fun requestJson(
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

            readJsonResponse(connection)
        } catch (_: Exception) {
            ApiResult.Failure(
                message = "Can't reach Nova right now. Check your connection and try again.",
            )
        } finally {
            connection?.disconnect()
        }
    }

    internal suspend fun requestMultipart(
        path: String,
        method: String,
        fields: Map<String, String>,
        fileField: String,
        file: UploadFile?,
        bearerToken: String,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        val boundary = "Nova-${UUID.randomUUID()}"
        val lineEnd = "\r\n"

        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 20_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $bearerToken")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            DataOutputStream(connection.outputStream).use { output ->
                fields.forEach { (name, value) ->
                    output.writeBytes("--$boundary$lineEnd")
                    output.writeBytes("Content-Disposition: form-data; name=\"$name\"$lineEnd")
                    output.writeBytes("Content-Type: text/plain; charset=UTF-8$lineEnd$lineEnd")
                    output.write(value.toByteArray(Charsets.UTF_8))
                    output.writeBytes(lineEnd)
                }

                if (file != null) {
                    output.writeBytes("--$boundary$lineEnd")
                    output.writeBytes(
                        "Content-Disposition: form-data; name=\"$fileField\"; filename=\"${file.fileName}\"$lineEnd",
                    )
                    output.writeBytes("Content-Type: ${file.mimeType}$lineEnd$lineEnd")
                    output.write(file.bytes)
                    output.writeBytes(lineEnd)
                }

                output.writeBytes("--$boundary--$lineEnd")
                output.flush()
            }

            readJsonResponse(connection)
        } catch (_: Exception) {
            ApiResult.Failure(
                message = "Nova couldn't upload that right now. Check your connection and try again.",
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun readJsonResponse(connection: HttpURLConnection): ApiResult<JSONObject> {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val json = if (raw.isBlank()) JSONObject() else runCatching { JSONObject(raw) }
            .getOrElse { JSONObject().put("detail", raw) }

        return if (status in 200..299) {
            ApiResult.Success(json)
        } else {
            ApiResult.Failure(
                message = parseError(json, status),
                statusCode = status,
            )
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
                    "avatar" -> "Photo: $message"
                    "image" -> "Photo: $message"
                    "caption" -> "Caption: $message"
                    "body" -> "Comment: $message"
                    else -> message
                }
            }
        }

        return when (statusCode) {
            400 -> "Check your details and try again."
            401 -> "Your session expired. Please log in again."
            404 -> "Nova couldn't find that resource."
            429 -> "Too many requests. Give Nova a moment and try again."
            in 500..599 -> "Nova's server had a problem. Try again in a moment."
            else -> "Something went wrong. Please try again."
        }
    }
}
