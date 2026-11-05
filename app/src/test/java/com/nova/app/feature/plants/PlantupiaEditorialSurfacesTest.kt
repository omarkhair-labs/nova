package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaEditorialSurfacesTest {
    @Test fun todaySummaryOnlyCountsActuallyScheduledPersonalCheckIns() {
        assertEquals(
            "Your living space starts with a real plant.",
            todayCareBrief(activeCount = 0, dueCount = 0),
        )
        assertEquals(
            "No personal check-ins due. Your plants are here when you need them.",
            todayCareBrief(activeCount = 2, dueCount = 0),
        )
        assertEquals(
            "1 personal check-in due.",
            todayCareBrief(activeCount = 3, dueCount = 1),
        )
        assertEquals(
            "3 personal check-ins due.",
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
