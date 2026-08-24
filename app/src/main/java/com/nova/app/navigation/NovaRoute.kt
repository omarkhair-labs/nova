package com.nova.app.navigation

sealed interface NovaRoute {
    data object Welcome : NovaRoute
    data object Terms : NovaRoute
    data object Privacy : NovaRoute
    data object CreateAccount : NovaRoute
    data object Login : NovaRoute
    data object ProfileSetup : NovaRoute
    data object Home : NovaRoute
    data object Orbit : NovaRoute
    data object Create : NovaRoute
    data object Pulse : NovaRoute
    data object Tonight : NovaRoute
    data object CreatePost : NovaRoute
    data class PostDetail(val postId: Long) : NovaRoute
    data class PostComments(val postId: Long) : NovaRoute
    data object People : NovaRoute
    data class Person(val username: String) : NovaRoute
    data object Profile : NovaRoute
    data object EditProfile : NovaRoute
}
