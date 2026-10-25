package com.nova.app.feature.plants.data

import android.content.Context
import android.net.Uri
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.auth.authenticatedRequest
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
    // Explicit public post-author identity. These are never private plant
    // records; deleted accounts receive a generic, non-identifying placeholder.
    val authorId: Long = 0L,
    val authorAvatarUrl: String = "",
    val authorAvatarVariant: Int = -1,
    val authorDeleted: Boolean = false,
)

/**
 * Uses the existing Nova Oracle-hosted API and login session.
 * Nova's existing sessions/media/social architecture are not rebuilt.
 * The Plantupia domain remains a separate backend app with object-level permissions.
 */
class PlantupiaRepository(
    context: Context,
    private val api: NovaApiClient = NovaApiClient(NovaMessagingRepository.PRODUCTION_API_URL),
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)
    // A repository belongs to the login that created it, never a later account.
    private val initialSession = sessionStore.load()
    fun isCurrentSession(): Boolean = initialSession?.let { sessionStore.current(it.identity) != null } == true

    private val submissions by lazy {
        PlantupiaSubmissionJournal(java.io.File(appContext.noBackupFilesDir, "plantupia-submissions"), NovaMessagingRepository.PRODUCTION_API_URL)
    }
    private val submissionAccount get() = initialSession?.cachedUser?.id?.takeIf { it > 0L } ?: error("Please log in again.")

    suspend fun delivery(token: String, confirm: Boolean = false): ApiResult<PlantupiaDelivery> = authorized { bearer ->
        val payload = JSONObject().put("invitation", token)
        if (confirm) payload.put("confirm", true)
        when (val response = api.requestJson(
            path = if (confirm) "plantupia/handovers/claim/" else "plantupia/handovers/preview/",
            method = "POST", body = payload, bearerToken = bearer,
        )) {
            is ApiResult.Failure -> response.copy(message = handoverFailure(response.statusCode))
            is ApiResult.Success -> parsePlantupiaResponse(response.value) { parseDelivery(it, confirm) }
        }
    }

    suspend fun careGuide(plantId: String): ApiResult<PlantupiaCareGuide> = authorized { bearer ->
        when (val response = api.requestJson("plantupia/plants/$plantId/care-guide/", bearerToken = bearer)) {
            is ApiResult.Failure -> response
            is ApiResult.Success -> parsePlantupiaResponse(response.value) { parseCareGuide(it, plantId) }
        }
    }

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
                is ApiResult.Success -> parsePlantupiaResponse(response.value) { requiredPlantupiaId(it) }
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
            is ApiResult.Success -> parsePlantupiaResponse(response.value) { requiredPlantupiaId(it, spaceId); Unit }
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
            is ApiResult.Success -> parsePlantupiaResponse(response.value) { requiredPlantupiaId(it) }
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
            is ApiResult.Success -> parsePlantupiaResponse(response.value) { requiredPlantupiaId(it, plantId); Unit }
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
                is ApiResult.Success -> parsePlantupiaResponse(response.value) { requiredPlantupiaId(it, plantId); Unit }
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
                    name = requiredPlantupiaString(raw, "name"),
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
                    spaceId = requiredPlantupiaString(raw, "space"),
                    label = requiredPlantupiaString(raw, "label"),
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
                is ApiResult.Success -> parsePlantupiaResponse(response.value) { body ->
                    val events = plantupiaRows(body).map { raw ->
                        PlantupiaCareEvent(
                            id = requiredPlantupiaId(raw),
                            kind = requiredPlantupiaString(raw, "kind"),
                            note = raw.optString("note"),
                            evidence = raw.optString("evidence_type"),
                            observedAt = requiredPlantupiaString(raw, "observed_at"),
                            photoUrl = api.resolveMediaUrl(raw.optString("photo")),
                            followUpOn = raw.optString("follow_up_on").takeUnless { it == "null" }.orEmpty(),
                            followUpCompletedAt = raw.optString("follow_up_completed_at").takeUnless { it == "null" }.orEmpty(),
                            followUpFor = raw.optString("follow_up_for").takeUnless { it == "null" }.orEmpty(),
                        )
                    }
                    val next = plantupiaNextOffset(body, pageOffset)
                    require(events.isNotEmpty() || next == null)
                    PlantupiaHistoryPage(events, next)
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
                is ApiResult.Success -> parsePlantupiaResponse(response.value) {
                    requiredPlantupiaId(it, eventId)
                    require(it.opt("follow_up_on") == newDate && it.opt("pending") == true)
                    Unit
                }
                is ApiResult.Failure -> response
            }
        }

    suspend fun recordWithPhoto(
        plantId: String, kind: String, note: String, photoUri: Uri,
        followUpOn: String? = null, followUpFor: String? = null, reminder: Boolean = false,
    ): ApiResult<String> = prepareSave(plantId, "event", eventFields(kind, note, followUpOn, followUpFor), photoUri, reminder)

    suspend fun nickname(plantId: String, value: String): ApiResult<Unit> =
        authorized { token ->
            when (val result = api.requestJson(
                path = "plantupia/plants/$plantId/nickname/",
                method = "PUT",
                body = JSONObject().put("nickname", value.trim()),
                bearerToken = token,
            )) {
                is ApiResult.Success -> parsePlantupiaResponse(result.value) {
                    requiredPlantupiaId(it, plantId)
                    requiredPlantupiaString(it, "my_name")
                    Unit
                }
                is ApiResult.Failure -> result
            }
        }

    suspend fun record(
        plantId: String, kind: String, note: String, followUpOn: String? = null,
        followUpFor: String? = null, reminder: Boolean = false,
    ): ApiResult<String> = prepareSave(plantId, "event", eventFields(kind, note, followUpOn, followUpFor), null, reminder)

    private fun eventFields(kind: String, note: String, followUpOn: String?, followUpFor: String?) =
        mapOf("kind" to kind, "note" to note.trim(), "evidence_type" to "observed") +
            listOfNotNull(followUpOn?.let { "follow_up_on" to it }, followUpFor?.let { "follow_up_for" to it }).toMap()

    private suspend fun prepareSave(
        plantId: String, operation: String, fields: Map<String, String>, photoUri: Uri?, reminder: Boolean = false,
    ): ApiResult<String> {
        if (!isCurrentSession()) return changedSession()
        val prepared = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = photoUri?.let { uri ->
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        val out = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            check(out.size() <= 10 * 1024 * 1024) { "Choose a photo smaller than 10 MB." }
                        }
                        out.toByteArray().also { check(it.isNotEmpty()) { "Choose a non-empty photo." } }
                    } ?: error("Couldn't read the selected photo.")
                }
                val mime = photoUri?.let { uri ->
                    appContext.contentResolver.getType(uri) ?: if (uri.scheme == "file") {
                        when (java.io.File(uri.path.orEmpty()).extension) { "png" -> "image/png"; "jpg" -> "image/jpeg"; "webp" -> "image/webp"; else -> null }
                    } else null
                }
                if (photoUri != null) check(mime in setOf("image/jpeg", "image/png", "image/webp")) { "Use a JPEG, PNG or WebP photo." }
                val slot = if (operation == "question") "question" else fields["follow_up_for"]?.let { "outcome-$it" } ?: "event"
                submissions.prepare(submissionAccount, plantId, slot, fields, bytes, mime, reminder)
            }
        }
        if (!isCurrentSession()) return changedSession()
        val submission = prepared.getOrElse { return ApiResult.Failure(it.message ?: "Couldn't preserve this save. Please retry.") }
        return retrySubmission(submission, publicConsentConfirmed = operation == "question")
    }

    internal suspend fun pendingSubmissions(plantId: String): ApiResult<List<PlantupiaSubmission>> {
        if (!isCurrentSession()) return changedSession()
        val result = withContext(Dispatchers.IO) {
            runCatching { submissions.pending(submissionAccount, plantId) }
        }
        if (!isCurrentSession()) return changedSession()
        return result.fold({ ApiResult.Success(it) }, { ApiResult.Failure("An unconfirmed save couldn't be read. Its identity was preserved; please retry.") })
    }

    internal suspend fun confirmSubmission(plantId: String, slot: String, resultId: String) = withContext(Dispatchers.IO) {
        if (isCurrentSession()) runCatching { submissions.confirm(submissionAccount, plantId, slot, resultId) }.getOrDefault(false)
    }

    internal suspend fun retrySubmission(submission: PlantupiaSubmission, publicConsentConfirmed: Boolean = false): ApiResult<String> {
        if (submission.operation == "question" && !publicConsentConfirmed) return ApiResult.Failure("Review the saved question and confirm public sharing again.")
        if (!isCurrentSession() || submission.account != submissionAccount) return changedSession()
        val stored = withContext(Dispatchers.IO) { runCatching { submissions.load(submissionAccount, submission.plant, submission.slot) } }
            .getOrElse { return ApiResult.Failure("The original saved submission couldn't be read. Its identity was preserved.") }
        if (!isCurrentSession() || stored?.copy(acknowledgedResult = null) != submission.copy(acknowledgedResult = null)) return changedSession()
        val fields = submission.fields + ("client_submission_id" to submission.id)
        val result = authorized { token ->
            val path = "plantupia/plants/${submission.plant}/${submission.operation}-submissions/"
            val response = if (submission.photo == null) api.requestJson(path, "POST", JSONObject(fields), token)
                else api.requestMultipart(path, "POST", fields,
                    if (submission.operation == "event") "photo" else "image",
                    UploadFile(fileName = submission.photo.name, mimeType = requireNotNull(submission.mime), sourceFile = submission.photo), token)
            when (response) {
                is ApiResult.Failure -> response
                is ApiResult.Success -> parsePlantupiaResponse(response.value) {
                    require(requiredPlantupiaString(it, "client_submission_id") == submission.id)
                    val id = requiredPlantupiaId(it)
                    if (submission.operation == "question") requiredPlantupiaLong(it, "post_id").toString() else id
                }
            }
        }
        val retained = withContext(Dispatchers.IO) { runCatching {
            when (result) {
                is ApiResult.Success -> submissions.acknowledge(submission, result.value)
                is ApiResult.Failure -> if (result.statusCode == 400 && result.errorCode == "submission_rejected" && result.requestId == submission.id) submissions.reject(submission)
            }
        } }
        if (retained.isFailure) return ApiResult.Failure("The save couldn't be confirmed locally. Retry the original saved submission.")
        return if (isCurrentSession()) result else changedSession()
    }

    suspend fun circle(cursor: String? = null): ApiResult<PlantupiaCirclePage> =
        authorized { token ->
            val safeCursor = cursor?.toLongOrNull()?.takeIf { it > 0L }?.toString()
            val suffix = safeCursor?.let { "?cursor=$it" }.orEmpty()
            when (val response = api.requestJson(
                path = "plantupia/circle/$suffix", bearerToken = token,
            )) {
                is ApiResult.Failure -> response
                is ApiResult.Success -> parsePlantupiaResponse(response.value) { body ->
                    val questions = plantupiaRows(body).map(::parseQuestion)
                    val next = plantupiaNextCursor(body)
                    require(questions.isNotEmpty() || next == null)
                    PlantupiaCirclePage(questions, next)
                }
            }
        }

    suspend fun plantQuestions(plantId: String): ApiResult<List<PlantupiaQuestion>> =
        authorized { token -> parseQuestionList("plantupia/plants/$plantId/questions/", token) }

    private suspend fun parseQuestionList(path: String, token: String): ApiResult<List<PlantupiaQuestion>> =
        parseList(path, token, ::parseQuestion)

    private fun parseQuestion(raw: JSONObject): PlantupiaQuestion {
        val post = raw.getJSONObject("post")
        val author = post.getJSONObject("author")
        val authorId = requiredPlantupiaLong(author, "id")
        return PlantupiaQuestion(
            id = raw.optString("id"),
            postId = requiredPlantupiaLong(post, "id"),
            topic = requiredPlantupiaString(raw, "topic"),
            caption = post.optString("caption"),
            imageUrl = api.resolveMediaUrl(
                post.optString("thumbnail_url").ifBlank { post.optString("image_url") }
            ),
            authorName = author.optString("name").ifBlank { author.optString("username") },
            authorId = authorId,
            authorAvatarUrl = api.resolveMediaUrl(author.optString("avatar_url")),
            authorAvatarVariant = author.optInt("plantupia_avatar_variant", -1).takeIf { it in -1..7 } ?: -1,
            authorDeleted = author.optString("username") == "deleted",
            comments = post.optInt("comments_count"),
            resolved = raw.optBoolean("resolved"),
            isMine = post.optBoolean("is_mine"),
        )
    }

    suspend fun askWithPhoto(plantId: String, publicTopic: String, question: String, imageUri: Uri): ApiResult<Long> =
        when (val result = prepareSave(plantId, "question", mapOf(
            "public_topic" to publicTopic.trim(), "caption" to question.trim(),
            "public_share_consent" to "true", "media_type" to "image",
        ), imageUri)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toLong())
            is ApiResult.Failure -> result
        }

    suspend fun resolveQuestion(plantId: String, discussionId: String, outcome: String): ApiResult<Unit> =
        authorized { token ->
            when (val result = api.requestJson(
                path = "plantupia/plants/$plantId/questions/$discussionId/resolve/",
                method = "POST",
                body = JSONObject().put("note", outcome.trim()),
                bearerToken = token,
            )) {
                is ApiResult.Success -> parsePlantupiaResponse(result.value) {
                    requiredPlantupiaId(it, discussionId)
                    requiredPlantupiaString(it, "event_id")
                    require(it.opt("resolved") == true)
                    Unit
                }
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
            is ApiResult.Success -> parsePlantupiaList(result.value, parseItem)
        }

    private fun changedSession() = ApiResult.Failure("Your account changed. Reopen your Plantupia space.", 409)

    private suspend fun <T> authorized(block: suspend (String) -> ApiResult<T>): ApiResult<T> {
        if (!isCurrentSession()) return changedSession()
        val session = sessionStore.load() ?: return changedSession()
        return sessionStore.authenticatedRequest(
            stored = session,
            refresh = { api.refresh(session.refreshToken) },
            request = block,
        )
    }
}
