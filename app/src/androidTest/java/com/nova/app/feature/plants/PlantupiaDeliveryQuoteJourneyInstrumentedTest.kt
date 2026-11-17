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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Source-only Compose fixtures; no device location permission or server write. */
class PlantupiaDeliveryQuoteJourneyInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun draft() = ShopDeliveryDraft(
        recipient = "Example Recipient", phone = "+20 100 000 0000",
        city = "Cairo", street = "42 Example street", note = "Door B",
        latitude = 30.0251234, longitude = 31.2249999, pinConfirmed = true,
        paymentPreference = "cod",
    )
    private fun cart() = JSONObject()
        .put("fingerprint", "a".repeat(64))
        .put("can_request_shipping_quote", true)
        .put("subtotal", "300.00")
        .put("results", JSONArray().put(JSONObject()
            .put("product", JSONObject()
                .put("id", "fictional-sku")
                .put("product_id", "fictional-object")
                .put("name", "FICTIONAL QA object")
                .put("kind", "plant")
                .put("variant_label", "16 cm pot")
                .put("price", "300.00"))
            .put("quantity", 1)
            .put("line_total", "300.00")
            .put("available", true)))

    @Test fun detailsShowMapControlAndPaymentChoiceWithoutTakingMoney() {
        var paymentSelected = ""
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    fixture(draft(), cart(), "details",
                        onPayment = { paymentSelected = it })
                }
            }
        }
        compose.onNodeWithText("Where should these objects go?").assertExists()
        compose.onNodeWithText("✓ Exact pin selected · Edit").assertExists()
        compose.onNodeWithText("Manual bank transfer").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("manual_transfer", paymentSelected) }
        compose.onNodeWithText("Review delivery quote request  ↗").assertExists()
        compose.onAllNodesWithText("Pay now").assertCountEquals(0)
    }

    @Test fun reviewKeepsKnownCartPriceSeparateFromUnknownShipping() {
        var submits = 0
        var edits = 0
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    fixture(draft(), cart(), "review",
                        onSubmit = { submits++ }, onEdit = { edits++ })
                }
            }
        }
        compose.onNodeWithText("FICTIONAL QA object").assertExists()
        compose.onNodeWithText("16 cm pot · 1 unit(s)").assertExists()
        compose.onNodeWithText("Shipping: awaiting a company quote · No full total yet.").assertExists()
        compose.onNodeWithText("Send delivery quote request").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, submits) }
        compose.onNodeWithText("Edit address or preference").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, edits) }
    }

    @Test fun receivedSceneRequiresSuppliedServerReceiptAndGivesNextAction() {
        val receipt = ShopDeliveryQuoteReceipt(
            reference = "e70b1769-51d3-45b9-84fa-5bc2b217e001",
            productSubtotal = "300.00", recipient = "Example Recipient",
            city = "Cairo", paymentPreference = "cod",
        )
        var orders = 0
        var collection = 0
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    fixture(draft(), null, "received", receipt = receipt,
                        onOrders = { orders++ }, onCollection = { collection++ })
                }
            }
        }
        compose.onNodeWithText("Delivery quote requested").assertExists()
        compose.onNodeWithText("No payment was taken and no stock was reserved.").assertExists()
        compose.onNodeWithText("View this request  ↗").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, orders) }
        compose.onNodeWithText("Return to the living collection").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, collection) }
    }

    @Test fun invalidDetailsCannotSendWithoutRevisitingRealAddressAndCart() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    fixture(ShopDeliveryDraft(), cart(), "review",
                        error = "No confirmed receipt. Check your request list.")
                }
            }
        }
        compose.onNodeWithText("Send delivery quote request").assertIsNotEnabled()
        compose.onNodeWithText("No confirmed receipt. Check your request list.").assertExists()
    }

    @Composable
    private fun fixture(
        data: ShopDeliveryDraft, savedCart: JSONObject?, state: String,
        receipt: ShopDeliveryQuoteReceipt? = null,
        error: String? = null,
        onPayment: (String) -> Unit = {},
        onSubmit: () -> Unit = {},
        onEdit: () -> Unit = {},
        onOrders: () -> Unit = {},
        onCollection: () -> Unit = {},
    ) {
        PlantupiaDeliveryQuoteJourney(
            draft = data, cart = savedCart, step = state, receipt = receipt,
            busy = false, error = error, showErrors = true,
            onRecipient = {}, onPhone = {}, onCity = {}, onStreet = {},
            onBuilding = {}, onLandmark = {}, onNote = {}, onPin = { _, _ -> },
            onPayment = onPayment, onReview = {}, onEdit = onEdit,
            onSubmit = onSubmit, onCart = {}, onOrders = onOrders,
            onCollection = onCollection,
        )
    }
}
