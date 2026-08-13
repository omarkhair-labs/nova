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
}
