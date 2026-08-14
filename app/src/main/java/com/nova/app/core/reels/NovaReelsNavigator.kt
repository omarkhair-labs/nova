package com.nova.app.core.reels

import android.app.Activity
import android.content.Context
import com.nova.app.app.appContainer
import com.nova.app.feature.reels.ReelsRouteArgs
import com.nova.app.feature.reels.ReelsRouteFactory
import com.nova.app.navigation.AppDestination
import com.nova.app.navigation.AppNavigator


object NovaReelsNavigator {
    private fun appNavigator(context: Context): AppNavigator = context.appContainer.appNavigator

    fun open(context: Context, replaceCurrentActivity: Boolean = false) {
        if (appNavigator(context).navigate(AppDestination.Reels)) {
            return
        }

        context.startActivity(ReelsRouteFactory.rootIntent(context))
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
            ReelsRouteFactory.profileIntent(
                context,
                ReelsRouteArgs.Profile(cleanUsername, initialReelId),
            ),
        )
    }
}
