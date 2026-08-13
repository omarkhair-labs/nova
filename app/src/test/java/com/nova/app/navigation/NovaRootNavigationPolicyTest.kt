package com.nova.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NovaRootNavigationPolicyTest {
    @Test
    fun everyRootIsStableWhenRequestedAgain() {
        NovaRootTab.entries.forEach { tab ->
            assertEquals(emptyList<NovaRootTab>(), rootNavigationPlan(tab, tab))
        }
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

    @Test
    fun allNineRootTransitionsRemainCharacterized() {
        val expected = mapOf(
            (NovaRootTab.Home to NovaRootTab.Home) to emptyList(),
            (NovaRootTab.Home to NovaRootTab.People) to listOf(NovaRootTab.People),
            (NovaRootTab.Home to NovaRootTab.Profile) to listOf(NovaRootTab.Profile),
            (NovaRootTab.People to NovaRootTab.Home) to listOf(NovaRootTab.Home),
            (NovaRootTab.People to NovaRootTab.People) to emptyList(),
            (NovaRootTab.People to NovaRootTab.Profile) to listOf(
                NovaRootTab.Home,
                NovaRootTab.Profile,
            ),
            (NovaRootTab.Profile to NovaRootTab.Home) to listOf(NovaRootTab.Home),
            (NovaRootTab.Profile to NovaRootTab.People) to listOf(
                NovaRootTab.Home,
                NovaRootTab.People,
            ),
            (NovaRootTab.Profile to NovaRootTab.Profile) to emptyList(),
        )

        expected.forEach { (transition, plan) ->
            assertEquals(plan, rootNavigationPlan(transition.first, transition.second))
        }
    }
}
