package com.nova.app.core.messaging

import android.app.Activity
import android.content.Context
import com.nova.app.app.appContainer
import com.nova.app.feature.messages.MessagesRouteArgs
import com.nova.app.feature.messages.MessagesRouteFactory
import com.nova.app.navigation.AppDestination
import com.nova.app.navigation.AppNavigator


object NovaMessagingNavigator {
    private fun appNavigator(context: Context): AppNavigator = context.appContainer.appNavigator

    const val EXTRA_CONVERSATION_ID = MessagesRouteFactory.EXTRA_CONVERSATION_ID
    const val EXTRA_USERNAME = MessagesRouteFactory.EXTRA_USERNAME
    const val EXTRA_DISPLAY_NAME = MessagesRouteFactory.EXTRA_DISPLAY_NAME
    const val EXTRA_AVATAR_URL = MessagesRouteFactory.EXTRA_AVATAR_URL
    const val EXTRA_KIND = MessagesRouteFactory.EXTRA_KIND
    const val EXTRA_MEMBERS_COUNT = MessagesRouteFactory.EXTRA_MEMBERS_COUNT
    const val EXTRA_CURRENT_USER_ROLE = MessagesRouteFactory.EXTRA_CURRENT_USER_ROLE

    fun openInbox(context: Context, replaceCurrentActivity: Boolean = false) {
        if (appNavigator(context).navigate(AppDestination.Messages)) {
            return
        }

        context.startActivity(MessagesRouteFactory.inboxIntent(context))
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
            MessagesRouteFactory.conversationIntent(
                context = context,
                args = MessagesRouteArgs(
                    id = conversationId,
                    username = username,
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    kind = kind,
                    membersCount = membersCount,
                    currentUserRole = currentUserRole,
                ),
            ),
        )
    }
}
