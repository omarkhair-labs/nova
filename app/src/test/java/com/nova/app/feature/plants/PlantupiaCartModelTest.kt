package com.nova.app.feature.plants

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaCartModelTest {
    private fun sku(
        id: String = "sku-1",
        withCover: Boolean = true,
        withGallery: Boolean = true,
        stock: Int = 5,
    ) = JSONObject()
        .put("id", id)
        .put("product_id", "object-1")
        .put("name", "FICTIONAL QA vessel")
        .put("kind", "planter")
        .put("variant_label", "Large · sand")
        .put("sku", "QA-VESSEL-L")
        .put("price", "245.00")
        .put("available_units", stock)
        .put("in_stock", stock > 0)
        .put("has_image", withCover)
        .put("image_description", "Uploaded product photo")
        .put("photos", if (withGallery) JSONArray()
            .put(JSONObject().put("id", "gallery-1").put("alt_text", "Actual side view"))
            else JSONArray())

    private fun row(
        id: String = "sku-1",
        quantity: Int = 2,
        available: Boolean = true,
        image: Boolean = true,
        gallery: Boolean = true,
        stock: Int = 5,
    ) = JSONObject()
        .put("product", sku(id, image, gallery, stock))
        .put("quantity", quantity)
        .put("available", available)
        .put("notice", if (available) "" else "Stock review required")
        .put("line_total", "490.00")

    private fun cart(vararg rows: JSONObject) = JSONObject()
        .put("results", JSONArray().also { array -> rows.forEach { array.put(it) } })
        .put("subtotal", "490.00")
        .put("can_request_shipping_quote", rows.isNotEmpty())
        .put("currency", "EGP")

    @Test fun cartItemCarriesExactVariantPriceQuantityAndProductImage() {
        val actual = shopCartObjects(cart(row())).single()
        assertEquals("sku-1", actual.skuId)
        assertEquals("object-1", actual.productId)
        assertEquals("FICTIONAL QA vessel", actual.name)
        assertEquals("Large · sand", actual.variant)
        assertEquals("QA-VESSEL-L", actual.skuCode)
        assertEquals(2, actual.quantity)
        assertEquals("245.00", actual.unitPrice)
        assertEquals("490.00", actual.lineTotal)
        assertEquals(true, actual.lineAvailable)
        assertEquals(ShopProductPhoto(null, "Uploaded product photo"), actual.photo)
    }

    @Test fun usesUploadedGalleryWhenCoverAbsentAndNeverFabricatesImage() {
        assertEquals(
            ShopProductPhoto("gallery-1", "Actual side view"),
            shopCartObjects(cart(row(image = false))).single().photo,
        )
        assertNull(shopCartObjects(cart(row(image = false, gallery = false))).single().photo)
    }

    @Test fun persistedMutationMustConfirmExactQuantityOrActualRemoval() {
        val current = cart(row())
        assertTrue(shopCartMutationConfirmed(current, "sku-1", 2))
        assertFalse(shopCartMutationConfirmed(current, "sku-1", 1))
        assertFalse(shopCartMutationConfirmed(current, "sku-1", 0))
        assertTrue(shopCartMutationConfirmed(cart(), "sku-1", 0))
        assertFalse(shopCartMutationConfirmed(cart(), "sku-1", 1))
        assertFalse(shopCartMutationConfirmed(JSONObject(), "sku-1", 0))
        assertFalse(shopCartMutationConfirmed(current, "", 2))
        assertFalse(shopCartMutationConfirmed(current, "sku-1", 11))
    }

    @Test fun duplicatedSkuCannotBeMistakenForConfirmedSavedState() {
        assertFalse(shopCartMutationConfirmed(cart(row(), row()), "sku-1", 2))
    }

    @Test fun increaseStopsAtStockAndTenUnitCap() {
        val available = shopCartObjects(cart(row(quantity = 2, stock = 5))).single()
        assertTrue(shopCartCanIncrease(available))
        assertFalse(shopCartCanIncrease(shopCartObjects(cart(row(quantity = 5, stock = 5))).single()))
        assertFalse(shopCartCanIncrease(shopCartObjects(cart(row(quantity = 10, stock = 20))).single()))
        val invalid = shopCartObjects(cart(row(quantity = 2, available = false))).single()
        assertFalse(shopCartCanIncrease(invalid))
        assertEquals("Stock review required", invalid.notice)
    }

    @Test fun removalCandidateCanOnlyComeFromExistingExactSku() {
        val actual = shopCartRemovalCandidate(cart(row()), "sku-1")
        assertEquals(ShopRemovedCartObject("sku-1", 2, "FICTIONAL QA vessel"), actual)
        assertNull(shopCartRemovalCandidate(cart(row()), "missing"))
        assertNull(shopCartRemovalCandidate(cart(row(quantity = 0)), "sku-1"))
    }

    @Test fun cartWithMalformedRowsNeverInventsAProductForDisplay() {
        assertTrue(shopCartObjects(cart(JSONObject().put("quantity", 1))).isEmpty())
    }
}
