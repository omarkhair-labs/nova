package com.nova.app.feature.publishing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
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
        val story = MediaPublishWorker.uniqueName(MediaPublishTarget.STORY, 12L, "same-client")
        val pulse = MediaPublishWorker.uniqueName(MediaPublishTarget.PULSE, 12L, "same-client")
        assertNotEquals(post, anotherAccount)
        assertNotEquals(post, reel)
        assertNotEquals(reel, story)
        assertNotEquals(story, pulse)
        assertEquals(MediaPublishTarget.PULSE, MediaPublishTarget.fromWire("pulse"))
    }

    @Test
    fun `publish progress switches from preparation to real upload progress`() {
        val preparing = advancePublishProgress(uploadStarted = false, value = 42)
        assertFalse(preparing.uploadStarted)
        assertEquals(MediaPublishWorker.STAGE_PREPARING, preparing.stage)
        assertEquals(42, preparing.progress)

        val uploadStart = advancePublishProgress(uploadStarted = false, value = 100)
        assertTrue(uploadStart.uploadStarted)
        assertEquals(MediaPublishWorker.STAGE_UPLOADING, uploadStart.stage)
        assertEquals(0, uploadStart.progress)

        val uploading = advancePublishProgress(uploadStarted = true, value = 63)
        assertTrue(uploading.uploadStarted)
        assertEquals(MediaPublishWorker.STAGE_UPLOADING, uploading.stage)
        assertEquals(63, uploading.progress)
    }

    @Test
    fun `old completed work is not replayed as a new publish event`() {
        assertFalse(isPublishSuccessFresh(finishedAtMs = 9_999L, observerStartedAtMs = 10_000L))
        assertFalse(isPublishSuccessFresh(finishedAtMs = 0L, observerStartedAtMs = 10_000L))
        assertTrue(isPublishSuccessFresh(finishedAtMs = 10_000L, observerStartedAtMs = 10_000L))
        assertTrue(isPublishSuccessFresh(finishedAtMs = 10_001L, observerStartedAtMs = 10_000L))
    }

    @Test
    fun `durable publish never crosses signed-in account`() {
        assertTrue(publishAccountMatches(expectedUserId = 12L, activeUserId = 12L))
        assertFalse(publishAccountMatches(expectedUserId = 12L, activeUserId = 13L))
        assertFalse(publishAccountMatches(expectedUserId = 12L, activeUserId = null))
    }
}
