package com.nova.app.feature.privacy

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.data.PeoplePagingRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.people.domain.model.NovaPersonPage
import com.nova.app.feature.people.domain.model.NovaProfilePostPage
import com.nova.app.feature.privacy.data.FollowRequestRepository
import com.nova.app.feature.privacy.data.PrivacyRepository
import com.nova.app.feature.privacy.domain.model.NovaFollowRequest
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import com.nova.app.feature.privacy.domain.model.NovaPrivacySummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class PrivacyStateOwnerTest {
    @Test
    fun `summary bundle keeps summary requests close friends order`() = runBlocking {
        val order = mutableListOf<String>()
        val request = followRequest(7, "pending")
        val friend = person(9, "friend")
        val owner = owner(
            privacy = FakePrivacyRepository(
                summaryResults = mutableListOf(ApiResult.Success(summary(pending = 1, closeFriends = 1))),
                closeFriendListResults = mutableListOf(ApiResult.Success(listOf(friend))),
                order = order,
            ),
            followRequests = FakeFollowRequestRepository(
                loadResults = mutableListOf(ApiResult.Success(listOf(request))),
                order = order,
            ),
        )

        owner.loadSummaryBundleNow()

        assertEquals(listOf("summary", "followRequests", "closeFriends"), order)
        assertEquals(1, owner.state.summary?.pendingFollowRequests)
        assertEquals(listOf(request), owner.state.requests)
        assertEquals(listOf(friend), owner.state.closeFriends)
        assertFalse(owner.state.loading)
        assertNull(owner.state.error)
    }

    @Test
    fun `summary bundle reports 401 terminal but continues remaining bundle calls`() = runBlocking {
        val order = mutableListOf<String>()
        val terminal = mutableListOf<String>()
        val request = followRequest(3, "alice")
        val friend = person(4, "bob")
        val owner = owner(
            privacy = FakePrivacyRepository(
                summaryResults = mutableListOf(ApiResult.Failure("expired", 401)),
                closeFriendListResults = mutableListOf(ApiResult.Success(listOf(friend))),
                order = order,
            ),
            followRequests = FakeFollowRequestRepository(
                loadResults = mutableListOf(ApiResult.Success(listOf(request))),
                order = order,
            ),
            onSessionExpired = { terminal += "expired" },
        )

        owner.loadSummaryBundleNow()

        assertEquals(listOf("summary", "followRequests", "closeFriends"), order)
        assertEquals(listOf("expired"), terminal)
        assertEquals(listOf(request), owner.state.requests)
        assertEquals(listOf(friend), owner.state.closeFriends)
        assertNull(owner.state.error)
        assertFalse(owner.state.loading)
    }

    @Test
    fun `followers paging trims query and preserves duplicates inside incoming page`() = runBlocking {
        val scopeJob = Job()
        val paging = FakePeoplePagingRepository(
            followerResults = mutableListOf(
                ApiResult.Success(NovaPersonPage(listOf(person(1, "one")), "next")),
                ApiResult.Success(
                    NovaPersonPage(
                        listOf(
                            person(1, "one"),
                            person(2, "two"),
                            person(2, "two"),
                        ),
                        null,
                    )
                ),
            )
        )
        val owner = owner(
            paging = paging,
            scope = CoroutineScope(scopeJob + Dispatchers.Unconfined),
        )

        owner.setFollowerQuery("  nova user  ")
        owner.loadFollowersNow(reset = true)
        owner.loadFollowersNow(reset = false)

        assertEquals(listOf(1L, 2L, 2L), owner.state.followers.map { it.id })
        assertNull(owner.state.followerCursor)
        assertEquals(
            listOf(
                FollowerCall("omar", "nova user", null),
                FollowerCall("omar", "nova user", "next"),
            ),
            paging.followerCalls,
        )
        assertFalse(owner.state.loadingFollowers)
        assertFalse(owner.state.loadingMore)
        scopeJob.cancel()
    }

    @Test
    fun `followers failures keep non401 inline and report 401 terminal`() = runBlocking {
        val terminal = mutableListOf<String>()
        val non401 = owner(
            paging = FakePeoplePagingRepository(
                followerResults = mutableListOf(ApiResult.Failure("offline", 503)),
            ),
            onSessionExpired = { terminal += "expired" },
        )
        non401.loadFollowersNow(reset = true)
        assertEquals("offline", non401.state.error)
        assertTrue(terminal.isEmpty())

        val expired = owner(
            paging = FakePeoplePagingRepository(
                followerResults = mutableListOf(ApiResult.Failure("expired", 401)),
            ),
            onSessionExpired = { terminal += "expired" },
        )
        expired.loadFollowersNow(reset = true)
        assertNull(expired.state.error)
        assertEquals(listOf("expired"), terminal)
    }

    @Test
    fun `private toggle accepted pending clears requests and refreshes followers`() = runBlocking {
        val requestA = followRequest(1, "alice")
        val requestB = followRequest(2, "bob")
        val paging = FakePeoplePagingRepository(
            followerResults = mutableListOf(
                ApiResult.Success(NovaPersonPage(listOf(person(8, "follower")), null)),
            )
        )
        val privacy = FakePrivacyRepository(
            summaryResults = mutableListOf(ApiResult.Success(summary(isPrivate = true, pending = 2))),
            setPrivateResults = mutableListOf(
                ApiResult.Success(
                    summary(
                        isPrivate = false,
                        pending = 0,
                        acceptedPending = 2,
                    )
                )
            ),
        )
        val owner = owner(
            privacy = privacy,
            followRequests = FakeFollowRequestRepository(
                loadResults = mutableListOf(ApiResult.Success(listOf(requestA, requestB))),
            ),
            paging = paging,
        )
        owner.loadSummaryBundleNow()

        owner.togglePrivateNow(enabled = false)

        assertEquals(false, owner.state.summary?.isPrivate)
        assertTrue(owner.state.requests.isEmpty())
        assertEquals("2 pending follow requests were accepted.", owner.state.feedback)
        assertEquals(listOf(8L), owner.state.followers.map { it.id })
        assertEquals(1, paging.followerCalls.size)
        assertFalse(owner.state.privacyBusy)
    }

    @Test
    fun `follow request decision keeps one global busy id and refreshes only accept`() = runBlocking {
        val requestA = followRequest(1, "alice")
        val requestB = followRequest(2, "bob")
        val release = CompletableDeferred<Unit>()
        val followRequests = FakeFollowRequestRepository(
            loadResults = mutableListOf(ApiResult.Success(listOf(requestA, requestB))),
            decisionResults = mutableListOf(ApiResult.Success(Unit), ApiResult.Success(Unit)),
            blockFirstDecision = release,
        )
        val paging = FakePeoplePagingRepository()
        val owner = owner(followRequests = followRequests, paging = paging)
        owner.loadSummaryBundleNow()

        owner.decideFollowRequest(requestA, accept = true)
        assertEquals(1L, owner.state.requestBusyId)
        owner.decideFollowRequest(requestB, accept = false)
        assertEquals(listOf("accept:1"), followRequests.decisionCalls)

        release.complete(Unit)

        assertNull(owner.state.requestBusyId)
        assertEquals(listOf(2L), owner.state.requests.map { it.id })
        assertEquals(1, owner.state.summary?.pendingFollowRequests)
        assertEquals("@alice can now follow you.", owner.state.feedback)
        assertEquals(1, paging.followerCalls.size)

        owner.decideFollowRequestNow(requestB, accept = false)
        assertTrue(owner.state.requests.isEmpty())
        assertEquals(0, owner.state.summary?.pendingFollowRequests)
        assertEquals("Follow request declined.", owner.state.feedback)
        assertEquals(1, paging.followerCalls.size)
    }

    @Test
    fun `close friend toggle keeps one global busy id and updates summary count`() = runBlocking {
        val alice = person(1, "alice")
        val bob = person(2, "bob")
        val release = CompletableDeferred<Unit>()
        val privacy = FakePrivacyRepository(
            summaryResults = mutableListOf(ApiResult.Success(summary(closeFriends = 0))),
            setCloseFriendResults = mutableListOf(ApiResult.Success(Unit), ApiResult.Success(Unit)),
            blockFirstCloseFriendMutation = release,
        )
        val owner = owner(privacy = privacy)
        owner.loadSummaryBundleNow()

        owner.toggleCloseFriend(alice)
        assertEquals(1L, owner.state.closeFriendBusyId)
        owner.toggleCloseFriend(bob)
        assertEquals(listOf("alice:true"), privacy.closeFriendMutationCalls)

        release.complete(Unit)

        assertNull(owner.state.closeFriendBusyId)
        assertEquals(listOf(1L), owner.state.closeFriends.map { it.id })
        assertEquals(1, owner.state.summary?.closeFriendsCount)
        assertEquals("Added @alice to Close Friends.", owner.state.feedback)

        owner.toggleCloseFriendNow(alice)
        assertTrue(owner.state.closeFriends.isEmpty())
        assertEquals(0, owner.state.summary?.closeFriendsCount)
        assertEquals("Removed @alice from Close Friends.", owner.state.feedback)
        assertEquals(listOf("alice:true", "alice:false"), privacy.closeFriendMutationCalls)
    }

    @Test
    fun `follower query keeps 50 character cap and 280 ms debounce contract`() {
        val scopeJob = Job()
        val owner = owner(scope = CoroutineScope(scopeJob + Dispatchers.Unconfined))

        owner.setFollowerQuery("x".repeat(70))

        assertEquals(50, owner.state.followerQuery.length)
        assertEquals(50, PrivacyStateOwner.FOLLOWER_QUERY_MAX_LENGTH)
        assertEquals(280L, PrivacyStateOwner.FOLLOWER_SEARCH_DEBOUNCE_MS)
        scopeJob.cancel()
    }

    @Test
    fun `load more without cursor stays a no op`() = runBlocking {
        val paging = FakePeoplePagingRepository(
            followerResults = mutableListOf(
                ApiResult.Success(NovaPersonPage(listOf(person(1, "one")), null)),
            )
        )
        val owner = owner(paging = paging)
        owner.loadFollowersNow(reset = true)

        owner.loadFollowersNow(reset = false)

        assertEquals(1, paging.followerCalls.size)
        assertEquals(listOf(1L), owner.state.followers.map { it.id })
        assertFalse(owner.state.loadingMore)
    }

    private fun owner(
        username: String = "omar",
        privacy: PrivacyRepository = FakePrivacyRepository(),
        followRequests: FollowRequestRepository = FakeFollowRequestRepository(),
        paging: PeoplePagingRepository = FakePeoplePagingRepository(),
        onSessionExpired: () -> Unit = {},
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
    ) = PrivacyStateOwner(
        username = username,
        privacyRepository = privacy,
        followRequestRepository = followRequests,
        peoplePagingRepository = paging,
        scope = scope,
        onSessionExpired = onSessionExpired,
    )
}


