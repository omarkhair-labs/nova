package com.nova.app.feature.plants

import org.json.JSONObject

/**
 * A cart is a saved collection of *exact* commercial SKU choices. These are
 * not reserved stock, confirmed orders or payments.
 */
internal data class ShopCartObject(
    val skuId: String,
    val productId: String,
    val name: String,
    val kind: String,
    val variant: String,
    val skuCode: String,
    val quantity: Int,
    val unitPrice: String,
    val lineTotal: String,
    val availableUnits: Int,
    val lineAvailable: Boolean,
    val notice: String,
    val photo: ShopProductPhoto?,
)

internal data class ShopRemovedCartObject(
    val skuId: String,
    val previousQuantity: Int,
    val name: String,
)

internal fun shopCartObjects(cart: JSONObject?): List<ShopCartObject> {
    val lines = cart?.optJSONArray("results") ?: return emptyList()
    return (0 until lines.length()).mapNotNull { position ->
        val line = lines.optJSONObject(position) ?: return@mapNotNull null
        val sku = line.optJSONObject("product") ?: return@mapNotNull null
        val skuId = sku.optString("id").trim()
        val productId = sku.optString("product_id").trim()
        if (skuId.isBlank() || productId.isBlank()) return@mapNotNull null
        ShopCartObject(
            skuId = skuId,
            productId = productId,
            name = sku.optString("name"),
            kind = sku.optString("kind"),
            variant = storeVariantName(sku),
            skuCode = sku.optString("sku"),
            quantity = line.optInt("quantity").coerceAtLeast(0),
            unitPrice = sku.optString("price"),
            lineTotal = line.optString("line_total"),
            availableUnits = sku.optInt("available_units").coerceAtLeast(0),
            lineAvailable = line.optBoolean("available", false),
            notice = line.optString("notice"),
            photo = shopProductPhotos(sku).firstOrNull(),
        )
    }
}

/** A mutation response must explicitly reflect the desired persisted state. */
internal fun shopCartMutationConfirmed(cart: JSONObject, skuId: String, desired: Int): Boolean {
    if (skuId.isBlank() || desired !in 0..10) return false
    val rows = cart.optJSONArray("results") ?: return false
    val matches = (0 until rows.length()).mapNotNull { rowIndex ->
        rows.optJSONObject(rowIndex)?.takeIf {
            it.optJSONObject("product")?.optString("id") == skuId
        }
    }
    return if (desired == 0) matches.isEmpty()
    else matches.size == 1 && matches.single().optInt("quantity", -1) == desired
}

internal fun shopCartCanIncrease(item: ShopCartObject): Boolean =
    item.lineAvailable && item.quantity in 1..9 && item.quantity < item.availableUnits

internal fun shopCartRemovalCandidate(
    cart: JSONObject?,
    skuId: String,
): ShopRemovedCartObject? =
    shopCartObjects(cart).firstOrNull { it.skuId == skuId && it.quantity > 0 }?.let {
        ShopRemovedCartObject(it.skuId, it.quantity, it.name)
    }
