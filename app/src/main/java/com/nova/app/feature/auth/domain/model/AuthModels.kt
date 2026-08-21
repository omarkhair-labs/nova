package com.nova.app.feature.auth.domain.model


data class NovaUser(
    val id: Long,
    val email: String,
    val username: String,
    val name: String,
    val avatarUrl: String,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
)


data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: NovaUser,
)
