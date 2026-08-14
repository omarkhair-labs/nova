package com.nova.app.navigation


/** Typed destinations owned by the MainActivity application host. */
sealed interface AppDestination {
    data object Home : AppDestination
    data object People : AppDestination
    data object Reels : AppDestination
    data object Messages : AppDestination
    data object Profile : AppDestination
}
