package com.nova.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


enum class NovaRootTab {
    Home,
    People,
    Profile,
}


/**
 * Returns the single root destination requested by the bottom bar.
 *
 * V5 keeps one owner for primary navigation, so switching People ↔ You no longer
 * needs an intermediate Home hop. The legacy signal remains only as a fallback
 * for deep-link Activities that still need to hand control back to MainActivity.
 */
internal fun rootNavigationPlan(
    current: NovaRootTab,
    requested: NovaRootTab,
): List<NovaRootTab> {
    if (current == requested) return emptyList()
    return listOf(requested)
}


object NovaRootNavigationSignal {
    var requestVersion by mutableIntStateOf(0)
        private set

    var pendingTab by mutableStateOf<NovaRootTab?>(null)
        private set

    fun request(tab: NovaRootTab) {
        pendingTab = tab
        requestVersion += 1
    }

    fun consume(tab: NovaRootTab) {
        if (pendingTab == tab) pendingTab = null
    }
}
