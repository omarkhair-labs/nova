package com.nova.app.feature.plants

import androidx.compose.ui.unit.IntOffset
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlantupiaOutlineProposalTest {
    @Test fun acceptsOnlyAnOriginalGridCandidateWithAServerToken() {
        val raw = JSONObject("""{"status":"candidate","method":"reviewed-grabcut-points-v1",
            "vertices":[[2,3],[9,3],[8,12]],"suggestion_token":"signed.fixture"}""")
        val candidate = parseOutlineProposal(raw, 20, 20)
        assertEquals(listOf(IntOffset(2,3), IntOffset(9,3), IntOffset(8,12)), candidate?.vertices)
        assertEquals("signed.fixture", candidate?.token)
        val neural = JSONObject(raw.toString()).put("method", "reviewed-magic-touch-point-v1")
        assertEquals("reviewed-magic-touch-point-v1", parseOutlineProposal(neural, 20, 20)?.method)
        assertNull(parseOutlineProposal(neural.put("suggestion_token", ""), 20, 20))
        assertNull(parseOutlineProposal(JSONObject(raw.toString()).put("status", "abstained"), 20, 20))
        assertNull(parseOutlineProposal(JSONObject(raw.toString()).put("suggestion_token", ""), 20, 20))
        assertNull(parseOutlineProposal(raw, 8, 20))
        assertNull(parseOutlineProposal(JSONObject(raw.toString()).put("method", "unknown"), 20, 20))
    }
}
