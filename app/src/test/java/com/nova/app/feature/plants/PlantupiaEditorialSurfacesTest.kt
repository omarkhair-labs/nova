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
            "See what's next, or record a moment of care.",
            todayCareBrief(activeCount = 2, dueCount = 0),
        )
        assertEquals(
            "One personal check-in ready to review.",
            todayCareBrief(activeCount = 3, dueCount = 1),
        )
        assertEquals(
            "3 personal check-ins ready to review.",
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
