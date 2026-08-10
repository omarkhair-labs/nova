package com.nova.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


sealed interface NovaContentTarget {
    data class Post(val postId: Long) : NovaContentTarget
    data class Profile(val username: String) : NovaContentTarget
}


/**
 * Bridges content taps from secondary Android activities/dialogs back into the
 * single Nova root Nav3 stack without creating parallel Post/Profile activities.
 */
object NovaContentNavigationSignal {
    var requestVersion by mutableIntStateOf(0)
        private set

    var pendingTarget by mutableStateOf<NovaContentTarget?>(null)
        private set

    fun request(target: NovaContentTarget) {
        pendingTarget = target
        requestVersion += 1
    }

    fun consume(target: NovaContentTarget) {
        if (pendingTarget == target) pendingTarget = null
    }
}
