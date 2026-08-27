package com.nova.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


enum class NovaRootTab {
    Home,
    Orbit,
    Create,
    Profile,
}


/**
 * Returns the callback sequence needed to move between Nova's nested social roots
 * without letting People/Profile accumulate in the Nav3 back stack.
 *
 * Home, Orbit, Create and Profile share the social Nav3 owner. Inbox is the
 * existing messaging overlay, so every transition inside this tree resets to
 * the requested root instead of accumulating primary destinations.
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

    var tonightRequestVersion by mutableIntStateOf(0)
        private set

    var pendingTonight by mutableStateOf(false)
        private set

    var personRequestVersion by mutableIntStateOf(0)
        private set

    var pendingPersonUsername by mutableStateOf<String?>(null)
        private set

    fun request(tab: NovaRootTab) {
        pendingTab = tab
        requestVersion += 1
    }

    fun consume(tab: NovaRootTab) {
        if (pendingTab == tab) pendingTab = null
    }

    fun requestTonight() {
        pendingTonight = true
        tonightRequestVersion += 1
    }

    fun consumeTonight() {
        pendingTonight = false
    }

    fun requestPerson(username: String) {
        val normalized = username.trim().lowercase()
        if (normalized.isBlank()) return
        pendingPersonUsername = normalized
        personRequestVersion += 1
    }

    fun consumePerson(username: String) {
        if (pendingPersonUsername == username) pendingPersonUsername = null
    }
}
