package com.nova.app.core.messaging

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPostAuthor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID


class NovaGroupManagementRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)
    private val authApi = NovaApiClient(baseUrl)

    suspend fun detail(conversationId: Long): ApiResult<NovaManagedGroupDetail> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/group/manage/",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseDetail(response.value))
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun rename(
        conversationId: Long,
        title: String,
    ): ApiResult<NovaManagedGroupDetail> {
        val clean = title.trim()
        if (clean.isBlank()) return ApiResult.Failure("Give the group a name.")
        if (clean.length > 80) return ApiResult.Failure("Group name must be 80 characters or fewer.")
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/group/manage/",
                    method = "POST",
                    body = JSONObject().put("title", clean),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseDetail(response.value))
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun updateAvatar(
        conversationId: Long,
        uri: Uri,
    ): ApiResult<NovaManagedGroupDetail> {
        val upload = readUpload(uri)
            ?: return ApiResult.Failure("Nova couldn't read that photo. Choose another image.")
        return authenticatedCall { token ->
            when (
                val response = requestMultipart(
                    path = "conversations/$conversationId/group/manage/",
                    fileName = upload.fileName,
                    mimeType = upload.mimeType,
                    bytes = upload.bytes,
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseDetail(response.value))
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun removeAvatar(conversationId: Long): ApiResult<NovaManagedGroupDetail> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/group/manage/",
                    method = "POST",
                    body = JSONObject().put("remove_avatar", true),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseDetail(response.value))
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun setRole(
        conversationId: Long,
        username: String,
        role: String,
    ): ApiResult<NovaManagedGroupDetail> {
        val cleanRole = role.trim().lowercase()
        if (cleanRole != "admin" && cleanRole != "member") {
            return ApiResult.Failure("Invalid group role.")
        }
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/group/members/${encode(username.trim().lowercase())}/role/",
                    method = "POST",
                    body = JSONObject().put("role", cleanRole),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseDetail(response.value))
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
            parseResponse(connection)
        } catch (_: Exception) {
            ApiResult.Failure("Can't reach Nova right now. Check your connection and try again.")
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun requestMultipart(
        path: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        bearerToken: String,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        val boundary = "NovaGroup-${UUID.randomUUID()}"
        val body = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { out ->
                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"avatar\"; filename=\"${fileName.replace("\"", "")}\"\r\n")
                out.writeBytes("Content-Type: $mimeType\r\n\r\n")
                out.write(bytes)
                out.writeBytes("\r\n--$boundary--\r\n")
            }
            buffer.toByteArray()
        }

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $bearerToken")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setFixedLengthStreamingMode(body.size)
            }
            connection.outputStream.use { it.write(body) }
            parseResponse(connection)
        } catch (_: Exception) {
            ApiResult.Failure("Couldn't upload the group photo. Check your connection and try again.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseResponse(connection: HttpURLConnection): ApiResult<JSONObject> {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val json = raw.takeIf { it.isNotBlank() }?.let {
            runCatching { JSONObject(it) }.getOrElse { JSONObject().put("detail", raw) }
        } ?: JSONObject()
        return if (statusCode in 200..299) {
            ApiResult.Success(json)
        } else {
            ApiResult.Failure(
                message = json.optString("detail").ifBlank {
                    when (statusCode) {
                        401 -> "Your session expired. Please log in again."
                        403 -> "You can't manage this group."
                        404 -> "That group is no longer available."
                        in 500..599 -> "Nova's server had a problem. Try again in a moment."
                        else -> "Something went wrong. Please try again."
                    }
                },
                statusCode = statusCode,
            )
        }
    }

    private fun readUpload(uri: Uri): GroupAvatarUpload? {
        val resolver = appContext.contentResolver
        val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
        }
        if (size != null && size > MAX_GROUP_AVATAR_BYTES) return null

        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }.orEmpty().ifBlank { "group-photo.jpg" }
        val mime = resolver.getType(uri).orEmpty().ifBlank {
            val extension = MimeTypeMap.getFileExtensionFromUrl(displayName)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()).orEmpty()
        }.ifBlank { "image/jpeg" }
        if (!mime.startsWith("image/")) return null

        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = input.read(chunk)
                if (count <= 0) break
                total += count
                if (total > MAX_GROUP_AVATAR_BYTES) return null
                output.write(chunk, 0, count)
            }
            output.toByteArray()
        } ?: return null
        return GroupAvatarUpload(bytes, displayName, mime)
    }

    private fun parseDetail(json: JSONObject): NovaManagedGroupDetail {
        val conversation = json.optJSONObject("conversation") ?: JSONObject()
        val rows = json.optJSONArray("members") ?: JSONArray()
        val members = buildList {
            for (index in 0 until rows.length()) {
                val item = rows.optJSONObject(index) ?: continue
                val user = item.optJSONObject("user") ?: continue
                add(
                    NovaGroupMember(
                        user = parseAuthor(user),
                        role = item.optString("role"),
                        joinedAt = item.optString("joined_at"),
                    )
                )
            }
        }
        return NovaManagedGroupDetail(
            title = conversation.optString("title").ifBlank { "Nova group" },
            avatarUrl = resolveMediaUrl(conversation.optString("group_avatar_url")),
            membersCount = conversation.optInt("members_count", members.size),
            currentUserRole = conversation.optString("current_user_role"),
            members = members,
        )
    }

    private fun parseAuthor(json: JSONObject): NovaPostAuthor {
        return NovaPostAuthor(
            id = json.optLong("id"),
            username = json.optString("username"),
            name = json.optString("name"),
            avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
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

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private data class GroupAvatarUpload(
        val bytes: ByteArray,
        val fileName: String,
        val mimeType: String,
    )

    private companion object {
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
        const val MAX_GROUP_AVATAR_BYTES = 10 * 1024 * 1024L
    }
}
