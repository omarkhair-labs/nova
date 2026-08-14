package com.nova.app.feature.messages.conversation


/** Local draft boundary used by the conversation state owner. */
interface ConversationDraftStore {
    fun load(conversationId: Long): String

    fun save(conversationId: Long, draft: String)

    fun remove(conversationId: Long)
}
