package com.nova.app.feature.posts.domain.model

import com.nova.app.core.network.NovaPostAuthor


data class NovaPost(
    val id: Long,
    val author: NovaPostAuthor,
    val imageUrl: String,
    val caption: String,
    val createdAt: String,
    val isMine: Boolean,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
)


data class NovaPostPage(
    val posts: List<NovaPost>,
    val nextCursor: String?,
)


data class NovaComment(
    val id: Long,
    val author: NovaPostAuthor,
    val body: String,
    val createdAt: String,
    val isMine: Boolean,
    val parentId: Long? = null,
    val repliesCount: Int = 0,
    val replies: List<NovaComment> = emptyList(),
)


data class NovaCommentMutation(
    val comment: NovaComment,
    val post: NovaPost,
)
