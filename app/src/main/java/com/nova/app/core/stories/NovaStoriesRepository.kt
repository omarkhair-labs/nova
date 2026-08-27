package com.nova.app.core.stories

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.media.NovaVideoPreparer
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.feature.stories.data.StoriesRepository
import com.nova.app.feature.stories.domain.model.NovaStory
import com.nova.app.feature.stories.domain.model.NovaStoryAuthor
import com.nova.app.feature.stories.domain.model.NovaStoryGroup
import com.nova.app.feature.stories.domain.model.NovaStorySharedPost
import com.nova.app.feature.stories.domain.model.NovaStorySharedReel
import com.nova.app.feature.stories.domain.model.NovaStoryViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


private data class PreparedStoryMedia(
    val file: File,
    val mimeType: String,
    val fileName: String,
)


class NovaStoriesRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) : StoriesRepository {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)
    private val authApi = NovaApiClient(baseUrl)
    private val videoPreparer = NovaVideoPreparer(appContext)

    override suspend fun stories(): ApiResult<List<NovaStoryGroup>> {
        return authenticatedCall { token ->
            when (val result = requestJson("stories/", bearerToken = token)) {
                is ApiResult.Success -> {
                    val rows = result.value.optJSONArray("results") ?: JSONArray()
                    ApiResult.Success(
                        buildList {
                            for (index in 0 until rows.length()) {
                                val item = rows.optJSONObject(index) ?: continue
                                add(parseGroup(item))
                            }
                        }
                    )
                }
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun createStory(
        mediaUri: Uri,
        caption: String,
        audience: String,
    ): ApiResult<NovaStory> = createStory(
        mediaUri = mediaUri,
        caption = caption,
        audience = audience,
        clientPublishId = "",
        onProgress = {},
        expectedUserId = null,
    )

    override suspend fun createStory(
        mediaUri: Uri,
        caption: String,
        audience: String,
        clientPublishId: String,
        onProgress: (Int?) -> Unit,
        expectedUserId: Long?,
    ): ApiResult<NovaStory> {
        if (!storyPublishAccountMatches(expectedUserId, sessionStore.load()?.cachedUser?.id)) {
            return ApiResult.Failure("This publish belongs to a different signed-in account.", 409)
        }
        val cleanAudience = audience.takeIf { it == "followers" || it == "close_friends" }
            ?: return ApiResult.Failure("Choose a valid Story audience.")
        val mimeType = resolveStoryMimeType(mediaUri)
            ?: return ApiResult.Failure("Stories support photos and videos only.")

        return if (mimeType.startsWith("video/")) {
            val prepared = when (
                val result = videoPreparer.prepare(
                    source = mediaUri,
                    maxSourceBytes = 60L * 1024 * 1024,
                    sizeMessage = "Story video must be 60 MB or smaller.",
                    onProgress = onProgress,
                )
            ) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure -> return result
            }
            try {
                uploadStoryMedia(
                    media = UploadFile(
                        fileName = "nova-story-${System.currentTimeMillis()}.mp4",
                        mimeType = "video/mp4",
                        sourceFile = prepared.videoFile,
                    ),
                    thumbnail = UploadFile(
                        fileName = "nova-story-${System.currentTimeMillis()}.jpg",
                        mimeType = "image/jpeg",
                        sourceFile = prepared.thumbnailFile,
                    ),
                    caption = caption,
                    audience = cleanAudience,
                    clientPublishId = clientPublishId,
                    expectedUserId = expectedUserId,
                )
            } finally {
                prepared.delete()
            }
        } else {
            val prepared = when (
                val result = prepareStoryMedia(mediaUri, mimeType, 15L * 1024 * 1024)
            ) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure -> return result
            }
            try {
                uploadStoryMedia(
                    media = UploadFile(
                        fileName = prepared.fileName,
                        mimeType = prepared.mimeType,
                        sourceFile = prepared.file,
                    ),
                    thumbnail = null,
                    caption = caption,
                    audience = cleanAudience,
                    clientPublishId = clientPublishId,
                    expectedUserId = expectedUserId,
                )
            } finally {
                prepared.file.delete()
            }
        }
    }

    override suspend fun createTextStory(
        text: String,
        backgroundStyle: String,
        audience: String,
    ): ApiResult<NovaStory> {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return ApiResult.Failure("Write something for your Story.")
        val cleanAudience = audience.takeIf { it == "followers" || it == "close_friends" }
            ?: return ApiResult.Failure("Choose a valid Story audience.")
        val cleanBackground = backgroundStyle.takeIf {
            it in setOf("midnight", "sunset", "ocean", "forest")
        } ?: return ApiResult.Failure("Choose a valid Story background.")

        return authenticatedCall { token ->
            when (
                val result = requestJson(
                    path = "stories/",
                    method = "POST",
                    body = JSONObject()
                        .put("media_type", "text")
                        .put("caption", cleanText.take(240))
                        .put("background_style", cleanBackground)
                        .put("audience", cleanAudience),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseStory(result.value))
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun markViewed(storyId: Long): ApiResult<Unit> {
        return authenticatedCall { token ->
            when (
                val result = requestJson(
                    path = "stories/$storyId/view/",
                    method = "POST",
                    body = JSONObject(),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun react(storyId: Long, emoji: String): ApiResult<String> {
        return authenticatedCall { token ->
            when (
                val result = requestJson(
                    path = "stories/$storyId/reaction/",
                    method = "POST",
                    body = JSONObject().put("emoji", emoji),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(result.value.optString("reaction"))
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun removeReaction(storyId: Long): ApiResult<Unit> {
        return authenticatedCall { token ->
            when (
                val result = requestJson(
                    path = "stories/$storyId/reaction/",
                    method = "DELETE",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun reply(storyId: Long, body: String): ApiResult<Unit> {
        val clean = body.trim()
        if (clean.isBlank()) return ApiResult.Failure("Write a reply first.")
        return authenticatedCall { token ->
            when (
                val result = requestJson(
                    path = "stories/$storyId/reply/",
                    method = "POST",
                    body = JSONObject().put("body", clean.take(1000)),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun viewers(storyId: Long): ApiResult<List<NovaStoryViewer>> {
        return authenticatedCall { token ->
            when (val result = requestJson("stories/$storyId/viewers/", bearerToken = token)) {
                is ApiResult.Success -> {
                    val rows = result.value.optJSONArray("results") ?: JSONArray()
                    ApiResult.Success(
                        buildList {
                            for (index in 0 until rows.length()) {
                                val item = rows.optJSONObject(index) ?: continue
                                val user = item.optJSONObject("user") ?: continue
                                add(
                                    NovaStoryViewer(
                                        user = parseAuthor(user),
                                        viewedAt = item.optString("viewed_at"),
                                        reaction = item.optString("reaction"),
                                    )
                                )
                            }
                        }
                    )
                }
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun deleteStory(storyId: Long): ApiResult<Unit> {
        return authenticatedCall { token ->
            when (
                val result = requestJson(
                    path = "stories/$storyId/",
                    method = "DELETE",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> result
            }
        }
    }

    private fun resolveStoryMimeType(uri: Uri): String? {
        val resolver = appContext.contentResolver
        val direct = resolver.getType(uri)
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            .orEmpty()
        if (direct.startsWith("image/") || direct.startsWith("video/")) return direct

        val displayName = runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) null else cursor.getString(index)
            }
        }.getOrNull().orEmpty()

        val extension = sequenceOf(displayName, uri.lastPathSegment.orEmpty())
            .map { candidate -> candidate.substringAfterLast('.', "").lowercase() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val inferred = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?.lowercase()
            .orEmpty()
        return inferred.takeIf { it.startsWith("image/") || it.startsWith("video/") }
    }

    private suspend fun prepareStoryMedia(
        mediaUri: Uri,
        mimeType: String,
        maxBytes: Long,
    ): ApiResult<PreparedStoryMedia> = withContext(Dispatchers.IO) {
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { it.isNotBlank() }
            ?: if (mimeType.startsWith("video/")) "mp4" else "jpg"
        val tempFile = File.createTempFile("nova-story-", ".$extension", appContext.cacheDir)
        val tooLargeMessage = if (mimeType.startsWith("video/")) {
            "Story video must be 60 MB or smaller."
        } else {
            "Story photo must be 15 MB or smaller."
        }

        try {
            val input = appContext.contentResolver.openInputStream(mediaUri)
                ?: throw IllegalStateException("Couldn't read that story media.")
            input.use { source ->
                tempFile.outputStream().buffered().use { target ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > maxBytes) {
                            tempFile.delete()
                            return@withContext ApiResult.Failure(tooLargeMessage)
                        }
                        target.write(buffer, 0, read)
                    }
                    target.flush()
                }
            }
            if (tempFile.length() <= 0L) {
                tempFile.delete()
                ApiResult.Failure("Nova couldn't read that Story file. Pick it again and retry.")
            } else {
                ApiResult.Success(
                    PreparedStoryMedia(
                        file = tempFile,
                        mimeType = mimeType,
                        fileName = "nova-story-${System.currentTimeMillis()}.$extension",
                    )
                )
            }
        } catch (_: Exception) {
            tempFile.delete()
            ApiResult.Failure("Nova couldn't read that Story file. Pick it again and retry.")
        }
    }

    private suspend fun uploadStoryMedia(
        media: UploadFile,
        thumbnail: UploadFile?,
        caption: String,
        audience: String,
        clientPublishId: String,
        expectedUserId: Long?,
    ): ApiResult<NovaStory> = authenticatedCall(expectedUserId) { token ->
        when (
            val response = authApi.requestMultipart(
                path = "stories/",
                method = "POST",
                fields = buildMap {
                    put("caption", caption.trim().take(240))
                    put("audience", audience)
                    clientPublishId.takeIf(String::isNotBlank)?.let {
                        put("client_publish_id", it)
                    }
                },
                files = buildMap {
                    put("media", media)
                    thumbnail?.let { put("thumbnail", it) }
                },
                bearerToken = token,
            )
        ) {
                is ApiResult.Success -> ApiResult.Success(parseStory(response.value))
                is ApiResult.Failure -> response
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
                    setRequestProperty("Content-Type", "application/json")
                }
            }
            if (body != null) {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            }
            readJsonResponse(connection)
        } catch (_: Exception) {
            ApiResult.Failure("Can't reach Nova right now. Check your connection and try again.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun readJsonResponse(connection: HttpURLConnection): ApiResult<JSONObject> {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val json = if (raw.isBlank()) JSONObject() else runCatching { JSONObject(raw) }
            .getOrElse { JSONObject().put("detail", raw) }
        return if (statusCode in 200..299) {
            ApiResult.Success(json)
        } else {
            ApiResult.Failure(
                message = when (statusCode) {
                    400 -> json.optString("detail").ifBlank { "Nova couldn't publish that Story." }
                    401 -> "Your session expired. Please log in again."
                    403 -> json.optString("detail").ifBlank { "You can't interact with this Story." }
                    404 -> "That Story is no longer available."
                    413 -> "That Story file is too large."
                    in 500..599 -> "Nova's server had a problem. Try again in a moment."
                    else -> json.optString("detail").ifBlank { "Something went wrong. Please try again." }
                },
                statusCode = statusCode,
            )
        }
    }

    private suspend fun <T> authenticatedCall(
        expectedUserId: Long? = null,
        call: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)
        if (!storyPublishAccountMatches(expectedUserId, stored.cachedUser?.id)) {
            return ApiResult.Failure("This publish belongs to a different signed-in account.", 409)
        }
        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return first
            is ApiResult.Failure -> if (first.statusCode != 401) return first
        }
        return when (val refreshed = authApi.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                if (!storyPublishAccountMatches(expectedUserId, sessionStore.load()?.cachedUser?.id)) {
                    return ApiResult.Failure(
                        "This publish belongs to a different signed-in account.",
                        409,
                    )
                }
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

    private fun parseGroup(json: JSONObject): NovaStoryGroup {
        val author = parseAuthor(json.optJSONObject("author") ?: JSONObject())
        val rows = json.optJSONArray("stories") ?: JSONArray()
        return NovaStoryGroup(
            author = author,
            stories = buildList {
                for (index in 0 until rows.length()) {
                    val story = rows.optJSONObject(index) ?: continue
                    add(parseStory(story))
                }
            },
            hasUnseen = json.optBoolean("has_unseen"),
            isMine = json.optBoolean("is_mine"),
        )
    }

    private fun parseStory(json: JSONObject): NovaStory {
        val sharedPost = json.optJSONObject("shared_post")?.let { item ->
            NovaStorySharedPost(
                id = item.optLong("id"),
                author = parseAuthor(item.optJSONObject("author") ?: JSONObject()),
                imageUrl = resolveMediaUrl(item.optString("image_url")),
                caption = item.optString("caption"),
            )
        }
        val sharedReel = json.optJSONObject("shared_reel")?.let { item ->
            NovaStorySharedReel(
                id = item.optLong("id"),
                author = parseAuthor(item.optJSONObject("author") ?: JSONObject()),
                videoUrl = resolveMediaUrl(item.optString("video_url")),
                caption = item.optString("caption"),
            )
        }
        return NovaStory(
            id = json.optLong("id"),
            author = parseAuthor(json.optJSONObject("author") ?: JSONObject()),
            mediaUrl = resolveMediaUrl(json.optString("media_url")),
            mediaType = json.optString("media_type"),
            caption = json.optString("caption"),
            createdAt = json.optString("created_at"),
            expiresAt = json.optString("expires_at"),
            isMine = json.optBoolean("is_mine"),
            isViewed = json.optBoolean("is_viewed"),
            myReaction = json.optString("my_reaction"),
            viewsCount = if (json.isNull("views_count")) null else json.optInt("views_count"),
            audience = json.optString("audience").ifBlank { "followers" },
            backgroundStyle = json.optString("background_style").ifBlank { "midnight" },
            sharedPost = sharedPost,
            sharedReel = sharedReel,
            thumbnailUrl = resolveMediaUrl(json.optString("thumbnail_url")),
        )
    }

    private fun parseAuthor(json: JSONObject): NovaStoryAuthor {
        return NovaStoryAuthor(
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

    private companion object {
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
    }
}


internal fun storyPublishAccountMatches(expectedUserId: Long?, activeUserId: Long?): Boolean =
    expectedUserId == null || (expectedUserId > 0L && activeUserId == expectedUserId)
