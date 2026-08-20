package com.nova.app.feature.privacy.data.remote

import android.content.Context
import com.nova.app.core.network.ApiResult
import com.nova.app.core.privacy.NovaFollowRequest as CoreFollowRequest
import com.nova.app.core.privacy.NovaPrivacyRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.data.FollowRequestRepository
import com.nova.app.feature.privacy.domain.model.NovaFollowRequest


class CoreFollowRequestRepositoryAdapter(context: Context) : FollowRequestRepository {
    private val delegate = NovaPrivacyRepository(context.applicationContext)

    override suspend fun followRequests(): ApiResult<List<NovaFollowRequest>> {
        return when (val result = delegate.followRequests()) {
            is ApiResult.Success -> ApiResult.Success(result.value.map(CoreFollowRequest::toStableFollowRequest))
            is ApiResult.Failure -> result
        }
    }

    override suspend fun acceptFollowRequest(requestId: Long): ApiResult<Unit> =
        delegate.acceptFollowRequest(requestId)

    override suspend fun declineFollowRequest(requestId: Long): ApiResult<Unit> =
        delegate.declineFollowRequest(requestId)
}


internal fun CoreFollowRequest.toStableFollowRequest(): NovaFollowRequest = NovaFollowRequest(
    id = id,
    requester = NovaPerson(
        id = requester.id,
        username = requester.username,
        name = requester.name,
        avatarUrl = requester.avatarUrl,
        followersCount = requester.followersCount,
        followingCount = requester.followingCount,
        postsCount = requester.postsCount,
        isFollowing = requester.isFollowing,
    ),
    createdAt = createdAt,
)
