package com.nova.app.feature.plants

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaOrderTrackingTest {
    private val t0 = "2026-11-18T08:00:00Z"
    private val t1 = "2026-11-18T09:00:00Z"
    private val t2 = "2026-11-18T10:00:00Z"
    private val t3 = "2026-11-18T11:00:00Z"

    private fun events(vararg entries: Pair<String, String>): JSONArray =
        JSONArray().also { a ->
            entries.forEach { (kind, time) ->
                a.put(JSONObject().put("kind", kind).put("label", "Real recorded event")
                    .put("at", time))
            }
        }

    private fun order(status: String, timeline: JSONArray = JSONArray(),
                      fulfilled: Boolean = false, deliveryProof: Boolean = false): JSONObject {
        val obj = JSONObject().put("id", "fictional-order").put("status", status)
            .put("timeline", timeline)
        if (fulfilled) obj.put("fulfillment", JSONObject()
            .put("carrier_name", "Example authorized delivery carrier")
            .put("tracking_reference", "QA-TRACKING-001")
            .put("delivery_proof_recorded", deliveryProof))
        return obj
    }

    @Test fun pendingQuoteIsNotYetAnOrderedOrDispatchedProduct() {
        val result = shopOrderTracking(order("quote_pending",
            events("quote_requested" to t0)))
        assertEquals("Request received", result.stageTitle)
        assertEquals(listOf("quote_requested", "shipping_quoted"), result.steps.map { it.key })
        assertEquals("Request received", result.steps[0].title)
        assertTrue(result.steps[0].recorded)
        assertFalse(result.steps[1].recorded)
        assertNull(result.trackingReference)
        assertFalse(result.liveLocationAvailable)
    }

    @Test fun acceptedQuoteRecordsOnlyActualConfirmedStage() {
        val result = shopOrderTracking(order("confirmed",
            events("quote_requested" to t0, "shipping_quoted" to t1,
                "quote_accepted" to t2)))
        assertEquals("Order confirmed", result.stageTitle)
        assertEquals(listOf(true, false, false, false), result.steps.map { it.recorded })
        assertEquals(t2, result.steps[0].recordedAt)
        assertNull(result.trackingReference)
    }

    @Test fun inPreparationCannotPretendCourierCollectedParcel() {
        val result = shopOrderTracking(order("preparing",
            events("quote_accepted" to t0, "preparing" to t1)))
        assertEquals(listOf(true, true, false, false), result.steps.map { it.recorded })
        assertNull(result.carrier)
        assertFalse(result.liveLocationAvailable)
    }

    @Test fun physicalHandoverNeedsCarrierIdentityReferenceAndEvent() {
        val result = shopOrderTracking(order("dispatched",
            events("quote_accepted" to t0, "preparing" to t1, "dispatched" to t2),
            fulfilled = true))
        assertEquals(listOf(true, true, true, false), result.steps.map { it.recorded })
        assertEquals("QA-TRACKING-001", result.trackingReference)
        assertEquals("Example authorized delivery carrier", result.carrier)
        assertFalse(result.liveLocationAvailable)
        val unverified = shopOrderTracking(order("dispatched",
            events("quote_accepted" to t0, "preparing" to t1, "dispatched" to t2)))
        assertFalse(unverified.steps[2].recorded)
        assertTrue(unverified.description.contains("unavailable"))
    }

    @Test fun deliveredNeedsBothHandoverAndRealRetainedProof() {
        val timeline = events("quote_accepted" to t0, "preparing" to t1,
            "dispatched" to t2, "delivered" to t3)
        val verified = shopOrderTracking(order("delivered", timeline,
            fulfilled = true, deliveryProof = true))
        assertEquals(listOf(true, true, true, true), verified.steps.map { it.recorded })
        val withoutProof = shopOrderTracking(order("delivered", timeline,
            fulfilled = true, deliveryProof = false))
        assertFalse(withoutProof.steps.last().recorded)
        assertTrue(withoutProof.description.contains("proof"))
    }

    @Test fun invalidEventTimestampCannotBeShownAsVerifiedActivity() {
        val result = shopOrderTracking(order("dispatched",
            events("quote_accepted" to "not-a-timestamp", "dispatched" to "no-date"),
            fulfilled = true))
        assertTrue(result.steps.none { it.recorded })
        assertFalse(result.liveLocationAvailable)
    }

    @Test fun cancellationAndReturnsNeverInventAResumedDelivery() {
        val cancelled = shopOrderTracking(order("cancelled",
            events("quote_accepted" to t0, "cancelled" to t1)))
        assertEquals("Order cancelled", cancelled.stageTitle)
        assertTrue(cancelled.description.contains("cancellation or return"))
        val returned = shopOrderTracking(order("returned",
            events("quote_accepted" to t0, "preparing" to t1, "dispatched" to t2,
                "delivered" to t3), fulfilled = true, deliveryProof = true))
        assertEquals("Return recorded", returned.stageTitle)
        assertFalse(returned.liveLocationAvailable)
    }
}
