package com.nova.app.feature.plants

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class PlantupiaApprovedInspectionReminderTest {
    @Test fun reminderFollowsSourceTimezoneAndNotAnAssumedWateringCadence() {
        val now = ZonedDateTime.of(2026, 11, 3, 8, 0, 0, 0, ZoneId.of("Africa/Cairo"))
        assertEquals(
            60L * 60L * 1000L,
            approvedInspectionDelay("2026-11-03", "Africa/Cairo", now),
        )
        assertNull(approvedInspectionDelay("2026-11-02", "Africa/Cairo", now))
        assertNull(approvedInspectionDelay("invalid", "Africa/Cairo", now))
        assertNull(approvedInspectionDelay("2026-11-04", "Invalid/Zone", now))
    }

    @Test fun revalidatedOccurrenceStopsOnStaleDateRevisionOrRevokedPlan() {
        val matches = correctInspectionOccurrence(
            "plan-1", "plan-1", true, true, 8, 8,
            "2026-11-03", "2026-11-03", "plant-1", "plant-1",
        )
        assertTrue(matches)
        assertFalse(correctInspectionOccurrence(
            "plan-1", "plan-1", true, true, 9, 8,
            "2026-11-03", "2026-11-03", "plant-1", "plant-1",
        ))
        assertFalse(correctInspectionOccurrence(
            "plan-1", "plan-1", true, true, 8, 8,
            "2026-11-04", "2026-11-03", "plant-1", "plant-1",
        ))
        assertFalse(correctInspectionOccurrence(
            "plan-1", "plan-1", true, false, 8, 8,
            "2026-11-03", "2026-11-03", "plant-1", "plant-1",
        ))
        assertFalse(correctInspectionOccurrence(
            "plan-1", "plan-1", false, true, 8, 8,
            "2026-11-03", "2026-11-03", "plant-1", "plant-1",
        ))
        assertFalse(correctInspectionOccurrence(
            "plan-1", "plan-1", true, true, 8, 8,
            "2026-11-03", "2026-11-03", "plant-1", "other",
        ))
    }
}
