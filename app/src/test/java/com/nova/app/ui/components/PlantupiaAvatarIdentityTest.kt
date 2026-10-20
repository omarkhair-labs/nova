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

    @Test fun portraitDetailsScaleWithoutChangingSavedIdentity() {
        assertEquals(PlantupiaPortraitDetail.Micro, plantupiaPortraitDetail(24f))
        assertEquals(PlantupiaPortraitDetail.Micro, plantupiaPortraitDetail(32f))
        assertEquals(PlantupiaPortraitDetail.Micro, plantupiaPortraitDetail(40f))
        assertEquals(PlantupiaPortraitDetail.Standard, plantupiaPortraitDetail(48f))
        assertEquals(PlantupiaPortraitDetail.Standard, plantupiaPortraitDetail(64f))
        assertEquals(PlantupiaPortraitDetail.Hero, plantupiaPortraitDetail(80f))
        assertEquals(PlantupiaPortraitDetail.Hero, plantupiaPortraitDetail(96f))
        val selected = plantupiaAvatarStyle(1024L, 7)
        assertEquals(7, selected.figure)
        assertEquals(selected, plantupiaAvatarStyle(1024L, 7))
    }

    @Test fun everySavedPortraitHasDistinctRealNativeArtworkAtBothSizes() {
        val heroes = (0..7).map {
            plantupiaPortraitResource(it, PlantupiaPortraitDetail.Hero)
        }
        val micro = (0..7).map {
            plantupiaPortraitResource(it, PlantupiaPortraitDetail.Micro)
        }
        assertEquals(8, heroes.distinct().size)
        assertEquals(8, micro.distinct().size)
        (0..7).forEach { variant ->
            assertNotEquals(heroes[variant], micro[variant])
            assertEquals(
                heroes[variant],
                plantupiaPortraitResource(variant, PlantupiaPortraitDetail.Standard),
            )
        }
        assertEquals(
            heroes[0],
            plantupiaPortraitResource(-4, PlantupiaPortraitDetail.Hero),
        )
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
