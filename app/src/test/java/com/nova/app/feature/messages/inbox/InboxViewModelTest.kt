package com.nova.app.feature.messages.inbox

import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.messages.data.FakeInboxRepository
import com.nova.app.feature.messages.data.InboxRepository
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.messages.domain.model.NovaConversationPage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class InboxViewModelTest {
    @Test
    fun successfulLoadOwnsPageAndUnreadEffectState() = runBlocking {
        val fake = FakeInboxRepository().apply {
            conversationsResult = successPage(ids = listOf(1L), unreadCount = 3, nextCursor = "next")
        }
        val viewModel = viewModel(fake)

        viewModel.loadInbox(search = "nova", reset = true, showSpinner = true)

        assertEquals(listOf(1L), viewModel.state.conversations.map { it.id })
        assertEquals(3, viewModel.state.unreadCount)
        assertEquals("next", viewModel.state.nextCursor)
        assertEquals(1, viewModel.state.unreadUpdateVersion)
        assertFalse(viewModel.state.isLoading)
        assertEquals(
            listOf(FakeInboxRepository.ConversationsCall("nova", null)),
            fake.calls,
        )
    }

    @Test
    fun nextPageKeepsExistingOrderAndDeduplicatesIds() = runBlocking {
        val fake = FakeInboxRepository().apply {
            conversationsResult = successPage(ids = listOf(1L), nextCursor = "page-2")
        }
        val viewModel = viewModel(fake)
        viewModel.loadInbox(search = "", reset = true, showSpinner = true)
        fake.conversationsResult = successPage(ids = listOf(1L, 2L), nextCursor = null)

        viewModel.loadInbox(search = "", reset = false, showSpinner = false)

        assertEquals(listOf(1L, 2L), viewModel.state.conversations.map { it.id })
        assertEquals(
            listOf(
                FakeInboxRepository.ConversationsCall("", null),
                FakeInboxRepository.ConversationsCall("", "page-2"),
            ),
            fake.calls,
        )
    }

    @Test
    fun staleResponseCannotReplaceTheLatestSearch() = runBlocking {
        val first = CompletableDeferred<ApiResult<NovaConversationPage>>()
        val second = CompletableDeferred<ApiResult<NovaConversationPage>>()
        val repository = ControlledInboxRepository(listOf(first, second))
        val viewModel = viewModel(repository)

        val firstJob = launch {
            viewModel.loadInbox(search = "first", reset = true, showSpinner = true)
        }
        yield()
        val secondJob = launch {
            viewModel.loadInbox(search = "second", reset = true, showSpinner = true)
        }
        yield()
        second.complete(successPage(ids = listOf(2L)))
        secondJob.join()
        first.complete(successPage(ids = listOf(1L)))
        firstJob.join()

        assertEquals(listOf(2L), viewModel.state.conversations.map { it.id })
        assertEquals(listOf("first", "second"), repository.queries)
    }

    @Test
    fun terminalUnauthorizedFailureEmitsSessionExpiryWithoutInlineError() = runBlocking {
        val fake = FakeInboxRepository().apply {
            conversationsResult = ApiResult.Failure("expired", statusCode = 401)
        }
        val viewModel = viewModel(fake)

        viewModel.loadInbox(search = "", reset = true, showSpinner = true)

        assertEquals(1, viewModel.state.sessionExpiryVersion)
        assertNull(viewModel.state.errorMessage)
        assertFalse(viewModel.state.isLoading)
    }

    @Test
    fun queryIsCappedAndDebouncedByTheStateOwner() = runBlocking {
        val fake = FakeInboxRepository().apply {
            conversationsResult = successPage(ids = emptyList())
        }
        val viewModel = InboxViewModel(
            repository = fake,
            workScope = this,
            searchDebounceMillis = 0L,
            autoLoad = false,
        )

        viewModel.onQueryChanged("x".repeat(45))
        yield()

        assertEquals(40, viewModel.state.query.length)
        assertEquals(listOf("x".repeat(40)), fake.calls.map { it.query })
        assertTrue(viewModel.state.conversations.isEmpty())
    }

    private fun viewModel(repository: InboxRepository) = InboxViewModel(
        repository = repository,
        autoLoad = false,
    )

    private fun successPage(
        ids: List<Long>,
        unreadCount: Int = 0,
        nextCursor: String? = null,
    ): ApiResult.Success<NovaConversationPage> = ApiResult.Success(
        NovaConversationPage(
            conversations = ids.map(::conversation),
            unreadCount = unreadCount,
            nextCursor = nextCursor,
        )
    )

    private fun conversation(id: Long) = NovaConversation(
        id = id,
        otherUser = NovaPostAuthor(id, "user$id", "User $id", ""),
        lastMessage = null,
        unreadCount = 0,
        createdAt = "",
        updatedAt = "",
    )

    private class ControlledInboxRepository(
        private val responses: List<CompletableDeferred<ApiResult<NovaConversationPage>>>,
    ) : InboxRepository {
        val queries = mutableListOf<String>()

        override suspend fun conversations(
            query: String,
            cursor: String?,
        ): ApiResult<NovaConversationPage> {
            val index = queries.size
            queries += query
            return responses[index].await()
        }
    }
}
