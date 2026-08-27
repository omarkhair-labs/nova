package com.nova.app.core.stories

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class NovaStoriesRepositoryTest {
    @Test
    fun `durable Story publish is account scoped while direct calls remain compatible`() {
        assertTrue(storyPublishAccountMatches(expectedUserId = 12L, activeUserId = 12L))
        assertFalse(storyPublishAccountMatches(expectedUserId = 12L, activeUserId = 13L))
        assertFalse(storyPublishAccountMatches(expectedUserId = 12L, activeUserId = null))
        assertTrue(storyPublishAccountMatches(expectedUserId = null, activeUserId = 13L))
    }
}
