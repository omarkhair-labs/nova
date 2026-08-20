package com.nova.app.feature.notifications.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.notifications.domain.model.NovaNotificationPage


interface NotificationsRepository {
    suspend fun notifications(cursor: String? = null): ApiResult<NovaNotificationPage>

    suspend fun markAllRead(): ApiResult<Int>
}
