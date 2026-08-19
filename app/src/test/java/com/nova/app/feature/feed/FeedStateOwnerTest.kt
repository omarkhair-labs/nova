package com.nova.app.feature.feed

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaComment
import com.nova.app.core.network.NovaCommentMutation
import com.nova.app.core.network.NovaPost
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.core.network.NovaPostPage
import com.nova.app.feature.feed.data.FeedRepository
import com.nova.app.feature.posts.data.PostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class FeedStateOwnerTest {
    @Test
    fun `paging skips existing ids but preserves duplicates inside incoming page`() = runBlocking {
        val feed = QueueFeedRepository(
            ApiResult.Success(NovaPostPage(listOf(post(1)), "next")),
            ApiResult.Success(NovaPostPage(listOf(post(1), post(2), post(2), post(3)), null)),
        )
        val owner = FeedStateOwner(feed, NoOpPostRepository(), CoroutineScope(Dispatchers.Unconfined))

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
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadFeedNow()

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertNull(owner.state.errorMessage)
        assertFalse(owner.state.isLoading)
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


private class QueueFeedRepository(
    vararg results: ApiResult<NovaPostPage>,
) : FeedRepository {
    private val queue = ArrayDeque(results.toList())

    override suspend fun feed(cursor: String?): ApiResult<NovaPostPage> = queue.removeFirst()

    override fun cachedFeed(userId: Long): NovaPostPage? = null
}


private class NoOpPostRepository : PostRepository {
    override suspend fun personPosts(username: String): ApiResult<List<NovaPost>> = unsupported()
    override suspend fun post(postId: Long): ApiResult<NovaPost> = unsupported()
    override suspend fun createPost(caption: String, imageUri: Uri): ApiResult<NovaPost> = unsupported()
    override suspend fun deletePost(postId: Long): ApiResult<Unit> = unsupported()
    override suspend fun setLiked(postId: Long, liked: Boolean): ApiResult<NovaPost> = unsupported()
    override suspend fun comments(postId: Long): ApiResult<List<NovaComment>> = unsupported()
    override suspend fun addComment(postId: Long, body: String, parentId: Long?): ApiResult<NovaCommentMutation> = unsupported()
    override suspend fun deleteComment(commentId: Long): ApiResult<NovaPost> = unsupported()
    override suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaPost> = unsupported()

    private fun <T> unsupported(): T = error("not used")
}
