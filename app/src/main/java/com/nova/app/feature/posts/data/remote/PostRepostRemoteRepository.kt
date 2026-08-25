package com.nova.app.feature.posts.data.remote

import com.nova.app.core.network.ApiResult
import com.nova.app.core.sharing.NovaSharingRepository
import com.nova.app.feature.posts.data.PostRepostRepository
import com.nova.app.feature.posts.data.PostRepostResult


/** Post-owned adapter over Nova's existing concrete repost transport. */
class PostRepostRemoteRepository(
    private val transport: NovaSharingRepository,
) : PostRepostRepository {
    override suspend fun setPostReposted(
        postId: Long,
        reposted: Boolean,
    ): ApiResult<PostRepostResult> = when (val result = transport.setReposted(postId, reposted)) {
        is ApiResult.Success -> ApiResult.Success(
            PostRepostResult(
                postId = result.value.postId,
                repostsCount = result.value.repostsCount,
                isReposted = result.value.isReposted,
                stillInFeed = result.value.stillInFeed,
                repostedBy = result.value.feedRepostedBy,
            )
        )
        is ApiResult.Failure -> result
    }
}
