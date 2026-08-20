package com.nova.app.feature.privacy.domain.model


data class NovaPersonPrivacyState(
    val isPrivate: Boolean,
    val followRequested: Boolean,
    val canViewContent: Boolean,
)


data class NovaPrivacySummary(
    val isPrivate: Boolean,
    val pendingFollowRequests: Int,
    val closeFriendsCount: Int,
    val acceptedPendingRequests: Int = 0,
)
