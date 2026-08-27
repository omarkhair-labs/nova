package com.nova.app.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class NovaVideoPreparerTest {
    @Test
    fun `full duration accepts normal container rounding`() {
        assertTrue(isDurationPreserved(sourceDurationMs = 20_000L, outputDurationMs = 19_850L))
        assertTrue(isDurationPreserved(sourceDurationMs = 2_000L, outputDurationMs = 1_550L))
    }

    @Test
    fun `short output is rejected instead of silently publishing a loop`() {
        assertFalse(isDurationPreserved(sourceDurationMs = 20_000L, outputDurationMs = 4_000L))
        assertFalse(isDurationPreserved(sourceDurationMs = 20_000L, outputDurationMs = 0L))
    }
}
