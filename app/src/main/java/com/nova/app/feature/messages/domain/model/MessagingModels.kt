package com.nova.app.feature.messages.domain.model

import com.nova.app.core.network.NovaPostAuthor


data class NovaReplyPreview(
    val id: Long,
    val sender: NovaPostAuthor,
    val body: String,
    val imageUrl: String,
    val audioUrl: String = "",
    val audioDurationMs: Long? = null,
    val isDeleted: Boolean = false,
)


data class NovaMessageReaction(
    val emoji: String,
    val count: Int,
    val reactedByMe: Boolean,
)


data class NovaSharedPost(
    val id: Long,
    val author: NovaPostAuthor,
    val imageUrl: String,
    val caption: String,
)


data class NovaSharedReel(
    val id: Long,
    val author: NovaPostAuthor,
    val videoUrl: String,
    val caption: String,
)


data class NovaMessageShare(
    val kind: String,
    val available: Boolean,
    val post: NovaSharedPost? = null,
    val profile: NovaPostAuthor? = null,
    val reel: NovaSharedReel? = null,
)


data class NovaMessage(
    val id: Long,
    val clientId: String,
    val sender: NovaPostAuthor,
    val body: String,
    val imageUrl: String,
    val replyTo: NovaReplyPreview?,
    val reactions: List<NovaMessageReaction>,
    val createdAt: String,
    val deliveredAt: String?,
    val readAt: String?,
    val isMine: Boolean,
    val audioUrl: String = "",
    val audioDurationMs: Long? = null,
    val editedAt: String? = null,
    val deletedAt: String? = null,
    val share: NovaMessageShare? = null,
) {
    val isDeleted: Boolean
        get() = deletedAt != null
}


data class NovaConversation(
    val id: Long,
    val otherUser: NovaPostAuthor,
    val lastMessage: NovaMessage?,
    val unreadCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val kind: String = "direct",
    val title: String = "",
    val membersPreview: List<NovaPostAuthor> = emptyList(),
    val membersCount: Int = 2,
    val currentUserRole: String = "",
) {
    val isGroup: Boolean
        get() = kind == "group"

    val displayName: String
        get() = if (isGroup) {
            title.ifBlank { "Nova group" }
        } else {
            otherUser.name.ifBlank { otherUser.username }
        }

    val displaySubtitle: String
        get() = if (isGroup) {
            "$membersCount ${if (membersCount == 1) "member" else "members"}"
        } else {
            "@${otherUser.username}"
        }
}


data class NovaConversationList(
    val conversations: List<NovaConversation>,
    val unreadCount: Int,
)


data class NovaMessagePage(
    val messages: List<NovaMessage>,
    val nextCursor: String?,
)
