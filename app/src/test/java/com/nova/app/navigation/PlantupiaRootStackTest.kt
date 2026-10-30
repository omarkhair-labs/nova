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
    @Test fun avatarOpensAContextualProfileAndSwitchingTabRemovesIt() {
        val original = listOf(NovaRoute.Home, NovaRoute.Shop)
        val profile = plantupiaRootStack(original, NovaRoute.Profile)
        assertEquals(original + NovaRoute.Profile, profile)
        assertEquals(profile, plantupiaRootStack(profile, NovaRoute.Profile))
        assertEquals(original + NovaRoute.Create, plantupiaRootStack(profile, NovaRoute.Create))
    }
}
