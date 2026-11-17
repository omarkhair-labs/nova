package com.nova.app.feature.plants

import org.json.JSONObject
import java.util.UUID

/**
 * The first action in this flow is a quote request, not a confirmed purchase.
 * Keep the existing backend's shipping/stock and idempotency contract intact.
 */
internal data class ShopDeliveryDraft(
    val recipient: String = "",
    val phone: String = "",
    val city: String = "",
    val street: String = "",
    val building: String = "",
    val landmark: String = "",
    val note: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pinConfirmed: Boolean = false,
    val paymentPreference: String = "cod",
)

internal fun shopDeliveryFieldIssues(draft: ShopDeliveryDraft): List<String> = buildList {
    if (draft.recipient.trim().length < 2) add("Recipient name")
    val phone = normalizedDeliveryPhone(draft.phone)
    if (!Regex("[+0-9 ()-]{7,32}").matches(phone) ||
        phone.count(Char::isDigit) < 7) add("Phone number")
    if (draft.city.trim().length < 2) add("City / governorate")
    if (draft.street.trim().length < 5) add("Street / address")
    if (!draft.pinConfirmed || draft.latitude == null || draft.longitude == null ||
        !validDeliveryPin(draft.latitude, draft.longitude)) add("Confirmed map pin")
    if (draft.paymentPreference !in setOf("cod", "manual_transfer")) add("Payment preference")
}

internal fun shopDeliveryReady(draft: ShopDeliveryDraft, cart: JSONObject?): Boolean =
    shopDeliveryFieldIssues(draft).isEmpty() &&
        cart?.optBoolean("can_request_shipping_quote") == true &&
        cart.optString("fingerprint").matches(Regex("[a-f0-9]{64}")) &&
        cart.optJSONArray("results")?.length()?.let {
            it in 1..20 && shopCartObjects(cart).size == it
        } == true

internal fun shopDeliveryQuotePayload(draft: ShopDeliveryDraft, cart: JSONObject): JSONObject =
    JSONObject()
        .put("cart_fingerprint", cart.optString("fingerprint"))
        .put("contact_name", draft.recipient.trim())
        .put("contact_phone", normalizedDeliveryPhone(draft.phone))
        .put("city", draft.city.trim())
        .put("delivery_address", draft.street.trim())
        .put("building_details", draft.building.trim())
        .put("landmark", draft.landmark.trim())
        .put("customer_note", draft.note.trim())
        .put("delivery_latitude", draft.latitude)
        .put("delivery_longitude", draft.longitude)
        .put("delivery_pin_confirmed", draft.pinConfirmed)
        .put("payment_preference", draft.paymentPreference)

/** Positive ACK is an actual quote-request record; no stock hold/payment. */
internal data class ShopDeliveryQuoteReceipt(
    val reference: String,
    val productSubtotal: String,
    val recipient: String,
    val city: String,
    val paymentPreference: String,
)

internal fun shopDeliveryReceiptFromResponse(response: JSONObject): ShopDeliveryQuoteReceipt? {
    val reference = response.optString("id")
    if (runCatching { UUID.fromString(reference) }.getOrNull() == null) return null
    if (response.optString("status") != "quote_pending") return null
    if (response.optBoolean("stock_reserved") || response.optBoolean("charged")) return null
    if (response.has("shipping_fee") && !response.isNull("shipping_fee")) return null
    val customer = response.optJSONObject("customer") ?: return null
    val subtotal = response.optString("subtotal")
    val recipient = customer.optString("name").trim()
    val city = customer.optString("city").trim()
    if (subtotal.isBlank() || recipient.isBlank() || city.isBlank()) return null
    return ShopDeliveryQuoteReceipt(
        reference = reference,
        productSubtotal = subtotal,
        recipient = recipient,
        city = city,
        paymentPreference = response.optString("payment_preference"),
    )
}
