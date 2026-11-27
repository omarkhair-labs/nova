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

internal fun shopDeliveryReady(draft: ShopDeliveryDraft, cart: JSONObject?): Boolean {
    if (cart == null || shopDeliveryFieldIssues(draft).isNotEmpty()) return false
    if (!cart.optBoolean("can_request_shipping_quote")) return false
    if (!cart.optString("fingerprint").matches(Regex("[a-f0-9]{64}"))) return false
    val count = cart.optJSONArray("results")?.length() ?: return false
    return count in 1..20 && shopCartObjects(cart).size == count
}

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

/** Authoritative city fee and exact total, sourced from the authenticated backend. */
internal data class ShopDeliveryExactPrice(
    val city: String,
    val cartFingerprint: String,
    val rateRevision: Int,
    val shippingFee: String,
    val total: String,
)

internal fun shopDeliveryExactPrice(
    response: JSONObject, draft: ShopDeliveryDraft, cart: JSONObject?,
): ShopDeliveryExactPrice? {
    if (cart == null || !response.optBoolean("available")) return null
    if (response.optString("city").trim() != draft.city.trim() ||
        response.optString("cart_fingerprint") != cart.optString("fingerprint") ||
        response.optString("currency") != "EGP" ||
        response.optInt("rate_revision", 0) < 1) return null
    val subtotal = response.optString("subtotal").toBigDecimalOrNull() ?: return null
    val expectedSubtotal = cart.optString("subtotal").toBigDecimalOrNull() ?: return null
    val fee = response.optString("shipping_fee").toBigDecimalOrNull() ?: return null
    val total = response.optString("final_total").toBigDecimalOrNull() ?: return null
    if (subtotal.compareTo(expectedSubtotal) != 0 ||
        fee.signum() < 0 || total.compareTo(subtotal + fee) != 0) return null
    return ShopDeliveryExactPrice(
        city = draft.city.trim(),
        cartFingerprint = cart.optString("fingerprint"),
        rateRevision = response.optInt("rate_revision"),
        shippingFee = response.optString("shipping_fee"),
        total = response.optString("final_total"),
    )
}

/** A receipt describes the saved customer request, never a charge or stock reservation. */
internal data class ShopDeliveryReceiptLine(
    val name: String,
    val variant: String,
    val quantity: Int,
    val subtotal: String,
)

internal data class ShopDeliveryQuoteReceipt(
    val reference: String,
    val productSubtotal: String,
    val recipient: String,
    val city: String,
    val paymentPreference: String,
    val lines: List<ShopDeliveryReceiptLine> = emptyList(),
    val confirmed: Boolean = false,
    val shippingFee: String? = null,
    val finalTotal: String? = null,
)

internal fun shopDeliveryReceiptFromResponse(
    response: JSONObject,
    draft: ShopDeliveryDraft,
    requestedCart: JSONObject,
    confirmedPrice: ShopDeliveryExactPrice? = null,
): ShopDeliveryQuoteReceipt? {
    val reference = response.optString("id")
    if (runCatching { UUID.fromString(reference) }.getOrNull() == null) return null
    val confirmed = response.optString("status") == "confirmed"
    if (!confirmed && response.optString("status") != "quote_pending") return null
    if (!response.has("stock_reserved") || !response.has("charged") ||
        !response.has("shipping_fee")) return null
    if (response.optBoolean("charged")) return null
    if (confirmed) {
        val price = confirmedPrice ?: return null
        if (!response.optBoolean("stock_reserved") ||
            response.optString("shipping_fee") != price.shippingFee ||
            response.optString("final_total") != price.total ||
            price.cartFingerprint != requestedCart.optString("fingerprint") ||
            price.city != draft.city.trim()) return null
    } else if (response.optBoolean("stock_reserved") || !response.isNull("shipping_fee")) {
        return null
    }
    val customer = response.optJSONObject("customer") ?: return null
    val subtotal = response.optString("subtotal")
    val recipient = customer.optString("name").trim()
    val city = customer.optString("city").trim()
    if (subtotal.isBlank() || recipient.isBlank() || city.isBlank()) return null
    // The acknowledgement must match the exact customer's submission, not
    // merely any quote-pending record returned from a broken endpoint.
    if (subtotal != requestedCart.optString("subtotal")) return null
    if (recipient != draft.recipient.trim() || city != draft.city.trim()) return null
    if (customer.optString("phone") != normalizedDeliveryPhone(draft.phone)) return null
    if (customer.optString("address") != draft.street.trim()) return null
    if (!customer.optBoolean("pin_confirmed")) return null
    val acknowledgedLatitude = customer.optString("latitude").toDoubleOrNull() ?: return null
    val acknowledgedLongitude = customer.optString("longitude").toDoubleOrNull() ?: return null
    if (draft.latitude == null || draft.longitude == null ||
        kotlin.math.abs(acknowledgedLatitude - draft.latitude) > 0.000001 ||
        kotlin.math.abs(acknowledgedLongitude - draft.longitude) > 0.000001) return null
    if (response.optString("payment_preference") != draft.paymentPreference) return null

    // The receipt is based on confirmed server order-line snapshots, not
    // an optimistic copy of what happened to be visible in the local cart.
    val requested = shopCartObjects(requestedCart)
    val saved = response.optJSONArray("lines") ?: return null
    if (requested.isEmpty() || saved.length() != requested.size ||
        requested.map { it.skuId }.toSet().size != requested.size) return null
    val bySku = requested.associateBy { it.skuId }
    val seen = mutableSetOf<String>()
    val lines = (0 until saved.length()).map { index ->
        val line = saved.optJSONObject(index) ?: return null
        val skuId = line.optString("sku_id")
        val original = bySku[skuId] ?: return null
        if (!seen.add(skuId) || line.optInt("quantity", -1) != original.quantity ||
            line.optString("subtotal") != original.lineTotal ||
            line.optString("unit_price") != original.unitPrice) return null
        val name = line.optString("name").trim()
        if (name.isBlank()) return null
        ShopDeliveryReceiptLine(
            name = name,
            variant = line.optString("variant").trim(),
            quantity = original.quantity,
            subtotal = line.optString("subtotal"),
        )
    }
    return ShopDeliveryQuoteReceipt(
        reference = reference,
        productSubtotal = subtotal,
        recipient = recipient,
        city = city,
        paymentPreference = response.optString("payment_preference"),
        lines = lines,
        confirmed = confirmed,
        shippingFee = if (confirmed) response.optString("shipping_fee") else null,
        finalTotal = if (confirmed) response.optString("final_total") else null,
    )
}
