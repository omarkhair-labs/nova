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
    val canAddPlant: Boolean = false,
    // A missing flag on an older deployed preview is NOT proof of denied access.
    // Creation stays disabled until authority is confirmed by the server.
    val canAddPlantPermissionReported: Boolean = true,
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
    val canRecord: Boolean,
    val lastEventKind: String,
    val lastEventAt: String,
    val nextFollowUpOn: String = "",
)

/** An actual server-generated answer. No scripted/mock reply is used in the UI. */
data class PlantupiaAssistantReply(
    val answer: String,
    val scope: String,
    val uncertainty: String,
    val sourceLinks: List<String>,
)

data class PlantupiaAssistantTurn(val role: String, val content: String)

data class PlantupiaHistoryPage(val events: List<PlantupiaCareEvent>, val nextOffset: Int?)

data class PlantupiaCareEvent(
    val id: String,
    val kind: String,
    val note: String,
    val evidence: String,
    val observedAt: String,
    val photoUrl: String,
    val followUpOn: String,
    val followUpCompletedAt: String,
    // UUID of the original private care/observation event closed by an outcome.
    val followUpFor: String = "",
)


data class PlantupiaCirclePage(
    val questions: List<PlantupiaQuestion>,
    val nextCursor: String?,
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

    /** The AI key lives server-side; private plant context is opt-in per request. */
    suspend fun askAssistant(
        message: String,
        plantId: String? = null,
        includeContext: Boolean = false,
        conversation: List<PlantupiaAssistantTurn> = emptyList(),
    ): ApiResult<PlantupiaAssistantReply> = authorized { token ->
        val payload = JSONObject()
            .put("message", message.trim())
            .put("include_my_plant_context", includeContext)
        if (!plantId.isNullOrBlank()) payload.put("plant_id", plantId)
        if (conversation.isNotEmpty()) {
            payload.put("conversation", JSONArray().apply {
                conversation.takeLast(8).forEach { turn ->
                    put(JSONObject()
                        .put("role", turn.role)
                        .put("content", turn.content.take(1200)))
                }
            })
        }
        when (val response = api.requestJson(
            path = "plantupia/assistant/ask/",
            method = "POST",
            body = payload,
            bearerToken = token,
            readTimeoutMillis = 26_000,
        )) {
            is ApiResult.Failure -> response
            is ApiResult.Success -> {
                val json = response.value
                val answer = json.optString("answer").trim()
                if (answer.isEmpty()) {
                    ApiResult.Failure("The assistant returned an incomplete response. Please try again.")
                } else {
                    val sources = json.optJSONArray("source_links")
                    ApiResult.Success(
                        PlantupiaAssistantReply(
                            answer = answer,
                            scope = json.optString("scope", "general"),
                            uncertainty = json.optString("uncertainty"),
                            sourceLinks = buildList {
                                if (sources != null) for (i in 0 until sources.length()) {
                                    val url = sources.optString(i)
                                    if (url.startsWith("https://")) add(url)
                                }
                            },
                        )
                    )
                }
            }
        }
    }

    /** A new user's first space is owned by that user, not a company admin. */
    suspend fun createSpace(name: String, description: String, location: String): ApiResult<String> =
        authorized { token ->
            when (val response = api.requestJson(
                path = "plantupia/spaces/",
                method = "POST",
                body = JSONObject()
                    .put("name", name.trim())
                    .put("description", description.trim())
                    .put("location_note", location.trim()),
                bearerToken = token,
            )) {
                is ApiResult.Failure -> response
                is ApiResult.Success -> response.value.optString("id")
                    .takeIf(String::isNotBlank)?.let { ApiResult.Success(it) }
                    ?: ApiResult.Failure("The new space was not returned. Try refreshing.")
            }
        }

    /** Authorized private space metadata only, never its ownership or memberships. */
    suspend fun updateSpaceDetails(
        spaceId: String,
        name: String,
        description: String,
        location: String,
    ): ApiResult<Unit> = authorized { token ->
        when (val response = api.requestJson(
            path = "plantupia/spaces/$spaceId/",
            method = "PATCH",
            body = JSONObject()
                .put("name", name.trim())
                .put("description", description.trim())
                .put("location_note", location.trim()),
            bearerToken = token,
        )) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> response
        }
    }

    /** Uploading a cover photo is private; it never creates a public post. */
    suspend fun createPlant(
        spaceId: String,
        name: String,
        species: String,
        placement: String,
        photoUri: Uri?,
    ): ApiResult<String> = authorized { token ->
        val fields = mapOf(
            "space" to spaceId,
            "label" to name.trim(),
            "species_label" to species.trim(),
            "placement_note" to placement.trim(),
            "unit_type" to "individual",
        )
        val response = if (photoUri == null) {
            api.requestJson(
                path = "plantupia/plants/",
                method = "POST",
                body = JSONObject().apply { fields.forEach { (key, value) -> put(key, value) } },
                bearerToken = token,
            )
        } else {
            val photo = when (val result = loadPrivatePhoto(photoUri)) {
                is ApiResult.Failure -> return@authorized result
                is ApiResult.Success -> result.value
            }
            api.requestMultipart(
                path = "plantupia/plants/",
                method = "POST",
                fields = fields,
                fileField = "cover_photo",
                file = photo,
                bearerToken = token,
            )
        }
        when (response) {
            is ApiResult.Failure -> response
            is ApiResult.Success -> response.value.optString("id")
                .takeIf(String::isNotBlank)?.let { ApiResult.Success(it) }
                ?: ApiResult.Failure("The new plant was not returned. Try refreshing.")
        }
    }

    /** Changes shared plant context only for owners/managers on the server. */
    suspend fun updatePlantDetails(
        plantId: String,
        sharedName: String,
        species: String,
        placement: String,
    ): ApiResult<Unit> = authorized { token ->
        when (val response = api.requestJson(
            path = "plantupia/plants/$plantId/",
            method = "PATCH",
            body = JSONObject()
                .put("label", sharedName.trim())
                .put("species_label", species.trim())
                .put("placement_note", placement.trim()),
            bearerToken = token,
        )) {
            is ApiResult.Failure -> response
            is ApiResult.Success -> ApiResult.Success(Unit)
        }
    }

    /** Reversible server-authorized archive; private photos and events remain intact. */
    suspend fun setPlantArchived(plantId: String, archived: Boolean): ApiResult<Unit> =
        authorized { token ->
            when (val response = api.requestJson(
                path = "plantupia/plants/$plantId/",
                method = "PATCH",
                body = JSONObject().put("is_active", !archived),
                bearerToken = token,
            )) {
                is ApiResult.Failure -> response
                is ApiResult.Success -> ApiResult.Success(Unit)
            }
        }

    private suspend fun loadPrivatePhoto(uri: Uri): ApiResult<UploadFile> {
        val mimeType = appContext.contentResolver.getType(uri)
            ?.takeIf { it == "image/jpeg" || it == "image/png" || it == "image/webp" }
            ?: return ApiResult.Failure("Choose a JPEG, PNG or WebP photo.")
        val bytes = withContext(Dispatchers.IO) {
            runCatching {
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val size = input.read(buffer)
                        if (size < 0) break
                        output.write(buffer, 0, size)
                        if (output.size() > 10 * 1024 * 1024) error("Too big")
                    }
                    output.toByteArray()
                }
            }.getOrNull()
        } ?: return ApiResult.Failure("Couldn't read photo. Choose an image smaller than 10 MB.")
        if (bytes.isEmpty()) return ApiResult.Failure("Choose a non-empty photo.")
        val ext = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return ApiResult.Success(UploadFile(bytes = bytes, fileName = "plant-cover.$ext", mimeType = mimeType))
    }

    suspend fun spaces(): ApiResult<List<PlantupiaSpace>> =
        authorized { token ->
            parseList("plantupia/spaces/", token) { raw ->
                PlantupiaSpace(
                    id = raw.optString("id"),
                    name = raw.optString("name"),
                    description = raw.optString("description"),
                    location = raw.optString("location_note"),
                    canAddPlant = raw.optBoolean("can_add_plant", false),
                    canAddPlantPermissionReported = raw.has("can_add_plant"),
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
                    coverPhoto = api.resolveMediaUrl(raw.optString("cover_photo")),
                    active = raw.optBoolean("is_active", true),
                    canRecord = raw.optBoolean("can_record", false),
                    lastEventKind = raw.optString("last_event_kind").takeUnless { it == "null" }.orEmpty(),
                    lastEventAt = raw.optString("last_event_at").takeUnless { it == "null" }.orEmpty(),
                    nextFollowUpOn = raw.optString("next_follow_up_on").takeUnless { it == "null" }.orEmpty(),
                )
            }
        }

    suspend fun historyPage(plantId: String, offset: Int = 0, pendingOnly: Boolean = false): ApiResult<PlantupiaHistoryPage> =
        authorized { token ->
            val pageOffset = offset.coerceAtLeast(0)
            when (val response = api.requestJson(
                path = "plantupia/plants/$plantId/events/?offset=$pageOffset" +
                    if (pendingOnly) "&pending_followups=1" else "",
                bearerToken = token,
            )) {
                is ApiResult.Failure -> response
                is ApiResult.Success -> {
                    val body = response.value
                    val rows = body.optJSONArray("results") ?: JSONArray()
                    val events = buildList {
                        repeat(rows.length()) { index ->
                            rows.optJSONObject(index)?.let { raw ->
                                add(PlantupiaCareEvent(
                                    id = raw.optString("id"),
                                    kind = raw.optString("kind"),
                                    note = raw.optString("note"),
                                    evidence = raw.optString("evidence_type"),
                                    observedAt = raw.optString("observed_at"),
                                    photoUrl = api.resolveMediaUrl(raw.optString("photo")),
                                    followUpOn = raw.optString("follow_up_on").takeUnless { it == "null" }.orEmpty(),
                                    followUpCompletedAt = raw.optString("follow_up_completed_at").takeUnless { it == "null" }.orEmpty(),
                                    followUpFor = raw.optString("follow_up_for").takeUnless { it == "null" }.orEmpty(),
                                ))
                            }
                        }
                    }
                    val next = if (body.isNull("next_offset")) null
                        else body.optInt("next_offset").takeIf { it > pageOffset }
                    ApiResult.Success(PlantupiaHistoryPage(events, next))
                }
            }
        }


    /** Postpone a human-chosen review; the server records the change in private history. */
    suspend fun moveCheckIn(plantId: String, eventId: String, newDate: String): ApiResult<Unit> =
        authorized { token ->
            when (val response = api.requestJson(
                path = "plantupia/plants/$plantId/events/$eventId/follow-up/",
                method = "PATCH",
                body = JSONObject().put("follow_up_on", newDate),
                bearerToken = token,
            )) {
                is ApiResult.Success -> ApiResult.Success(Unit)
                is ApiResult.Failure -> response
            }
        }

    suspend fun recordWithPhoto(
        plantId: String,
        kind: String,
        note: String,
        photoUri: Uri,
        followUpOn: String? = null,
        followUpFor: String? = null,
    ): ApiResult<String> = authorized { token ->
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
            ?: return@authorized ApiResult.Failure("Use a JPEG, PNG or WebP photo.")
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
            ) + listOfNotNull(
                followUpOn?.let { "follow_up_on" to it },
                followUpFor?.let { "follow_up_for" to it },
            ).toMap(),
            fileField = "photo",
            file = UploadFile(bytes = data, fileName = "plant-event.$ext", mimeType = mimeType),
            bearerToken = token,
        )) {
            is ApiResult.Success -> ApiResult.Success(result.value.optString("id"))
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
        followUpOn: String? = null,
        followUpFor: String? = null,
    ): ApiResult<String> =
        authorized { token ->
            when (val result = api.requestJson(
                path = "plantupia/plants/$plantId/events/",
                method = "POST",
                body = JSONObject()
                    .put("kind", kind)
                    .put("note", note.trim())
                    .put("evidence_type", "observed")
                    .put("observed_at", Instant.now().toString())
                    .apply {
                        followUpOn?.let { put("follow_up_on", it) }
                        followUpFor?.let { put("follow_up_for", it) }
                    },
                bearerToken = token,
            )) {
                is ApiResult.Success -> ApiResult.Success(result.value.optString("id"))
                is ApiResult.Failure -> result
            }
        }


    suspend fun circle(cursor: String? = null): ApiResult<PlantupiaCirclePage> =
        authorized { token ->
            val safeCursor = cursor?.toLongOrNull()?.takeIf { it > 0L }?.toString()
            val suffix = safeCursor?.let { "?cursor=$it" }.orEmpty()
            when (val response = api.requestJson(
                path = "plantupia/circle/$suffix", bearerToken = token,
            )) {
                is ApiResult.Failure -> response
                is ApiResult.Success -> {
                    val rows = response.value.optJSONArray("results") ?: JSONArray()
                    val questions = buildList {
                        repeat(rows.length()) { index ->
                            rows.optJSONObject(index)?.let { add(parseQuestion(it)) }
                        }
                    }
                    val next = response.value.optString("next_cursor")
                        .takeUnless { it.isBlank() || it == "null" }
                    ApiResult.Success(PlantupiaCirclePage(questions, next))
                }
            }
        }

    suspend fun plantQuestions(plantId: String): ApiResult<List<PlantupiaQuestion>> =
        authorized { token -> parseQuestionList("plantupia/plants/$plantId/questions/", token) }

    private suspend fun parseQuestionList(path: String, token: String): ApiResult<List<PlantupiaQuestion>> =
        parseList(path, token, ::parseQuestion)

    private fun parseQuestion(raw: JSONObject): PlantupiaQuestion {
        val post = raw.optJSONObject("post") ?: JSONObject()
        val author = post.optJSONObject("author") ?: JSONObject()
        return PlantupiaQuestion(
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
        val mimeType = appContext.contentResolver.getType(imageUri)
            ?.takeIf { it == "image/jpeg" || it == "image/png" || it == "image/webp" }
            ?: return@authorized ApiResult.Failure("Use a JPEG, PNG or WebP question photo.")
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
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
            file = UploadFile(bytes = bytesResult, fileName = "plant-question.$extension", mimeType = mimeType),
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
