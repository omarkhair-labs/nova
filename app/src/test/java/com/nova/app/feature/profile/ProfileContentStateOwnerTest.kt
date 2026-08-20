package com.nova.app.feature.profile

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.people.data.PeoplePagingRepository
import com.nova.app.feature.people.domain.model.NovaPersonPage
import com.nova.app.feature.people.domain.model.NovaProfilePostPage
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaCommentMutation
import com.nova.app.feature.posts.domain.model.NovaPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class ProfileContentStateOwnerTest {
    @Test
    fun `first page 401 becomes terminal session effect`() = runBlocking {
        val owner = ProfileContentStateOwner(
            username = "omar",
            postRepository = QueuePostRepository(ApiResult.Failure("expired", 401)),
            pagingRepository = QueueProfilePagingRepository(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadPostsNow()

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertNull(owner.state.postsError)
        assertFalse(owner.state.postsLoading)
    }

    @Test
    fun `load more 401 stays inline and does not expire session`() = runBlocking {
        val firstPage = (1L..24L).map(::post)
        val owner = ProfileContentStateOwner(
            username = "omar",
            postRepository = QueuePostRepository(ApiResult.Success(firstPage)),
            pagingRepository = QueueProfilePagingRepository(
                profilePostsResults = mutableListOf(ApiResult.Failure("expired", 401)),
            ),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadPostsNow()

        owner.loadMorePostsNow("24")

        assertEquals(0, owner.state.sessionExpiryVersion)
        assertEquals("expired", owner.state.postsPagingError)
        assertFalse(owner.state.postsLoadingMore)
    }

    @Test
    fun `post paging skips existing ids but preserves duplicates inside incoming page`() = runBlocking {
        val firstPage = (1L..24L).map(::post)
        val paging = QueueProfilePagingRepository(
            profilePostsResults = mutableListOf(
                ApiResult.Success(
                    NovaProfilePostPage(
                        posts = listOf(post(24), post(25), post(25), post(26)),
                        nextCursor = null,
                    )
                )
            ),
        )
        val owner = ProfileContentStateOwner(
            username = "omar",
            postRepository = QueuePostRepository(ApiResult.Success(firstPage)),
            pagingRepository = paging,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadPostsNow()

        owner.loadMorePostsNow("24")

        assertEquals((1L..24L).toList() + listOf(25L, 25L, 26L), owner.state.posts.map { it.id })
        assertNull(owner.state.postsNextCursor)
    }

    @Test
    fun `fresh first page keeps previously loaded older posts and current paging cursor`() = runBlocking {
        val firstPage = (1L..24L).map(::post)
        val refreshed = listOf(post(100)) + (1L..23L).map(::post)
        val postRepository = QueuePostRepository(
            ApiResult.Success(firstPage),
            ApiResult.Success(refreshed),
        )
        val paging = QueueProfilePagingRepository(
            profilePostsResults = mutableListOf(
                ApiResult.Success(NovaProfilePostPage(listOf(post(25), post(26)), "older"))
            ),
        )
        val owner = ProfileContentStateOwner(
            username = "omar",
            postRepository = postRepository,
            pagingRepository = paging,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadPostsNow()
        owner.loadMorePostsNow("24")

        owner.loadPostsNow()

        assertEquals(listOf(100L) + (1L..23L).toList() + listOf(24L, 25L, 26L), owner.state.posts.map { it.id })
        assertEquals("older", owner.state.postsNextCursor)
    }

    @Test
    fun `returning to Posts resets loaded older page like the old composable lifecycle`() = runBlocking {
        val firstPage = (1L..24L).map(::post)
        val owner = ProfileContentStateOwner(
            username = "omar",
            postRepository = QueuePostRepository(ApiResult.Success(firstPage)),
            pagingRepository = QueueProfilePagingRepository(
                profilePostsResults = mutableListOf(
                    ApiResult.Success(NovaProfilePostPage(listOf(post(25)), "older"))
                ),
            ),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadPostsNow()
        owner.loadMorePostsNow("24")
        assertEquals(25, owner.state.posts.size)

        owner.selectTab(ProfileContentTab.Reels)
        owner.selectTab(ProfileContentTab.Posts)

        assertEquals(firstPage.map { it.id }, owner.state.posts.map { it.id })
        assertEquals("24", owner.state.postsNextCursor)
        assertNull(owner.state.postsPagingError)
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


private class QueuePostRepository(
    vararg results: ApiResult<List<NovaPost>>,
) : PostRepository {
    private val personPostsResults = results.toMutableList()

    override suspend fun personPosts(username: String): ApiResult<List<NovaPost>> = personPostsResults.removeAt(0)
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


private class QueueProfilePagingRepository(
    private val profilePostsResults: MutableList<ApiResult<NovaProfilePostPage>> = mutableListOf(),
    private val profileRepostsResults: MutableList<ApiResult<NovaProfilePostPage>> = mutableListOf(),
) : PeoplePagingRepository {
    override suspend fun people(query: String, cursor: String?): ApiResult<NovaPersonPage> = unsupported()
    override suspend fun followers(username: String, query: String, cursor: String?): ApiResult<NovaPersonPage> = unsupported()
    override suspend fun following(username: String, query: String, cursor: String?): ApiResult<NovaPersonPage> = unsupported()
    override suspend fun profilePosts(username: String, cursor: String?): ApiResult<NovaProfilePostPage> =
        profilePostsResults.removeAt(0)

    override suspend fun profileReposts(username: String, cursor: String?): ApiResult<NovaProfilePostPage> =
        profileRepostsResults.removeAt(0)

    private fun <T> unsupported(): T = error("not used")
}
