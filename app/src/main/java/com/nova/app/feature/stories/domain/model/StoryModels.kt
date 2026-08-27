package com.nova.app.feature.stories.domain.model


data class NovaStoryAuthor(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
) {
    val displayName: String get() = name.ifBlank { username }
}


data class NovaStorySharedPost(
    val id: Long,
    val author: NovaStoryAuthor,
    val imageUrl: String,
    val caption: String,
)


data class NovaStorySharedReel(
    val id: Long,
    val author: NovaStoryAuthor,
    val videoUrl: String,
    val caption: String,
)


data class NovaStory(
    val id: Long,
    val author: NovaStoryAuthor,
    val mediaUrl: String,
    val mediaType: String,
    val caption: String,
    val createdAt: String,
    val expiresAt: String,
    val isMine: Boolean,
    val isViewed: Boolean,
    val myReaction: String,
    val viewsCount: Int?,
    val audience: String = "followers",
    val backgroundStyle: String = "midnight",
    val sharedPost: NovaStorySharedPost? = null,
    val sharedReel: NovaStorySharedReel? = null,
    val thumbnailUrl: String = "",
)


data class NovaStoryGroup(
    val author: NovaStoryAuthor,
    val stories: List<NovaStory>,
    val hasUnseen: Boolean,
    val isMine: Boolean,
)


data class NovaStoryViewer(
    val user: NovaStoryAuthor,
    val viewedAt: String,
    val reaction: String,
)
