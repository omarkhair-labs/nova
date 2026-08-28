package com.nova.app.feature.pulse.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class PulseRemoteRepositoryTest {
    @Test
    fun `durable pulse publish is account scoped`() {
        assertTrue(pulsePublishAccountMatches(expectedUserId = null, activeUserId = null))
        assertTrue(pulsePublishAccountMatches(expectedUserId = 42L, activeUserId = 42L))
        assertFalse(pulsePublishAccountMatches(expectedUserId = 42L, activeUserId = 7L))
        assertFalse(pulsePublishAccountMatches(expectedUserId = 42L, activeUserId = null))
        assertFalse(pulsePublishAccountMatches(expectedUserId = 0L, activeUserId = 0L))
    }
}
