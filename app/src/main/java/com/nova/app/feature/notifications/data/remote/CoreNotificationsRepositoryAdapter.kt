package com.nova.app.feature.notifications.data.remote

import android.content.Context
import com.nova.app.core.network.ApiResult
import com.nova.app.core.notifications.NovaNotification as CoreNotification
import com.nova.app.core.notifications.NovaNotificationPage as CoreNotificationPage
import com.nova.app.core.notifications.NovaNotificationRepository
import com.nova.app.feature.notifications.data.NotificationsRepository
import com.nova.app.feature.notifications.domain.model.NovaNotification
import com.nova.app.feature.notifications.domain.model.NovaNotificationPage


class CoreNotificationsRepositoryAdapter(context: Context) : NotificationsRepository {
    private val delegate = NovaNotificationRepository(context.applicationContext)

    override suspend fun notifications(cursor: String?): ApiResult<NovaNotificationPage> {
        return when (val result = delegate.notifications(cursor)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toStableNotificationPage())
            is ApiResult.Failure -> result
        }
    }

    override suspend fun markAllRead(): ApiResult<Int> = delegate.markAllRead()
}


internal fun CoreNotification.toStableNotification(): NovaNotification = NovaNotification(
    id = id,
    kind = kind,
    actor = actor,
    postId = postId,
    reelId = reelId,
    reelAuthorUsername = reelAuthorUsername,
    commentPreview = commentPreview,
    createdAt = createdAt,
    isRead = isRead,
)


internal fun CoreNotificationPage.toStableNotificationPage(): NovaNotificationPage = NovaNotificationPage(
    notifications = notifications.map(CoreNotification::toStableNotification),
    nextCursor = nextCursor,
    unreadCount = unreadCount,
)
