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
    fun everyDifferentPrimaryRootResetsDirectlyToItsTarget() {
        NovaRootTab.entries.forEach { current ->
            NovaRootTab.entries.filterNot { it == current }.forEach { requested ->
                assertEquals(
                    listOf(requested),
                    rootNavigationPlan(current, requested),
                )
            }
        }
    }

    @Test
    fun approvedSocialRootsAreHomeOrbitCreateAndProfile() {
        assertEquals(
            listOf(
                NovaRootTab.Home,
                NovaRootTab.Orbit,
                NovaRootTab.Create,
                NovaRootTab.Profile,
            ),
            NovaRootTab.entries,
        )
    }
}
