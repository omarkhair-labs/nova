package com.nova.app.feature.publishing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class MediaPublishWorkerTest {
    @Test
    fun `transient failures retry within bounded attempts`() {
        assertTrue(shouldRetryPublish(statusCode = null, runAttemptCount = 0))
        assertTrue(shouldRetryPublish(statusCode = 503, runAttemptCount = 1))
        assertTrue(shouldRetryPublish(statusCode = 429, runAttemptCount = 0))
        assertFalse(shouldRetryPublish(statusCode = 400, runAttemptCount = 0))
        assertFalse(shouldRetryPublish(statusCode = 503, runAttemptCount = 2))
    }

    @Test
    fun `publish identity is scoped by account destination and client id`() {
        val post = MediaPublishWorker.uniqueName(MediaPublishTarget.POST, 12L, "same-client")
        val anotherAccount = MediaPublishWorker.uniqueName(MediaPublishTarget.POST, 13L, "same-client")
        val reel = MediaPublishWorker.uniqueName(MediaPublishTarget.REEL, 12L, "same-client")
        assertNotEquals(post, anotherAccount)
        assertNotEquals(post, reel)
    }

    @Test
    fun `durable publish never crosses signed-in account`() {
        assertTrue(publishAccountMatches(expectedUserId = 12L, activeUserId = 12L))
        assertFalse(publishAccountMatches(expectedUserId = 12L, activeUserId = 13L))
        assertFalse(publishAccountMatches(expectedUserId = 12L, activeUserId = null))
    }
}
