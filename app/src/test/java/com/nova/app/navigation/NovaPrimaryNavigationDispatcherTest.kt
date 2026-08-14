package com.nova.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaPrimaryNavigationDispatcherTest {
    @Test
    fun inactiveHostFallsBackWithoutDispatching() {
        val destinations = mutableListOf<AppDestination>()
        val handler: (AppDestination) -> Unit = { destinations += it }

        NovaPrimaryNavigationDispatcher.attach(handler)
        NovaPrimaryNavigationDispatcher.setHostActive(false)

        assertFalse(NovaPrimaryNavigationDispatcher.navigate(AppDestination.Messages))
        assertEquals(emptyList<AppDestination>(), destinations)

        NovaPrimaryNavigationDispatcher.detach(handler)
    }

    @Test
    fun activeHostConsumesPrimaryNavigation() {
        val destinations = mutableListOf<AppDestination>()
        val handler: (AppDestination) -> Unit = { destinations += it }

        NovaPrimaryNavigationDispatcher.attach(handler)
        NovaPrimaryNavigationDispatcher.setHostActive(true)

        assertTrue(NovaPrimaryNavigationDispatcher.navigate(AppDestination.Reels))
        assertEquals(listOf(AppDestination.Reels), destinations)

        NovaPrimaryNavigationDispatcher.detach(handler)
    }

    @Test
    fun detachedHostCannotConsumeNavigation() {
        val destinations = mutableListOf<AppDestination>()
        val handler: (AppDestination) -> Unit = { destinations += it }

        NovaPrimaryNavigationDispatcher.attach(handler)
        NovaPrimaryNavigationDispatcher.setHostActive(true)
        NovaPrimaryNavigationDispatcher.detach(handler)

        assertFalse(NovaPrimaryNavigationDispatcher.navigate(AppDestination.Messages))
        assertEquals(emptyList<AppDestination>(), destinations)
    }

    @Test
    fun staleDetachDoesNotRemoveTheCurrentHandler() {
        val firstDestinations = mutableListOf<AppDestination>()
        val secondDestinations = mutableListOf<AppDestination>()
        val first: (AppDestination) -> Unit = { firstDestinations += it }
        val second: (AppDestination) -> Unit = { secondDestinations += it }

        NovaPrimaryNavigationDispatcher.attach(first)
        NovaPrimaryNavigationDispatcher.attach(second)
        NovaPrimaryNavigationDispatcher.setHostActive(true)
        NovaPrimaryNavigationDispatcher.detach(first)

        assertTrue(NovaPrimaryNavigationDispatcher.navigate(AppDestination.Profile))
        assertEquals(emptyList<AppDestination>(), firstDestinations)
        assertEquals(listOf(AppDestination.Profile), secondDestinations)

        NovaPrimaryNavigationDispatcher.detach(second)
    }
}
