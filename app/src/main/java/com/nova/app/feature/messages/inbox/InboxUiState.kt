package com.nova.app.feature.messages.inbox

import com.nova.app.feature.messages.domain.model.NovaConversation


data class InboxUiState(
    val query: String = "",
    val conversations: List<NovaConversation> = emptyList(),
    val unreadCount: Int = 0,
    val nextCursor: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val unreadUpdateVersion: Int = 0,
    val sessionExpiryVersion: Int = 0,
)
