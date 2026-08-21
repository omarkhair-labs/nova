package com.nova.app.feature.messages

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.data.FakeMessagesRepository
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test


class NewMessageViewModelTest {
    @Test
    fun searchPreservesQueryCapAndPublishesPeople() = runBlocking {
        val queries = mutableListOf<String>()
        val viewModel = NewMessageViewModel(
            messagesRepository = FakeMessagesRepository(),
            peopleSearch = { query ->
                queries += query
                ApiResult.Success(listOf(person("alice")))
            },
            debounceMillis = 0L,
            workScope = this,
        )
        yield()

        viewModel.updateQuery("x".repeat(50))
        yield()

        assertEquals(40, viewModel.state.query.length)
        assertEquals("x".repeat(40), queries.last())
        assertEquals(listOf("alice"), viewModel.state.people.map { it.username })
        assertFalse(viewModel.state.isLoading)
    }

    @Test
    fun successfulOpenUsesStableMessagesRepositoryAndPublishesConversation() = runBlocking {
        val messages = FakeMessagesRepository().apply {
            openConversationResult = ApiResult.Success(conversation())
        }
        val viewModel = NewMessageViewModel(
            messagesRepository = messages,
            peopleSearch = { ApiResult.Success(emptyList()) },
            debounceMillis = 0L,
            workScope = this,
        )
        yield()

        viewModel.openConversation(person("alice"))
        yield()

        assertEquals(listOf("alice"), messages.openedUsernames)
        assertEquals(1, viewModel.state.conversationReadyVersion)
        assertEquals(42L, viewModel.state.readyConversation?.id)
        assertNull(viewModel.state.openingUsername)
    }

    @Test
    fun terminal401FromSearchProducesSessionEffectWithoutInlineError() = runBlocking {
        val viewModel = NewMessageViewModel(
            messagesRepository = FakeMessagesRepository(),
            peopleSearch = { ApiResult.Failure("expired", 401) },
            debounceMillis = 0L,
            workScope = this,
        )
        yield()

        assertEquals(1, viewModel.state.sessionExpiryVersion)
        assertNull(viewModel.state.errorMessage)
        assertFalse(viewModel.state.isLoading)
    }

    @Test
    fun failedOpenUnlocksDialogAndKeepsExistingPeople() = runBlocking {
        val messages = FakeMessagesRepository().apply {
            openConversationResult = ApiResult.Failure("could not open", 500)
        }
        val viewModel = NewMessageViewModel(
            messagesRepository = messages,
            peopleSearch = { ApiResult.Success(listOf(person("alice"))) },
            debounceMillis = 0L,
            workScope = this,
        )
        yield()

        viewModel.openConversation(person("alice"))
        yield()

        assertNull(viewModel.state.openingUsername)
        assertEquals("could not open", viewModel.state.errorMessage)
        assertEquals(listOf("alice"), viewModel.state.people.map { it.username })
    }

    private companion object {
        fun person(username: String) = NovaPerson(
            id = 1L,
            username = username,
            name = "Alice",
            avatarUrl = "",
            followersCount = 0,
            followingCount = 0,
            postsCount = 0,
            isFollowing = false,
        )

        fun conversation() = NovaConversation(
            id = 42L,
            otherUser = NovaPostAuthor(1L, "alice", "Alice", ""),
            lastMessage = null,
            unreadCount = 0,
            createdAt = "",
            updatedAt = "",
        )
    }
}
