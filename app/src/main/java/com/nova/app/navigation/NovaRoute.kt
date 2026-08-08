package com.nova.app.navigation

sealed interface NovaRoute {
    data object Welcome : NovaRoute
    data object CreateAccount : NovaRoute
    data object Login : NovaRoute
    data object ProfileSetup : NovaRoute
    data object Home : NovaRoute
    data object Profile : NovaRoute
}
