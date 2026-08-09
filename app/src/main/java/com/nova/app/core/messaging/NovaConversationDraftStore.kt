package com.nova.app.core.messaging

import android.content.Context


class NovaConversationDraftStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(conversationId: Long): String {
        if (conversationId <= 0L) return ""
        return prefs.getString(key(conversationId), "").orEmpty()
    }

    fun save(conversationId: Long, body: String) {
        if (conversationId <= 0L) return
        val trimmed = body.take(MAX_DRAFT_LENGTH)
        if (trimmed.isBlank()) {
            remove(conversationId)
            return
        }
        prefs.edit().putString(key(conversationId), trimmed).apply()
    }

    fun remove(conversationId: Long) {
        if (conversationId <= 0L) return
        prefs.edit().remove(key(conversationId)).apply()
    }

    private fun key(conversationId: Long) = "conversation_$conversationId"

    private companion object {
        const val PREFS_NAME = "nova_message_drafts"
        const val MAX_DRAFT_LENGTH = 2_000
    }
}
