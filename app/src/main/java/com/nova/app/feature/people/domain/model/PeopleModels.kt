package com.nova.app.feature.people.domain.model

import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState


data class NovaPerson(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
    val bio: String = "",
    val location: String = "",
    val link: String = "",
    val interests: List<String> = emptyList(),
    val profileTheme: String = "violet",
    val showOrbit: Boolean = true,
    val isVerified: Boolean = false,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int,
    val isFollowing: Boolean,
)


data class NovaPersonPage(
    val people: List<NovaPerson>,
    val nextCursor: String?,
    val privacyByUserId: Map<Long, NovaPersonPrivacyState> = emptyMap(),
)


data class NovaProfilePostPage(
    val posts: List<NovaPost>,
    val nextCursor: String?,
)
