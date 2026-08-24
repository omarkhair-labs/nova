package com.nova.app.feature.rooms.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.feature.rooms.data.RoomRepository
import com.nova.app.feature.rooms.data.parseRoomDetail
import com.nova.app.feature.rooms.data.parseRoomItem
import com.nova.app.feature.rooms.data.parseRoomItemPage
import com.nova.app.feature.rooms.data.parseRoomTonightSnapshot
import com.nova.app.feature.rooms.data.parseRooms
import com.nova.app.feature.rooms.domain.model.RoomDetail
import com.nova.app.feature.rooms.domain.model.RoomItem
import com.nova.app.feature.rooms.domain.model.RoomItemPage
import com.nova.app.feature.rooms.domain.model.RoomSummary
import com.nova.app.feature.rooms.domain.model.RoomTonightSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


class RoomRemoteRepository(
    context: Context,
    private val api: NovaApiClient,
) : RoomRepository {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

    override suspend fun rooms(): ApiResult<List<RoomSummary>> = authenticatedCall { token ->
        when (val response = api.requestJson("rooms/", bearerToken = token)) {
            is ApiResult.Success -> ApiResult.Success(
                parseRooms(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    override suspend fun room(conversationId: Long): ApiResult<RoomDetail> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "rooms/$conversationId/",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                parseRoomDetail(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    override suspend fun items(
        conversationId: Long,
        kind: String?,
        before: Long?,
        limit: Int,
    ): ApiResult<RoomItemPage> = authenticatedCall { token ->
        val params = buildList {
            kind?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { add("kind=$it") }
            before?.takeIf { it > 0L }?.let { add("before=$it") }
            add("limit=${limit.coerceIn(1, 50)}")
        }.joinToString("&")
        when (
            val response = api.requestJson(
                path = "rooms/$conversationId/items/?$params",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                parseRoomItemPage(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    override suspend fun createItem(
        conversationId: Long,
        kind: String,
        title: String,
        body: String,
        url: String,
        scheduledFor: String?,
        mediaUri: Uri?,
    ): ApiResult<RoomItem> {
        val cleanKind = kind.trim().lowercase()
        if (cleanKind !in ROOM_ITEM_KINDS) return ApiResult.Failure("Choose a valid Room item type.")
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        val cleanUrl = url.trim()
        val cleanScheduledFor = scheduledFor?.trim()?.takeIf { it.isNotBlank() }
        if (cleanTitle.length > 120) return ApiResult.Failure("Room item title must be 120 characters or fewer.")
        if (cleanBody.length > 500) return ApiResult.Failure("Room item text must be 500 characters or fewer.")
        if (cleanUrl.length > 700) return ApiResult.Failure("Room item link is too long.")
        when (cleanKind) {
            "note" -> if (cleanBody.isBlank()) return ApiResult.Failure("Write something for this Room note.")
            "plan" -> if (cleanTitle.isBlank()) return ApiResult.Failure("Give this plan a title.")
            "music", "saved" -> if (cleanUrl.isBlank()) return ApiResult.Failure("Add a link for this Room item.")
            "photo", "video" -> if (mediaUri == null) return ApiResult.Failure("Choose a $cleanKind first.")
        }

        val path = "rooms/$conversationId/items/"
        if (cleanKind == "photo" || cleanKind == "video") {
            val source = mediaUri ?: return ApiResult.Failure("Choose a $cleanKind first.")
            val media = when (
                val prepared = withContext(Dispatchers.IO) { prepareMedia(source, cleanKind) }
            ) {
                is ApiResult.Success -> prepared.value
                is ApiResult.Failure -> return prepared
            }
            return authenticatedCall { token ->
                when (
                    val response = api.requestMultipart(
                        path = path,
                        method = "POST",
                        fields = buildMap {
                            put("kind", cleanKind)
                            put("title", cleanTitle)
                            put("body", cleanBody)
                            put("url", cleanUrl)
                            cleanScheduledFor?.let { put("scheduled_for", it) }
                        },
                        fileField = "media",
                        file = media,
                        bearerToken = token,
                    )
                ) {
                    is ApiResult.Success -> ApiResult.Success(
                        parseRoomItem(response.value, api::resolveMediaUrl),
                    )
                    is ApiResult.Failure -> response
                }
            }
        }

        return authenticatedCall { token ->
            val payload = JSONObject()
                .put("kind", cleanKind)
                .put("title", cleanTitle)
                .put("body", cleanBody)
                .put("url", cleanUrl)
            cleanScheduledFor?.let { payload.put("scheduled_for", it) }
            when (
                val response = api.requestJson(
                    path = path,
                    method = "POST",
                    body = payload,
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(
                    parseRoomItem(response.value, api::resolveMediaUrl),
                )
                is ApiResult.Failure -> response
            }
        }
    }

    override suspend fun roomTonight(utcOffsetMinutes: Int): ApiResult<RoomTonightSnapshot> =
        authenticatedCall { token ->
            when (
                val response = api.requestJson(
                    path = "rooms/tonight/?utc_offset_minutes=$utcOffsetMinutes",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(
                    parseRoomTonightSnapshot(response.value, api::resolveMediaUrl),
                )
                is ApiResult.Failure -> response
            }
        }

    override suspend fun updateDescription(
        conversationId: Long,
        description: String,
    ): ApiResult<RoomDetail> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "rooms/$conversationId/",
                method = "PATCH",
                body = JSONObject().put("description", description.trim()),
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                parseRoomDetail(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    override suspend fun setReminder(
        conversationId: Long,
        itemId: Long,
        enabled: Boolean,
    ): ApiResult<RoomItem> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "rooms/$conversationId/items/$itemId/reminder/",
                method = if (enabled) "POST" else "DELETE",
                body = if (enabled) JSONObject() else null,
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                parseRoomItem(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    private fun prepareMedia(uri: Uri, kind: String): ApiResult<UploadFile> {
        val resolver = appContext.contentResolver
        val mimeType = resolver.getType(uri)
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            .orEmpty()
            .ifBlank {
                val extension = displayName(uri).substringAfterLast('.', "").lowercase()
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension).orEmpty()
            }
        val expectsPhoto = kind == "photo"
        if (expectsPhoto && !mimeType.startsWith("image/")) {
            return ApiResult.Failure("Room photos must be image files.")
        }
        if (!expectsPhoto && !mimeType.startsWith("video/")) {
            return ApiResult.Failure("Room videos must be video files.")
        }

        val maxBytes = if (expectsPhoto) 12L * 1024 * 1024 else 60L * 1024 * 1024
        val sizeMessage = if (expectsPhoto) {
            "Room photo must be 12 MB or smaller."
        } else {
            "Room video must be 60 MB or smaller."
        }
        val reportedSize = mediaSize(uri)
        if (reportedSize != null && reportedSize > maxBytes) return ApiResult.Failure(sizeMessage)

        val bytes = runCatching {
            resolver.openInputStream(uri)?.use { input -> input.readBytes() }
        }.getOrNull() ?: return ApiResult.Failure("Nova couldn't read that file.")
        if (bytes.size.toLong() > maxBytes) return ApiResult.Failure(sizeMessage)

        val fileName = displayName(uri).ifBlank {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty()
            val base = if (expectsPhoto) "room-photo" else "room-video"
            if (extension.isBlank()) base else "$base.$extension"
        }
        return ApiResult.Success(
            UploadFile(
                bytes = bytes,
                fileName = fileName,
                mimeType = mimeType,
            ),
        )
    }

    private fun displayName(uri: Uri): String = runCatching {
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use ""
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index < 0) "" else cursor.getString(index).orEmpty()
        }.orEmpty()
    }.getOrDefault("")

    private fun mediaSize(uri: Uri): Long? = runCatching {
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index < 0 || cursor.isNull(index)) null else cursor.getLong(index)
        }
    }.getOrNull()

    private suspend fun <T> authenticatedCall(
        call: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return first
            is ApiResult.Failure -> if (first.statusCode != 401) return first
        }

        return when (val refreshed = api.refresh(stored.refreshToken)) {
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

    private companion object {
        val ROOM_ITEM_KINDS = setOf("note", "photo", "video", "music", "plan", "saved")
    }
}
