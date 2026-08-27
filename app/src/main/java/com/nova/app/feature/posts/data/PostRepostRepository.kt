package com.nova.app.feature.posts.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.posts.domain.model.NovaPostAuthor


data class PostRepostResult(
    val postId: Long,
    val repostsCount: Int,
    val isReposted: Boolean,
    val stillInFeed: Boolean,
    val repostedBy: NovaPostAuthor?,
)


/** Narrow post-owned mutation seam over the existing sharing transport. */
interface PostRepostRepository {
    suspend fun setPostReposted(postId: Long, reposted: Boolean): ApiResult<PostRepostResult>
}
