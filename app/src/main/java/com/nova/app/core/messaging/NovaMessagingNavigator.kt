package com.nova.app.core.messaging

import android.content.Context
import android.content.Intent
import com.nova.app.MessagesActivity


object NovaMessagingNavigator {
    const val EXTRA_CONVERSATION_ID = "nova_conversation_id"
    const val EXTRA_USERNAME = "nova_conversation_username"
    const val EXTRA_DISPLAY_NAME = "nova_conversation_display_name"
    const val EXTRA_AVATAR_URL = "nova_conversation_avatar_url"

    fun openInbox(context: Context) {
        context.startActivity(Intent(context, MessagesActivity::class.java))
    }

    fun openConversation(context: Context, conversation: NovaConversation) {
        openConversation(
            context = context,
            conversationId = conversation.id,
            username = conversation.otherUser.username,
            displayName = conversation.otherUser.name.ifBlank { conversation.otherUser.username },
            avatarUrl = conversation.otherUser.avatarUrl,
        )
    }

    fun openConversation(
        context: Context,
        conversationId: Long,
        username: String,
        displayName: String,
        avatarUrl: String,
    ) {
        context.startActivity(
            Intent(context, MessagesActivity::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_USERNAME, username)
                putExtra(EXTRA_DISPLAY_NAME, displayName.ifBlank { username })
                putExtra(EXTRA_AVATAR_URL, avatarUrl)
            }
        )
    }
}
