package com.nova.app.feature.plants.data

import android.content.Context
import android.net.Uri
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.core.messaging.NovaMessagingRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class PlantupiaSpace(
    val id: String,
    val name: String,
    val description: String,
    val location: String,
)

data class PlantupiaPlant(
    val id: String,
    val spaceId: String,
    val label: String,
    val name: String,
    val species: String,
    val placement: String,
    val coverPhoto: String,
    val active: Boolean,
)

data class PlantupiaCareEvent(
    val id: String,
    val kind: String,
    val note: String,
    val evidence: String,
    val observedAt: String,
    val photoUrl: String,
)


data class PlantupiaQuestion(
    val id: String,
    val postId: Long,
    val topic: String,
    val caption: String,
    val imageUrl: String,
    val authorName: String,
    val comments: Int,
    val resolved: Boolean,
    val isMine: Boolean,
)

/**
 * Uses the existing Nova Oracle-hosted API and login session.
 * Nova's existing sessions/media/social architecture are not rebuilt.
 * The Plantupia domain remains a separate backend app with object-level permissions.
 */
class PlantupiaRepository(context: Context) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)
    private val api = NovaApiClient(NovaMessagingRepository.PRODUCTION_API_URL)

    suspend fun spaces(): ApiResult<List<PlantupiaSpace>> =
        authorized { token ->
            parseList("plantupia/spaces/", token) { raw ->
                PlantupiaSpace(
                    id = raw.optString("id"),
                    name = raw.optString("name"),
                    description = raw.optString("description"),
                    location = raw.optString("location_note"),
                )
            }
        }

    suspend fun plants(): ApiResult<List<PlantupiaPlant>> =
        authorized { token ->
            parseList("plantupia/plants/", token) { raw ->
                PlantupiaPlant(
                    id = raw.optString("id"),
                    spaceId = raw.optString("space"),
                    label = raw.optString("label"),
                    name = raw.optString("my_name").ifBlank { raw.optString("label") },
                    species = raw.optString("species_label"),
                    placement = raw.optString("placement_note"),
                    coverPhoto = raw.optString("cover_photo"),
                    active = raw.optBoolean("is_active", true),
                )
            }
        }

    suspend fun history(plantId: String): ApiResult<List<PlantupiaCareEvent>> =
        authorized { token ->
            parseList("plantupia/plants/$plantId/events/", token) { raw ->
                PlantupiaCareEvent(
                    id = raw.optString("id"),
                    kind = raw.optString("kind"),
                    note = raw.optString("note"),
                    evidence = raw.optString("evidence_type"),
                    observedAt = raw.optString("observed_at"),
                    photoUrl = api.resolveMediaUrl(raw.optString("photo")),
                )
            }
        }


    suspend fun recordWithPhoto(
        plantId: String,
        kind: String,
        note: String,
        photoUri: Uri,
    ): ApiResult<Unit> = authorized { token ->
        val data = withContext(Dispatchers.IO) {
            runCatching {
                val input = appContext.contentResolver.openInputStream(photoUri)
                    ?: error("Can't open photo")
                input.use { stream ->
                    val out = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    while (true) {
                        val bytes = stream.read(chunk)
                        if (bytes < 0) break
                        out.write(chunk, 0, bytes)
                        if (out.size() > 10 * 1024 * 1024) error("Photo too large")
                    }
                    out.toByteArray()
                }
            }.getOrNull()
        } ?: return@authorized ApiResult.Failure("Please choose a photo smaller than 10 MB.")
        val mimeType = appContext.contentResolver.getType(photoUri)
            ?.takeIf { it == "image/jpeg" || it == "image/png" || it == "image/webp" }
            ?: "image/jpeg"
        val ext = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        when (val result = api.requestMultipart(
            path = "plantupia/plants/$plantId/events/",
            method = "POST",
            fields = mapOf(
                "kind" to kind,
                "note" to note.trim(),
                "evidence_type" to "observed",
                "observed_at" to Instant.now().toString(),
            ),
            fileField = "photo",
            file = UploadFile(bytes = data, fileName = "plant-event.$ext", mimeType = mimeType),
            bearerToken = token,
        )) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> result
        }
    }

    suspend fun nickname(plantId: String, value: String): ApiResult<Unit> =
        authorized { token ->
            when (val result = api.requestJson(
                path = "plantupia/plants/$plantId/nickname/",
                method = "PUT",
                body = JSONObject().put("nickname", value.trim()),
                bearerToken = token,
            )) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> result
            }
        }

    suspend fun record(
        plantId: String,
        kind: String,
        note: String,
    ): ApiResult<Unit> =
        authorized { token ->
            when (val result = api.requestJson(
                path = "plantupia/plants/$plantId/events/",
                method = "POST",
                body = JSONObject()
                    .put("kind", kind)
                    .put("note", note.trim())
                    .put("evidence_type", "observed")
                    .put("observed_at", Instant.now().toString()),
                bearerToken = token,
            )) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> result
            }
        }


    suspend fun circle(): ApiResult<List<PlantupiaQuestion>> =
        authorized { token -> parseQuestionList("plantupia/circle/", token) }

    suspend fun plantQuestions(plantId: String): ApiResult<List<PlantupiaQuestion>> =
        authorized { token -> parseQuestionList("plantupia/plants/$plantId/questions/", token) }

    private suspend fun parseQuestionList(path: String, token: String): ApiResult<List<PlantupiaQuestion>> =
        parseList(path, token) { raw ->
            val post = raw.optJSONObject("post") ?: JSONObject()
            val author = post.optJSONObject("author") ?: JSONObject()
            PlantupiaQuestion(
                id = raw.optString("id"),
                postId = post.optLong("id"),
                topic = raw.optString("topic"),
                caption = post.optString("caption"),
                imageUrl = api.resolveMediaUrl(
                    post.optString("thumbnail_url").ifBlank { post.optString("image_url") }
                ),
                authorName = author.optString("name").ifBlank { author.optString("username") },
                comments = post.optInt("comments_count"),
                resolved = raw.optBoolean("resolved"),
                isMine = post.optBoolean("is_mine"),
            )
        }

    suspend fun askWithPhoto(
        plantId: String,
        publicTopic: String,
        question: String,
        imageUri: Uri,
    ): ApiResult<Long> = authorized { token ->
        val bytesResult = withContext(Dispatchers.IO) {
            runCatching {
                val input = appContext.contentResolver.openInputStream(imageUri)
                    ?: error("Unable to open the selected photo")
                input.use { stream ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        out.write(buffer, 0, count)
                        if (out.size() > 10 * 1024 * 1024) {
                            error("Choose an image smaller than 10 MB")
                        }
                    }
                    out.toByteArray()
                }
            }.getOrNull()
        } ?: return@authorized ApiResult.Failure("Couldn't read this photo (maximum 10 MB).")

        if (bytesResult.isEmpty()) return@authorized ApiResult.Failure("Select a non-empty photo.")
        when (val response = api.requestMultipart(
            path = "plantupia/plants/$plantId/questions/",
            method = "POST",
            fields = mapOf(
                "public_topic" to publicTopic.trim(),
                "caption" to question.trim(),
                "public_share_consent" to "true",
                "media_type" to "image",
            ),
            fileField = "image",
            file = UploadFile(bytes = bytesResult, fileName = "plant-question.jpg", mimeType = appContext.contentResolver.getType(imageUri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"),
            bearerToken = token,
        )) {
            is ApiResult.Failure -> response
            is ApiResult.Success -> ApiResult.Success(response.value.optLong("post_id"))
        }
    }

    suspend fun resolveQuestion(plantId: String, discussionId: String, outcome: String): ApiResult<Unit> =
        authorized { token ->
            when (val result = api.requestJson(
                path = "plantupia/plants/$plantId/questions/$discussionId/resolve/",
                method = "POST",
                body = JSONObject().put("note", outcome.trim()),
                bearerToken = token,
            )) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> result
            }
        }

    private suspend fun <T> parseList(
        path: String,
        bearer: String,
        parseItem: (JSONObject) -> T,
    ): ApiResult<List<T>> =
        when (val result = api.requestJson(path = path, bearerToken = bearer)) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val rows = result.value.optJSONArray("results") ?: JSONArray()
                ApiResult.Success(
                    buildList {
                        repeat(rows.length()) { index ->
                            rows.optJSONObject(index)?.let { add(parseItem(it)) }
                        }
                    },
                )
            }
        }

    private suspend fun <T> authorized(block: suspend (String) -> ApiResult<T>): ApiResult<T> {
        val session = sessionStore.load()
            ?: return ApiResult.Failure("Please sign in to open your Plantupia space.", 401)
        val first = block(session.accessToken)
        if (first !is ApiResult.Failure || first.statusCode != 401) return first
        return when (val refreshed = api.refresh(session.refreshToken)) {
            is ApiResult.Failure -> refreshed
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                block(refreshed.value)
            }
        }
    }
}
