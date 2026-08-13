package com.nova.app.core.reels

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.nova.app.ReelsActivity


object NovaReelsNavigator {
    fun open(context: Context, replaceCurrentActivity: Boolean = false) {
        context.startActivity(Intent(context, ReelsActivity::class.java))
        if (replaceCurrentActivity) {
            (context as? Activity)?.finish()
        }
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
