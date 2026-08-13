package com.nova.app.feature.people

import com.nova.app.core.privacy.NovaPersonPrivacyState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class PrivateProfileBadgeV4Test {
    @Test
    fun privateBadge_onlyShowsWhenPrivateContentIsStillLocked() {
        assertTrue(
            shouldShowPrivateProfileBadgeV4(
                NovaPersonPrivacyState(
                    isPrivate = true,
                    followRequested = true,
                    canViewContent = false,
                )
            )
        )
        assertFalse(
            shouldShowPrivateProfileBadgeV4(
                NovaPersonPrivacyState(
                    isPrivate = true,
                    followRequested = false,
                    canViewContent = true,
                )
            )
        )
        assertFalse(
            shouldShowPrivateProfileBadgeV4(
                NovaPersonPrivacyState(
                    isPrivate = false,
                    followRequested = false,
                    canViewContent = true,
                )
            )
        )
    }
}
