package com.nova.app.core.reels

import android.content.Context
import android.content.Intent
import com.nova.app.ReelsActivity


object NovaReelsNavigator {
    fun open(context: Context) {
        context.startActivity(Intent(context, ReelsActivity::class.java))
    }

    fun openProfile(
        context: Context,
        username: String,
        initialReelId: Long,
    ) {
        val cleanUsername = username.trim().lowercase()
        if (cleanUsername.isBlank() || initialReelId <= 0L) return

        context.startActivity(
            Intent(context, ReelsActivity::class.java)
                .putExtra(ReelsActivity.EXTRA_PROFILE_USERNAME, cleanUsername)
                .putExtra(ReelsActivity.EXTRA_INITIAL_REEL_ID, initialReelId)
        )
    }
}
