package com.nova.app.feature.plants.data

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaDaylightTest {
    @Test fun samplesTheCivilDayAcrossDifferentUtcOffsets() {
        val zone = ZoneId.of("Europe/London")
        val spring = daylightSampleTimes(LocalDate.of(2026, 3, 29), zone)
        val autumn = daylightSampleTimes(LocalDate.of(2026, 10, 25), zone)
        assertEquals(46, spring.size)
        assertEquals(50, autumn.size)
        assertEquals(2, autumn.filter { it.hour == 1 && it.minute == 30 }.map { it.offset }.distinct().size)
        assertTrue(autumn.zipWithNext().all { (a, b) -> a.isBefore(b) })
    }

    @Test fun anotherPointCanBeTestedAgainstTheSameFootprint() {
        val corners = listOf(1.0 to 0.2, 2.0 to 0.2, 2.0 to 1.5, 1.0 to 1.5)
        assertTrue(inDirectSunFootprint(1.2 to 0.5, corners))
        assertTrue(inDirectSunFootprint(1.2 to 0.5, corners.reversed()))
        assertTrue(inDirectSunFootprint(2.0 to 1.1, corners))
        assertFalse(inDirectSunFootprint(2.1 to 1.1, corners))
        assertFalse(inDirectSunFootprint(1.2 to 0.5, emptyList()))
        assertFalse(inDirectSunFootprint(1.2 to 0.2, listOf(corners[0], corners[1], 3.0 to 0.2)))
    }
}
