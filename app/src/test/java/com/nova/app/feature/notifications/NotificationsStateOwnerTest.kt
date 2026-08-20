package com.nova.app.feature.notifications

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.notifications.data.NotificationsRepository
import com.nova.app.feature.notifications.domain.model.NovaNotification
import com.nova.app.feature.notifications.domain.model.NovaNotificationPage
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaCommentMutation
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.privacy.data.FollowRequestRepository
import com.nova.app.feature.privacy.domain.model.NovaFollowRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class NotificationsStateOwnerTest {
    @Test
    fun `reset loads follow requests then activity and marks unread notifications read`() {
        val order = mutableListOf<String>()
        val request = followRequest(8, "pending")
        val followRequests = FakeFollowRequestRepository(
            loadResults = mutableListOf(ApiResult.Success(listOf(request))),
            order = order,
        )
        val notifications = FakeNotificationsRepository(
            pageResults = mutableListOf(
                ApiResult.Success(
                    NovaNotificationPage(
                        notifications = listOf(notification(1, "follow")),
                        nextCursor = "next",
                        unreadCount = 3,
                    )
                )
            ),
            markResults = mutableListOf(ApiResult.Success(0)),
            order = order,
        )
        val unreadCounts = mutableListOf<Int>()
        val owner = owner(
            notifications = notifications,
            followRequests = followRequests,
            onUnreadCountChanged = unreadCounts::add,
        )

        owner.start()

        assertEquals(listOf("followRequests", "notifications:null", "markAllRead"), order)
        assertEquals(listOf(request), owner.state.followRequests)
        assertEquals(listOf(1L), owner.state.notifications.map { it.id })
        assertEquals("next", owner.state.nextCursor)
        assertEquals(listOf(3, 0), unreadCounts)
        assertFalse(owner.state.isLoading)
        assertFalse(owner.state.requestsLoading)
    }

    @Test
    fun `load more filters preexisting ids only and preserves duplicates inside incoming page`() = runBlocking {
        val notifications = FakeNotificationsRepository(
            pageResults = mutableListOf(
                ApiResult.Success(NovaNotificationPage(listOf(notification(1, "follow")), "next", 0)),
                ApiResult.Success(
                    NovaNotificationPage(
                        listOf(
                            notification(1, "follow"),
                            notification(2, "like", postId = 20),
                            notification(2, "like", postId = 20),
                        ),
                        null,
                        0,
                    )
                ),
            )
        )
        val owner = owner(notifications = notifications)

        owner.loadActivityRequestNow(reset = true, cursor = null)
        owner.loadActivityRequestNow(reset = false, cursor = "next")

        assertEquals(listOf(1L, 2L, 2L), owner.state.notifications.map { it.id })
        assertNull(owner.state.nextCursor)
        assertFalse(owner.state.isLoadingMore)
    }

    @Test
    fun `activity failures keep non401 inline and report 401 as terminal`() = runBlocking {
        val terminalEvents = mutableListOf<String>()
        val non401Owner = owner(
            notifications = FakeNotificationsRepository(
                pageResults = mutableListOf(ApiResult.Failure("offline", 503)),
            ),
            onSessionExpired = { terminalEvents += "expired" },
        )

        non401Owner.loadActivityRequestNow(reset = true, cursor = null)

        assertEquals("offline", non401Owner.state.errorMessage)
        assertTrue(terminalEvents.isEmpty())

        val terminalOwner = owner(
            notifications = FakeNotificationsRepository(
                pageResults = mutableListOf(ApiResult.Failure("expired", 401)),
            ),
            onSessionExpired = { terminalEvents += "expired" },
        )
        terminalOwner.loadActivityRequestNow(reset = true, cursor = null)

        assertNull(terminalOwner.state.errorMessage)
        assertEquals(listOf("expired"), terminalEvents)
    }

    @Test
    fun `mark all read ignores non401 failure but reports 401 terminal`() = runBlocking {
        val terminalEvents = mutableListOf<String>()
        val non401 = owner(
            notifications = FakeNotificationsRepository(
                pageResults = mutableListOf(ApiResult.Success(NovaNotificationPage(emptyList(), null, 2))),
                markResults = mutableListOf(ApiResult.Failure("mark failed", 500)),
            ),
            onSessionExpired = { terminalEvents += "expired" },
        )
        non401.loadActivityRequestNow(reset = true, cursor = null)

        assertNull(non401.state.errorMessage)
        assertTrue(terminalEvents.isEmpty())

        val terminal = owner(
            notifications = FakeNotificationsRepository(
                pageResults = mutableListOf(ApiResult.Success(NovaNotificationPage(emptyList(), null, 1))),
                markResults = mutableListOf(ApiResult.Failure("expired", 401)),
            ),
            onSessionExpired = { terminalEvents += "expired" },
        )
        terminal.loadActivityRequestNow(reset = true, cursor = null)

        assertEquals(listOf("expired"), terminalEvents)
    }

    @Test
    fun `follow request failures preserve inline versus terminal 401 semantics`() = runBlocking {
        val terminalEvents = mutableListOf<String>()
        val non401 = owner(
            followRequests = FakeFollowRequestRepository(
                loadResults = mutableListOf(ApiResult.Failure("request error", 500)),
            ),
            onSessionExpired = { terminalEvents += "expired" },
        )
        non401.loadFollowRequestsNow()
        assertEquals("request error", non401.state.requestError)
        assertFalse(non401.state.requestsLoading)

        val terminal = owner(
            followRequests = FakeFollowRequestRepository(
                loadResults = mutableListOf(ApiResult.Failure("expired", 401)),
            ),
            onSessionExpired = { terminalEvents += "expired" },
        )
        terminal.loadFollowRequestsNow()
        assertNull(terminal.state.requestError)
        assertEquals(listOf("expired"), terminalEvents)
    }

    @Test
    fun `follow request decision uses one global busy id and removes success`() = runBlocking {
        val requestA = followRequest(1, "alice")
        val requestB = followRequest(2, "bob")
        val release = CompletableDeferred<Unit>()
        val followRequests = FakeFollowRequestRepository(
            loadResults = mutableListOf(ApiResult.Success(listOf(requestA, requestB))),
            blockFirstDecision = release,
        )
        val owner = owner(followRequests = followRequests)
        owner.loadFollowRequestsNow()

        owner.decideFollowRequest(requestA, accept = true)
        assertEquals(1L, owner.state.requestBusyId)
        owner.decideFollowRequest(requestB, accept = false)
        assertEquals(listOf("accept:1"), followRequests.decisionCalls)

        release.complete(Unit)

        assertNull(owner.state.requestBusyId)
        assertEquals(listOf(2L), owner.state.followRequests.map { it.id })
    }

    @Test
    fun `open post clears busy before success callback and keeps failure semantics`() = runBlocking {
        val post = post(77)
        var busyAtCallback: Long? = -1L
        lateinit var successOwner: NotificationsStateOwner
        successOwner = owner(
            posts = FakePostRepository(postResults = mutableListOf(ApiResult.Success(post))),
            onPostOpened = { busyAtCallback = successOwner.state.openingPostId },
        )

        successOwner.openPostNow(77)

        assertNull(busyAtCallback)
        assertNull(successOwner.state.openingPostId)

        val terminalEvents = mutableListOf<String>()
        val failureOwner = owner(
            posts = FakePostRepository(postResults = mutableListOf(ApiResult.Failure("gone", 404))),
            onSessionExpired = { terminalEvents += "expired" },
        )
        failureOwner.openPostNow(88)
        assertEquals("gone", failureOwner.state.errorMessage)
        assertNull(failureOwner.state.openingPostId)
        assertTrue(terminalEvents.isEmpty())

        val terminalOwner = owner(
            posts = FakePostRepository(postResults = mutableListOf(ApiResult.Failure("expired", 401))),
            onSessionExpired = { terminalEvents += "expired" },
        )
        terminalOwner.openPostNow(99)
        assertNull(terminalOwner.state.errorMessage)
        assertEquals(listOf("expired"), terminalEvents)
    }

    @Test
    fun `load more threshold stays four items from the end`() = runBlocking {
        val owner = owner(
            notifications = FakeNotificationsRepository(
                pageResults = mutableListOf(
                    ApiResult.Success(NovaNotificationPage(listOf(notification(1, "follow")), "next", 0))
                )
            )
        )
        owner.loadActivityRequestNow(reset = true, cursor = null)

        assertEquals(4, NotificationsStateOwner.LOAD_MORE_THRESHOLD)
        assertFalse(owner.shouldLoadMore(lastVisible = 5, totalItems = 10))
        assertTrue(owner.shouldLoadMore(lastVisible = 6, totalItems = 10))
    }

    @Test
    fun `notification click routing preserves post reel and actor fallbacks`() {
        val owner = owner()

        assertEquals(
            NotificationOpenTarget.Person("actor"),
            owner.openTarget(notification(1, "follow")),
        )
        assertEquals(
            NotificationOpenTarget.Post(42),
            owner.openTarget(notification(2, "comment", postId = 42)),
        )
        assertEquals(
            NotificationOpenTarget.Person("actor"),
            owner.openTarget(notification(3, "comment", postId = null)),
        )
        assertEquals(
            NotificationOpenTarget.Reel(9, "reelowner"),
            owner.openTarget(notification(4, "reel_like", reelId = 9, reelAuthor = " ReelOwner ")),
        )
        assertEquals(
            NotificationOpenTarget.Person("actor"),
            owner.openTarget(notification(5, "reel_reply", reelId = null, reelAuthor = "owner")),
        )
        assertEquals(
            NotificationOpenTarget.None,
            owner.openTarget(notification(6, "reel_comment", reelId = 0, reelAuthor = "owner")),
        )
        assertEquals(
            NotificationOpenTarget.Person("actor"),
            owner.openTarget(notification(7, "unknown")),
        )
    }

    private fun owner(
        notifications: NotificationsRepository = FakeNotificationsRepository(),
        followRequests: FollowRequestRepository = FakeFollowRequestRepository(),
        posts: PostRepository = FakePostRepository(),
        onUnreadCountChanged: (Int) -> Unit = {},
        onSessionExpired: () -> Unit = {},
        onPostOpened: (NovaPost) -> Unit = {},
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
    ) = NotificationsStateOwner(
        notificationsRepository = notifications,
        followRequestRepository = followRequests,
        postRepository = posts,
        scope = scope,
        onUnreadCountChanged = onUnreadCountChanged,
        onSessionExpired = onSessionExpired,
        onPostOpened = onPostOpened,
    )
}


