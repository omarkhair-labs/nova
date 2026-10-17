package com.nova.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaAvatarIdentityTest {
    @Test fun sameAccountAlwaysGetsSamePortrait() {
        assertEquals(plantupiaAvatarStyle(1234L), plantupiaAvatarStyle(1234L))
    }

    @Test fun consecutiveUsersDoNotGetTheSameInitialPortrait() {
        assertNotEquals(plantupiaAvatarStyle(1L), plantupiaAvatarStyle(2L))
        assertNotEquals(plantupiaAvatarStyle(8L), plantupiaAvatarStyle(9L))
    }

    @Test fun savedAvatarChoiceAndResetStayStable() {
        val id = 1234L
        assertEquals(5, plantupiaAvatarStyle(id, 5).figure)
        assertEquals(plantupiaAvatarStyle(id, 5), plantupiaAvatarStyle(id, 5))
        assertEquals(plantupiaAvatarStyle(id), plantupiaAvatarStyle(id, -1))
        assertEquals(plantupiaAvatarStyle(id), plantupiaAvatarStyle(id, 99))
    }

    @Test fun allPartsStayWithinTheAuthoredFamily() {
        listOf(0L, 1L, 7L, 40L, 99L, Long.MAX_VALUE).forEach { id ->
            val style = plantupiaAvatarStyle(id)
            assertTrue(style.figure in 0..7)
            assertTrue(style.palette in 0..4)
            assertTrue(style.accent in 0..2)
        }
    }
}
