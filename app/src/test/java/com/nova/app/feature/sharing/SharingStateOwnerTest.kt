package com.nova.app.feature.sharing

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.data.MessagesRepository
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.messages.domain.model.NovaConversationList
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaMessagePage
import com.nova.app.feature.messages.domain.model.NovaMessageReaction
import com.nova.app.feature.people.data.PeopleRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import com.nova.app.feature.sharing.data.SharingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File


class SharingStateOwnerTest {
    @Test
    fun `search keeps 220ms contract and caps query at sixty characters`() {
        val owner = owner()

        owner.setQuery("x".repeat(75))

        assertEquals(220L, SharingStateOwner.SEARCH_DEBOUNCE_MS)
        assertEquals(60, SharingStateOwner.QUERY_MAX_LENGTH)
        assertEquals(60, owner.state.query.length)
        assertNull(owner.state.error)
    }

    @Test
    fun `search loads conversations first then filters matching direct people`() = runBlocking {
        val order = mutableListOf<String>()
        val direct = conversation(1, "alice")
        val group = conversation(2, "group", group = true, title = "Nova crew")
        val messages = FakeMessagesRepository(
            conversationResults = mutableListOf(ApiResult.Success(NovaConversationList(listOf(direct, group), 0))),
            order = order,
        )
        val people = FakePeopleRepository(
            results = mutableListOf(ApiResult.Success(listOf(person(1, "ALICE"), person(2, "bob")))),
            order = order,
        )
        val owner = owner(messages = messages, people = people)

        owner.searchNow()

        assertEquals(listOf("messages", "people"), order)
        assertEquals(listOf(1L, 2L), owner.state.conversations.map { it.id })
        assertEquals(listOf("bob"), owner.state.people.map { it.username })
        assertFalse(owner.state.loadingConversations)
        assertFalse(owner.state.loadingPeople)
        assertNull(owner.state.error)
    }

    @Test
    fun `conversation failure remains visible when people search later succeeds`() = runBlocking {
        val messages = FakeMessagesRepository(
            conversationResults = mutableListOf(ApiResult.Failure("chat error", 401)),
        )
        val people = FakePeopleRepository(
            results = mutableListOf(ApiResult.Success(listOf(person(2, "bob")))),
        )
        val owner = owner(messages = messages, people = people)

        owner.searchNow()

        assertEquals("chat error", owner.state.error)
        assertEquals(listOf("bob"), owner.state.people.map { it.username })
        assertTrue(owner.state.conversations.isEmpty())
    }

    @Test
    fun `people failure becomes inline error when conversations succeeded`() = runBlocking {
        val messages = FakeMessagesRepository(
            conversationResults = mutableListOf(ApiResult.Success(NovaConversationList(emptyList(), 0))),
        )
        val people = FakePeopleRepository(
            results = mutableListOf(ApiResult.Failure("people expired", 401)),
        )
        val owner = owner(messages = messages, people = people)

        owner.searchNow()

        assertEquals("people expired", owner.state.error)
        assertTrue(owner.state.people.isEmpty())
    }

    @Test
    fun `direct conversation shares to person while group shares by conversation id`() = runBlocking {
        val sharing = FakeSharingRepository()
        val owner = owner(target = SharingTarget.Reel(77), sharing = sharing)

        owner.sendToConversationNow(conversation(10, "friend"))
        owner.sendToConversationNow(conversation(11, "group", group = true, title = "Crew"))

        assertEquals(
            listOf(
                "shareReel:friend:77",
                "shareReelToConversation:11:77",
            ),
            sharing.calls,
        )
        assertEquals("Sent to Crew", owner.state.message)
        assertEquals(setOf(10L, 11L), owner.state.sentConversationIds)
        assertNull(owner.state.error)
    }

