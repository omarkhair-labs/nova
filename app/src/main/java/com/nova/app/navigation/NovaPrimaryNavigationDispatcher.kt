package com.nova.app.navigation


/**
 * Compatibility bridge for app-level primary navigation.
 *
 * MainActivity owns the active handler while it is resumed. Feature navigators
 * can then request a primary destination without starting another root Activity.
 * When MainActivity is paused (for example beneath a deep-link Activity), callers
 * fall back to their existing Activity navigation instead of routing behind the
 * visible screen.
 */
object NovaPrimaryNavigationDispatcher : AppNavigator {
    private var handler: ((AppDestination) -> Unit)? = null
    private var hostActive: Boolean = false

    fun attach(value: (AppDestination) -> Unit) {
        handler = value
    }

    fun detach(value: (AppDestination) -> Unit) {
        if (handler === value) {
            handler = null
            hostActive = false
        }
    }

    fun setHostActive(active: Boolean) {
        hostActive = active
    }

    override fun navigate(destination: AppDestination): Boolean {
        if (!hostActive) return false
        val activeHandler = handler ?: return false
        activeHandler(destination)
        return true
    }
}
