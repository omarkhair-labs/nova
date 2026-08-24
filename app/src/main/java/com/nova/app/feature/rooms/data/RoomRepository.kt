package com.nova.app.feature.rooms.data

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.rooms.domain.model.RoomDetail
import com.nova.app.feature.rooms.domain.model.RoomItem
import com.nova.app.feature.rooms.domain.model.RoomItemPage
import com.nova.app.feature.rooms.domain.model.RoomSummary
import com.nova.app.feature.rooms.domain.model.RoomTonightSnapshot


interface RoomRepository {
    suspend fun rooms(): ApiResult<List<RoomSummary>>

    suspend fun rooms(view: String): ApiResult<List<RoomSummary>> = rooms()

    suspend fun joinRoom(conversationId: Long): ApiResult<RoomSummary> =
        ApiResult.Failure("Room joining is unavailable.")

    suspend fun followRoom(conversationId: Long, enabled: Boolean): ApiResult<RoomSummary> =
        ApiResult.Failure("Room following is unavailable.")

    suspend fun room(conversationId: Long): ApiResult<RoomDetail>

    suspend fun items(
        conversationId: Long,
        kind: String? = null,
        before: Long? = null,
        limit: Int = 30,
    ): ApiResult<RoomItemPage>

    suspend fun createItem(
        conversationId: Long,
        kind: String,
        title: String = "",
        body: String = "",
        url: String = "",
        scheduledFor: String? = null,
        mediaUri: Uri? = null,
    ): ApiResult<RoomItem>

    suspend fun roomTonight(utcOffsetMinutes: Int): ApiResult<RoomTonightSnapshot>

    suspend fun updateDescription(
        conversationId: Long,
        description: String,
    ): ApiResult<RoomDetail>

    suspend fun updateProfile(
        conversationId: Long,
        description: String,
        isPublic: Boolean,
        topics: List<String>,
    ): ApiResult<RoomDetail> = updateDescription(conversationId, description)

    suspend fun setReminder(
        conversationId: Long,
        itemId: Long,
        enabled: Boolean,
    ): ApiResult<RoomItem> = ApiResult.Failure("Room reminders are unavailable.")
}
