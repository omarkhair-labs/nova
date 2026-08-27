package com.nova.app.feature.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test


class NovaProfileIdentityTest {
    @Test
    fun `profile links add https when the saved host has no scheme`() {
        assertEquals(
            "https://nova.example/omar",
            normalizedProfileExternalUrl("nova.example/omar"),
        )
    }

    @Test
    fun `profile links reject unsafe or hostless schemes`() {
        assertNull(normalizedProfileExternalUrl("javascript:alert(1)"))
        assertNull(normalizedProfileExternalUrl("file:///private/profile"))
        assertNull(normalizedProfileExternalUrl("https:///missing-host"))
    }

    @Test
    fun `saved profile themes produce a visible accent and unknown values fall back`() {
        assertNotEquals(
            novaProfileThemePalette("violet"),
            novaProfileThemePalette("cyan"),
        )
        assertEquals(
            novaProfileThemePalette("violet"),
            novaProfileThemePalette("future-theme"),
        )
    }
}
