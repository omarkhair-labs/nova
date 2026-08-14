package com.nova.app.feature.messages

import android.content.Context
import android.content.Intent
import com.nova.app.MessagesActivity


data class MessagesRouteArgs(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val kind: String = "direct",
    val membersCount: Int = 2,
    val currentUserRole: String = "",
)


/** One intent contract for normal inbox fallback and direct special entry. */
object MessagesRouteFactory {
    const val EXTRA_CONVERSATION_ID = "nova_conversation_id"
    const val EXTRA_USERNAME = "nova_conversation_username"
    const val EXTRA_DISPLAY_NAME = "nova_conversation_display_name"
    const val EXTRA_AVATAR_URL = "nova_conversation_avatar_url"
    const val EXTRA_KIND = "nova_conversation_kind"
    const val EXTRA_MEMBERS_COUNT = "nova_conversation_members_count"
    const val EXTRA_CURRENT_USER_ROLE = "nova_conversation_current_user_role"

    fun inboxIntent(context: Context): Intent = Intent(context, MessagesActivity::class.java)

    fun conversationIntent(context: Context, args: MessagesRouteArgs): Intent =
        inboxIntent(context).apply {
            putExtra(EXTRA_CONVERSATION_ID, args.id)
            putExtra(EXTRA_USERNAME, args.username)
            putExtra(EXTRA_DISPLAY_NAME, args.displayName.ifBlank { args.username })
            putExtra(EXTRA_AVATAR_URL, args.avatarUrl)
            putExtra(EXTRA_KIND, args.kind)
            putExtra(EXTRA_MEMBERS_COUNT, args.membersCount)
            putExtra(EXTRA_CURRENT_USER_ROLE, args.currentUserRole)
        }

    fun fromIntent(intent: Intent): MessagesRouteArgs? = intent.getLongExtra(
        EXTRA_CONVERSATION_ID,
        -1L,
    ).takeIf { it > 0L }?.let { conversationId ->
        MessagesRouteArgs(
            id = conversationId,
            username = intent.getStringExtra(EXTRA_USERNAME).orEmpty(),
            displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME).orEmpty(),
            avatarUrl = intent.getStringExtra(EXTRA_AVATAR_URL).orEmpty(),
            kind = intent.getStringExtra(EXTRA_KIND).orEmpty().ifBlank { "direct" },
            membersCount = intent.getIntExtra(EXTRA_MEMBERS_COUNT, 2),
            currentUserRole = intent.getStringExtra(EXTRA_CURRENT_USER_ROLE).orEmpty(),
        )
    }
}
