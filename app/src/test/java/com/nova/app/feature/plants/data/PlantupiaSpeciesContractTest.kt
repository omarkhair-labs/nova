package com.nova.app.feature.plants.data

import com.nova.app.core.network.ApiResult
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaSpeciesContractTest {
    @Test fun publishedLibraryUsesCuratedSlugRatherThanPrivatePlantUuid() {
        val alias = JSONObject().put("name", "Rubber plant").put("language", "en")
        val item = JSONObject().put("key", "ficus-elastica")
            .put("scientific_name", "Ficus elastica")
            .put("aliases", JSONArray().put(alias))
            .put("has_published_guide", true)
        val result = parsePlantupiaSpeciesList(JSONObject().put("results", JSONArray().put(item)))
        assertTrue(result is ApiResult.Success)
        val species = (result as ApiResult.Success).value.single()
        assertEquals("ficus-elastica", species.key)
        assertEquals("Rubber plant", species.aliases.single())
        assertTrue(species.reviewedGuideAvailable)
    }

    @Test fun libraryFailsClosedOnMalformedEnvelopes() {
        val invalid = listOf(
            JSONObject(),
            JSONObject().put("results", JSONObject()),
            JSONObject().put("results", JSONArray().put(JSONObject().put("id", "private-plant"))),
            JSONObject().put("results", JSONArray().put("not a curated type")),
        )
        invalid.forEach { payload ->
            assertTrue(parsePlantupiaSpeciesList(payload) is ApiResult.Failure)
        }
    }

    @Test fun emptyPublishedLibraryIsValidAndDoesNotFakePlants() {
        val result = parsePlantupiaSpeciesList(JSONObject().put("results", JSONArray()))
        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).value.isEmpty())
    }
}
