package com.nova.app.feature.plants

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PlantupiaCollectionViewTest {
    private val today = LocalDate.of(2026, 11, 26)
    @Test fun onlyExplicitValidDatesCountAsDue() {
        assertTrue(plantupiaCollectionIncludes("due", "2026-11-26", "", today))
        assertTrue(plantupiaCollectionIncludes("due", "2026-11-25", "", today))
        assertFalse(plantupiaCollectionIncludes("due", "2026-11-27", "", today))
        assertFalse(plantupiaCollectionIncludes("due", "", "", today))
        assertFalse(plantupiaCollectionIncludes("due", "invalid", "", today))
    }
    @Test fun portraitFilterIsIndependentOfCareStatus() {
        assertTrue(plantupiaCollectionIncludes("no_photo", "", " ", today))
        assertFalse(plantupiaCollectionIncludes("no_photo", "", "https://qa.invalid/portrait", today))
        assertTrue(plantupiaCollectionIncludes("all", "bad", "", today))
    }
}
