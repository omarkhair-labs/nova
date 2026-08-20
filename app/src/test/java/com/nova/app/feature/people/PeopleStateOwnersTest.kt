package com.nova.app.feature.people

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.data.PeoplePagingRepository
import com.nova.app.feature.people.data.PeopleRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.people.domain.model.NovaPersonPage
import com.nova.app.feature.people.domain.model.NovaProfilePostPage
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaCommentMutation
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class PeopleStateOwnersTest {
    @Test
    fun `people paging skips existing ids but preserves duplicates inside incoming page`() = runBlocking {
        val paging = QueuePeoplePagingRepository(
            ApiResult.Success(NovaPersonPage(listOf(person(1)), "next")),
            ApiResult.Success(NovaPersonPage(listOf(person(1), person(2), person(2), person(3)), null)),
        )
        val owner = PeopleStateOwner(NoOpPeopleRepository(), paging, CoroutineScope(Dispatchers.Unconfined))

        owner.loadPageNow(reset = true)
        owner.loadPageNow(reset = false, cursorOverride = "next")

        assertEquals(listOf(1L, 2L, 2L, 3L), owner.state.people.map { it.id })
        assertNull(owner.state.nextCursor)
        assertFalse(owner.state.loadingMore)
    }

    @Test
    fun `people page 401 becomes session expiry while retaining paging error`() = runBlocking {
        val owner = PeopleStateOwner(
            NoOpPeopleRepository(),
            QueuePeoplePagingRepository(ApiResult.Failure("expired", 401)),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadPageNow(reset = true)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertEquals("expired", owner.state.pagingError)
        assertFalse(owner.state.firstPageLoading)
    }

    @Test
    fun `cancel request 401 stays local instead of expiring session`() = runBlocking {
        val selected = person(7)
        val paging = QueuePeoplePagingRepository(
            ApiResult.Success(
                NovaPersonPage(
                    people = listOf(selected),
                    nextCursor = null,
                    privacyByUserId = mapOf(
                        selected.id to NovaPersonPrivacyState(
                            isPrivate = true,
                            followRequested = true,
                            canViewContent = false,
                        )
                    ),
                )
            )
        )
        val people = QueuePeopleRepository(followResults = mutableListOf(ApiResult.Failure("expired", 401)))
        val owner = PeopleStateOwner(people, paging, CoroutineScope(Dispatchers.Unconfined))
        owner.loadPageNow(reset = true)

        owner.toggleFollow(selected)

        assertEquals(0, owner.state.sessionExpiryVersion)
        assertEquals("expired", owner.state.pagingError)
        assertFalse(owner.state.privacyByUserId.getValue(selected.id).canViewContent)
    }

    @Test
    fun `normal follow error survives the automatic paging refresh`() = runBlocking {
        val selected = person(9)
        val paging = QueuePeoplePagingRepository(
            ApiResult.Success(NovaPersonPage(listOf(selected), null)),
            ApiResult.Success(NovaPersonPage(listOf(selected), null)),
        )
        val people = QueuePeopleRepository(
            followResults = mutableListOf(ApiResult.Failure("follow failed", 500)),
        )
        val owner = PeopleStateOwner(people, paging, CoroutineScope(Dispatchers.Unconfined))
        owner.loadPageNow(reset = true)

        owner.toggleFollow(selected)

        assertEquals("follow failed", owner.state.followError)
        assertNull(owner.state.pagingError)
        assertEquals(0, owner.state.sessionExpiryVersion)
    }

    @Test
    fun `social graph 401 is terminal and does not become inline error`() = runBlocking {
        val owner = SocialConnectionsStateOwner(
            username = "omar",
            mode = MODE_FOLLOWERS,
            currentUserId = 1L,
            pagingRepository = QueuePeoplePagingRepository(ApiResult.Failure("expired", 401)),
            peopleRepository = NoOpPeopleRepository(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadNow(reset = true)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertNull(owner.state.errorMessage)
        assertFalse(owner.state.isLoading)
    }

    @Test
    fun `person follow success updates person and emits both refresh effects`() = runBlocking {
        val before = person(8, following = false)
        val after = before.copy(isFollowing = true, followersCount = before.followersCount + 1)
        val people = QueuePeopleRepository(
            personResults = mutableListOf(ApiResult.Success(before)),
            followResults = mutableListOf(ApiResult.Success(after)),
        )
        val owner = PersonStateOwner(
            username = before.username,
            peopleRepository = people,
            postRepository = NoOpPostRepository(),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadPersonNow()

        owner.toggleFollowNow(before)

        assertEquals(after, owner.state.person)
        assertEquals(1, owner.state.profileRefreshVersion)
        assertEquals(1, owner.state.feedRefreshVersion)
        assertFalse(owner.state.isLoading)
    }

    private fun person(id: Long, following: Boolean = false) = NovaPerson(
        id = id,
        username = "person$id",
        name = "Person $id",
        avatarUrl = "",
        followersCount = 10,
        followingCount = 4,
        postsCount = 2,
        isFollowing = following,
    )
}


private class QueuePeopleRepository(
    private val personResults: MutableList<ApiResult<NovaPerson>> = mutableListOf(),
    private val followResults: MutableList<ApiResult<NovaPerson>> = mutableListOf(),
) : PeopleRepository {
    override suspend fun people(query: String): ApiResult<List<NovaPerson>> = ApiResult.Success(emptyList())

    override suspend fun person(username: String): ApiResult<NovaPerson> =
        personResults.takeFirstOrNull() ?: ApiResult.Failure("not configured")

    override suspend fun setFollowing(username: String, follow: Boolean): ApiResult<NovaPerson> =
        followResults.takeFirstOrNull() ?: ApiResult.Failure("not configured")

    override suspend fun setBlocked(username: String, blocked: Boolean): ApiResult<Unit> = ApiResult.Success(Unit)

    override suspend fun report(username: String, reason: String, details: String): ApiResult<String> =
        ApiResult.Success("ok")
}


private class NoOpPeopleRepository : PeopleRepository {
    override suspend fun people(query: String): ApiResult<List<NovaPerson>> = unsupported()
    override suspend fun person(username: String): ApiResult<NovaPerson> = unsupported()
    override suspend fun setFollowing(username: String, follow: Boolean): ApiResult<NovaPerson> = unsupported()
    override suspend fun setBlocked(username: String, blocked: Boolean): ApiResult<Unit> = unsupported()
    override suspend fun report(username: String, reason: String, details: String): ApiResult<String> = unsupported()

    private fun <T> unsupported(): T = error("not used")
}


private class QueuePeoplePagingRepository(
    vararg results: ApiResult<NovaPersonPage>,
) : PeoplePagingRepository {
    private val queue = results.toMutableList()

    override suspend fun people(query: String, cursor: String?): ApiResult<NovaPersonPage> = queue.removeAt(0)
    override suspend fun followers(username: String, query: String, cursor: String?): ApiResult<NovaPersonPage> = queue.removeAt(0)
    override suspend fun following(username: String, query: String, cursor: String?): ApiResult<NovaPersonPage> = queue.removeAt(0)
    override suspend fun profilePosts(username: String, cursor: String?): ApiResult<NovaProfilePostPage> = unsupported()
    override suspend fun profileReposts(username: String, cursor: String?): ApiResult<NovaProfilePostPage> = unsupported()

    private fun <T> unsupported(): T = error("not used")
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


private fun <T> MutableList<T>.takeFirstOrNull(): T? = if (isEmpty()) null else removeAt(0)
