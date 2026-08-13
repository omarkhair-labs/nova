package com.nova.app.navigation


/**
 * App-host navigation contract.
 *
 * Returns true only when the active MainActivity host consumed the destination;
 * feature navigators retain their existing special-Activity fallback otherwise.
 */
fun interface AppNavigator {
    fun navigate(destination: AppDestination): Boolean
}
