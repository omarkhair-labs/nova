package com.nova.app.feature.privacy.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import com.nova.app.feature.privacy.domain.model.NovaPrivacySummary


interface PrivacyRepository {
    suspend fun summary(): ApiResult<NovaPrivacySummary>

    suspend fun setPrivate(isPrivate: Boolean): ApiResult<NovaPrivacySummary>

    suspend fun personState(username: String): ApiResult<NovaPersonPrivacyState>

    suspend fun closeFriends(): ApiResult<List<NovaPerson>>

    suspend fun setCloseFriend(username: String, enabled: Boolean): ApiResult<Unit>
}
