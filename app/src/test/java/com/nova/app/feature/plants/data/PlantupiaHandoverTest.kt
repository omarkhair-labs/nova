package com.nova.app.feature.plants.data

import com.nova.app.core.network.ApiResult
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

class PlantupiaHandoverTest {
    @Test fun actualQrCodecRoundTripProducesTheSameOpaqueInvitation() {
        val code = "synthetic_" + "a".repeat(43)
        val image = QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, 256, 256)
        val pixels = IntArray(256 * 256) { index -> if (image[index % 256, index / 256]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(256, 256, pixels))))
        assertEquals(code, deliveryCode(decoded.text))
        assertEquals(BarcodeFormat.QR_CODE, decoded.barcodeFormat)
    }
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

    @Test fun userSelectedSpeciesGetsOnlyClearlyQualifiedGeneralGuidance() {
        val guide = JSONObject()
            .put("version", 1).put("species", "Synthetic QA type")
            .put("instructions", "Synthetic general guidance")
            .put("context_notes", "").put("source_url", "https://example.test/qa")
            .put("source_reference", "Synthetic QA reference")
            .put("reviewer", "qa-reviewer")
            .put("reviewed_at", "2026-11-03T08:00:00Z")
        val body = JSONObject().put("plant_id", "plant")
            .put("state", "general").put("identity_verified", false)
            .put("identity_basis", "user_selected")
            .put("placement", "indoor").put("guide", guide)
        val parsed = parseCareGuide(body, "plant")
        assertEquals("user_selected", parsed.identityBasis)
        assertFalse(parsed.verifiedIdentity)
        assertEquals(1, parsed.version)
        // Neither a forged staff verification nor an incorrectly labeled
        // "available" guide may be accepted for a user-reported type.
        body.put("identity_basis", "staff_verified")
        assertTrue(parsePlantupiaResponse(body) { parseCareGuide(it, "plant") } is ApiResult.Failure)
        body.put("identity_basis", "user_selected").put("state", "available")
        assertTrue(parsePlantupiaResponse(body) { parseCareGuide(it, "plant") } is ApiResult.Failure)
    }

    @Test fun publishedCareTopicsAreTypedOrderedAndPreserveFullLegacyText() {
        val section = JSONObject()
            .put("topic", "water").put("title", "Water inspection")
            .put("instructions", "Synthetic observed conditions, not an instruction to water.")
            .put("context_notes", "Synthetic QA only.")
            .put("source_url", "https://example.test/qa-section")
            .put("source_reference", "Synthetic section source")
            .put("position", 2)
        val second = JSONObject(section.toString())
            .put("topic", "fertilizing").put("title", "Fertilizing context")
            .put("position", 1)
        val guide = JSONObject()
            .put("version", 3).put("species", "Synthetic test plant")
            .put("instructions", "Full reviewed synthetic narrative")
            .put("legacy_instructions", "Original authored synthetic narrative")
            .put("sections", JSONArray().put(section).put(second))
            .put("context_notes", "")
            .put("source_url", "https://example.test/reviewer-guide")
            .put("source_reference", "Synthetic source")
            .put("reviewer", "qa-reviewer").put("reviewed_at", "2026-11-03T08:00:00Z")
        val body = JSONObject().put("plant_id", "my-plant")
            .put("state", "general").put("identity_verified", false)
            .put("identity_basis", "user_selected")
            .put("placement", "window").put("guide", guide)
        val parsed = parseCareGuide(body, "my-plant")
        assertFalse(parsed.verifiedIdentity)
        assertEquals("user_selected", parsed.identityBasis)
        assertEquals(listOf("fertilizing", "water"), parsed.topics.map { it.topic })
        assertEquals("Original authored synthetic narrative", parsed.legacyInstructions)
        assertEquals("Synthetic QA only.", parsed.topics.last().contextNotes)
    }

    @Test fun malformedOrUnsafePublishedCareTopicsFailClosed() {
        val item = JSONObject().put("topic", "water").put("title", "Synthetic water")
            .put("instructions", "Only a synthetic fixture")
            .put("context_notes", "").put("source_url", "http://unsafe.test/")
            .put("source_reference", "test").put("position", 0)
        val guide = JSONObject()
            .put("version", 1).put("species", "Synthetic plant")
            .put("instructions", "Synthetic fallback")
            .put("context_notes", "").put("source_url", "https://example.test")
            .put("source_reference", "test")
            .put("reviewer", "qa-reviewer")
            .put("reviewed_at", "2026-11-03T08:00:00Z")
            .put("sections", JSONArray().put(item))
        val body = JSONObject().put("plant_id", "plant")
            .put("state", "available").put("identity_verified", true)
            .put("identity_basis", "staff_verified")
            .put("placement", "indoor").put("guide", guide)
        assertTrue(parsePlantupiaResponse(body) { parseCareGuide(it, "plant") } is ApiResult.Failure)
        item.put("source_url", "https://example.test/topic")
        item.put("topic", "not-a-topic")
        assertTrue(parsePlantupiaResponse(body) { parseCareGuide(it, "plant") } is ApiResult.Failure)
        item.put("topic", "water")
        guide.put("sections", JSONArray().put(item).put(JSONObject(item.toString())))
        assertTrue(parsePlantupiaResponse(body) { parseCareGuide(it, "plant") } is ApiResult.Failure)
    }

    @Test fun errorsOfferBoundedRecoveryWithoutExposingServerDetails() {
        assertTrue(handoverFailure(404).contains("another account"))
        assertTrue(handoverFailure(410).contains("expired"))
        assertTrue(handoverFailure(503).contains("not available"))
        assertTrue(handoverFailure(null).contains("retry the same code"))
    }
}
