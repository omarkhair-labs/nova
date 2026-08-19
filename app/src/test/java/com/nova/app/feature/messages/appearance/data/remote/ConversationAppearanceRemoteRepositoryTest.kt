package com.nova.app.feature.messages.appearance.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test


class ConversationAppearanceRemoteRepositoryTest {
    @Test
    fun themeKeyNormalizationPreservesLegacyRules() {
        assertEquals("nova", normalizeConversationThemeKey(""))
        assertEquals("nova", normalizeConversationThemeKey("   "))
        assertEquals("nova", normalizeConversationThemeKey(" NOVA "))
        assertEquals("sunset", normalizeConversationThemeKey(" Sunset "))
        assertEquals("midnight", normalizeConversationThemeKey("MIDNIGHT"))
    }
}
