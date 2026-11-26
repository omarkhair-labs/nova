package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaEditorialSurfacesTest {
    @Test fun todaySummaryOnlyCountsActuallyScheduledPersonalCheckIns() {
        assertEquals(
            "No plants yet",
            todayCareBrief(activeCount = 0, dueCount = 0),
        )
        assertEquals(
            "No check-ins due",
            todayCareBrief(activeCount = 2, dueCount = 0),
        )
        assertEquals(
            "1 check-in due",
            todayCareBrief(activeCount = 3, dueCount = 1),
        )
        assertEquals(
            "3 check-ins due",
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
