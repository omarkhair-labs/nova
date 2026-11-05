package com.nova.app.feature.plants

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaProfileIdentityGateTest {
    @Test fun guestAndUnknownAccountsNeverFetchPrivateSpaces() {
        assertFalse(mayFetchPrivateSpaces(0L))
        assertFalse(mayFetchPrivateSpaces(-1L))
        assertFalse(mayFetchPrivateSpaces(Long.MIN_VALUE))
    }

    @Test fun signedInIdentityCanRequestItsOwnPrivateSpaces() {
        assertTrue(mayFetchPrivateSpaces(1L))
        assertTrue(mayFetchPrivateSpaces(187L))
        assertTrue(mayFetchPrivateSpaces(Long.MAX_VALUE))
    }
}
