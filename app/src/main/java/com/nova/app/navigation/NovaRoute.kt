package com.nova.app.navigation

sealed interface NovaRoute {
    data object Welcome : NovaRoute
    data object CreateAccount : NovaRoute
    data object Login : NovaRoute
    data object ProfileSetup : NovaRoute
    data object Home : NovaRoute
    data object Notifications : NovaRoute
    data object CreatePost : NovaRoute
    data class PostDetail(val postId: Long) : NovaRoute
    data class PostComments(val postId: Long) : NovaRoute
    data object People : NovaRoute
    data class Person(val username: String) : NovaRoute
    data object Profile : NovaRoute
    data object EditProfile : NovaRoute
}
