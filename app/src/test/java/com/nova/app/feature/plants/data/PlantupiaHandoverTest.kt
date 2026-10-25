package com.nova.app.feature.plants.data

import com.nova.app.core.network.ApiResult
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaHandoverTest {
    @Test fun codeParserAcceptsOnlyOpaqueDeliverySecretsIncludingScannedPayloads() {
        val code = "a_-".repeat(15)
        assertEquals(code, deliveryCode(" $code\n"))
        for (wrong in listOf("", "a".repeat(39), "a".repeat(161), "https://example.test/#$code", "{\"code\":\"$code\"}")) {
            assertNull(deliveryCode(wrong))
        }
    }

    @Test fun restartIntentExpiresAndNeverCrossesBoundAccount() {
        val code = "a".repeat(43)
        val unbound = PendingPlantupiaHandover(code, null, 1000)
        assertTrue(canResumeHandover(unbound, null, 1001))
        assertTrue(canResumeHandover(unbound, 7, 1001))
        val bound = PendingPlantupiaHandover(code, 7, 1000)
        assertTrue(canResumeHandover(bound, 7, 1001))
        assertFalse(canResumeHandover(bound, 8, 1001))
        assertFalse(canResumeHandover(bound, null, 1001))
        assertFalse(canResumeHandover(bound, 7, 999))
        assertFalse(canResumeHandover(bound, 7, 1000 + 24 * 60 * 60 * 1000L))
        assertFalse(bound.toString().contains(code))
    }

    private fun delivery(state: String = "ready") = JSONObject().put("state", state)
        .put("plant_count", 7).put("space_label", "Synthetic seven plants").put("ownership_mode", "customer_owned")

    @Test fun previewCannotBecomeClaimSuccessOrInventSpaceIdentity() {
        val preview = delivery()
        assertNull(parseDelivery(preview, false).spaceId)
        assertFalse(parseDelivery(preview, false).claimed)
        assertTrue(parsePlantupiaResponse(preview) { parseDelivery(it, true) } is ApiResult.Failure)
        assertTrue(parsePlantupiaResponse(delivery("claimed")) { parseDelivery(it, true) } is ApiResult.Failure)
        val claim = parseDelivery(delivery("claimed").put("space_id", "original-space"), true)
        assertEquals("original-space", claim.spaceId)
        assertEquals(7, claim.plantCount)
    }

    @Test fun malformedServerAcknowledgementsFailClosed() {
        for (body in listOf(delivery("unknown"), delivery().put("plant_count", 0),
            delivery().put("plant_count", "7"), delivery().put("ownership_mode", "other"),
            delivery().put("space_label", JSONObject.NULL))) {
            assertTrue(parsePlantupiaResponse(body) { parseDelivery(it, false) } is ApiResult.Failure)
        }
    }

    @Test fun missingGuideIsHonestAndNeverBecomesVerifiedAdvice() {
        val body = JSONObject().put("plant_id", "plant").put("state", "unverified")
            .put("identity_verified", false).put("placement", "window").put("guide", JSONObject.NULL)
        val guide = parseCareGuide(body, "plant")
        assertNull(guide.version)
        assertFalse(guide.verifiedIdentity)
        assertEquals("window", guide.placement)
        assertTrue(parsePlantupiaResponse(body) { parseCareGuide(it, "other") } is ApiResult.Failure)
        body.put("state", "available")
        assertTrue(parsePlantupiaResponse(body) { parseCareGuide(it, "plant") } is ApiResult.Failure)
    }

    @Test fun errorsOfferBoundedRecoveryWithoutExposingServerDetails() {
        assertTrue(handoverFailure(404).contains("another account"))
        assertTrue(handoverFailure(410).contains("expired"))
        assertTrue(handoverFailure(503).contains("not available"))
        assertTrue(handoverFailure(null).contains("retry the same code"))
    }
}
