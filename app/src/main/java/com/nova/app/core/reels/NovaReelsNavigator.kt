package com.nova.app.core.reels

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.nova.app.ReelsActivity
import com.nova.app.navigation.NovaPrimaryDestination
import com.nova.app.navigation.NovaPrimaryNavigationDispatcher


object NovaReelsNavigator {
    fun open(context: Context, replaceCurrentActivity: Boolean = false) {
        if (NovaPrimaryNavigationDispatcher.navigate(NovaPrimaryDestination.Reels)) {
            if (replaceCurrentActivity) {
                (context as? Activity)?.finish()
            }
            return
        }

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
