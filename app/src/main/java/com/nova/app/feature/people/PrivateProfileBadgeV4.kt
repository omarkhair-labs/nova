package com.nova.app.feature.people

import com.nova.app.core.privacy.NovaPersonPrivacyState


internal fun shouldShowPrivateProfileBadgeV4(state: NovaPersonPrivacyState): Boolean {
    return state.isPrivate && !state.canViewContent
}
