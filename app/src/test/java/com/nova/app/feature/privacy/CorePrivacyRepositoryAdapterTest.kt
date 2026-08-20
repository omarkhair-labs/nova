package com.nova.app.feature.privacy

import com.nova.app.core.privacy.NovaPersonPrivacyState as CorePersonPrivacyState
import com.nova.app.core.privacy.NovaPrivacySummary as CorePrivacySummary
import com.nova.app.feature.privacy.data.remote.toStablePersonPrivacyState
import com.nova.app.feature.privacy.data.remote.toStablePrivacySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class CorePrivacyRepositoryAdapterTest {
    @Test
    fun `privacy summary mapping preserves every live field`() {
        val stable = CorePrivacySummary(
            isPrivate = true,
            pendingFollowRequests = 4,
            closeFriendsCount = 7,
            acceptedPendingRequests = 3,
        ).toStablePrivacySummary()

        assertTrue(stable.isPrivate)
        assertEquals(4, stable.pendingFollowRequests)
        assertEquals(7, stable.closeFriendsCount)
        assertEquals(3, stable.acceptedPendingRequests)
    }

    @Test
    fun `person privacy mapping preserves private request and content state`() {
        val stable = CorePersonPrivacyState(
            isPrivate = true,
            followRequested = true,
            canViewContent = false,
        ).toStablePersonPrivacyState()

        assertTrue(stable.isPrivate)
        assertTrue(stable.followRequested)
        assertFalse(stable.canViewContent)
    }
}
