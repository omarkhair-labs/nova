package com.nova.app.feature.posts.data

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaCommentMutation
import com.nova.app.feature.posts.domain.model.NovaPost


interface PostRepository {
    suspend fun personPosts(username: String): ApiResult<List<NovaPost>>

    suspend fun post(postId: Long): ApiResult<NovaPost>

    suspend fun createPost(
        caption: String,
        imageUri: Uri,
    ): ApiResult<NovaPost>

    suspend fun deletePost(postId: Long): ApiResult<Unit>

    suspend fun setLiked(
        postId: Long,
        liked: Boolean,
    ): ApiResult<NovaPost>

    suspend fun comments(postId: Long): ApiResult<List<NovaComment>>

    suspend fun addComment(
        postId: Long,
        body: String,
        parentId: Long? = null,
    ): ApiResult<NovaCommentMutation>

    suspend fun deleteComment(commentId: Long): ApiResult<NovaPost>

    suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaPost>
}
