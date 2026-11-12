package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PlantupiaCareCalendarModelTest {
    @Test fun monthGridStartsOnMondayAndContainsLeapDay() {
        val days = careCalendarMonthDays(YearMonth.of(2028, 2))
        assertEquals("2028-01-31", days.first().toString())
        assertTrue(LocalDate.parse("2028-02-29") in days)
        assertEquals(35, days.size)
    }

    @Test fun sixWeekMonthsAreNotTruncated() {
        val days = careCalendarMonthDays(YearMonth.of(2026, 8))
        assertEquals(42, days.size)
        assertEquals("2026-07-27", days.first().toString())
        assertEquals("2026-09-06", days.last().toString())
    }

    @Test fun weekStartsOnMondayAndKeepsCalendarDays() {
        val days = careCalendarWeekDays(LocalDate.parse("2026-11-11"))
        assertEquals("2026-11-09", days.first().toString())
        assertEquals("2026-11-15", days.last().toString())
    }

    @Test fun emptyInvalidAndMissingDatesNeverInventAnEntry() {
        assertNull(parseCareCalendarDate(""))
        assertNull(parseCareCalendarDate("null"))
        assertNull(parseCareCalendarDate("2026-02-29"))
        assertNull(parseCareCalendarDate(null))
    }

    @Test fun mergeRespectsDistinctPlantsAndDoesNotMultiplyRetries() {
        val date = LocalDate.parse("2026-11-12")
        val first = CareCalendarEntry(date, "plant-a", "Monstera", "Check leaves", "Reviewed plan")
        val second = CareCalendarEntry(date, "plant-b", "Monstera", "Check leaves", "Reviewed plan")
        val merged = careCalendarEntries(listOf(first, first), listOf(second))
        assertEquals(2, merged.size)
        assertEquals(setOf("plant-a", "plant-b"), merged.map { it.plantId }.toSet())
    }
}
