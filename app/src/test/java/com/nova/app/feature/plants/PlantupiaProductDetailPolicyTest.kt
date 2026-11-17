package com.nova.app.feature.plants

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaProductDetailPolicyTest {
    private fun sku(id: String, inStock: Boolean, units: Int) =
        JSONObject().put("id", id).put("in_stock", inStock).put("available_units", units)

    @Test fun photoListHasCoverAndUniqueGalleryEntries() {
        val product = JSONObject()
            .put("has_image", true).put("image_description", "Approved cover")
            .put("photos", JSONArray()
                .put(JSONObject().put("id", "a").put("alt_text", "Side view"))
                .put(JSONObject().put("id", "a").put("alt_text", "Duplicate"))
                .put(JSONObject().put("id", "").put("alt_text", "Unusable"))
                .put(JSONObject().put("id", "b").put("alt_text", "Close detail")))
        assertEquals(
            listOf(ShopProductPhoto(null, "Approved cover"),
                ShopProductPhoto("a", "Side view"), ShopProductPhoto("b", "Close detail")),
            shopProductPhotos(product),
        )
        assertTrue(shopProductPhotos(JSONObject().put("has_image", false)).isEmpty())
    }

    @Test fun galleryOnlyPhotoNeedsNoCoverAndNeverGuessesVariantMedia() {
        val product = JSONObject()
            .put("has_image", false)
            .put("photos", JSONArray().put(JSONObject().put("id", "one").put("alt_text", "Actual detail")))
        assertEquals(listOf(ShopProductPhoto("one", "Actual detail")), shopProductPhotos(product))
    }

    @Test fun selectionPreservesExactSkuOrFallsBackOnlyToRealAvailableChoice() {
        val variants = listOf(sku("a", false, 0), sku("b", true, 3), sku("c", false, 0))
        assertEquals("a", shopSelectVariant(variants, "a")?.optString("id"))
        assertEquals("b", shopSelectVariant(variants, "removed")?.optString("id"))
        assertEquals("a", shopSelectVariant(listOf(variants[0], variants[2]), null)?.optString("id"))
        assertNull(shopSelectVariant(emptyList(), "b"))
    }

    @Test fun addButtonRequiresServerCartStateAndAvailableQuantity() {
        assertFalse(shopDetailCanAdd(false, false, 5, true, 0))
        assertFalse(shopDetailCanAdd(true, true, 5, true, 0))
        assertFalse(shopDetailCanAdd(true, false, 0, false, 0))
        assertFalse(shopDetailCanAdd(true, false, 1, true, 1))
        assertFalse(shopDetailCanAdd(true, false, 20, true, 10))
        assertTrue(shopDetailCanAdd(true, false, 2, true, 1))
    }

    @Test fun swipeStopsAtBoundariesAndIgnoresSmallJitters() {
        assertEquals(0, shopPhotoIndexAfterSwipe(0, 4, -8f))
        assertEquals(1, shopPhotoIndexAfterSwipe(0, 4, -88f))
        assertEquals(0, shopPhotoIndexAfterSwipe(0, 4, 88f))
        assertEquals(3, shopPhotoIndexAfterSwipe(3, 4, -88f))
        assertEquals(1, shopPhotoIndexAfterSwipe(2, 4, 88f))
        assertEquals(0, shopPhotoIndexAfterSwipe(0, 0, -100f))
    }

    @Test fun availabilityCopyIsFactual() {
        assertEquals("Currently unavailable", shopStockDescription(0, false))
        assertEquals("Currently unavailable", shopStockDescription(2, false))
        assertEquals("1 available", shopStockDescription(1, true))
        assertEquals("5 available", shopStockDescription(5, true))
    }
}
