package com.nova.app.navigation

sealed interface NovaRoute {
    data object Welcome : NovaRoute
    data object Terms : NovaRoute
    data object Privacy : NovaRoute
    data object CreateAccount : NovaRoute
    data object Login : NovaRoute
    data object ProfileSetup : NovaRoute
    data object Home : NovaRoute
    data object People : NovaRoute
    data object Reels : NovaRoute
    data object Messages : NovaRoute
    data object Profile : NovaRoute
    data object CreatePost : NovaRoute
    data class PostDetail(val postId: Long) : NovaRoute
    data class PostComments(val postId: Long) : NovaRoute
    data class Person(val username: String) : NovaRoute
    data object EditProfile : NovaRoute
}
