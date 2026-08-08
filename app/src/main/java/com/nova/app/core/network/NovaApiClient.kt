package com.nova.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID


data class NovaUser(
    val id: Long,
    val email: String,
    val username: String,
    val name: String,
    val avatarUrl: String,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
)


data class NovaPerson(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int,
    val isFollowing: Boolean,
)


data class NovaPostAuthor(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
)


data class NovaPost(
    val id: Long,
    val author: NovaPostAuthor,
    val imageUrl: String,
    val caption: String,
    val createdAt: String,
    val isMine: Boolean,
)


data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: NovaUser,
)


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

    suspend fun updateProfile(
        accessToken: String,
        name: String,
        username: String,
        avatar: UploadFile? = null,
    ): ApiResult<NovaUser> {
        return when (
            val response = requestMultipart(
                path = "me/",
                method = "PUT",
                fields = mapOf(
                    "name" to name,
                    "username" to username,
                ),
                fileField = "avatar",
                file = avatar,
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseUser(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun people(
        accessToken: String,
        query: String = "",
    ): ApiResult<List<NovaPerson>> {
        val cleanQuery = query.trim()
        val path = if (cleanQuery.isBlank()) {
            "people/"
        } else {
            "people/?q=${encode(cleanQuery)}"
        }

        return when (val response = requestJson(path, bearerToken = accessToken)) {
            is ApiResult.Success -> {
                val array = response.value.optJSONArray("results") ?: JSONArray()
                val people = buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { add(parsePerson(it)) }
                    }
                }
                ApiResult.Success(people)
            }

            is ApiResult.Failure -> response
        }
    }

    suspend fun person(
        accessToken: String,
        username: String,
    ): ApiResult<NovaPerson> {
        return when (
            val response = requestJson(
                path = "people/${encode(username.trim().lowercase())}/",
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parsePerson(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun setFollowing(
        accessToken: String,
        username: String,
        follow: Boolean,
    ): ApiResult<NovaPerson> {
        val path = "people/${encode(username.trim().lowercase())}/follow/"
        val response = if (follow) {
            requestJson(path, method = "POST", body = JSONObject(), bearerToken = accessToken)
        } else {
            requestJson(path, method = "DELETE", bearerToken = accessToken)
        }

        return when (response) {
            is ApiResult.Success -> ApiResult.Success(parsePerson(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun feed(accessToken: String): ApiResult<List<NovaPost>> {
        return when (val response = requestJson("feed/", bearerToken = accessToken)) {
            is ApiResult.Success -> {
                val array = response.value.optJSONArray("results") ?: JSONArray()
                val posts = buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { add(parsePost(it)) }
                    }
                }
                ApiResult.Success(posts)
            }

            is ApiResult.Failure -> response
        }
    }

    suspend fun createPost(
        accessToken: String,
        caption: String,
        image: UploadFile,
    ): ApiResult<NovaPost> {
        return when (
            val response = requestMultipart(
                path = "posts/",
                method = "POST",
                fields = mapOf("caption" to caption),
                fileField = "image",
                file = image,
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parsePost(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun deletePost(
        accessToken: String,
        postId: Long,
    ): ApiResult<Unit> {
        return when (
            val response = requestJson(
                path = "posts/$postId/",
                method = "DELETE",
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(Unit)
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
            avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
            followersCount = json.optInt("followers_count", 0),
            followingCount = json.optInt("following_count", 0),
            postsCount = json.optInt("posts_count", 0),
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

    private fun parsePostAuthor(json: JSONObject): NovaPostAuthor {
        return NovaPostAuthor(
            id = json.optLong("id"),
            username = json.optString("username"),
            name = json.optString("name"),
            avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
        )
    }

    private fun parsePost(json: JSONObject): NovaPost {
        val author = json.optJSONObject("author") ?: JSONObject()
        return NovaPost(
            id = json.optLong("id"),
            author = parsePostAuthor(author),
            imageUrl = resolveMediaUrl(json.optString("image_url")),
            caption = json.optString("caption"),
            createdAt = json.optString("created_at"),
            isMine = json.optBoolean("is_mine", false),
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

            readJsonResponse(connection)
        } catch (_: Exception) {
            ApiResult.Failure(
                message = "Can't reach Nova right now. Make sure the local server is running.",
            )
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun requestMultipart(
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
                message = "Nova couldn't upload that right now. Check the local server and try again.",
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
                    else -> message
                }
            }
        }

        return when (statusCode) {
            400 -> "Check your details and try again."
            401 -> "Your session expired. Please log in again."
            404 -> "Nova couldn't find that resource."
            else -> "Something went wrong. Please try again."
        }
    }
}
