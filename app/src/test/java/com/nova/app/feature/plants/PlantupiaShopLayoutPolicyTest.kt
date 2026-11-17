package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Test

class PlantupiaShopLayoutPolicyTest {
    @Test fun narrowerPhonesAndLargeTextNeverForceTinyTwoUpProductCards() {
        assertEquals(1, plantupiaShopProductColumns(279f, 1f))
        assertEquals(1, plantupiaShopProductColumns(329f, 1f))
        assertEquals(2, plantupiaShopProductColumns(330f, 1f))
        assertEquals(1, plantupiaShopProductColumns(460f, 1.3f))
        assertEquals(2, plantupiaShopProductColumns(460f, 1.2f))
    }

    @Test fun commercialCategoryRailCountsProductFamiliesNotVariants() {
        val kinds = listOf("plant", "plant", "planter", "care")
        assertEquals(4, shopDiscoveryCategoryCount(kinds, "all"))
        assertEquals(2, shopDiscoveryCategoryCount(kinds, "plant"))
        assertEquals(1, shopDiscoveryCategoryCount(kinds, "planter"))
        assertEquals(1, shopDiscoveryCategoryCount(kinds, "care"))
        assertEquals(0, shopDiscoveryCategoryCount(kinds, "accessory"))
    }

    @Test fun cartLabelDoesNotPretendAnUnavailableCartIsEmpty() {
        assertEquals("Cart", shopCartButtonLabel(null))
        assertEquals("Cart · 0", shopCartButtonLabel(0))
        assertEquals("Cart · 3", shopCartButtonLabel(3))
    }

    @Test fun shopHeaderRespectsNarrowPhonesAndLargeText() {
        assertEquals(true, plantupiaShopNavigationStacked(280f, 1f))
        assertEquals(true, plantupiaShopNavigationStacked(329f, 1f))
        assertEquals(false, plantupiaShopNavigationStacked(330f, 1f))
        assertEquals(false, plantupiaShopNavigationStacked(390f, 1.2f))
        assertEquals(true, plantupiaShopNavigationStacked(390f, 2f))
    }

    @Test fun browsingIsIncrementalButEveryRealFamilyIsReachable() {
        assertEquals(0, plantupiaVisibleFamilies(0, 8))
        assertEquals(5, plantupiaVisibleFamilies(5, 8))
        assertEquals(8, plantupiaVisibleFamilies(100, 0))
        assertEquals(16, plantupiaVisibleFamilies(100, 16))
        assertEquals(100, plantupiaVisibleFamilies(100, 112))
    }
}
