package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaEditorialSurfacesTest {
    @Test fun todaySummaryOnlyCountsActuallyScheduledPersonalCheckIns() {
        assertEquals(
            "Start with a real plant. Its record begins here.",
            todayCareBrief(activeCount = 0, dueCount = 0),
        )
        assertEquals(
            "A place for the small moments you actually notice.",
            todayCareBrief(activeCount = 2, dueCount = 0),
        )
        assertEquals(
            "Your next check is ready to open below.",
            todayCareBrief(activeCount = 3, dueCount = 1),
        )
        assertEquals(
            "Your next check is ready to open below.",
            todayCareBrief(activeCount = 3, dueCount = 3),
        )
    }

    @Test fun shopShelfMatchesCatalogApiKindsWithoutChangingProductIdentity() {
        assertEquals(
            listOf("all", "plant", "planter", "accessory"),
            plantupiaShopShelfCategories.map { it.key },
        )
        assertEquals(4, plantupiaShopShelfCategories.map { it.label }.toSet().size)
        assertEquals(4, plantupiaShopShelfCategories.map { it.tint }.toSet().size)
        assertEquals("Care", plantupiaShopShelfCategories.last().label)
    }
}
