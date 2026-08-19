package com.nova.app.feature.messages.group.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.messages.group.model.GroupDetail


interface GroupMembershipRepository {
    suspend fun createGroup(
        title: String,
        usernames: List<String>,
    ): ApiResult<NovaConversation>

    suspend fun detail(conversationId: Long): ApiResult<GroupDetail>

    suspend fun addMembers(
        conversationId: Long,
        usernames: List<String>,
    ): ApiResult<GroupDetail>

    suspend fun removeMember(
        conversationId: Long,
        username: String,
    ): ApiResult<GroupDetail?>

    suspend fun leaveGroup(conversationId: Long): ApiResult<Boolean>

    suspend fun deleteGroup(conversationId: Long): ApiResult<Unit>
}
