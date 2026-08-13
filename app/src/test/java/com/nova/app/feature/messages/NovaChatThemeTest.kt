package com.nova.app.feature.messages

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class NovaChatThemeTest {
    @Test
    fun knownThemeKeysResolveWithoutChangingIdentity() {
        NovaChatThemes.All.forEach { theme ->
            assertEquals(theme.key, NovaChatThemes.resolve(theme.key).key)
            assertTrue(NovaChatThemes.isSupported(theme.key))
        }
    }

    @Test
    fun unknownThemeFallsBackToNova() {
        assertEquals("nova", NovaChatThemes.resolve("unknown").key)
        assertEquals("nova", NovaChatThemes.resolve(null).key)
        assertFalse(NovaChatThemes.isSupported("unknown"))
    }
}