    @Test
    fun `typed target routes post profile and reel sends to matching operations`() = runBlocking {
        val person = person(8, "target")

        val postSharing = FakeSharingRepository()
        owner(target = SharingTarget.Post(41), sharing = postSharing).sendToPersonNow(person)
        assertEquals(listOf("sharePost:target:41"), postSharing.calls)

        val profileSharing = FakeSharingRepository()
        owner(target = SharingTarget.Profile("profile_owner"), sharing = profileSharing).sendToPersonNow(person)
        assertEquals(listOf("shareProfile:target:profile_owner"), profileSharing.calls)

        val reelSharing = FakeSharingRepository()
        owner(target = SharingTarget.Reel(42), sharing = reelSharing).sendToPersonNow(person)
        assertEquals(listOf("shareReel:target:42"), reelSharing.calls)
    }

    @Test
    fun `story success copy and profile no-op preserve current eligibility semantics`() = runBlocking {
        val sharing = FakeSharingRepository()
        val postOwner = owner(target = SharingTarget.Post(7), sharing = sharing)

        postOwner.addToStoryNow("close_friends")

        assertEquals(listOf("addPostToStory:7:close_friends"), sharing.calls)
        assertEquals("Added to your Close Friends Story", postOwner.state.message)
        assertEquals(setOf("close_friends"), postOwner.state.addedStoryAudiences)
        assertFalse(postOwner.state.addingToStory)

        postOwner.addToStoryNow("close_friends")
        assertEquals(listOf("addPostToStory:7:close_friends"), sharing.calls)

        val profileSharing = FakeSharingRepository()
        val profileOwner = owner(target = SharingTarget.Profile("me"), sharing = profileSharing)
        profileOwner.addToStoryNow("followers")

        assertFalse(profileOwner.canAddToStory)
        assertTrue(profileSharing.calls.isEmpty())
        assertNull(profileOwner.state.message)
        assertNull(profileOwner.state.error)
    }

    @Test
    fun `all share failures including 401 stay inline`() = runBlocking {
        val sharing = FakeSharingRepository(
            results = mutableListOf(ApiResult.Failure("expired inline", 401)),
        )
        val owner = owner(target = SharingTarget.Profile("owner"), sharing = sharing)

        owner.sendToPersonNow(person(2, "friend"))

        assertEquals("expired inline", owner.state.error)
        assertNull(owner.state.message)
        assertFalse(owner.state.busy)
    }

    @Test
    fun `global busy lock blocks competing story action while send is in flight`() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val sharing = FakeSharingRepository(blockFirstCall = release)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val owner = owner(target = SharingTarget.Post(9), sharing = sharing, scope = scope)

        owner.sendToPerson(person(3, "busy_friend"))
        assertTrue(owner.state.busy)
        assertEquals("busy_friend", owner.state.busyUsername)

        owner.addToStory("followers")
        assertEquals(listOf("sharePost:busy_friend:9"), sharing.calls)

        release.complete(Unit)
        assertFalse(owner.state.busy)
        assertEquals("Sent to @busy_friend", owner.state.message)
    }

    private fun owner(
        target: SharingTarget = SharingTarget.Post(1),
        messages: MessagesRepository = FakeMessagesRepository(),
        people: PeopleRepository = FakePeopleRepository(),
        sharing: SharingRepository = FakeSharingRepository(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
    ) = SharingStateOwner(target, messages, people, sharing, scope)
}


private class FakeMessagesRepository(
    private val conversationResults: MutableList<ApiResult<NovaConversationList>> = mutableListOf(
        ApiResult.Success(NovaConversationList(emptyList(), 0)),
    ),
    private val order: MutableList<String>? = null,
) : MessagesRepository {
    override suspend fun conversations(query: String): ApiResult<NovaConversationList> {
        order?.add("messages")
        return conversationResults.removeFirst()
    }

    override suspend fun openConversation(username: String): ApiResult<NovaConversation> = error("unused")
    override suspend fun messages(conversationId: Long, cursor: String?): ApiResult<NovaMessagePage> = error("unused")
    override suspend fun sendMessage(
        conversationId: Long,
        body: String,
        clientId: String,
        replyToId: Long?,
        imageUri: Uri?,
        audioFile: File?,
        audioDurationMs: Long?,
    ): ApiResult<NovaMessage> = error("unused")

    override suspend fun editMessage(messageId: Long, body: String): ApiResult<NovaMessage> = error("unused")
    override suspend fun deleteMessage(messageId: Long): ApiResult<String> = error("unused")
    override suspend fun setReaction(messageId: Long, emoji: String?): ApiResult<List<NovaMessageReaction>> = error("unused")
    override suspend fun markRead(conversationId: Long): ApiResult<Int> = error("unused")
    override suspend fun realtimeAccessToken(): ApiResult<String> = error("unused")
}


