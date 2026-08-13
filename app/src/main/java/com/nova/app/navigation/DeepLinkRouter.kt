package com.nova.app.navigation

import android.content.Intent


internal data class NovaPushIntentData(
    val kind: String = "",
    val conversationId: String? = null,
    val conversationKind: String = "",
    val groupTitle: String = "",
    val actorUsername: String = "",
    val actorName: String = "",
    val actorAvatarUrl: String = "",
    val reelId: String? = null,
    val reelAuthorUsername: String = "",
)


internal sealed interface NovaDeepLinkDecision {
    data class Conversation(
        val conversationId: Long,
        val username: String,
        val displayName: String,
        val avatarUrl: String,
        val kind: String,
        val membersCount: Int,
    ) : NovaDeepLinkDecision

    data class ProfileReel(
        val username: String,
        val initialReelId: Long,
    ) : NovaDeepLinkDecision

    data object InAppSignal : NovaDeepLinkDecision
}


internal object DeepLinkRouter {
    private val reelActivityKinds = setOf(
        "reel_like",
        "reel_comment",
        "reel_repost",
        "reel_reply",
    )

    fun decide(intent: Intent?): NovaDeepLinkDecision = decide(
        NovaPushIntentData(
            kind = intent?.getStringExtra("kind").orEmpty(),
            conversationId = intent?.getStringExtra("conversation_id"),
            conversationKind = intent?.getStringExtra("conversation_kind").orEmpty(),
            groupTitle = intent?.getStringExtra("group_title").orEmpty(),
            actorUsername = intent?.getStringExtra("actor_username").orEmpty(),
            actorName = intent?.getStringExtra("actor_name").orEmpty(),
            actorAvatarUrl = intent?.getStringExtra("actor_avatar_url").orEmpty(),
            reelId = intent?.getStringExtra("reel_id"),
            reelAuthorUsername = intent?.getStringExtra("reel_author_username").orEmpty(),
        )
    )

    fun decide(data: NovaPushIntentData): NovaDeepLinkDecision {
        if (data.kind == "message") {
            val conversationId = data.conversationId?.toLongOrNull()
            val conversationKind = data.conversationKind.ifBlank { "direct" }
            if (conversationId != null && conversationId > 0L) {
                if (conversationKind == "group") {
                    return NovaDeepLinkDecision.Conversation(
                        conversationId = conversationId,
                        username = "group",
                        displayName = data.groupTitle.ifBlank { "Nova group" },
                        avatarUrl = "",
                        kind = "group",
                        membersCount = 0,
                    )
                }

                if (data.actorUsername.isNotBlank()) {
                    return NovaDeepLinkDecision.Conversation(
                        conversationId = conversationId,
                        username = data.actorUsername,
                        displayName = data.actorName,
                        avatarUrl = data.actorAvatarUrl,
                        kind = "direct",
                        membersCount = 2,
                    )
                }
            }
        }

        if (data.kind in reelActivityKinds) {
            val reelId = data.reelId?.toLongOrNull()
            val authorUsername = data.reelAuthorUsername.trim().lowercase()
            if (reelId != null && reelId > 0L && authorUsername.isNotBlank()) {
                return NovaDeepLinkDecision.ProfileReel(
                    username = authorUsername,
                    initialReelId = reelId,
                )
            }
        }

        return NovaDeepLinkDecision.InAppSignal
    }
}
