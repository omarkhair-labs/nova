package com.nova.app.feature.plants.data

import com.nova.app.core.network.ApiResult
import org.json.JSONObject

/**
 * Library identities use stable curated slugs, unlike private PlantUnit UUIDs.
 * Do not pass them through the PlantUnit/Space list envelope (which requires id).
 */
internal fun parsePlantupiaSpeciesList(body: JSONObject): ApiResult<List<PlantupiaLibrarySpecies>> =
    parsePlantupiaResponse(body) { envelope ->
        val rows = envelope.optJSONArray("results")
            ?: throw IllegalArgumentException("Missing species list")
        require(rows.length() <= 25)
        List(rows.length()) { index ->
            val raw = rows.optJSONObject(index)
                ?: throw IllegalArgumentException("Invalid species item")
            val aliases = raw.optJSONArray("aliases")
            PlantupiaLibrarySpecies(
                key = requiredPlantupiaString(raw, "key"),
                scientificName = requiredPlantupiaString(raw, "scientific_name"),
                aliases = buildList {
                    if (aliases != null) for (i in 0 until aliases.length()) {
                        aliases.optJSONObject(i)?.optString("name")
                            ?.takeIf { it.isNotBlank() }?.let(::add)
                    }
                },
                reviewedGuideAvailable = raw.optBoolean("has_published_guide"),
            )
        }
    }
