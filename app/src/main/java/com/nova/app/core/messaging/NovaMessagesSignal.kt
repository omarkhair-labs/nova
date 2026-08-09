package com.nova.app.core.messaging

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


object NovaMessagesSignal {
    var openMessagesVersion by mutableIntStateOf(0)
        private set

    var conversationVersion by mutableIntStateOf(0)
        private set

    var inboxRefreshVersion by mutableIntStateOf(0)
        private set

    var pendingConversation by mutableStateOf<NovaConversation?>(null)
        private set

    var unreadCount by mutableIntStateOf(0)
        private set

    fun requestMessages() {
        openMessagesVersion += 1
    }

    fun requestConversation(conversation: NovaConversation) {
        pendingConversation = conversation
        conversationVersion += 1
    }

    fun requestInboxRefresh() {
        inboxRefreshVersion += 1
    }

    fun consumeConversation() {
        pendingConversation = null
    }

    fun updateUnreadCount(value: Int) {
        unreadCount = value.coerceAtLeast(0)
    }

    fun incrementUnreadCount() {
        unreadCount += 1
    }

    fun reset() {
        pendingConversation = null
        unreadCount = 0
        inboxRefreshVersion += 1
    }
}
