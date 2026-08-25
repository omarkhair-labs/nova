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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
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

    @Test
    fun `like failure preserves a repost that succeeds while like is in flight`() = runBlocking {
        val original = post(12).copy(likesCount = 8, repostsCount = 2)
        val reposter = NovaPostAuthor(88L, "reposter", "Reposter", "")
        val likeResult = CompletableDeferred<ApiResult<NovaPost>>()
        val owner = FeedStateOwner(
            QueueFeedRepository(ApiResult.Success(NovaPostPage(listOf(original), null))),
            DeferredLikePostRepository(likeResult),
            QueuePostRepostRepository(
                ApiResult.Success(PostRepostResult(12L, 3, true, true, reposter)),
            ),
            CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadFeedNow()

        val likeJob = launch(start = CoroutineStart.UNDISPATCHED) {
            owner.toggleLikeNow(original)
        }
        owner.toggleRepostNow(owner.state.posts.single())
        likeResult.complete(ApiResult.Failure("offline"))
        likeJob.join()

        val finalPost = owner.state.posts.single()
        assertFalse(finalPost.isLiked)
        assertEquals(8, finalPost.likesCount)
        assertTrue(finalPost.isReposted)
        assertEquals(3, finalPost.repostsCount)
        assertEquals(reposter, finalPost.repostedBy)
    }

    @Test
    fun `repost failure preserves a like that succeeds while repost is in flight`() = runBlocking {
        val original = post(13).copy(likesCount = 8, repostsCount = 2)
        val repostResult = CompletableDeferred<ApiResult<PostRepostResult>>()
        val owner = FeedStateOwner(
            QueueFeedRepository(ApiResult.Success(NovaPostPage(listOf(original), null))),
            LikePostRepository(ApiResult.Success(original.copy(isLiked = true, likesCount = 9))),
            DeferredPostRepostRepository(repostResult),
            CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadFeedNow()

        val repostJob = launch(start = CoroutineStart.UNDISPATCHED) {
            owner.toggleRepostNow(original)
        }
        owner.toggleLikeNow(owner.state.posts.single())
        repostResult.complete(ApiResult.Failure("offline"))
        repostJob.join()

        val finalPost = owner.state.posts.single()
        assertTrue(finalPost.isLiked)
        assertEquals(9, finalPost.likesCount)
        assertFalse(finalPost.isReposted)
        assertEquals(2, finalPost.repostsCount)
        assertNull(finalPost.repostedBy)
    }

    @Test
    fun `enter hydrates the matching user cache before authoritative refresh`() = runBlocking {
        val cached = NovaPostPage(listOf(post(21)), "cached-next")
        val server = NovaPostPage(listOf(post(22)), "server-next")
        val refresh = CompletableDeferred<ApiResult<NovaPostPage>>()
        val feed = CachedDeferredFeedRepository(
            caches = mapOf(101L to cached),
            responses = listOf(refresh),
        )
        val owner = FeedStateOwner(
            feed,
            NoOpPostRepository(),
            NoOpPostRepostRepository(),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.enter(101L)

        assertEquals(101L, owner.state.userId)
        assertEquals(listOf(21L), owner.state.posts.map { it.id })
        assertEquals("cached-next", owner.state.nextCursor)
        assertTrue(owner.state.isLoading)

        refresh.complete(ApiResult.Success(server))
        yield()

        assertEquals(listOf(22L), owner.state.posts.map { it.id })
        assertEquals("server-next", owner.state.nextCursor)
        assertFalse(owner.state.isLoading)
    }

    @Test
    fun `account switch hydrates only the new account and ignores the old refresh`() = runBlocking {
        val firstRefresh = CompletableDeferred<ApiResult<NovaPostPage>>()
        val secondRefresh = CompletableDeferred<ApiResult<NovaPostPage>>()
        val feed = CachedDeferredFeedRepository(
            caches = mapOf(
                101L to NovaPostPage(listOf(post(31)), null),
                202L to NovaPostPage(listOf(post(41)), null),
            ),
            responses = listOf(firstRefresh, secondRefresh),
        )
        val owner = FeedStateOwner(
            feed,
            NoOpPostRepository(),
            NoOpPostRepostRepository(),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.enter(101L)
        owner.enter(202L)

        assertEquals(202L, owner.state.userId)
        assertEquals(listOf(41L), owner.state.posts.map { it.id })
        assertEquals(listOf(101L, 202L), feed.cacheRequests)

        firstRefresh.complete(ApiResult.Success(NovaPostPage(listOf(post(32)), null)))
        yield()
        assertEquals(202L, owner.state.userId)
        assertEquals(listOf(41L), owner.state.posts.map { it.id })
        assertTrue(owner.state.isLoading)

        secondRefresh.complete(ApiResult.Success(NovaPostPage(listOf(post(42)), null)))
        yield()
        assertEquals(listOf(42L), owner.state.posts.map { it.id })
        assertFalse(owner.state.isLoading)
    }

    @Test
    fun `re-entering the same account does not duplicate the startup refresh`() = runBlocking {
        val refresh = CompletableDeferred<ApiResult<NovaPostPage>>()
        val feed = CachedDeferredFeedRepository(
            caches = emptyMap(),
            responses = listOf(refresh),
        )
        val owner = FeedStateOwner(
            feed,
            NoOpPostRepository(),
            NoOpPostRepostRepository(),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.enter(303L)
        owner.enter(303L)

        assertEquals(1, feed.feedCalls)
        assertEquals(listOf(303L), feed.cacheRequests)
        assertTrue(owner.state.posts.isEmpty())
        assertTrue(owner.state.isLoading)

        refresh.complete(ApiResult.Success(NovaPostPage(emptyList(), null)))
        yield()
        assertFalse(owner.state.isLoading)
        assertTrue(owner.state.posts.isEmpty())
    }

    @Test
    fun `startup refresh does not overwrite a like completed after the request began`() = runBlocking {
        val original = post(51).copy(likesCount = 8, isLiked = false)
        val refresh = CompletableDeferred<ApiResult<NovaPostPage>>()
        val feed = CachedDeferredFeedRepository(
            caches = mapOf(404L to NovaPostPage(listOf(original), null)),
            responses = listOf(refresh),
        )
        val owner = FeedStateOwner(
            feed,
            LikePostRepository(ApiResult.Success(original.copy(isLiked = true, likesCount = 9))),
            NoOpPostRepostRepository(),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.enter(404L)
        owner.toggleLikeNow(owner.state.posts.single())
        refresh.complete(ApiResult.Success(NovaPostPage(listOf(original), null)))
        yield()

        assertTrue(owner.state.posts.single().isLiked)
        assertEquals(9, owner.state.posts.single().likesCount)
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


private class DeferredPostRepostRepository(
    private val result: CompletableDeferred<ApiResult<PostRepostResult>>,
) : PostRepostRepository {
    override suspend fun setPostReposted(postId: Long, reposted: Boolean) = result.await()
}


private class LikePostRepository(
    private val result: ApiResult<NovaPost>,
) : NoOpPostRepository() {
    override suspend fun setLiked(postId: Long, liked: Boolean): ApiResult<NovaPost> = result
}


private class DeferredLikePostRepository(
    private val result: CompletableDeferred<ApiResult<NovaPost>>,
) : NoOpPostRepository() {
    override suspend fun setLiked(postId: Long, liked: Boolean): ApiResult<NovaPost> = result.await()
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


private class CachedDeferredFeedRepository(
    private val caches: Map<Long, NovaPostPage>,
    private val responses: List<CompletableDeferred<ApiResult<NovaPostPage>>>,
) : FeedRepository {
    var feedCalls = 0
        private set
    val cacheRequests = mutableListOf<Long>()

    override suspend fun feed(cursor: String?): ApiResult<NovaPostPage> {
        val response = responses[feedCalls]
        feedCalls += 1
        return response.await()
    }

    override fun cachedFeed(userId: Long): NovaPostPage? {
        cacheRequests += userId
        return caches[userId]
    }
}
