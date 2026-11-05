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

    @Test fun browsingIsIncrementalButEveryRealFamilyIsReachable() {
        assertEquals(0, plantupiaVisibleFamilies(0, 8))
        assertEquals(5, plantupiaVisibleFamilies(5, 8))
        assertEquals(8, plantupiaVisibleFamilies(100, 0))
        assertEquals(16, plantupiaVisibleFamilies(100, 16))
        assertEquals(100, plantupiaVisibleFamilies(100, 112))
    }
}
