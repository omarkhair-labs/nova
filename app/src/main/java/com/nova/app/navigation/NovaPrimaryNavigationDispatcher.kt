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
 * MainActivity's NovaApp installs the active handler. Feature navigators can then
 * request a primary destination without starting another root Activity. When no
 * NovaApp host is installed (for example a legacy/deep-link Activity), callers
 * can fall back to their existing Activity navigation.
 */
object NovaPrimaryNavigationDispatcher {
    private var handler: ((NovaPrimaryDestination) -> Unit)? = null

    fun attach(value: (NovaPrimaryDestination) -> Unit) {
        handler = value
    }

    fun detach(value: (NovaPrimaryDestination) -> Unit) {
        if (handler === value) handler = null
    }

    fun navigate(destination: NovaPrimaryDestination): Boolean {
        val activeHandler = handler ?: return false
        activeHandler(destination)
        return true
    }
}
