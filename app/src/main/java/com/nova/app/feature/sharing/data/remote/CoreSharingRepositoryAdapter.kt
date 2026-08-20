package com.nova.app.feature.sharing.data.remote

import android.content.Context
import com.nova.app.core.network.ApiResult
import com.nova.app.core.sharing.NovaSharingRepository
import com.nova.app.feature.sharing.data.SharingRepository


class CoreSharingRepositoryAdapter(context: Context) : SharingRepository {
    private val delegate = NovaSharingRepository(context.applicationContext)

    override suspend fun sharePost(recipientUsername: String, postId: Long): ApiResult<Unit> =
        delegate.sharePost(recipientUsername, postId)

    override suspend fun shareReel(recipientUsername: String, reelId: Long): ApiResult<Unit> =
        delegate.shareReel(recipientUsername, reelId)

    override suspend fun shareProfile(recipientUsername: String, profileUsername: String): ApiResult<Unit> =
        delegate.shareProfile(recipientUsername, profileUsername)

    override suspend fun sharePostToConversation(conversationId: Long, postId: Long): ApiResult<Unit> =
        delegate.sharePostToConversation(conversationId, postId)

    override suspend fun shareReelToConversation(conversationId: Long, reelId: Long): ApiResult<Unit> =
        delegate.shareReelToConversation(conversationId, reelId)

    override suspend fun shareProfileToConversation(
        conversationId: Long,
        profileUsername: String,
    ): ApiResult<Unit> = delegate.shareProfileToConversation(conversationId, profileUsername)

    override suspend fun addPostToStory(
        postId: Long,
        caption: String,
        audience: String,
    ): ApiResult<Unit> = delegate.addPostToStory(postId, caption, audience)

    override suspend fun addReelToStory(
        reelId: Long,
        caption: String,
        audience: String,
    ): ApiResult<Unit> = delegate.addReelToStory(reelId, caption, audience)
}
