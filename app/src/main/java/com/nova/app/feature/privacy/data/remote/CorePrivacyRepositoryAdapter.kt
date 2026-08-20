package com.nova.app.feature.privacy.data.remote

import android.content.Context
import com.nova.app.core.network.ApiResult
import com.nova.app.core.privacy.NovaPersonPrivacyState as CorePersonPrivacyState
import com.nova.app.core.privacy.NovaPrivacyRepository
import com.nova.app.core.privacy.NovaPrivacySummary as CorePrivacySummary
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.data.PrivacyRepository
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import com.nova.app.feature.privacy.domain.model.NovaPrivacySummary


class CorePrivacyRepositoryAdapter(context: Context) : PrivacyRepository {
    private val delegate = NovaPrivacyRepository(context.applicationContext)

    override suspend fun summary(): ApiResult<NovaPrivacySummary> = when (val result = delegate.summary()) {
        is ApiResult.Success -> ApiResult.Success(result.value.toStablePrivacySummary())
        is ApiResult.Failure -> result
    }

    override suspend fun setPrivate(isPrivate: Boolean): ApiResult<NovaPrivacySummary> =
        when (val result = delegate.setPrivate(isPrivate)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toStablePrivacySummary())
            is ApiResult.Failure -> result
        }

    override suspend fun personState(username: String): ApiResult<NovaPersonPrivacyState> =
        when (val result = delegate.personState(username)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toStablePersonPrivacyState())
            is ApiResult.Failure -> result
        }

    override suspend fun closeFriends(): ApiResult<List<NovaPerson>> = delegate.closeFriends()

    override suspend fun setCloseFriend(username: String, enabled: Boolean): ApiResult<Unit> =
        delegate.setCloseFriend(username, enabled)
}


internal fun CorePrivacySummary.toStablePrivacySummary(): NovaPrivacySummary = NovaPrivacySummary(
    isPrivate = isPrivate,
    pendingFollowRequests = pendingFollowRequests,
    closeFriendsCount = closeFriendsCount,
    acceptedPendingRequests = acceptedPendingRequests,
)


internal fun CorePersonPrivacyState.toStablePersonPrivacyState(): NovaPersonPrivacyState = NovaPersonPrivacyState(
    isPrivate = isPrivate,
    followRequested = followRequested,
    canViewContent = canViewContent,
)
