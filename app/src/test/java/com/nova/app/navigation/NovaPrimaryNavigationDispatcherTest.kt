package com.nova.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaPrimaryNavigationDispatcherTest {
    @Test
    fun inactiveHostFallsBackWithoutDispatching() {
        val destinations = mutableListOf<NovaPrimaryDestination>()
        val handler: (NovaPrimaryDestination) -> Unit = { destinations += it }

        NovaPrimaryNavigationDispatcher.attach(handler)
        NovaPrimaryNavigationDispatcher.setHostActive(false)

        assertFalse(
            NovaPrimaryNavigationDispatcher.navigate(NovaPrimaryDestination.Messages)
        )
        assertEquals(emptyList<NovaPrimaryDestination>(), destinations)

        NovaPrimaryNavigationDispatcher.detach(handler)
    }

    @Test
    fun activeHostConsumesPrimaryNavigation() {
        val destinations = mutableListOf<NovaPrimaryDestination>()
        val handler: (NovaPrimaryDestination) -> Unit = { destinations += it }

        NovaPrimaryNavigationDispatcher.attach(handler)
        NovaPrimaryNavigationDispatcher.setHostActive(true)

        assertTrue(
            NovaPrimaryNavigationDispatcher.navigate(NovaPrimaryDestination.Reels)
        )
        assertEquals(listOf(NovaPrimaryDestination.Reels), destinations)

        NovaPrimaryNavigationDispatcher.detach(handler)
    }

    @Test
    fun detachedHostCannotConsumeNavigation() {
        val destinations = mutableListOf<NovaPrimaryDestination>()
        val handler: (NovaPrimaryDestination) -> Unit = { destinations += it }

        NovaPrimaryNavigationDispatcher.attach(handler)
        NovaPrimaryNavigationDispatcher.setHostActive(true)
        NovaPrimaryNavigationDispatcher.detach(handler)

        assertFalse(
            NovaPrimaryNavigationDispatcher.navigate(NovaPrimaryDestination.Messages)
        )
        assertEquals(emptyList<NovaPrimaryDestination>(), destinations)
    }

    @Test
    fun staleDetachDoesNotRemoveTheCurrentHandler() {
        val firstDestinations = mutableListOf<NovaPrimaryDestination>()
        val secondDestinations = mutableListOf<NovaPrimaryDestination>()
        val first: (NovaPrimaryDestination) -> Unit = { firstDestinations += it }
        val second: (NovaPrimaryDestination) -> Unit = { secondDestinations += it }

        NovaPrimaryNavigationDispatcher.attach(first)
        NovaPrimaryNavigationDispatcher.attach(second)
        NovaPrimaryNavigationDispatcher.setHostActive(true)
        NovaPrimaryNavigationDispatcher.detach(first)

        assertTrue(
            NovaPrimaryNavigationDispatcher.navigate(NovaPrimaryDestination.Profile)
        )
        assertEquals(emptyList<NovaPrimaryDestination>(), firstDestinations)
        assertEquals(listOf(NovaPrimaryDestination.Profile), secondDestinations)

        NovaPrimaryNavigationDispatcher.detach(second)
    }
}
