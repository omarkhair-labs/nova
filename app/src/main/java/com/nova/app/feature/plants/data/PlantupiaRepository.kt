package com.nova.app.feature.plants.data

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.messaging.NovaMessagingRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

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
)

/**
 * Uses the existing Nova Oracle-hosted API and login session.
 * Nova's existing sessions/media/social architecture are not rebuilt.
 * The Plantupia domain remains a separate backend app with object-level permissions.
 */
class PlantupiaRepository(context: Context) {
    private val sessionStore = NovaSessionStore(context.applicationContext)
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
                )
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
