package com.nova.app.navigation


enum class NovaPrimaryDestination {
    Home,
    People,
    Reels,
    Messages,
    Profile,
}


/**
 * Bridge for app-level primary navigation.
 *
 * MainActivity owns the active handler while it is resumed. Feature navigators
 * can then request a primary destination without starting another root Activity.
 * When MainActivity is paused (for example beneath a deep-link Activity), callers
 * fall back to their existing Activity navigation instead of routing behind the
 * visible screen.
 */
object NovaPrimaryNavigationDispatcher {
    private var handler: ((NovaPrimaryDestination) -> Unit)? = null
    private var hostActive: Boolean = false

    fun attach(value: (NovaPrimaryDestination) -> Unit) {
        handler = value
    }

    fun detach(value: (NovaPrimaryDestination) -> Unit) {
        if (handler === value) {
            handler = null
            hostActive = false
        }
    }

    fun setHostActive(active: Boolean) {
        hostActive = active
    }

    fun navigate(destination: NovaPrimaryDestination): Boolean {
        if (!hostActive) return false
        val activeHandler = handler ?: return false
        activeHandler(destination)
        return true
    }
}
