package com.nova.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NovaRootNavigationPolicyTest {
    @Test
    fun sameRootDoesNothing() {
        assertEquals(
            emptyList<NovaRootTab>(),
            rootNavigationPlan(NovaRootTab.People, NovaRootTab.People),
        )
    }

    @Test
    fun homeToSecondaryRootOpensOnlyTarget() {
        assertEquals(
            listOf(NovaRootTab.People),
            rootNavigationPlan(NovaRootTab.Home, NovaRootTab.People),
        )
        assertEquals(
            listOf(NovaRootTab.Profile),
            rootNavigationPlan(NovaRootTab.Home, NovaRootTab.Profile),
        )
    }

    @Test
    fun switchingBetweenNestedSecondaryRootsResetsThroughHome() {
        assertEquals(
            listOf(NovaRootTab.Home, NovaRootTab.Profile),
            rootNavigationPlan(NovaRootTab.People, NovaRootTab.Profile),
        )
        assertEquals(
            listOf(NovaRootTab.Home, NovaRootTab.People),
            rootNavigationPlan(NovaRootTab.Profile, NovaRootTab.People),
        )
    }

    @Test
    fun anySecondaryRootCanReturnToHomeDirectly() {
        assertEquals(
            listOf(NovaRootTab.Home),
            rootNavigationPlan(NovaRootTab.People, NovaRootTab.Home),
        )
        assertEquals(
            listOf(NovaRootTab.Home),
            rootNavigationPlan(NovaRootTab.Profile, NovaRootTab.Home),
        )
    }
}
