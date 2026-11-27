package com.nova.app.feature.plants.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaRoomRevisionTest {
    private val response = """
        {"space_id":"space", "revision":1,
         "room":{"room_id":"space","width_m":4.0,"length_m":2.8,"plants":[],"windows":[]}}
    """.trimIndent()

    @Test fun keepsOldRoomPayloadsAndWriteContractCompatible() {
        val room = parseRoom(JSONObject(response), "space")
        assertFalse(room.historical)
        assertTrue(room.revisions.isEmpty())
        val body = room.submission(4.0, 3.0, emptyList(), listOf(RoomWindow("east", 4.0, 1.0)))
        assertEquals(1, body.getInt("base_revision"))
        assertEquals(1, body.getJSONArray("windows").length())
        assertFalse(body.has("historical"))
    }

    @Test fun readsEarlierLayoutAndItsRevisionIndexSeparately() {
        val payload = JSONObject(response).put("historical", true)
        payload.put("layout_revisions", org.json.JSONArray("""
            [{"revision":2,"created_at":"2026-11-26T13:36:00Z"},
             {"revision":1,"created_at":"2026-11-26T13:02:00Z"}]
        """.trimIndent()))
        val room = parseRoom(payload, "space")
        assertTrue(room.historical)
        assertEquals(1, room.revision)
        assertEquals(listOf(2, 1), room.revisions.map { it.number })
    }
}
