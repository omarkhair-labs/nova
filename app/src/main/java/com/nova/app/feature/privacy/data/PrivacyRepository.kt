package com.nova.app.feature.privacy.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import com.nova.app.feature.privacy.domain.model.NovaPrivacySummary
import com.nova.app.feature.privacy.domain.model.NovaNotificationPreferences


interface PrivacyRepository {
    suspend fun summary(): ApiResult<NovaPrivacySummary>

    suspend fun setPrivate(isPrivate: Boolean): ApiResult<NovaPrivacySummary>

    suspend fun updateSettings(
        showActivityStatus: Boolean? = null,
        sendReadReceipts: Boolean? = null,
        storyAudience: String? = null,
    ): ApiResult<NovaPrivacySummary> = summary()

    suspend fun personState(username: String): ApiResult<NovaPersonPrivacyState>

    suspend fun closeFriends(): ApiResult<List<NovaPerson>>

    suspend fun setCloseFriend(username: String, enabled: Boolean): ApiResult<Unit>

    suspend fun notificationPreferences(): ApiResult<NovaNotificationPreferences> =
        ApiResult.Success(NovaNotificationPreferences())

    suspend fun updateNotificationPreference(
        key: String,
        enabled: Boolean,
    ): ApiResult<NovaNotificationPreferences> = notificationPreferences()
}
