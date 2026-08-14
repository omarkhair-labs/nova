package com.nova.app.feature.messages.conversation

import com.nova.app.core.messaging.NovaConversationPresence
import com.nova.app.core.messaging.NovaRealtimeStatus
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaReplyPreview


enum class PendingMessageStatus { Sending, Failed }


data class PendingMessage(
    val localId: Long,
    val clientId: String,
    val sender: NovaPostAuthor,
    val body: String,
    val imageUri: String,
    val audioPath: String,
    val audioDurationMs: Long?,
    val replyTo: NovaReplyPreview?,
    val createdAt: String,
    val status: PendingMessageStatus,
    val error: String? = null,
)


data class ConversationUiState(
    val messages: List<NovaMessage> = emptyList(),
    val pendingMessages: List<PendingMessage> = emptyList(),
    val nextCursor: String? = null,
    val isLoading: Boolean = true,
    val isLoadingEarlier: Boolean = false,
    val mutatingMessageId: Long? = null,
    val reactingMessageId: Long? = null,
    val errorMessage: String? = null,
    val draft: String = "",
    val draftBeforeEdit: String = "",
    val replyTarget: NovaMessage? = null,
    val editingTarget: NovaMessage? = null,
    val deleteTarget: NovaMessage? = null,
    val actionsForMessageId: Long? = null,
    val isOtherTyping: Boolean = false,
    val otherPresence: NovaConversationPresence? = null,
    val realtimeStatus: NovaRealtimeStatus = NovaRealtimeStatus.Connecting,
    val initialUnreadCaptured: Boolean = false,
    val unreadAnchorMessageId: Long? = null,
    val unreadCountAtOpen: Int = 0,
    val newMessagesAwayCount: Int = 0,
    val conversationReadVersion: Int = 0,
    val sessionExpiryVersion: Int = 0,
    val scrollRequestVersion: Int = 0,
    val scrollRequestAnimated: Boolean = true,
)
