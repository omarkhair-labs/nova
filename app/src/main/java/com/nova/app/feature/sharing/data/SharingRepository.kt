package com.nova.app.feature.sharing.data

import com.nova.app.core.network.ApiResult


/** Stable boundary for Nova's explicit share-dialog actions. */
interface SharingRepository {
    suspend fun sharePost(recipientUsername: String, postId: Long): ApiResult<Unit>

    suspend fun shareReel(recipientUsername: String, reelId: Long): ApiResult<Unit>

    suspend fun shareProfile(recipientUsername: String, profileUsername: String): ApiResult<Unit>

    suspend fun sharePostToConversation(conversationId: Long, postId: Long): ApiResult<Unit>

    suspend fun shareReelToConversation(conversationId: Long, reelId: Long): ApiResult<Unit>

    suspend fun shareProfileToConversation(
        conversationId: Long,
        profileUsername: String,
    ): ApiResult<Unit>

    suspend fun addPostToStory(
        postId: Long,
        caption: String = "",
        audience: String = "followers",
    ): ApiResult<Unit>

    suspend fun addReelToStory(
        reelId: Long,
        caption: String = "",
        audience: String = "followers",
    ): ApiResult<Unit>
}
