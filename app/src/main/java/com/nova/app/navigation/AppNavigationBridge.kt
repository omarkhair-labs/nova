package com.nova.app.navigation


/**
 * Application-scoped transport between feature entry points and the active app host.
 *
 * The bridge consumes navigation only while MainActivity is resumed and has an
 * attached handler. Special-entry Activities therefore retain their existing
 * fallback behavior while they are visible.
 */
class AppNavigationBridge : AppNavigator {
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
