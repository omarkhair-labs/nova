package com.nova.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationBridgeTest {
    private val bridge = AppNavigationBridge()

    @Test
    fun inactiveHostFallsBackWithoutDispatching() {
        val destinations = mutableListOf<AppDestination>()
        val handler: (AppDestination) -> Unit = { destinations += it }

        bridge.attach(handler)
        bridge.setHostActive(false)

        assertFalse(bridge.navigate(AppDestination.Inbox))
        assertEquals(emptyList<AppDestination>(), destinations)
    }

    @Test
    fun activeHostConsumesPrimaryNavigation() {
        val destinations = mutableListOf<AppDestination>()
        val handler: (AppDestination) -> Unit = { destinations += it }

        bridge.attach(handler)
        bridge.setHostActive(true)

        assertTrue(bridge.navigate(AppDestination.Reels))
        assertEquals(listOf(AppDestination.Reels), destinations)
    }

    @Test
    fun detachedHostCannotConsumeNavigation() {
        val destinations = mutableListOf<AppDestination>()
        val handler: (AppDestination) -> Unit = { destinations += it }

        bridge.attach(handler)
        bridge.setHostActive(true)
        bridge.detach(handler)

        assertFalse(bridge.navigate(AppDestination.Inbox))
        assertEquals(emptyList<AppDestination>(), destinations)
    }

    @Test
    fun staleDetachDoesNotRemoveTheCurrentHandler() {
        val firstDestinations = mutableListOf<AppDestination>()
        val secondDestinations = mutableListOf<AppDestination>()
        val first: (AppDestination) -> Unit = { firstDestinations += it }
        val second: (AppDestination) -> Unit = { secondDestinations += it }

        bridge.attach(first)
        bridge.attach(second)
        bridge.setHostActive(true)
        bridge.detach(first)

        assertTrue(bridge.navigate(AppDestination.Profile))
        assertEquals(emptyList<AppDestination>(), firstDestinations)
        assertEquals(listOf(AppDestination.Profile), secondDestinations)
    }
}
