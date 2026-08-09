package com.nova.app.core.push

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


data class NovaPushTarget(
    val kind: String,
    val actorUsername: String,
    val actorName: String,
    val actorAvatarUrl: String,
    val postId: Long?,
    val conversationId: Long?,
)


object NovaPushOpenSignal {
    var pendingTarget by mutableStateOf<NovaPushTarget?>(null)
        private set

    fun offer(intent: Intent?) {
        val kind = intent?.getStringExtra("kind").orEmpty()
        val actorUsername = intent?.getStringExtra("actor_username").orEmpty()
        val actorName = intent?.getStringExtra("actor_name").orEmpty()
        val actorAvatarUrl = intent?.getStringExtra("actor_avatar_url").orEmpty()
        val postId = intent?.getStringExtra("post_id")
            ?.takeIf { it.isNotBlank() }
            ?.toLongOrNull()
        val conversationId = intent?.getStringExtra("conversation_id")
            ?.takeIf { it.isNotBlank() }
            ?.toLongOrNull()

        if (
            kind.isBlank() &&
            actorUsername.isBlank() &&
            postId == null &&
            conversationId == null
        ) {
            return
        }

        pendingTarget = NovaPushTarget(
            kind = kind,
            actorUsername = actorUsername,
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl,
            postId = postId,
            conversationId = conversationId,
        )
    }

    fun consume() {
        pendingTarget = null
    }
}
