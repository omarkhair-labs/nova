package com.nova.app.feature.plants

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaCartAcknowledgementTest {
    private fun item(
        sku: String = "sku-b",
        quantity: Int = 2,
        cover: Boolean = true,
    ): JSONObject {
        val product = JSONObject()
            .put("id", sku)
            .put("product_id", "plant-one")
            .put("name", "Verified demo plant")
            .put("kind", "plant")
            .put("sku", "PLANT-B")
            .put("variant_label", "Medium")
            .put("price", "350.00")
            .put("has_image", cover)
            .put("image_description", "Approved photographed plant")
            .put("photos", JSONArray().put(JSONObject()
                .put("id", "gallery-one").put("alt_text", "Alternate real view")))
        return JSONObject()
            .put("product", product)
            .put("quantity", quantity)
            .put("line_total", "700.00")
    }

    private fun cart(vararg lines: JSONObject): JSONObject = JSONObject()
        .put("results", JSONArray().also { target -> lines.forEach { target.put(it) } })
        .put("subtotal", "950.00")

    @Test fun acknowledgesOnlyExactServerSavedSkuAndDesiredQuantity() {
        val saved = shopCartReceiptFromResponse(cart(item()), "sku-b", 2)
        assertNotNull(saved)
        assertEquals("plant-one", saved!!.productId)
        assertEquals("sku-b", saved.skuId)
        assertEquals("PLANT-B", saved.skuCode)
        assertEquals("Medium", saved.optionName)
        assertEquals("Verified demo plant", saved.productName)
        assertEquals("350.00", saved.price)
        assertEquals(2, saved.quantity)
        assertEquals("700.00", saved.lineTotal)
        assertEquals("950.00", saved.cartSubtotal)
        assertTrue(saved.hasPhoto)
        assertNull(saved.photoId) // cover is independent of gallery
    }

    @Test fun neverClaimsSuccessWhenBackendReturnedWrongSkuOrQuantity() {
        assertNull(shopCartReceiptFromResponse(cart(item()), "sku-a", 2))
        assertNull(shopCartReceiptFromResponse(cart(item()), "sku-b", 1))
        assertNull(shopCartReceiptFromResponse(cart(item()), "sku-b", 0))
        assertNull(shopCartReceiptFromResponse(cart(item()), "", 1))
        assertNull(shopCartReceiptFromResponse(cart(), "sku-b", 2))
        assertNull(shopCartReceiptFromResponse(JSONObject(), "sku-b", 2))
    }

    @Test fun galleryOnlyImageIsHonestlySourceBound() {
        val saved = shopCartReceiptFromResponse(cart(item(cover = false)), "sku-b", 2)
        assertEquals("gallery-one", saved?.photoId)
        assertEquals("Alternate real view", saved?.photoAlt)
        assertEquals(true, saved?.hasPhoto)
    }

    @Test fun savedButNoLongerAvailableCartLineRetainsActualServerWarning() {
        val row = item().put("available", false)
            .put("notice", "Item or quantity is no longer available. Please adjust your cart.")
        val saved = shopCartReceiptFromResponse(cart(row), "sku-b", 2)
        assertNotNull(saved)
        assertFalse(saved!!.lineAvailable)
        assertEquals(
            "Item or quantity is no longer available. Please adjust your cart.",
            saved.availabilityNotice,
        )
    }

    @Test fun imageAbsentDoesNotManufactureAProductPhoto() {
        val row = item(cover = false)
        row.getJSONObject("product").put("photos", JSONArray())
        val saved = shopCartReceiptFromResponse(cart(row), "sku-b", 2)
        assertNotNull(saved)
        assertFalse(saved!!.hasPhoto)
        assertNull(saved.photoId)
    }

    @Test fun malformedReturnedProductCannotBecomeAnAcknowledgement() {
        val row = item()
        row.put("product", JSONObject().put("id", "sku-b"))
        assertNull(shopCartReceiptFromResponse(cart(row), "sku-b", 2))
        row.put("product", JSONObject().put("id", "sku-b").put("name", "Name only"))
        assertNull(shopCartReceiptFromResponse(cart(row), "sku-b", 2))
    }
}
