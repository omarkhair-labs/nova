package com.nova.app.feature.reels.data

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.reels.domain.model.NovaReel
import com.nova.app.feature.reels.domain.model.NovaReelComment
import com.nova.app.feature.reels.domain.model.NovaReelCommentMutation
import com.nova.app.feature.reels.domain.model.NovaReelPage


interface ReelsRepository {
    suspend fun reels(cursor: String? = null): ApiResult<NovaReelPage>

    suspend fun createReel(videoUri: Uri, caption: String): ApiResult<NovaReel>

    suspend fun createReel(
        videoUri: Uri,
        caption: String,
        clientPublishId: String,
        onProgress: (Int?) -> Unit = {},
        expectedUserId: Long? = null,
    ): ApiResult<NovaReel> = createReel(videoUri, caption)

    suspend fun setLiked(reelId: Long, liked: Boolean): ApiResult<NovaReel>

    suspend fun setReposted(reelId: Long, reposted: Boolean): ApiResult<NovaReel>

    suspend fun comments(reelId: Long): ApiResult<List<NovaReelComment>>

    suspend fun addComment(
        reelId: Long,
        body: String,
        parentId: Long? = null,
    ): ApiResult<NovaReelCommentMutation>

    suspend fun deleteComment(commentId: Long): ApiResult<NovaReel>

    suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaReel>

    suspend fun deleteReel(reelId: Long): ApiResult<Unit>
}


interface ProfileReelsRepository {
    suspend fun reels(
        username: String,
        cursor: String? = null,
    ): ApiResult<NovaReelPage>

    suspend fun repostedReels(
        username: String,
        cursor: String? = null,
    ): ApiResult<NovaReelPage>
}


interface ReelWatchRepository {
    suspend fun record(
        reelId: Long,
        sessionId: String,
        watchedMs: Long,
        durationMs: Long,
        maxPositionMs: Long,
    ): ApiResult<Unit>
}
