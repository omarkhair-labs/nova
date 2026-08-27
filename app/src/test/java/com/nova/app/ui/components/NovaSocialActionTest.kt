package com.nova.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test


class NovaSocialActionTest {
    @Test
    fun `compact counts stay short across large social values`() {
        assertEquals("999", compactSocialCount(999))
        assertEquals("1K", compactSocialCount(1_000))
        assertEquals("12.4K", compactSocialCount(12_400))
        assertEquals("999.9K", compactSocialCount(999_900))
        assertEquals("1M", compactSocialCount(1_000_000))
    }
}
