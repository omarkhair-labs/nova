package com.nova.app.feature.memories.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.feature.memories.data.MemoryRepository
import com.nova.app.feature.memories.data.parseMemoryFilmPlan
import com.nova.app.feature.memories.data.parseMemoryDraft
import com.nova.app.feature.memories.data.parseWeeklyMemory
import com.nova.app.feature.memories.domain.model.MemoryFilmPlan
import com.nova.app.feature.memories.domain.model.MemoryDraft
import com.nova.app.feature.memories.domain.model.WeeklyMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray


class MemoryRemoteRepository(
    context: Context,
    private val api: NovaApiClient,
) : MemoryRepository {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

    override suspend fun week(
        utcOffsetMinutes: Int,
        weeksAgo: Int,
    ): ApiResult<WeeklyMemory> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "memories/week/?utc_offset_minutes=$utcOffsetMinutes&weeks_ago=$weeksAgo",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                parseWeeklyMemory(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    override suspend fun filmPlan(
        utcOffsetMinutes: Int,
        weeksAgo: Int,
    ): ApiResult<MemoryFilmPlan> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "memories/film-plan/?utc_offset_minutes=$utcOffsetMinutes&weeks_ago=$weeksAgo",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                parseMemoryFilmPlan(response.value, api::resolveMediaUrl),
            )
            is ApiResult.Failure -> response
        }
    }

    override suspend fun drafts(): ApiResult<List<MemoryDraft>> = authenticatedCall { token ->
        when (val response = api.requestJson("memories/drafts/", bearerToken = token)) {
            is ApiResult.Success -> {
                val rows = response.value.optJSONArray("drafts") ?: JSONArray()
                ApiResult.Success(
                    buildList {
                        for (index in 0 until rows.length()) {
                            rows.optJSONObject(index)?.let {
                                add(parseMemoryDraft(it, api::resolveMediaUrl))
                            }
                        }
                    }
                )
            }
            is ApiResult.Failure -> response
        }
    }

    override suspend fun createDraft(
        kind: String,
        title: String,
        note: String,
        mediaUri: Uri?,
    ): ApiResult<MemoryDraft> {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return ApiResult.Failure("Give this Memory a title.")
        if (cleanTitle.length > 120) return ApiResult.Failure("Memory title must be 120 characters or fewer.")
        val cleanNote = note.trim()
        if (cleanNote.length > 500) return ApiResult.Failure("Memory note must be 500 characters or fewer.")
        val cleanKind = kind.trim().lowercase().takeIf { it == "recap" || it == "film" }
            ?: return ApiResult.Failure("Choose a recap or film Memory.")
        val media = if (mediaUri == null) null else when (
            val prepared = withContext(Dispatchers.IO) { prepareMedia(mediaUri) }
        ) {
            is ApiResult.Success -> prepared.value
            is ApiResult.Failure -> return prepared
        }
        return authenticatedCall { token ->
            when (
                val response = api.requestMultipart(
                    path = "memories/drafts/",
                    method = "POST",
                    fields = mapOf("kind" to cleanKind, "title" to cleanTitle, "note" to cleanNote),
                    fileField = "media",
                    file = media,
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseMemoryDraft(response.value, api::resolveMediaUrl))
                is ApiResult.Failure -> response
            }
        }
    }

    override suspend fun deleteDraft(draftId: Long): ApiResult<Unit> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "memories/drafts/$draftId/",
                method = "DELETE",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> response
        }
    }

    override suspend fun updateDraft(
        draftId: Long,
        kind: String,
        title: String,
        note: String,
        mediaUri: Uri?,
    ): ApiResult<MemoryDraft> {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return ApiResult.Failure("Give this Memory a title.")
        val cleanKind = kind.trim().lowercase().takeIf { it == "recap" || it == "film" }
            ?: return ApiResult.Failure("Choose a recap or film Memory.")
        val media = if (mediaUri == null) null else when (
            val prepared = withContext(Dispatchers.IO) { prepareMedia(mediaUri) }
        ) {
            is ApiResult.Success -> prepared.value
            is ApiResult.Failure -> return prepared
        }
        return authenticatedCall { token ->
            when (
                val response = api.requestMultipart(
                    path = "memories/drafts/$draftId/",
                    method = "PATCH",
                    fields = mapOf("kind" to cleanKind, "title" to cleanTitle, "note" to note.trim()),
                    fileField = "media",
                    file = media,
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(parseMemoryDraft(response.value, api::resolveMediaUrl))
                is ApiResult.Failure -> response
            }
        }
    }

    private fun prepareMedia(uri: Uri): ApiResult<UploadFile> {
        val resolver = appContext.contentResolver
        val mimeType = resolver.getType(uri)?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        if (!mimeType.startsWith("image/") && !mimeType.startsWith("video/")) {
            return ApiResult.Failure("Memory drafts support photos and videos only.")
        }
        val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            ?: return ApiResult.Failure("Nova couldn't read that file.")
        if (bytes.size > 60 * 1024 * 1024) return ApiResult.Failure("Memory media must be 60 MB or smaller.")
        val displayName = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use ""
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index < 0) "" else cursor.getString(index).orEmpty()
            }.orEmpty()
        }.getOrDefault("")
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty()
        return ApiResult.Success(
            UploadFile(
                bytes = bytes,
                fileName = displayName.ifBlank { "memory-draft${if (extension.isBlank()) "" else ".$extension"}" },
                mimeType = mimeType,
            )
        )
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
}
