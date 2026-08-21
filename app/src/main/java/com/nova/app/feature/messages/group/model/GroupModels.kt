package com.nova.app.feature.messages.group.model

import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.posts.domain.model.NovaPostAuthor


data class GroupMember(
    val user: NovaPostAuthor,
    val role: String,
    val joinedAt: String,
)


data class GroupDetail(
    val conversation: NovaConversation,
    val members: List<GroupMember>,
)


data class ManagedGroupDetail(
    val title: String,
    val avatarUrl: String,
    val membersCount: Int,
    val currentUserRole: String,
    val members: List<GroupMember>,
)
