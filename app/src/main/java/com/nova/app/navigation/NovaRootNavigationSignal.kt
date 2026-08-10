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
 * Returns the callback sequence needed to move between Nova's root tabs without
 * letting People/Profile accumulate on top of each other in the Nav3 back stack.
 *
 * Home is the canonical Android root. Moving between the two secondary roots
 * first resets to Home, then opens the requested root. Moving from Home to a
 * secondary root needs only the target callback.
 */
internal fun rootNavigationPlan(
    current: NovaRootTab,
    requested: NovaRootTab,
): List<NovaRootTab> {
    if (current == requested) return emptyList()

    return when (requested) {
        NovaRootTab.Home -> listOf(NovaRootTab.Home)
        NovaRootTab.People -> if (current == NovaRootTab.Home) {
            listOf(NovaRootTab.People)
        } else {
            listOf(NovaRootTab.Home, NovaRootTab.People)
        }
        NovaRootTab.Profile -> if (current == NovaRootTab.Home) {
            listOf(NovaRootTab.Profile)
        } else {
            listOf(NovaRootTab.Home, NovaRootTab.Profile)
        }
    }
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
