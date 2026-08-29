package com.nova.app.core.reels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.media.NovaVideoPreparer
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.feature.reels.data.ReelsRepository
import com.nova.app.feature.reels.domain.model.NovaReel
import com.nova.app.feature.reels.domain.model.NovaReelAuthor
import com.nova.app.feature.reels.domain.model.NovaReelComment
import com.nova.app.feature.reels.domain.model.NovaReelCommentMutation
import com.nova.app.feature.reels.domain.model.NovaReelPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class NovaReelsRepository(
    context: Context,
    private val baseUrl: String = PRODUCTION_API_URL,
) : ReelsRepository {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)
    private val authApi = NovaApiClient(baseUrl)
    private val videoPreparer = NovaVideoPreparer(appContext)

    override suspend fun reels(cursor: String?): ApiResult<NovaReelPage> {
        val path = if (cursor.isNullOrBlank()) "reels/" else "reels/?cursor=${cursor.trim()}"
        return authenticatedCall { token ->
            when (val result = requestJson(path, bearerToken = token)) {
                is ApiResult.Success -> {
                    val rows = result.value.optJSONArray("results") ?: JSONArray()
                    val next = result.value.optString("next_cursor")
                        .takeIf { it.isNotBlank() && it != "null" }
                    ApiResult.Success(
                        NovaReelPage(
                            reels = buildList {
                                for (index in 0 until rows.length()) {
                                    rows.optJSONObject(index)?.let { add(parseReel(it)) }
                                }
                            },
                            nextCursor = next,
                        )
                    )
                }
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun createReel(videoUri: Uri, caption: String): ApiResult<NovaReel> =
        createReel(videoUri, caption, "")

    override suspend fun createReel(
        videoUri: Uri,
        caption: String,
        clientPublishId: String,
        onProgress: (Int?) -> Unit,
        expectedUserId: Long?,
    ): ApiResult<NovaReel> {
        if (!reelPublishAccountMatches(expectedUserId, sessionStore.load()?.cachedUser?.id)) {
            return ApiResult.Failure("This publish belongs to a different signed-in account.", 409)
        }
        resolveVideoMimeType(videoUri)
            ?: return ApiResult.Failure("Reels support video files only.")
        val prepared = when (
            val result = videoPreparer.prepare(
                source = videoUri,
                maxSourceBytes = MAX_REEL_VIDEO_BYTES,
                sizeMessage = "Reel video must be 120 MB or smaller.",
                onProgress = onProgress,
            )
        ) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result
        }
        return try {
            authenticatedCall(expectedUserId) { token ->
                when (
                    val response = authApi.requestMultipart(
                        path = "reels/",
                        method = "POST",
                        fields = buildMap {
                            put("caption", caption.trim().take(500))
                            clientPublishId.takeIf(String::isNotBlank)?.let {
                                put("client_publish_id", it)
                            }
                        },
                        files = mapOf(
                            "video" to UploadFile(
                                fileName = "nova-reel-${System.currentTimeMillis()}.mp4",
                                mimeType = "video/mp4",
                                sourceFile = prepared.videoFile,
                            ),
                            "thumbnail" to UploadFile(
                                fileName = "nova-reel-${System.currentTimeMillis()}.jpg",
                                mimeType = "image/jpeg",
                                sourceFile = prepared.thumbnailFile,
                            ),
                        ),
                        bearerToken = token,
                        onUploadProgress = onProgress,
                    )
                ) {
                    is ApiResult.Success -> ApiResult.Success(parseReel(response.value))
                    is ApiResult.Failure -> response
                }
            }
        } finally {
            prepared.delete()
        }
    }

    override suspend fun setLiked(reelId: Long, liked: Boolean): ApiResult<NovaReel> {
        return authenticatedCall { token ->
            val result = if (liked) {
                requestJson(
                    path = "reels/$reelId/like/",
                    method = "POST",
                    body = JSONObject(),
                    bearerToken = token,
                )
            } else {
                requestJson(
                    path = "reels/$reelId/like/",
                    method = "DELETE",
                    bearerToken = token,
                )
            }
            when (result) {
                is ApiResult.Success -> ApiResult.Success(parseReel(result.value))
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun setReposted(reelId: Long, reposted: Boolean): ApiResult<NovaReel> {
        return authenticatedCall { token ->
            val result = if (reposted) {
                requestJson(
                    path = "reels/$reelId/repost/",
                    method = "POST",
                    body = JSONObject(),
                    bearerToken = token,
                )
            } else {
                requestJson(
                    path = "reels/$reelId/repost/",
                    method = "DELETE",
                    bearerToken = token,
                )
            }
            when (result) {
                is ApiResult.Success -> ApiResult.Success(parseReel(result.value))
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun comments(reelId: Long): ApiResult<List<NovaReelComment>> {
        return authenticatedCall { token ->
            when (val result = requestJson("reels/$reelId/comments/", bearerToken = token)) {
                is ApiResult.Success -> {
                    val rows = result.value.optJSONArray("results") ?: JSONArray()
                    ApiResult.Success(
                        buildList {
                            for (index in 0 until rows.length()) {
                                rows.optJSONObject(index)?.let { add(parseComment(it)) }
                            }
                        }
                    )
                }
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun addComment(
        reelId: Long,
        body: String,
        parentId: Long?,
    ): ApiResult<NovaReelCommentMutation> {
        val clean = body.trim()
        if (clean.isBlank()) return ApiResult.Failure("Write a comment first.")
        val payload = JSONObject().put("body", clean.take(300))
        parentId?.takeIf { it > 0L }?.let { payload.put("parent_id", it) }
        return authenticatedCall { token ->
            when (
                val result = requestJson(
                    path = "reels/$reelId/comments/",
                    method = "POST",
                    body = payload,
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> {
                    val comment = result.value.optJSONObject("comment")
                    val reel = result.value.optJSONObject("reel")
                    if (comment == null || reel == null) {
                        ApiResult.Failure("Nova returned an invalid Reel comment response.")
                    } else {
                        ApiResult.Success(
                            NovaReelCommentMutation(
                                comment = parseComment(comment),
                                reel = parseReel(reel),
                            )
                        )
                    }
                }
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun deleteComment(commentId: Long): ApiResult<NovaReel> {
        return authenticatedCall { token ->
            when (
                val result = requestJson(
                    path = "reel-comments/$commentId/",
                    method = "DELETE",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> {
                    val reel = result.value.optJSONObject("reel")
                    if (reel == null) {
                        ApiResult.Failure("Nova returned an invalid Reel comment response.")
                    } else {
                        ApiResult.Success(parseReel(reel))
                    }
                }
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaReel> {
        return authenticatedCall { token ->
            when (
                val result = requestJson(
                    path = "reel-comment-replies/$replyId/",
                    method = "DELETE",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> {
                    val reel = result.value.optJSONObject("reel")
                    if (reel == null) {
                        ApiResult.Failure("Nova returned an invalid Reel comment response.")
                    } else {
                        ApiResult.Success(parseReel(reel))
                    }
                }
                is ApiResult.Failure -> result
            }
        }
    }

    override suspend fun deleteReel(reelId: Long): ApiResult<Unit> {
        return authenticatedCall { token ->
            when (
                val result = requestJson(
                    path = "reels/$reelId/",
                    method = "DELETE",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> result
            }
        }
    }

    private fun resolveVideoMimeType(uri: Uri): String? {
        val resolver = appContext.contentResolver
        val direct = resolver.getType(uri)
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            .orEmpty()
        if (direct.startsWith("video/")) return direct

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
            .map { it.substringAfterLast('.', "").lowercase() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?.lowercase()
            ?.takeIf { it.startsWith("video/") }
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
                readTimeout = 30_000
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
                    400 -> json.optString("detail").ifBlank { "Nova couldn't process that Reel." }
                    401 -> "Your session expired. Please log in again."
                    403 -> json.optString("detail").ifBlank { "You can't interact with this Reel." }
                    404 -> "That Reel is no longer available."
                    413 -> "That Reel video is too large."
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
        if (!reelPublishAccountMatches(expectedUserId, stored.cachedUser?.id)) {
            return ApiResult.Failure("This publish belongs to a different signed-in account.", 409)
        }
        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return first
            is ApiResult.Failure -> if (first.statusCode != 401) return first
        }
        return when (val refreshed = authApi.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                if (!reelPublishAccountMatches(expectedUserId, sessionStore.load()?.cachedUser?.id)) {
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

    private fun parseReel(json: JSONObject): NovaReel {
        return NovaReel(
            id = json.optLong("id"),
            author = parseAuthor(json.optJSONObject("author") ?: JSONObject()),
            videoUrl = resolveMediaUrl(json.optString("video_url")),
            thumbnailUrl = resolveMediaUrl(json.optString("thumbnail_url")),
            caption = json.optString("caption"),
            createdAt = json.optString("created_at"),
            isMine = json.optBoolean("is_mine"),
            likesCount = json.optInt("likes_count", 0),
            commentsCount = json.optInt("comments_count", 0),
            repostsCount = json.optInt("reposts_count", 0),
            isLiked = json.optBoolean("is_liked", false),
            isReposted = json.optBoolean("is_reposted", false),
            repostedBy = json.optJSONObject("reposted_by")?.let(::parseAuthor),
        )
    }

    private fun parseComment(json: JSONObject): NovaReelComment {
        val rawParentId = json.opt("parent_id")
        val parentId = when (rawParentId) {
            null, JSONObject.NULL -> null
            is Number -> rawParentId.toLong().takeIf { it > 0L }
            else -> rawParentId.toString().toLongOrNull()?.takeIf { it > 0L }
        }
        val replyRows = json.optJSONArray("replies") ?: JSONArray()
        val replies = buildList {
            for (index in 0 until replyRows.length()) {
                replyRows.optJSONObject(index)?.let { add(parseComment(it)) }
            }
        }
        return NovaReelComment(
            id = json.optLong("id"),
            author = parseAuthor(json.optJSONObject("author") ?: JSONObject()),
            body = json.optString("body"),
            createdAt = json.optString("created_at"),
            isMine = json.optBoolean("is_mine", false),
            parentId = parentId,
            repliesCount = json.optInt("replies_count", replies.size),
            replies = replies,
        )
    }

    private fun parseAuthor(json: JSONObject): NovaReelAuthor {
        return NovaReelAuthor(
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
        const val MAX_REEL_VIDEO_BYTES = 120L * 1024 * 1024
    }
}


internal fun reelPublishAccountMatches(expectedUserId: Long?, activeUserId: Long?): Boolean =
    expectedUserId == null || (expectedUserId > 0L && activeUserId == expectedUserId)
