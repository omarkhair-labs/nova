package com.nova.app.feature.messages.details.model

import com.nova.app.feature.posts.domain.model.NovaPostAuthor


data class ConversationToolMessage(
    val id: Long,
    val sender: NovaPostAuthor,
    val body: String,
    val imageUrl: String,
    val audioUrl: String,
    val audioDurationMs: Long?,
    val replyToId: Long?,
    val replyPreview: String,
    val createdAt: String,
    val isMine: Boolean,
    val isDeleted: Boolean,
)


data class ConversationMediaPage(
    val items: List<ConversationToolMessage>,
    val nextCursor: String?,
)


data class ConversationMessageContext(
    val items: List<ConversationToolMessage>,
    val targetMessageId: Long,
    val hasEarlier: Boolean,
    val hasLater: Boolean,
)
