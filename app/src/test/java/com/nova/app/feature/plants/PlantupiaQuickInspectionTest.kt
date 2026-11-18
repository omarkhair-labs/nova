package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaQuickInspectionTest {
    @Test fun eachApprovedInspectionOffersOnlyMatchingObservedOutcomes() {
        assertEquals(listOf("dry", "moist", "unsure"), inspectionFindingChoices("soil").map { it.first })
        assertEquals(listOf("leaves_ok", "leaves_changed", "unsure"),
            inspectionFindingChoices("leaves").map { it.first })
        assertEquals(listOf("drainage_ok", "drainage_concern", "unsure"),
            inspectionFindingChoices("drainage").map { it.first })
        assertEquals(listOf("light_ok", "light_changed", "unsure"),
            inspectionFindingChoices("rotation").map { it.first })
        assertTrue(inspectionFindingChoices("fertilize").isEmpty())
        assertFalse(inspectionFindingChoices("soil").any { it.second.contains("Watered") })
        assertFalse(inspectionFindingChoices("leaves").any { it.second.contains("Fertilized") })
    }
}
