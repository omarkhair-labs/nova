package com.nova.app.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit


data class UploadFile(
    val bytes: ByteArray? = null,
    val fileName: String,
    val mimeType: String,
    val sourceFile: File? = null,
) {
    init {
        require(bytes != null || sourceFile != null) { "UploadFile needs bytes or a source file." }
    }

    internal fun asRequestBody(): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = mimeType.toMediaTypeOrNull()

        override fun contentLength(): Long = bytes?.size?.toLong() ?: requireNotNull(sourceFile).length()

        override fun writeTo(sink: BufferedSink) {
            writeToSink(sink)
        }
    }

    private fun writeToSink(sink: BufferedSink) {
        bytes?.let {
            sink.write(it)
            return
        }
        requireNotNull(sourceFile).source().use { source ->
            sink.writeAll(source)
        }
    }
}


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
    private val multipartClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

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
        onUploadProgress: ((Int?) -> Unit)? = null,
    ): ApiResult<JSONObject> = requestMultipart(
        path = path,
        method = method,
        fields = fields,
        files = if (file == null) emptyMap() else mapOf(fileField to file),
        bearerToken = bearerToken,
        onUploadProgress = onUploadProgress,
    )

    internal suspend fun requestMultipart(
        path: String,
        method: String,
        fields: Map<String, String>,
        files: Map<String, UploadFile>,
        bearerToken: String,
        onUploadProgress: ((Int?) -> Unit)? = null,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .apply {
                    fields.forEach { (name, value) ->
                        addFormDataPart(name, value)
                    }
                    files.forEach { (fieldName, file) ->
                        addFormDataPart(fieldName, file.fileName, file.asRequestBody())
                    }
                }
                .build()
            val multipart: RequestBody = onUploadProgress?.let { callback ->
                UploadProgressRequestBody(multipartBody, callback)
            } ?: multipartBody
            val request = Request.Builder()
                .url(baseUrl + path)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $bearerToken")
                .method(method.uppercase(), multipart)
                .build()

            debugMultipart(
                method = method,
                path = path,
                fields = fields.keys,
                files = files.keys,
                contentLength = multipart.contentLength(),
            )
            multipartClient.newCall(request).execute().use { response ->
                val result = parseJsonResponse(
                    status = response.code,
                    raw = response.body.string(),
                )
                if (result is ApiResult.Failure) {
                    debugMultipart(
                        method = method,
                        path = path,
                        fields = fields.keys,
                        files = files.keys,
                        contentLength = multipart.contentLength(),
                        status = response.code,
                        error = result.message,
                    )
                }
                result
            }
        } catch (error: Exception) {
            debugMultipart(
                method = method,
                path = path,
                fields = fields.keys,
                files = files.keys,
                error = error::class.java.simpleName,
            )
            ApiResult.Failure(
                message = "Nova couldn't upload that right now. Check your connection and try again.",
            )
        }
    }

    private fun readJsonResponse(connection: HttpURLConnection): ApiResult<JSONObject> {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        return parseJsonResponse(status, raw)
    }

    private fun parseJsonResponse(status: Int, raw: String): ApiResult<JSONObject> {
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

    private fun debugMultipart(
        method: String,
        path: String,
        fields: Set<String>,
        files: Set<String>,
        contentLength: Long? = null,
        status: Int? = null,
        error: String? = null,
    ) {
        val message = buildString {
            append("multipart ${method.uppercase()} $path")
            append(" fields=${fields.sorted()}")
            append(" files=${files.sorted()}")
            contentLength?.let { append(" contentLength=$it") }
            status?.let { append(" status=$it") }
            error?.let { append(" error=$it") }
        }
        runCatching { Log.d(MULTIPART_LOG_TAG, message) }
    }

    private companion object {
        const val MULTIPART_LOG_TAG = "NovaMultipart"
    }
}


private class UploadProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (Int?) -> Unit,
) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val totalBytes = contentLength()
        var writtenBytes = 0L
        var lastProgress = -1

        if (totalBytes <= 0L) {
            onProgress(null)
            delegate.writeTo(sink)
            return
        }

        onProgress(0)
        lastProgress = 0
        val countingSink = object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                writtenBytes += byteCount
                val progress = ((writtenBytes * 100L) / totalBytes)
                    .toInt()
                    .coerceIn(0, 100)
                if (progress != lastProgress) {
                    lastProgress = progress
                    onProgress(progress)
                }
            }
        }
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
        if (lastProgress < 100) onProgress(100)
    }
}
