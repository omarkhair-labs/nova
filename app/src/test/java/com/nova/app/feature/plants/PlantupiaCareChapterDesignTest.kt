package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaCareChapterDesignTest {
    @Test fun reviewedTopicsHaveDistinctEditorialVisualIdentities() {
        val topics = listOf(
            "water", "light", "soil", "humidity", "fertilizing",
            "pruning", "repotting", "inspection", "warnings",
        )
        val palette = topics.map(::careChapterPalette)
        assertEquals(9, palette.map { it.label }.toSet().size)
        assertEquals(9, palette.map { it.background }.toSet().size)
        assertEquals(9, palette.map { it.ink }.toSet().size)
        assertEquals("Nutrition", careChapterPalette("fertilizing").label)
        assertEquals("Cautions", careChapterPalette("warnings").label)
        // Empty/unknown from an older backend gets a truthful neutral treatment.
        assertEquals("Care", careChapterPalette("other").label)
    }
}
