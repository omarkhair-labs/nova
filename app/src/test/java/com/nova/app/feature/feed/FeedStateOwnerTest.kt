package com.nova.app.feature.feed

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.feed.data.FeedRepository
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.data.PostRepostRepository
import com.nova.app.feature.posts.data.PostRepostResult
import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaCommentMutation
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import com.nova.app.feature.posts.domain.model.NovaPostPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class FeedStateOwnerTest {
    @Test
    fun `paging skips existing ids but preserves duplicates inside incoming page`() = runBlocking {
        val feed = QueueFeedRepository(
            ApiResult.Success(NovaPostPage(listOf(post(1)), "next")),
            ApiResult.Success(NovaPostPage(listOf(post(1), post(2), post(2), post(3)), null)),
        )
        val owner = FeedStateOwner(
            feed,
            NoOpPostRepository(),
            NoOpPostRepostRepository(),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadFeedNow()
        owner.loadMoreNow("next")

        assertEquals(listOf(1L, 2L, 2L, 3L), owner.state.posts.map { it.id })
        assertNull(owner.state.nextCursor)
        assertFalse(owner.state.isLoadingMore)
    }

    @Test
    fun `401 during first page becomes a session expiry effect instead of a feed error`() = runBlocking {
        val owner = FeedStateOwner(
            QueueFeedRepository(ApiResult.Failure("expired", 401)),
            NoOpPostRepository(),
            NoOpPostRepostRepository(),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadFeedNow()

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertNull(owner.state.errorMessage)
        assertFalse(owner.state.isLoading)
    }

    @Test
    fun `like failure rolls optimistic state back and keeps the error beside the post`() = runBlocking {
        val original = post(5).copy(likesCount = 8, isLiked = false)
        val owner = FeedStateOwner(
            QueueFeedRepository(ApiResult.Success(NovaPostPage(listOf(original), null))),
            LikePostRepository(ApiResult.Failure("offline")),
            NoOpPostRepostRepository(),
            CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadFeedNow()

        owner.toggleLikeNow(original)

        assertEquals(original, owner.state.posts.single())
        assertEquals(5L, owner.state.actionErrorPostId)
        assertTrue(owner.state.actionErrorMessage.orEmpty().contains("offline"))
        assertTrue(owner.state.likingPostIds.isEmpty())
    }

    @Test
    fun `repost success commits server counts and removes a feed-only repost when requested`() = runBlocking {
        val original = post(9).copy(repostsCount = 2, isReposted = true)
        val owner = FeedStateOwner(
            QueueFeedRepository(ApiResult.Success(NovaPostPage(listOf(original), null))),
            NoOpPostRepository(),
            QueuePostRepostRepository(
                ApiResult.Success(PostRepostResult(9, 1, false, false, null))
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadFeedNow()

        owner.toggleRepostNow(original)

        assertTrue(owner.state.posts.isEmpty())
        assertTrue(owner.state.repostingPostIds.isEmpty())
        assertEquals(1, owner.state.profileRefreshVersion)
    }

    private fun post(id: Long) = NovaPost(
        id = id,
        author = NovaPostAuthor(7L, "author", "Author", ""),
        imageUrl = "",
        caption = "",
        createdAt = "",
        isMine = false,
    )
}


private class NoOpPostRepostRepository : PostRepostRepository {
    override suspend fun setPostReposted(
        postId: Long,
        reposted: Boolean,
    ): ApiResult<PostRepostResult> = error("not used")
}


private class QueuePostRepostRepository(
    private val result: ApiResult<PostRepostResult>,
) : PostRepostRepository {
    override suspend fun setPostReposted(postId: Long, reposted: Boolean) = result
}


private class LikePostRepository(
    private val result: ApiResult<NovaPost>,
) : NoOpPostRepository() {
    override suspend fun setLiked(postId: Long, liked: Boolean): ApiResult<NovaPost> = result
}


private class QueueFeedRepository(
    vararg results: ApiResult<NovaPostPage>,
) : FeedRepository {
    private val queue = results.toMutableList()

    override suspend fun feed(cursor: String?): ApiResult<NovaPostPage> = queue.removeAt(0)

    override fun cachedFeed(userId: Long): NovaPostPage? = null
}


private open class NoOpPostRepository : PostRepository {
    override suspend fun personPosts(username: String): ApiResult<List<NovaPost>> = unsupported()
    override suspend fun post(postId: Long): ApiResult<NovaPost> = unsupported()
    override suspend fun createPost(caption: String, imageUri: Uri): ApiResult<NovaPost> = unsupported()
    override suspend fun deletePost(postId: Long): ApiResult<Unit> = unsupported()
    open override suspend fun setLiked(postId: Long, liked: Boolean): ApiResult<NovaPost> = unsupported()
    override suspend fun comments(postId: Long): ApiResult<List<NovaComment>> = unsupported()
    override suspend fun addComment(postId: Long, body: String, parentId: Long?): ApiResult<NovaCommentMutation> = unsupported()
    override suspend fun deleteComment(commentId: Long): ApiResult<NovaPost> = unsupported()
    override suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaPost> = unsupported()

    protected fun <T> unsupported(): T = error("not used")
}
