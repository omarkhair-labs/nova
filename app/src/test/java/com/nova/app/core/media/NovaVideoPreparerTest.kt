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
    fun `already compatible mp4 can skip expensive transcode`() {
        assertTrue(
            supportsNovaVideoContract(
                containerMime = "video/mp4",
                trackMimes = listOf("video/avc", "audio/mp4a-latm"),
            ),
        )
        assertTrue(
            supportsNovaVideoContract(
                containerMime = "VIDEO/MP4",
                trackMimes = listOf("video/avc"),
            ),
        )
    }

    @Test
    fun `non contract video still requires normalization`() {
        assertFalse(
            supportsNovaVideoContract(
                containerMime = "video/mp4",
                trackMimes = listOf("video/hevc", "audio/mp4a-latm"),
            ),
        )
        assertFalse(
            supportsNovaVideoContract(
                containerMime = "video/quicktime",
                trackMimes = listOf("video/avc", "audio/mp4a-latm"),
            ),
        )
        assertFalse(
            supportsNovaVideoContract(
                containerMime = "video/mp4",
                trackMimes = listOf("video/avc", "audio/opus"),
            ),
        )
    }

    @Test
    fun `short output is rejected instead of silently publishing a loop`() {
        assertFalse(isDurationPreserved(sourceDurationMs = 20_000L, outputDurationMs = 4_000L))
        assertFalse(isDurationPreserved(sourceDurationMs = 20_000L, outputDurationMs = 0L))
    }
}
