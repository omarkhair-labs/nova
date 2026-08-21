package com.nova.app.feature.notifications.domain.model

import com.nova.app.feature.posts.domain.model.NovaPostAuthor


data class NovaNotification(
    val id: Long,
    val kind: String,
    val actor: NovaPostAuthor,
    val postId: Long?,
    val reelId: Long?,
    val reelAuthorUsername: String,
    val commentPreview: String,
    val createdAt: String,
    val isRead: Boolean,
)


data class NovaNotificationPage(
    val notifications: List<NovaNotification>,
    val nextCursor: String?,
    val unreadCount: Int,
)
