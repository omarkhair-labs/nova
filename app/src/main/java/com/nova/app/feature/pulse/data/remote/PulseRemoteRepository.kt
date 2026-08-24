package com.nova.app.feature.pulse.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.feature.pulse.data.PulseRepository
import com.nova.app.feature.pulse.data.parseNovaPulse
import com.nova.app.feature.pulse.domain.model.NovaPulse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject


class PulseRemoteRepository(
    context: Context,
    private val api: NovaApiClient,
) : PulseRepository {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

    override suspend fun pulses(): ApiResult<List<NovaPulse>> = authenticatedCall { token ->
        when (val response = api.requestJson("pulses/", bearerToken = token)) {
            is ApiResult.Success -> ApiResult.Success(parsePulseList(response.value))
            is ApiResult.Failure -> response
        }
    }

    override suspend fun createTextPulse(
        note: String,
        audience: String,
    ): ApiResult<NovaPulse> = createTextPulse(note, audience, "vibes")

    override suspend fun createTextPulse(
        note: String,
        audience: String,
        category: String,
    ): ApiResult<NovaPulse> = createTextAt(
        path = "pulses/",
        note = note,
        audience = audience,
        category = category,
    )

    override suspend fun createMediaPulse(
        mediaUri: Uri,
        note: String,
        audience: String,
    ): ApiResult<NovaPulse> = createMediaPulse(mediaUri, note, audience, "vibes")

    override suspend fun createMediaPulse(
        mediaUri: Uri,
        note: String,
        audience: String,
        category: String,
    ): ApiResult<NovaPulse> = createMediaAt(
        path = "pulses/",
        mediaUri = mediaUri,
        note = note,
        audience = audience,
        category = category,
    )

    override suspend fun pulseChain(pulseId: Long): ApiResult<List<NovaPulse>> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "pulses/$pulseId/chain/",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parsePulseList(response.value))
            is ApiResult.Failure -> response
        }
    }

    override suspend fun replyTextPulse(
        pulseId: Long,
        note: String,
        audience: String,
    ): ApiResult<NovaPulse> = replyTextPulse(pulseId, note, audience, "vibes")

    override suspend fun replyTextPulse(
        pulseId: Long,
        note: String,
        audience: String,
        category: String,
    ): ApiResult<NovaPulse> = createTextAt(
        path = "pulses/$pulseId/reply/",
        note = note,
        audience = audience,
        category = category,
    )

    override suspend fun replyMediaPulse(
        pulseId: Long,
        mediaUri: Uri,
        note: String,
        audience: String,
    ): ApiResult<NovaPulse> = replyMediaPulse(pulseId, mediaUri, note, audience, "vibes")

    override suspend fun replyMediaPulse(
        pulseId: Long,
        mediaUri: Uri,
        note: String,
        audience: String,
        category: String,
    ): ApiResult<NovaPulse> = createMediaAt(
        path = "pulses/$pulseId/reply/",
        mediaUri = mediaUri,
        note = note,
        audience = audience,
        category = category,
    )

    override suspend fun deletePulse(pulseId: Long): ApiResult<Unit> = authenticatedCall { token ->
        when (
            val response = api.requestJson(
                path = "pulses/$pulseId/",
                method = "DELETE",
                bearerToken = token,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> response
        }
    }

    private suspend fun createTextAt(
        path: String,
        note: String,
        audience: String,
        category: String,
    ): ApiResult<NovaPulse> {
        val cleanNote = note.trim()
        if (cleanNote.isBlank()) return ApiResult.Failure("Write something for your Pulse.")
        if (cleanNote.length > 180) return ApiResult.Failure("Pulse note must be 180 characters or fewer.")
        val cleanAudience = validateAudience(audience) ?: return invalidAudience()
        val cleanCategory = validateCategory(category) ?: return invalidCategory()

        return authenticatedCall { token ->
            when (
                val response = api.requestJson(
                    path = path,
                    method = "POST",
                    body = JSONObject()
                        .put("media_type", "text")
                        .put("note", cleanNote)
                        .put("audience", cleanAudience)
                        .put("category", cleanCategory),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(
                    parseNovaPulse(response.value, api::resolveMediaUrl),
                )
                is ApiResult.Failure -> response
            }
        }
    }

    private suspend fun createMediaAt(
        path: String,
        mediaUri: Uri,
        note: String,
        audience: String,
        category: String,
    ): ApiResult<NovaPulse> {
        val cleanNote = note.trim()
        if (cleanNote.length > 180) return ApiResult.Failure("Pulse note must be 180 characters or fewer.")
        val cleanAudience = validateAudience(audience) ?: return invalidAudience()
        val cleanCategory = validateCategory(category) ?: return invalidCategory()
        val media = when (
            val prepared = withContext(Dispatchers.IO) { prepareMedia(mediaUri) }
        ) {
            is ApiResult.Success -> prepared.value
            is ApiResult.Failure -> return prepared
        }

        return authenticatedCall { token ->
            when (
                val response = api.requestMultipart(
                    path = path,
                    method = "POST",
                    fields = mapOf(
                        "note" to cleanNote,
                        "audience" to cleanAudience,
                        "category" to cleanCategory,
                    ),
                    fileField = "media",
                    file = media,
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(
                    parseNovaPulse(response.value, api::resolveMediaUrl),
                )
                is ApiResult.Failure -> response
            }
        }
    }

    private fun parsePulseList(json: JSONObject): List<NovaPulse> {
        val rows = json.optJSONArray("results") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                rows.optJSONObject(index)?.let {
                    add(parseNovaPulse(it, api::resolveMediaUrl))
                }
            }
        }
    }

    private fun prepareMedia(uri: Uri): ApiResult<UploadFile> {
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

        val maxBytes = when {
            mimeType.startsWith("image/") -> 15L * 1024 * 1024
            mimeType.startsWith("video/") -> 60L * 1024 * 1024
            else -> return ApiResult.Failure("Pulses support photos and videos only.")
        }
        val sizeMessage = if (mimeType.startsWith("image/")) {
            "Pulse photo must be 15 MB or smaller."
        } else {
            "Pulse video must be 60 MB or smaller."
        }

        val reportedSize = mediaSize(uri)
        if (reportedSize != null && reportedSize > maxBytes) {
            return ApiResult.Failure(sizeMessage)
        }

        val bytes = runCatching {
            resolver.openInputStream(uri)?.use { input -> input.readBytes() }
        }.getOrNull() ?: return ApiResult.Failure("Nova couldn't read that file.")
        if (bytes.size.toLong() > maxBytes) return ApiResult.Failure(sizeMessage)

        val fileName = displayName(uri).ifBlank {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty()
            if (extension.isBlank()) "pulse-upload" else "pulse-upload.$extension"
        }
        return ApiResult.Success(
            UploadFile(
                bytes = bytes,
                fileName = fileName,
                mimeType = mimeType,
            ),
        )
    }

    private fun displayName(uri: Uri): String {
        return runCatching {
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
    }

    private fun mediaSize(uri: Uri): Long? {
        return runCatching {
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
    }

    private fun validateAudience(value: String): String? =
        value.trim().lowercase().takeIf { it == "followers" || it == "close_friends" }

    private fun invalidAudience(): ApiResult.Failure =
        ApiResult.Failure("Choose a valid Pulse audience.")

    private fun validateCategory(value: String): String? =
        value.trim().lowercase().takeIf { it in setOf("live", "music", "talks", "vibes") }

    private fun invalidCategory(): ApiResult.Failure =
        ApiResult.Failure("Choose a valid Pulse category.")

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
