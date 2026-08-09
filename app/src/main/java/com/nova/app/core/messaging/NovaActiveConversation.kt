package com.nova.app.core.messaging


object NovaActiveConversation {
    @Volatile
    private var conversationId: Long? = null

    fun enter(id: Long) {
        conversationId = id
    }

    fun leave(id: Long) {
        if (conversationId == id) {
            conversationId = null
        }
    }

    fun isActive(id: Long?): Boolean {
        return id != null && conversationId == id
    }
}
