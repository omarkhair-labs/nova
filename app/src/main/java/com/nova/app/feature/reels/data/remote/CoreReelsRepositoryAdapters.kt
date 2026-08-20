package com.nova.app.feature.reels.data.remote

import android.content.Context
import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.core.reels.NovaProfileReelsRepository
import com.nova.app.core.reels.NovaReel as CoreNovaReel
import com.nova.app.core.reels.NovaReelAuthor as CoreNovaReelAuthor
import com.nova.app.core.reels.NovaReelComment as CoreNovaReelComment
import com.nova.app.core.reels.NovaReelCommentMutation as CoreNovaReelCommentMutation
import com.nova.app.core.reels.NovaReelPage as CoreNovaReelPage
import com.nova.app.core.reels.NovaReelWatchRepository
import com.nova.app.core.reels.NovaReelsRepository
import com.nova.app.feature.reels.data.ProfileReelsRepository
import com.nova.app.feature.reels.data.ReelWatchRepository
import com.nova.app.feature.reels.data.ReelsRepository
import com.nova.app.feature.reels.domain.model.NovaReel
import com.nova.app.feature.reels.domain.model.NovaReelAuthor
import com.nova.app.feature.reels.domain.model.NovaReelComment
import com.nova.app.feature.reels.domain.model.NovaReelCommentMutation
import com.nova.app.feature.reels.domain.model.NovaReelPage


class CoreReelsRepositoryAdapter(context: Context) : ReelsRepository {
    private val delegate = NovaReelsRepository(context.applicationContext)

    override suspend fun reels(cursor: String?): ApiResult<NovaReelPage> =
        delegate.reels(cursor).mapSuccess(CoreNovaReelPage::toStable)

    override suspend fun createReel(videoUri: Uri, caption: String): ApiResult<NovaReel> =
        delegate.createReel(videoUri, caption).mapSuccess(CoreNovaReel::toStable)

    override suspend fun setLiked(reelId: Long, liked: Boolean): ApiResult<NovaReel> =
        delegate.setLiked(reelId, liked).mapSuccess(CoreNovaReel::toStable)

    override suspend fun setReposted(reelId: Long, reposted: Boolean): ApiResult<NovaReel> =
        delegate.setReposted(reelId, reposted).mapSuccess(CoreNovaReel::toStable)

    override suspend fun comments(reelId: Long): ApiResult<List<NovaReelComment>> =
        delegate.comments(reelId).mapSuccess { rows -> rows.map(CoreNovaReelComment::toStable) }

    override suspend fun addComment(
        reelId: Long,
        body: String,
        parentId: Long?,
    ): ApiResult<NovaReelCommentMutation> =
        delegate.addComment(reelId, body, parentId).mapSuccess(CoreNovaReelCommentMutation::toStable)

    override suspend fun deleteComment(commentId: Long): ApiResult<NovaReel> =
        delegate.deleteComment(commentId).mapSuccess(CoreNovaReel::toStable)

    override suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaReel> =
        delegate.deleteCommentReply(replyId).mapSuccess(CoreNovaReel::toStable)

    override suspend fun deleteReel(reelId: Long): ApiResult<Unit> = delegate.deleteReel(reelId)
}


class CoreProfileReelsRepositoryAdapter(context: Context) : ProfileReelsRepository {
    private val delegate = NovaProfileReelsRepository(context.applicationContext)

    override suspend fun reels(username: String, cursor: String?): ApiResult<NovaReelPage> =
        delegate.reels(username, cursor).mapSuccess(CoreNovaReelPage::toStable)

    override suspend fun repostedReels(username: String, cursor: String?): ApiResult<NovaReelPage> =
        delegate.repostedReels(username, cursor).mapSuccess(CoreNovaReelPage::toStable)
}


class CoreReelWatchRepositoryAdapter(context: Context) : ReelWatchRepository {
    private val delegate = NovaReelWatchRepository(context.applicationContext)

    override suspend fun record(
        reelId: Long,
        sessionId: String,
        watchedMs: Long,
        durationMs: Long,
        maxPositionMs: Long,
    ): ApiResult<Unit> = delegate.record(
        reelId = reelId,
        sessionId = sessionId,
        watchedMs = watchedMs,
        durationMs = durationMs,
        maxPositionMs = maxPositionMs,
    )
}


internal fun CoreNovaReelAuthor.toStable(): NovaReelAuthor = NovaReelAuthor(
    id = id,
    username = username,
    name = name,
    avatarUrl = avatarUrl,
)


internal fun CoreNovaReel.toStable(): NovaReel = NovaReel(
    id = id,
    author = author.toStable(),
    videoUrl = videoUrl,
    caption = caption,
    createdAt = createdAt,
    isMine = isMine,
    likesCount = likesCount,
    commentsCount = commentsCount,
    repostsCount = repostsCount,
    isLiked = isLiked,
    isReposted = isReposted,
    repostedBy = repostedBy?.toStable(),
)


internal fun CoreNovaReelPage.toStable(): NovaReelPage = NovaReelPage(
    reels = reels.map(CoreNovaReel::toStable),
    nextCursor = nextCursor,
)


internal fun CoreNovaReelComment.toStable(): NovaReelComment = NovaReelComment(
    id = id,
    author = author.toStable(),
    body = body,
    createdAt = createdAt,
    isMine = isMine,
    parentId = parentId,
    repliesCount = repliesCount,
    replies = replies.map(CoreNovaReelComment::toStable),
)


internal fun CoreNovaReelCommentMutation.toStable(): NovaReelCommentMutation = NovaReelCommentMutation(
    comment = comment.toStable(),
    reel = reel.toStable(),
)


private inline fun <T, R> ApiResult<T>.mapSuccess(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> ApiResult.Failure(message = message, statusCode = statusCode)
}
