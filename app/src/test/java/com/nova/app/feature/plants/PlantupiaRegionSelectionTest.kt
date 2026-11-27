package com.nova.app.feature.plants

import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlantupiaRegionSelectionTest {
    private fun rect(id: Long, start: Int, end: Int): FieldRegion =
        FieldRegion(id, "leaf", (end - start) * (end - start),
            listOf(IntOffset(start, start), IntOffset(end, start), IntOffset(end, end), IntOffset(start, end)),
            null, emptyList())

    @Test fun tapSelectsExistingSmallestOverlappingTrace() {
        val outer = rect(1, 0, 100)
        val inner = rect(2, 20, 50)
        assertEquals(inner, fieldRegionAtPoint(listOf(outer, inner), IntOffset(35, 35)))
        assertEquals(outer, fieldRegionAtPoint(listOf(outer, inner), IntOffset(75, 75)))
        assertNull(fieldRegionAtPoint(listOf(outer, inner), IntOffset(120, 120)))
        assertNull(fieldRegionAtPoint(emptyList(), IntOffset(35, 35)))
    }
}
