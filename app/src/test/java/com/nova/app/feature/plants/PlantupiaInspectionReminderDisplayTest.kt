package com.nova.app.feature.plants

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PlantupiaInspectionReminderDisplayTest {
    private val today = LocalDate.of(2026, 11, 5)

    @Test fun doNotOfferPastOrSameDayMorningAlerts() {
        assertFalse(canScheduleUpcomingInspection("2026-11-03", today))
        assertFalse(canScheduleUpcomingInspection("2026-11-05", today))
        assertFalse(canScheduleUpcomingInspection("invalid", today))
        assertTrue(canScheduleUpcomingInspection("2026-11-06", today))
    }
}
