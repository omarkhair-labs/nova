package com.nova.app.core.messaging

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.nova.app.MessagesActivity
import com.nova.app.navigation.NovaPrimaryDestination
import com.nova.app.navigation.NovaPrimaryNavigationDispatcher


object NovaMessagingNavigator {
    const val EXTRA_CONVERSATION_ID = "nova_conversation_id"
    const val EXTRA_USERNAME = "nova_conversation_username"
    const val EXTRA_DISPLAY_NAME = "nova_conversation_display_name"
    const val EXTRA_AVATAR_URL = "nova_conversation_avatar_url"
    const val EXTRA_KIND = "nova_conversation_kind"
    const val EXTRA_MEMBERS_COUNT = "nova_conversation_members_count"
    const val EXTRA_CURRENT_USER_ROLE = "nova_conversation_current_user_role"

    fun openInbox(context: Context, replaceCurrentActivity: Boolean = false) {
        if (NovaPrimaryNavigationDispatcher.navigate(NovaPrimaryDestination.Messages)) {
            return
        }

        context.startActivity(Intent(context, MessagesActivity::class.java))
        if (replaceCurrentActivity) {
            (context as? Activity)?.finish()
        }
    }

    fun openConversation(context: Context, conversation: NovaConversation) {
        openConversation(
            context = context,
            conversationId = conversation.id,
            username = if (conversation.isGroup) "group" else conversation.otherUser.username,
            displayName = conversation.displayName,
            avatarUrl = if (conversation.isGroup) {
                conversation.membersPreview.firstOrNull()?.avatarUrl.orEmpty()
            } else {
                conversation.otherUser.avatarUrl
            },
            kind = conversation.kind,
            membersCount = conversation.membersCount,
            currentUserRole = conversation.currentUserRole,
        )
    }

    fun openConversation(
        context: Context,
        conversationId: Long,
        username: String,
        displayName: String,
        avatarUrl: String,
        kind: String = "direct",
        membersCount: Int = if (kind == "group") 0 else 2,
        currentUserRole: String = "",
    ) {
        context.startActivity(
            Intent(context, MessagesActivity::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_USERNAME, username)
                putExtra(EXTRA_DISPLAY_NAME, displayName.ifBlank { username })
                putExtra(EXTRA_AVATAR_URL, avatarUrl)
                putExtra(EXTRA_KIND, kind)
                putExtra(EXTRA_MEMBERS_COUNT, membersCount)
                putExtra(EXTRA_CURRENT_USER_ROLE, currentUserRole)
            }
        )
    }
}
