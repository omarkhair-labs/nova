package com.nova.app.feature.privacy.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.privacy.domain.model.NovaFollowRequest


interface FollowRequestRepository {
    suspend fun followRequests(): ApiResult<List<NovaFollowRequest>>
    suspend fun sentFollowRequests(): ApiResult<List<NovaFollowRequest>> = ApiResult.Success(emptyList())
    suspend fun acceptFollowRequest(requestId: Long): ApiResult<Unit>
    suspend fun declineFollowRequest(requestId: Long): ApiResult<Unit>
}
