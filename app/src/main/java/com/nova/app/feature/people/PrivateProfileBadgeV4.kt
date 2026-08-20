package com.nova.app.feature.people

import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState


internal fun shouldShowPrivateProfileBadgeV4(state: NovaPersonPrivacyState): Boolean {
    return state.isPrivate && !state.canViewContent
}
