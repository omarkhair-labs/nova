package com.nova.app.feature.plants

import org.json.JSONObject

/**
 * The commercial API defines product-level media. We never pretend it is a
 * selected-SKU-specific photograph without a real media/variant association.
 */
internal data class ShopProductPhoto(val id: String?, val description: String)

internal fun shopProductPhotos(product: JSONObject): List<ShopProductPhoto> {
    val photos = mutableListOf<ShopProductPhoto>()
    if (product.optBoolean("has_image")) {
        photos += ShopProductPhoto(null, product.optString("image_description"))
    }
    val gallery = product.optJSONArray("photos") ?: return photos
    val used = mutableSetOf<String>()
    for (index in 0 until gallery.length()) {
        val item = gallery.optJSONObject(index) ?: continue
        val id = item.optString("id").trim()
        if (id.isNotEmpty() && used.add(id)) {
            photos += ShopProductPhoto(id, item.optString("alt_text"))
        }
    }
    return photos
}

internal fun shopSelectVariant(variants: List<JSONObject>, selectedId: String?): JSONObject? =
    variants.firstOrNull { it.optString("id") == selectedId }
        ?: variants.firstOrNull { it.optBoolean("in_stock") && it.optInt("available_units") > 0 }
        ?: variants.firstOrNull()

internal fun shopDetailCanAdd(
    cartLoaded: Boolean, busy: Boolean, available: Int,
    inStock: Boolean, inCart: Int,
): Boolean = cartLoaded && !busy && inStock && available > inCart && inCart < 10

internal fun shopStockDescription(available: Int, inStock: Boolean): String = when {
    !inStock || available <= 0 -> "Currently unavailable"
    available == 1 -> "1 available"
    else -> "$available available"
}

internal fun shopPhotoIndexAfterSwipe(index: Int, count: Int, deltaX: Float): Int =
    if (count <= 1) 0 else if (kotlin.math.abs(deltaX) < 42f) index.coerceIn(0, count - 1)
    else (index + if (deltaX < 0) 1 else -1).coerceIn(0, count - 1)
