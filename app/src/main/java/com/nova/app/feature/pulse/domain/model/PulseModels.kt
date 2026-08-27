package com.nova.app.feature.pulse.domain.model


data class NovaPulseAuthor(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
)


data class NovaPulse(
    val id: Long,
    val author: NovaPulseAuthor,
    val mediaUrl: String,
    val thumbnailUrl: String = "",
    val mediaType: String,
    val audience: String,
    val category: String = "vibes",
    val note: String,
    val createdAt: String,
    val expiresAt: String,
    val isMine: Boolean,
    val replyToId: Long? = null,
    val chainRootId: Long? = null,
    val reactionsCount: Int = 0,
    val viewersCount: Int = 0,
    val isReacted: Boolean = false,
)