private class FakeNotificationsRepository(
    private val pageResults: MutableList<ApiResult<NovaNotificationPage>> = mutableListOf(
        ApiResult.Success(NovaNotificationPage(emptyList(), null, 0)),
    ),
    private val markResults: MutableList<ApiResult<Int>> = mutableListOf(ApiResult.Success(0)),
    private val order: MutableList<String>? = null,
) : NotificationsRepository {
    override suspend fun notifications(cursor: String?): ApiResult<NovaNotificationPage> {
        order?.add("notifications:$cursor")
        return pageResults.removeFirst()
    }

    override suspend fun markAllRead(): ApiResult<Int> {
        order?.add("markAllRead")
        return markResults.removeFirst()
    }
}


private class FakeFollowRequestRepository(
    private val loadResults: MutableList<ApiResult<List<NovaFollowRequest>>> = mutableListOf(
        ApiResult.Success(emptyList()),
    ),
    private val decisionResults: MutableList<ApiResult<Unit>> = mutableListOf(),
    private val blockFirstDecision: CompletableDeferred<Unit>? = null,
    private val order: MutableList<String>? = null,
) : FollowRequestRepository {
    val decisionCalls = mutableListOf<String>()
    private var decisionCount = 0

    override suspend fun followRequests(): ApiResult<List<NovaFollowRequest>> {
        order?.add("followRequests")
        return loadResults.removeFirst()
    }

    override suspend fun acceptFollowRequest(requestId: Long): ApiResult<Unit> = decision("accept:$requestId")

    override suspend fun declineFollowRequest(requestId: Long): ApiResult<Unit> = decision("decline:$requestId")

    private suspend fun decision(call: String): ApiResult<Unit> {
        decisionCalls += call
        decisionCount += 1
        if (decisionCount == 1) blockFirstDecision?.await()
        return if (decisionResults.isEmpty()) ApiResult.Success(Unit) else decisionResults.removeFirst()
    }
}


