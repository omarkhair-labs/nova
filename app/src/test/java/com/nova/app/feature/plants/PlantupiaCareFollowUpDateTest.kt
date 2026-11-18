package com.nova.app.feature.plants

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaCareFollowUpDateTest {
    private val today = LocalDate.of(2026, 10, 13)

    @Test fun aCareObservationCanBeSavedWithoutAReviewDate() {
        assertTrue(isValidCheckInDate(null, today))
        assertNull(selectedFollowUpDate(null, null, today))
    }

    @Test fun sevenAndFourteenDayPresetsAreHumanChosen() {
        assertEquals("2026-10-20", selectedFollowUpDate(null, 7, today))
        assertEquals("2026-10-27", selectedFollowUpDate(null, 14, today))
        assertNull(selectedFollowUpDate(null, 0, today))
    }

    @Test fun calendarChoiceOverridesPriorPresetAndCanSpanMonths() {
        val date = LocalDate.of(2026, 12, 5)
        assertEquals("2026-12-05", selectedFollowUpDate(date, 7, today))
        assertTrue(isValidCheckInDate(date, today))
    }

    @Test fun aPreviouslyChosenDateThatHasPassedCannotBeSubmitted() {
        assertFalse(isValidCheckInDate(today.minusDays(1), today))
        assertFalse(isValidCheckInDate(today, today.plusDays(1)))
        assertTrue(isValidCheckInDate(today, today))
    }
}
