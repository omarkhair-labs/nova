package com.nova.app.navigation

import org.junit.Assert.*
import org.junit.Test

class PlantupiaRootStackTest {
    @Test fun tabsRetainEachOtherButDropAnUnrelatedDetail() {
        val original = listOf(NovaRoute.Home, NovaRoute.Plants, NovaRoute.Person("someone"))
        val shop = plantupiaRootStack(original, NovaRoute.Shop)
        assertEquals(listOf(NovaRoute.Home, NovaRoute.Plants, NovaRoute.Shop), shop)
        assertEquals(listOf(NovaRoute.Home, NovaRoute.Shop, NovaRoute.Plants), plantupiaRootStack(shop, NovaRoute.Plants))
        assertEquals(3, plantupiaRootStack(shop, NovaRoute.Shop).size)
    }
    @Test fun aiIsPrimaryButCircleRemainsAContextualRoot() {
        val initial = listOf(NovaRoute.Home, NovaRoute.Plants)
        val ai = plantupiaRootStack(initial, NovaRoute.AI)
        assertEquals(initial + NovaRoute.AI, ai)
        assertEquals(ai, plantupiaRootStack(ai, NovaRoute.AI))
        val circle = plantupiaRootStack(ai, NovaRoute.Orbit)
        assertEquals(initial + NovaRoute.AI + NovaRoute.Orbit, circle)
        assertEquals(initial + NovaRoute.Orbit + NovaRoute.AI, plantupiaRootStack(circle, NovaRoute.AI))
    }

    @Test fun avatarOpensAContextualProfileAndSwitchingTabRemovesIt() {
        val original = listOf(NovaRoute.Home, NovaRoute.Shop)
        val profile = plantupiaRootStack(original, NovaRoute.Profile)
        assertEquals(original + NovaRoute.Profile, profile)
        assertEquals(profile, plantupiaRootStack(profile, NovaRoute.Profile))
        assertEquals(original + NovaRoute.Create, plantupiaRootStack(profile, NovaRoute.Create))
    }
}
