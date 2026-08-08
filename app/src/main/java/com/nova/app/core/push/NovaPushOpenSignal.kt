package com.nova.app.core.push

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


data class NovaPushTarget(
    val kind: String,
    val actorUsername: String,
    val postId: Long?,
)


object NovaPushOpenSignal {
    var pendingTarget by mutableStateOf<NovaPushTarget?>(null)
        private set

    fun offer(intent: Intent?) {
        val kind = intent?.getStringExtra("kind").orEmpty()
        val actorUsername = intent?.getStringExtra("actor_username").orEmpty()
        val postId = intent?.getStringExtra("post_id")
            ?.takeIf { it.isNotBlank() }
            ?.toLongOrNull()

        if (kind.isBlank() && actorUsername.isBlank() && postId == null) return

        pendingTarget = NovaPushTarget(
            kind = kind,
            actorUsername = actorUsername,
            postId = postId,
        )
    }

    fun consume() {
        pendingTarget = null
    }
}
