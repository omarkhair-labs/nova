package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test

/** Real Compose surface with fully synthetic, offline QA order data. */
class PlantupiaOrderTrackingInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun event(kind: String, at: String) = JSONObject()
        .put("kind", kind).put("at", at).put("label", "Recorded " + kind)

    @Test fun customerSeesOnlyRecordedOrderProgressAndTrueCarrierHandover() {
        val order = JSONObject()
            .put("id", "qa-test-order")
            .put("status", "dispatched")
            .put("timeline", JSONArray()
                .put(event("quote_accepted", "2026-11-18T08:00:00Z"))
                .put(event("preparing", "2026-11-18T09:00:00Z"))
                .put(event("dispatched", "2026-11-18T10:00:00Z")))
            .put("fulfillment", JSONObject()
                .put("carrier_name", "QA delivery team")
                .put("tracking_reference", "QA-REF-001")
                .put("delivery_proof_recorded", false))
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PlantupiaOrderTracking(order)
                }
            }
        }
        compose.onNodeWithText("Track your order", substring = true, ignoreCase = true).assertExists()
        compose.onNodeWithText("Handed to the delivery team").assertExists()
        compose.onNodeWithText("Preparing your order").assertExists()
        compose.onNodeWithText("Tracking reference · QA-REF-001").assertExists()
        compose.onAllNodesWithText("Live courier map").assertCountEquals(0)
        compose.onNodeWithText("See recorded activity").performScrollTo().performClick()
        compose.onNodeWithText("Recorded dispatched").assertExists()
    }

    @Test fun quotePendingIsNotDisplayedAsConfirmedOrder() {
        val order = JSONObject().put("id", "qa-quote")
            .put("status", "quote_pending")
            .put("timeline", JSONArray().put(event("quote_requested", "2026-11-18T08:00:00Z")))
        compose.setContent {
            MaterialTheme { PlantupiaOrderTracking(order) }
        }
        compose.onNodeWithText("Request received").assertExists()
        compose.onNodeWithText(
            "Your items and delivery details are saved. The team will confirm delivery cost before you choose to proceed. No payment or shipment yet."
        ).assertExists()
        compose.onAllNodesWithText("Order confirmed").assertCountEquals(0)
    }
}
