package com.nova.app.feature.plants

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaDeliveryQuotePolicyTest {
    private val validFingerprint = "a".repeat(64)
    private fun draft() = ShopDeliveryDraft(
        recipient = "  Test Recipient ",
        phone = "+20 100 000 0000",
        city = " Cairo ",
        street = " Example Street 42 ",
        building = " Floor 3 ",
        landmark = " Entry B ",
        note = " Leave at reception ",
        latitude = 30.0251234,
        longitude = 31.2249999,
        pinConfirmed = true,
        paymentPreference = "manual_transfer",
    )
    private fun cart() = JSONObject()
        .put("fingerprint", validFingerprint)
        .put("subtotal", "640.00")
        .put("can_request_shipping_quote", true)
        .put("results", JSONArray().put(JSONObject()
            .put("product", JSONObject()
                .put("id", "test-sku")
                .put("product_id", "test-object")
                .put("name", "FICTIONAL QA object")
                .put("kind", "plant")
                .put("variant_label", "Small")
                .put("price", "640.00")
                .put("available_units", 2))
            .put("quantity", 1)
            .put("line_total", "640.00")
            .put("available", true)))

    private fun response() = JSONObject()
        .put("id", "e70b1769-51d3-45b9-84fa-5bc2b217e001")
        .put("status", "quote_pending")
        .put("stock_reserved", false)
        .put("charged", false)
        .put("shipping_fee", JSONObject.NULL)
        .put("subtotal", "640.00")
        .put("payment_preference", "manual_transfer")
        .put("lines", JSONArray().put(JSONObject()
            .put("sku_id", "test-sku")
            .put("name", "FICTIONAL QA object")
            .put("variant", "Small")
            .put("quantity", 1)
            .put("unit_price", "640.00")
            .put("subtotal", "640.00")))
        .put("customer", JSONObject()
            .put("name", "Test Recipient")
            .put("phone", "+20 100 000 0000")
            .put("city", "Cairo")
            .put("address", "Example Street 42")
            .put("latitude", "30.0251234")
            .put("longitude", "31.2249999")
            .put("pin_confirmed", true))

    @Test fun realReviewedDraftAndRealCartCanEnterReview() {
        assertTrue(shopDeliveryFieldIssues(draft()).isEmpty())
        assertTrue(shopDeliveryReady(draft(), cart()))
    }

    @Test fun validationNamesExactlyWhatUserNeedsToCorrect() {
        val invalid = draft().copy(
            recipient = " ", phone = "123", city = " ", street = "x",
            latitude = null, longitude = null, pinConfirmed = false,
            paymentPreference = "unknown",
        )
        assertEquals(
            listOf("Recipient name", "Phone number", "City / governorate",
                "Street / address", "Confirmed map pin", "Payment preference"),
            shopDeliveryFieldIssues(invalid),
        )
        assertFalse(shopDeliveryReady(invalid, cart()))
    }

    @Test fun cartMustBeAnExactCurrentlyQuotableSourceSnapshot() {
        assertFalse(shopDeliveryReady(draft(), null))
        assertFalse(shopDeliveryReady(draft(), JSONObject()))
        assertFalse(shopDeliveryReady(draft(), cart().put("can_request_shipping_quote", false)))
        assertFalse(shopDeliveryReady(draft(), cart().put("fingerprint", "not-a-hash")))
        assertFalse(shopDeliveryReady(draft(), cart().put("results", JSONArray())))
        val unreadable = cart().put("results", JSONArray().put(JSONObject().put("quantity", 1)))
        assertFalse(shopDeliveryReady(draft(), unreadable))
    }

    @Test fun requestPayloadPreservesServerContractAndHumanNote() {
        val data = shopDeliveryQuotePayload(draft(), cart())
        assertEquals(validFingerprint, data.getString("cart_fingerprint"))
        assertEquals("Test Recipient", data.getString("contact_name"))
        assertEquals("+20 100 000 0000", data.getString("contact_phone"))
        assertEquals("Cairo", data.getString("city"))
        assertEquals("Example Street 42", data.getString("delivery_address"))
        assertEquals("Floor 3", data.getString("building_details"))
        assertEquals("Entry B", data.getString("landmark"))
        assertEquals("Leave at reception", data.getString("customer_note"))
        assertEquals("manual_transfer", data.getString("payment_preference"))
        assertEquals(true, data.getBoolean("delivery_pin_confirmed"))
        assertFalse(data.has("client_id")) // added by existing idempotent journal
    }

    @Test fun onlyMatchingUnpaidUnreservedQuotePendingIsAcknowledged() {
        val expected = shopDeliveryReceiptFromResponse(response(), draft(), cart())
        assertNotNull(expected)
        assertEquals("e70b1769-51d3-45b9-84fa-5bc2b217e001", expected!!.reference)
        assertEquals("640.00", expected.productSubtotal)
        assertEquals("FICTIONAL QA object", expected.lines.single().name)
        assertEquals("Small", expected.lines.single().variant)
        assertEquals(1, expected.lines.single().quantity)
        assertEquals("640.00", expected.lines.single().subtotal)
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("status", "confirmed"), draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("lines", JSONArray()), draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("lines", JSONArray().put(JSONObject()
                .put("sku_id", "other-sku").put("name", "Not yours")
                .put("quantity", 1).put("unit_price", "640.00")
                .put("subtotal", "640.00"))), draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("lines", JSONArray().put(JSONObject()
                .put("sku_id", "test-sku").put("name", "FICTIONAL QA object")
                .put("quantity", 2).put("unit_price", "640.00")
                .put("subtotal", "1280.00"))), draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("stock_reserved", true), draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("charged", true), draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("shipping_fee", "50.00"), draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("subtotal", "500.00"), draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("id", "not-uuid"), draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("payment_preference", "cod"), draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().getJSONObject("customer").put("address", "Another street")
                .let { response().put("customer", it) }, draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().getJSONObject("customer").put("pin_confirmed", false)
                .let { response().put("customer", it) }, draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            response().put("customer", JSONObject().put("name", "Another customer")
                .put("phone", "+20 100 000 0000").put("city", "Cairo")), draft(), cart()))
    }

    @Test fun previousPinMustNotRemainConfirmedAfterChangingStreetOrCity() {
        val original = draft()
        assertTrue(shopDeliveryFieldIssues(original).isEmpty())
        assertTrue(shopDeliveryFieldIssues(original.copy(street = "New street", pinConfirmed = false))
            .contains("Confirmed map pin"))
    }

    private fun exactPriceReply() = JSONObject()
        .put("available", true)
        .put("city", "Cairo")
        .put("cart_fingerprint", validFingerprint)
        .put("subtotal", "640.00")
        .put("shipping_fee", "35.00")
        .put("final_total", "675.00")
        .put("currency", "EGP")
        .put("rate_revision", 2)

    @Test fun directPriceRequiresExactCurrentCityCartAndServerArithmetic() {
        val price = shopDeliveryExactPrice(exactPriceReply(), draft(), cart())
        assertNotNull(price)
        assertEquals("675.00", price!!.total)
        assertEquals("35.00", price.shippingFee)
        assertEquals(2, price.rateRevision)
        assertNull(shopDeliveryExactPrice(exactPriceReply().put("city", "Giza"), draft(), cart()))
        assertNull(shopDeliveryExactPrice(
            exactPriceReply().put("cart_fingerprint", "b".repeat(64)), draft(), cart()))
        assertNull(shopDeliveryExactPrice(
            exactPriceReply().put("final_total", "665.00"), draft(), cart()))
        assertNull(shopDeliveryExactPrice(
            exactPriceReply().put("available", false), draft(), cart()))
        assertNull(shopDeliveryExactPrice(
            exactPriceReply().put("rate_revision", 0), draft(), cart()))
    }

    @Test fun directReceiptIsOnlyRenderedAfterBackendConfirmedExactKnownTotal() {
        val price = shopDeliveryExactPrice(exactPriceReply(), draft(), cart())!!
        val direct = response()
            .put("status", "confirmed")
            .put("stock_reserved", true)
            .put("shipping_fee", "35.00")
            .put("final_total", "675.00")
        val receipt = shopDeliveryReceiptFromResponse(direct, draft(), cart(), price)
        assertNotNull(receipt)
        assertTrue(receipt!!.confirmed)
        assertEquals("675.00", receipt.finalTotal)
        assertEquals("35.00", receipt.shippingFee)
        assertNull(shopDeliveryReceiptFromResponse(direct, draft(), cart()))
        assertNull(shopDeliveryReceiptFromResponse(
            direct.put("final_total", "800.00"), draft(), cart(), price))
        assertNull(shopDeliveryReceiptFromResponse(
            direct.put("shipping_fee", "80.00").put("final_total", "675.00"),
            draft(), cart(), price))
    }
}