private class FakePostRepository(
    private val postResults: MutableList<ApiResult<NovaPost>> = mutableListOf(ApiResult.Success(post(1))),
) : PostRepository {
    override suspend fun post(postId: Long): ApiResult<NovaPost> = postResults.removeFirst()
    override suspend fun personPosts(username: String): ApiResult<List<NovaPost>> = error("unused")
    override suspend fun createPost(caption: String, imageUri: Uri): ApiResult<NovaPost> = error("unused")
    override suspend fun deletePost(postId: Long): ApiResult<Unit> = error("unused")
    override suspend fun setLiked(postId: Long, liked: Boolean): ApiResult<NovaPost> = error("unused")
    override suspend fun comments(postId: Long): ApiResult<List<NovaComment>> = error("unused")
    override suspend fun addComment(postId: Long, body: String, parentId: Long?): ApiResult<NovaCommentMutation> = error("unused")
    override suspend fun deleteComment(commentId: Long): ApiResult<NovaPost> = error("unused")
    override suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaPost> = error("unused")
}


private fun followRequest(id: Long, username: String) = NovaFollowRequest(
    id = id,
    requester = person(id, username),
    createdAt = "2026-08-20T10:00:00Z",
)


private fun person(id: Long, username: String) = NovaPerson(
    id = id,
    username = username,
    name = username.replaceFirstChar { it.uppercase() },
    avatarUrl = "https://example.com/$username.jpg",
    followersCount = 1,
    followingCount = 2,
    postsCount = 3,
    isFollowing = false,
)


private fun notification(
    id: Long,
    kind: String,
    postId: Long? = null,
    reelId: Long? = null,
    reelAuthor: String = "",
) = NovaNotification(
    id = id,
    kind = kind,
    actor = NovaPostAuthor(10, "actor", "Actor", ""),
    postId = postId,
    reelId = reelId,
    reelAuthorUsername = reelAuthor,
    commentPreview = "preview",
    createdAt = "2026-08-20T10:00:00Z",
    isRead = false,
)


private fun post(id: Long) = NovaPost(
    id = id,
    author = NovaPostAuthor(10, "actor", "Actor", ""),
    imageUrl = "https://example.com/post.jpg",
    caption = "caption",
    createdAt = "2026-08-20T10:00:00Z",
    isMine = false,
)