private class FakePrivacyRepository(
    private val summaryResults: MutableList<ApiResult<NovaPrivacySummary>> = mutableListOf(
        ApiResult.Success(summary()),
    ),
    private val setPrivateResults: MutableList<ApiResult<NovaPrivacySummary>> = mutableListOf(
        ApiResult.Success(summary()),
    ),
    private val personStateResults: MutableList<ApiResult<NovaPersonPrivacyState>> = mutableListOf(
        ApiResult.Success(NovaPersonPrivacyState(false, false, true)),
    ),
    private val closeFriendListResults: MutableList<ApiResult<List<NovaPerson>>> = mutableListOf(
        ApiResult.Success(emptyList()),
    ),
    private val setCloseFriendResults: MutableList<ApiResult<Unit>> = mutableListOf(ApiResult.Success(Unit)),
    private val blockFirstCloseFriendMutation: CompletableDeferred<Unit>? = null,
    private val order: MutableList<String>? = null,
) : PrivacyRepository {
    val closeFriendMutationCalls = mutableListOf<String>()
    private var closeFriendMutationCount = 0

    override suspend fun summary(): ApiResult<NovaPrivacySummary> {
        order?.add("summary")
        return summaryResults.removeFirst()
    }

    override suspend fun setPrivate(isPrivate: Boolean): ApiResult<NovaPrivacySummary> =
        setPrivateResults.removeFirst()

    override suspend fun personState(username: String): ApiResult<NovaPersonPrivacyState> =
        personStateResults.removeFirst()

    override suspend fun closeFriends(): ApiResult<List<NovaPerson>> {
        order?.add("closeFriends")
        return closeFriendListResults.removeFirst()
    }

    override suspend fun setCloseFriend(username: String, enabled: Boolean): ApiResult<Unit> {
        closeFriendMutationCalls += "$username:$enabled"
        closeFriendMutationCount += 1
        if (closeFriendMutationCount == 1) blockFirstCloseFriendMutation?.await()
        return setCloseFriendResults.removeFirst()
    }
}


