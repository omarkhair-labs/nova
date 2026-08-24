package com.nova.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorContractTest {
    @Test
    fun everyTypedDestinationCanBeDeliveredThroughTheContract() {
        val delivered = mutableListOf<AppDestination>()
        val navigator = AppNavigator { destination ->
            delivered += destination
            true
        }

        val destinations = listOf(
            AppDestination.Home,
            AppDestination.Orbit,
            AppDestination.Create,
            AppDestination.Inbox,
            AppDestination.Reels,
            AppDestination.Profile,
        )

        destinations.forEach { destination ->
            assertTrue(navigator.navigate(destination))
        }
        assertEquals(destinations, delivered)
    }
}
