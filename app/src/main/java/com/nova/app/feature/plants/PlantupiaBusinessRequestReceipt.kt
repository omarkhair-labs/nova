package com.nova.app.feature.plants

import org.json.JSONObject
import java.util.UUID

/** A real B2B acknowledgement, never an order, installation or payment receipt. */
internal data class PlantupiaBusinessRequestReceipt(
    val reference: String,
    val kind: String,
    val organization: String,
    val city: String,
    val quantity: Int,
    val scope: String,
)

internal fun verifiedPlantupiaBusinessReceipt(
    response: JSONObject,
    kind: String,
    organization: String,
    contact: String,
    phone: String,
    city: String,
    quantity: Int,
    scope: String,
): PlantupiaBusinessRequestReceipt? {
    val id = response.optString("id")
    val identifier = runCatching { UUID.fromString(id) }.getOrNull() ?: return null
    if (!identifier.toString().equals(id, ignoreCase = true)) return null
    if (response.optString("status") != "received" ||
        response.optString("kind") != kind ||
        response.optString("company_name").trim() != organization.trim() ||
        response.optString("contact_name").trim() != contact.trim() ||
        response.optString("phone").trim() != phone.trim() ||
        response.optString("city").trim() != city.trim() ||
        response.optInt("quantity", -1) != quantity ||
        response.optString("scope").trim() != scope.trim() ||
        !response.isNull("quoted_amount")
    ) return null
    return PlantupiaBusinessRequestReceipt(
        reference = id,
        kind = kind,
        organization = organization.trim(),
        city = city.trim(),
        quantity = quantity,
        scope = scope.trim(),
    )
}
