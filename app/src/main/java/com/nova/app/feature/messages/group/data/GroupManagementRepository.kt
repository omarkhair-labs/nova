package com.nova.app.feature.messages.group.data

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.group.model.ManagedGroupDetail


interface GroupManagementRepository {
    suspend fun detail(conversationId: Long): ApiResult<ManagedGroupDetail>

    suspend fun rename(
        conversationId: Long,
        title: String,
    ): ApiResult<ManagedGroupDetail>

    suspend fun updateAvatar(
        conversationId: Long,
        uri: Uri,
    ): ApiResult<ManagedGroupDetail>

    suspend fun removeAvatar(conversationId: Long): ApiResult<ManagedGroupDetail>

    suspend fun setRole(
        conversationId: Long,
        username: String,
        role: String,
    ): ApiResult<ManagedGroupDetail>
}
