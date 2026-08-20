package com.nova.app.feature.people

import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class PrivateProfileBadgeTest {
    @Test
    fun privateBadge_onlyShowsWhenPrivateContentIsStillLocked() {
        assertTrue(
            shouldShowPrivateProfileBadge(
                NovaPersonPrivacyState(
                    isPrivate = true,
                    followRequested = true,
                    canViewContent = false,
                )
            )
        )
        assertFalse(
            shouldShowPrivateProfileBadge(
                NovaPersonPrivacyState(
                    isPrivate = true,
                    followRequested = false,
                    canViewContent = true,
                )
            )
        )
        assertFalse(
            shouldShowPrivateProfileBadge(
                NovaPersonPrivacyState(
                    isPrivate = false,
                    followRequested = false,
                    canViewContent = true,
                )
            )
        )
    }
}
