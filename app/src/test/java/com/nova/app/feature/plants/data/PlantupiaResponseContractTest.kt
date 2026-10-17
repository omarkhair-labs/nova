package com.nova.app.feature.plants.data

import com.nova.app.core.network.ApiResult
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaResponseContractTest {
    private fun rows(raw: String) = parsePlantupiaList(JSONObject(raw)) { requiredPlantupiaId(it) }

    @Test fun missingWrongOrPartiallyMalformedResultsNeverBecomeEmptyOrPartialRecords() {
        for (raw in listOf("{}", "{\"detail\":\"upstream error\"}", "{\"results\":null}",
            "{\"results\":{}}", "{\"results\":[null]}", "{\"results\":[1]}",
            "{\"results\":[{}]}", "{\"results\":[{\"id\":123}]}",
            "{\"results\":[{\"id\":\"real\"},{}]}")) {
            assertTrue(raw, rows(raw) is ApiResult.Failure)
        }
    }

    @Test fun genuineEmptyAndPopulatedListsRemainDistinctSuccesses() {
        assertEquals(ApiResult.Success(emptyList<String>()), rows("{\"results\":[]}"))
        assertEquals(ApiResult.Success(listOf("real")), rows("{\"results\":[{\"id\":\"real\"}]}"))
    }

    @Test fun missingLegacyPermissionFieldDoesNotInvalidateEnvelopeOrGrantAccess() {
        assertTrue(rows("{\"results\":[{\"id\":\"space\",\"name\":\"Home\"}]}") is ApiResult.Success)
        assertFalse(JSONObject("{\"id\":\"space\"}").optBoolean("can_add_plant", false))
    }

    @Test fun historyRequiresExplicitNullableForwardIntegerOffset() {
        assertNull(plantupiaNextOffset(JSONObject("{\"next_offset\":null}"), 0))
        assertEquals(20, plantupiaNextOffset(JSONObject("{\"next_offset\":20}"), 0))
        for (raw in listOf("{}", "{\"next_offset\":\"20\"}", "{\"next_offset\":true}",
            "{\"next_offset\":0}", "{\"next_offset\":-1}", "{\"next_offset\":1.5}", "{\"next_offset\":2147483648}")) {
            assertTrue(raw, parsePlantupiaResponse(JSONObject(raw)) { plantupiaNextOffset(it, 0) } is ApiResult.Failure)
        }
        assertTrue(parsePlantupiaResponse(JSONObject("{\"next_offset\":20}")) { plantupiaNextOffset(it, 20) } is ApiResult.Failure)
    }

    @Test fun circleRequiresExplicitNullablePositiveStringCursor() {
        assertNull(plantupiaNextCursor(JSONObject("{\"next_cursor\":null}")))
        assertEquals("42", plantupiaNextCursor(JSONObject("{\"next_cursor\":\"42\"}")))
        for (raw in listOf("{}", "{\"next_cursor\":42}", "{\"next_cursor\":\"null\"}",
            "{\"next_cursor\":\"0\"}", "{\"next_cursor\":\"bad\"}")) {
            assertTrue(raw, parsePlantupiaResponse(JSONObject(raw), ::plantupiaNextCursor) is ApiResult.Failure)
        }
    }

    @Test fun mutationAcknowledgementsRequireCorrectNonblankIdentity() {
        for (raw in listOf("{}", "{\"id\":null}", "{\"id\":true}", "{\"id\":\"\"}", "{\"id\":\"null\"}", "{\"id\":\"other-plant\"}")) {
            assertTrue(raw, parsePlantupiaResponse(JSONObject(raw)) { requiredPlantupiaId(it, "requested-plant") } is ApiResult.Failure)
        }
        assertEquals(ApiResult.Success("requested-plant"), parsePlantupiaResponse(JSONObject("{\"id\":\"requested-plant\"}")) { requiredPlantupiaId(it, "requested-plant") })
    }

    @Test fun publicPostAcknowledgementCannotBecomeZeroOrCoerceWrongTypes() {
        for (value in listOf("null", "0", "-1", "true", "1.5", "\"42\"")) {
            assertTrue(parsePlantupiaResponse(JSONObject("{\"post_id\":$value}")) { requiredPlantupiaLong(it, "post_id") } is ApiResult.Failure)
        }
        assertEquals(ApiResult.Success(42L), parsePlantupiaResponse(JSONObject("{\"post_id\":42}")) { requiredPlantupiaLong(it, "post_id") })
    }
}