private class FakeFollowRequestRepository(
    private val loadResults: MutableList<ApiResult<List<NovaFollowRequest>>> = mutableListOf(
        ApiResult.Success(emptyList()),
    ),
    private val decisionResults: MutableList<ApiResult<Unit>> = mutableListOf(ApiResult.Success(Unit)),
    private val blockFirstDecision: CompletableDeferred<Unit>? = null,
    private val order: MutableList<String>? = null,
) : FollowRequestRepository {
    val decisionCalls = mutableListOf<String>()
    private var decisionCount = 0

    override suspend fun followRequests(): ApiResult<List<NovaFollowRequest>> {
        order?.add("followRequests")
        return loadResults.removeFirst()
    }

    override suspend fun acceptFollowRequest(requestId: Long): ApiResult<Unit> =
        decide("accept:$requestId")

    override suspend fun declineFollowRequest(requestId: Long): ApiResult<Unit> =
        decide("decline:$requestId")

    private suspend fun decide(call: String): ApiResult<Unit> {
        decisionCalls += call
        decisionCount += 1
        if (decisionCount == 1) blockFirstDecision?.await()
        return decisionResults.removeFirst()
    }
}


private data class FollowerCall(
    val username: String,
    val query: String,
    val cursor: String?,
)


private class FakePeoplePagingRepository(
    private val followerResults: MutableList<ApiResult<NovaPersonPage>> = mutableListOf(
        ApiResult.Success(NovaPersonPage(emptyList(), null)),
    ),
) : PeoplePagingRepository {
    val followerCalls = mutableListOf<FollowerCall>()

    override suspend fun people(query: String, cursor: String?): ApiResult<NovaPersonPage> = error("unused")

    override suspend fun followers(
        username: String,
        query: String,
        cursor: String?,
    ): ApiResult<NovaPersonPage> {
        followerCalls += FollowerCall(username, query, cursor)
        return if (followerResults.isEmpty()) {
            ApiResult.Success(NovaPersonPage(emptyList(), null))
        } else {
            followerResults.removeFirst()
        }
    }

    override suspend fun following(
        username: String,
        query: String,
        cursor: String?,
    ): ApiResult<NovaPersonPage> = error("unused")

    override suspend fun profilePosts(username: String, cursor: String?): ApiResult<NovaProfilePostPage> = error("unused")

    override suspend fun profileReposts(username: String, cursor: String?): ApiResult<NovaProfilePostPage> = error("unused")
}


private fun summary(
    isPrivate: Boolean = true,
    pending: Int = 2,
    closeFriends: Int = 0,
    acceptedPending: Int = 0,
) = NovaPrivacySummary(
    isPrivate = isPrivate,
    pendingFollowRequests = pending,
    closeFriendsCount = closeFriends,
    acceptedPendingRequests = acceptedPending,
)


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
    followersCount = 10,
    followingCount = 5,
    postsCount = 3,
    isFollowing = true,
)