private class FakePeopleRepository(
    private val results: MutableList<ApiResult<List<NovaPerson>>> = mutableListOf(ApiResult.Success(emptyList())),
    private val order: MutableList<String>? = null,
) : PeopleRepository {
    override suspend fun people(query: String): ApiResult<List<NovaPerson>> {
        order?.add("people")
        return results.removeFirst()
    }

    override suspend fun person(username: String): ApiResult<NovaPerson> = error("unused")
    override suspend fun setFollowing(username: String, follow: Boolean): ApiResult<NovaPerson> = error("unused")
    override suspend fun setBlocked(username: String, blocked: Boolean): ApiResult<Unit> = error("unused")
    override suspend fun report(username: String, reason: String, details: String): ApiResult<String> = error("unused")
}


private class FakeSharingRepository(
    private val results: MutableList<ApiResult<Unit>> = mutableListOf(),
    private val blockFirstCall: CompletableDeferred<Unit>? = null,
) : SharingRepository {
    val calls = mutableListOf<String>()
    private var callCount = 0

    private suspend fun result(call: String): ApiResult<Unit> {
        calls += call
        callCount += 1
        if (callCount == 1) blockFirstCall?.await()
        return if (results.isEmpty()) ApiResult.Success(Unit) else results.removeFirst()
    }

    override suspend fun sharePost(recipientUsername: String, postId: Long) =
        result("sharePost:$recipientUsername:$postId")

    override suspend fun shareReel(recipientUsername: String, reelId: Long) =
        result("shareReel:$recipientUsername:$reelId")

    override suspend fun shareProfile(recipientUsername: String, profileUsername: String) =
        result("shareProfile:$recipientUsername:$profileUsername")

    override suspend fun sharePostToConversation(conversationId: Long, postId: Long) =
        result("sharePostToConversation:$conversationId:$postId")

    override suspend fun shareReelToConversation(conversationId: Long, reelId: Long) =
        result("shareReelToConversation:$conversationId:$reelId")

    override suspend fun shareProfileToConversation(conversationId: Long, profileUsername: String) =
        result("shareProfileToConversation:$conversationId:$profileUsername")

    override suspend fun addPostToStory(postId: Long, caption: String, audience: String) =
        result("addPostToStory:$postId:$audience")

    override suspend fun addReelToStory(reelId: Long, caption: String, audience: String) =
        result("addReelToStory:$reelId:$audience")
}


private fun person(id: Long, username: String) = NovaPerson(
    id = id,
    username = username,
    name = username.replaceFirstChar { it.uppercase() },
    avatarUrl = "",
    followersCount = 0,
    followingCount = 0,
    postsCount = 0,
    isFollowing = false,
)


private fun conversation(
    id: Long,
    username: String,
    group: Boolean = false,
    title: String = "",
): NovaConversation {
    val author = NovaPostAuthor(
        id = id,
        username = username,
        name = username.replaceFirstChar { it.uppercase() },
        avatarUrl = "",
    )
    return NovaConversation(
        id = id,
        otherUser = author,
        lastMessage = null,
        unreadCount = 0,
        createdAt = "",
        updatedAt = "",
        kind = if (group) "group" else "direct",
        title = title,
        membersPreview = emptyList(),
        membersCount = if (group) 3 else 2,
    )
}
