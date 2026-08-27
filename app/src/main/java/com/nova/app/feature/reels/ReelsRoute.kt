package com.nova.app.feature.reels

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import com.nova.app.ReelsActivity


sealed interface ReelsRouteArgs {
    data object Root : ReelsRouteArgs

    data class Profile(
        val username: String,
        val initialReelId: Long,
    ) : ReelsRouteArgs
}


/** One entry contract for the MainActivity root and the profile-Reel Activity. */
object ReelsRouteFactory {
    const val EXTRA_PROFILE_USERNAME = "profile_username"
    const val EXTRA_INITIAL_REEL_ID = "initial_reel_id"

    fun rootIntent(context: Context): Intent = Intent(context, ReelsActivity::class.java)

    fun profileIntent(context: Context, args: ReelsRouteArgs.Profile): Intent =
        rootIntent(context)
            .putExtra(EXTRA_PROFILE_USERNAME, args.username)
            .putExtra(EXTRA_INITIAL_REEL_ID, args.initialReelId)

    fun fromIntent(intent: Intent): ReelsRouteArgs {
        val username = intent.getStringExtra(EXTRA_PROFILE_USERNAME)
            .orEmpty()
            .trim()
            .lowercase()
        val initialReelId = intent.getLongExtra(EXTRA_INITIAL_REEL_ID, -1L)
        return if (username.isNotBlank() && initialReelId > 0L) {
            ReelsRouteArgs.Profile(username, initialReelId)
        } else {
            ReelsRouteArgs.Root
        }
    }
}


@Composable
fun ReelsRoute(
    route: ReelsRouteArgs,
    onFinish: () -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
    onPersonClick: (String) -> Unit,
) {
    when (route) {
        ReelsRouteArgs.Root -> ReelsScreen(
            onFinish = onFinish,
            onHomeClick = onHomeClick,
            onOrbitClick = onOrbitClick,
            onCreateClick = onCreateClick,
            onProfileClick = onProfileClick,
            onPersonClick = onPersonClick,
        )

        is ReelsRouteArgs.Profile -> ProfileReelsViewerScreen(
            username = route.username,
            initialReelId = route.initialReelId,
            onFinish = onFinish,
            onPersonClick = onPersonClick,
        )
    }
}
