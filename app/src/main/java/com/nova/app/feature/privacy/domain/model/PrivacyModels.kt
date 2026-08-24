package com.nova.app.feature.privacy.domain.model


data class NovaPersonPrivacyState(
    val isPrivate: Boolean,
    val followRequested: Boolean,
    val canViewContent: Boolean,
)


data class NovaPrivacySummary(
    val isPrivate: Boolean,
    val showActivityStatus: Boolean = true,
    val sendReadReceipts: Boolean = true,
    val storyAudience: String = "followers",
    val pendingFollowRequests: Int,
    val closeFriendsCount: Int,
    val acceptedPendingRequests: Int = 0,
)


data class NovaNotificationPreferences(
    val likesCommentsShares: Boolean = true,
    val mentionsTags: Boolean = true,
    val followers: Boolean = true,
    val messages: Boolean = true,
    val liveSessions: Boolean = true,
    val reelsStories: Boolean = true,
    val eventsSpaces: Boolean = true,
    val productUpdates: Boolean = true,
)
