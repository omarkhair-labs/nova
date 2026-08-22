package com.nova.app.feature.rooms.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.rooms.domain.model.RoomDetail
import com.nova.app.feature.rooms.domain.model.RoomItemPage
import com.nova.app.feature.rooms.domain.model.RoomSummary


interface RoomRepository {
    suspend fun rooms(): ApiResult<List<RoomSummary>>

    suspend fun room(conversationId: Long): ApiResult<RoomDetail>

    suspend fun items(
        conversationId: Long,
        kind: String? = null,
        before: Long? = null,
        limit: Int = 30,
    ): ApiResult<RoomItemPage>

    suspend fun updateDescription(
        conversationId: Long,
        description: String,
    ): ApiResult<RoomDetail>
}
