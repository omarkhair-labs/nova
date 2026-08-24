package com.nova.app.navigation


/** Typed destinations owned by the MainActivity application host. */
sealed interface AppDestination {
    data object Home : AppDestination
    data object Orbit : AppDestination
    data object Create : AppDestination
    data object Inbox : AppDestination
    data object Reels : AppDestination
    data object Profile : AppDestination
}
