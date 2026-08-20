package com.nova.app.feature.reels.domain.model


data class NovaReelAuthor(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
) {
    val displayName: String get() = name.ifBlank { username }
}


data class NovaReel(
    val id: Long,
    val author: NovaReelAuthor,
    val videoUrl: String,
    val caption: String,
    val createdAt: String,
    val isMine: Boolean,
    val likesCount: Int,
    val commentsCount: Int,
    val repostsCount: Int,
    val isLiked: Boolean,
    val isReposted: Boolean,
    val repostedBy: NovaReelAuthor?,
)


data class NovaReelPage(
    val reels: List<NovaReel>,
    val nextCursor: String?,
)


data class NovaReelComment(
    val id: Long,
    val author: NovaReelAuthor,
    val body: String,
    val createdAt: String,
    val isMine: Boolean,
    val parentId: Long? = null,
    val repliesCount: Int = 0,
    val replies: List<NovaReelComment> = emptyList(),
)


data class NovaReelCommentMutation(
    val comment: NovaReelComment,
    val reel: NovaReel,
)
