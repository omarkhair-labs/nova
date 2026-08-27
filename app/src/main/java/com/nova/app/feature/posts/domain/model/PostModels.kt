package com.nova.app.feature.posts.domain.model


data class NovaPostAuthor(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
)


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
    val repostsCount: Int = 0,
    val isReposted: Boolean = false,
    val repostedBy: NovaPostAuthor? = null,
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
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
)


data class NovaCommentMutation(
    val comment: NovaComment,
    val post: NovaPost,
)
