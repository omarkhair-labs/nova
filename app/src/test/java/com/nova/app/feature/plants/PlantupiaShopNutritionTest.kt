package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaShopNutritionTest {
    @Test fun nutritionIsInsideCareButIsNotAnotherNavigationRoot() {
        assertTrue(matchesShopCategory("accessory", "nutrition", "all", "all"))
        assertTrue(matchesShopCategory("accessory", "nutrition", "accessory", "all"))
        assertTrue(matchesShopCategory("accessory", "nutrition", "accessory", "nutrition"))
        assertFalse(matchesShopCategory("accessory", "tools", "accessory", "nutrition"))
        assertFalse(matchesShopCategory("plant", "", "accessory", "nutrition"))
        assertTrue(matchesShopCategory("plant", "", "plant", "all"))
    }

    @Test fun existingUncategorizedAccessoryRemainsVisibleToAllCare() {
        assertTrue(matchesShopCategory("accessory", "", "accessory", "all"))
        assertFalse(matchesShopCategory("accessory", "", "accessory", "nutrition"))
        assertTrue(matchesShopCategory("planter", "", "all", "all"))
    }
}
