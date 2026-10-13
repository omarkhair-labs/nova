package com.nova.app.feature.plants

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaCareReminderScheduleTest {
    private val time = ZonedDateTime.of(2026, 10, 13, 21, 0, 0, 0, ZoneId.of("Africa/Cairo"))

    @Test fun datesChosenByTheUserScheduleMorningCheckInNotAnAutomaticWateringRule() {
        val delay = checkInDelayMillis("2026-10-20", time)
        assertEquals(7L * 24 * 60 * 60 * 1000 - 12L * 60 * 60 * 1000, delay)
        assertTrue(delay!! > 0)
    }

    @Test fun invalidOrPastDatesCannotEnqueueNotifications() {
        assertNull(checkInDelayMillis("", time))
        assertNull(checkInDelayMillis("tomorrow", time))
        assertNull(checkInDelayMillis("2026-10-13", time))
        assertNull(checkInDelayMillis("2026-10-01", time))
    }

    @Test fun futureDateInLocalTimezoneCalculatesRemainingDelay() {
        val morning = time.withHour(8).withMinute(30)
        assertEquals(30L * 60 * 1000, checkInDelayMillis("2026-10-13", morning))
    }
}
