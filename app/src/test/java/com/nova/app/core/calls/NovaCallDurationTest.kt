package com.nova.app.core.calls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class NovaCallDurationTest {
    @Test
    fun formatsMinutesAndSeconds() {
        val start = 1_000_000L
        assertEquals("00:00", NovaCallDuration.label(start, start))
        assertEquals("01:05", NovaCallDuration.label(start, start + 65_000L))
        assertEquals("59:59", NovaCallDuration.label(start, start + 3_599_000L))
    }

    @Test
    fun formatsHoursWithoutResettingMinutes() {
        val start = 2_000_000L
        assertEquals("1:02:03", NovaCallDuration.label(start, start + 3_723_000L))
    }

    @Test
    fun parsesServerAnsweredAtAndRejectsInvalidValues() {
        assertEquals(
            1_786_602_608_000L,
            NovaCallDuration.answeredAtEpochMs("2026-08-13T06:56:48Z"),
        )
        assertNull(NovaCallDuration.answeredAtEpochMs("not-a-time"))
        assertNull(NovaCallDuration.answeredAtEpochMs(null))
    }
}
