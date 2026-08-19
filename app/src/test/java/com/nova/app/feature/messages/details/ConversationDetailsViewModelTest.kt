package com.nova.app.feature.messages.details

import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.messages.details.data.ConversationToolsRepository
import com.nova.app.feature.messages.details.model.ConversationMediaPage
import com.nova.app.feature.messages.details.model.ConversationMessageContext
import com.nova.app.feature.messages.details.model.ConversationToolMessage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class ConversationDetailsViewModelTest {
    @Test
    fun queryIsCappedTrimmedAndDebouncedByTheStateOwner() = runBlocking {
        val fake = FakeConversationToolsRepository()
        val viewModel = viewModel(fake, this)

        viewModel.onQueryChanged("x".repeat(205))
        yield()

        assertEquals(200, viewModel.state.query.length)
        assertEquals(listOf(SearchCall(CONVERSATION_ID, "x".repeat(200))), fake.searchCalls)
        assertFalse(viewModel.state.searchLoading)
    }

    @Test
    fun blankQueryClearsResultsWithoutCallingTheRepository() = runBlocking {
        val fake = FakeConversationToolsRepository().apply {
            searchResult = ApiResult.Success(listOf(message(1L)))
        }
        val viewModel = viewModel(fake, this)
        viewModel.onQueryChanged("nova")
        yield()
        assertEquals(listOf(1L), viewModel.state.searchResults.map { it.id })

        viewModel.onQueryChanged("   ")

        assertTrue(viewModel.state.searchResults.isEmpty())
        assertNull(viewModel.state.searchError)
        assertEquals(1, fake.searchCalls.size)
    }

    @Test
    fun mediaTabLoadsOnceAndActiveFilterRetainsLegacyNoRelaunchBehavior() = runBlocking {
        val fake = FakeConversationToolsRepository().apply {
            mediaResult = ApiResult.Success(
                ConversationMediaPage(
                    items = listOf(message(2L)),
                    nextCursor = "page-2",
                )
            )
        }
        val viewModel = viewModel(fake, this)

        viewModel.onTabSelected(ConversationDetailsTab.Media)
        yield()

        assertEquals(listOf(MediaCall(CONVERSATION_ID, "all", null)), fake.mediaCalls)
        assertEquals(listOf(2L), viewModel.state.mediaItems.map { it.id })
        assertEquals("page-2", viewModel.state.mediaCursor)

        viewModel.onMediaTypeChanged("all")
        yield()

        assertTrue(viewModel.state.mediaItems.isEmpty())
        assertEquals(1, fake.mediaCalls.size)
    }

    @Test
    fun legacyLoadMoreRequestEntersLoadingWithoutIssuingAnotherRequest() = runBlocking {
        val fake = FakeConversationToolsRepository().apply {
            mediaResult = ApiResult.Success(
                ConversationMediaPage(
                    items = listOf(message(3L)),
                    nextCursor = "page-2",
                )
            )
        }
        val viewModel = viewModel(fake, this)
        viewModel.onTabSelected(ConversationDetailsTab.Media)
        yield()

        viewModel.onLoadMore()
        yield()

        assertTrue(viewModel.state.mediaLoading)
        assertEquals("page-2", viewModel.state.mediaCursor)
        assertEquals(1, fake.mediaCalls.size)
    }

    @Test
    fun contextTargetOwnsLoadingAndCloseClearsTheContext() = runBlocking {
        val fake = FakeConversationToolsRepository().apply {
            contextResult = ApiResult.Success(
                ConversationMessageContext(
                    items = listOf(message(4L)),
                    targetMessageId = 4L,
                    hasEarlier = true,
                    hasLater = false,
                )
            )
        }
        val viewModel = viewModel(fake, this)

        viewModel.openContext(4L)
        yield()

        assertEquals(4L, viewModel.state.contextTargetId)
        assertEquals(4L, viewModel.state.messageContext?.targetMessageId)
        assertFalse(viewModel.state.contextLoading)

        viewModel.closeContext()

        assertNull(viewModel.state.contextTargetId)
        assertNull(viewModel.state.messageContext)
        assertNull(viewModel.state.contextError)
    }

    @Test
    fun muteToggleUsesCurrentStateAndTerminal401EmitsSessionExpiry() = runBlocking {
        val fake = FakeConversationToolsRepository().apply {
            mutedResult = ApiResult.Success(true)
        }
        val viewModel = viewModel(fake, this)
        viewModel.loadMuted()
        assertTrue(viewModel.state.muted)

        fake.setMutedResult = ApiResult.Success(false)
        viewModel.toggleMute()
        yield()

        assertEquals(listOf(MuteCall(CONVERSATION_ID, false)), fake.muteCalls)
        assertFalse(viewModel.state.muted)
        assertFalse(viewModel.state.muteSaving)

        fake.searchResult = ApiResult.Failure("expired", 401)
        viewModel.onQueryChanged("nova")
        yield()

        assertEquals(1, viewModel.state.sessionExpiryVersion)
        assertNull(viewModel.state.searchError)
    }

    private fun viewModel(
        repository: ConversationToolsRepository,
        scope: kotlinx.coroutines.CoroutineScope,
    ) = ConversationDetailsViewModel(
        conversationId = CONVERSATION_ID,
        repository = repository,
        workScope = scope,
        searchDebounceMillis = 0L,
    )

    private class FakeConversationToolsRepository : ConversationToolsRepository {
        var searchResult: ApiResult<List<ConversationToolMessage>> = ApiResult.Success(emptyList())
        var contextResult: ApiResult<ConversationMessageContext> = ApiResult.Success(
            ConversationMessageContext(emptyList(), 0L, false, false)
        )
        var mediaResult: ApiResult<ConversationMediaPage> = ApiResult.Success(
            ConversationMediaPage(emptyList(), null)
        )
        var mutedResult: ApiResult<Boolean> = ApiResult.Success(false)
        var setMutedResult: ApiResult<Boolean> = ApiResult.Success(false)

        val searchCalls = mutableListOf<SearchCall>()
        val contextCalls = mutableListOf<ContextCall>()
        val mediaCalls = mutableListOf<MediaCall>()
        val muteCalls = mutableListOf<MuteCall>()

        override suspend fun searchMessages(
            conversationId: Long,
            query: String,
        ): ApiResult<List<ConversationToolMessage>> {
            searchCalls += SearchCall(conversationId, query)
            return searchResult
        }

        override suspend fun messageContext(
            conversationId: Long,
            messageId: Long,
        ): ApiResult<ConversationMessageContext> {
            contextCalls += ContextCall(conversationId, messageId)
            return contextResult
        }

        override suspend fun sharedMedia(
            conversationId: Long,
            type: String,
            cursor: String?,
        ): ApiResult<ConversationMediaPage> {
            mediaCalls += MediaCall(conversationId, type, cursor)
            return mediaResult
        }

        override suspend fun isMuted(conversationId: Long): ApiResult<Boolean> = mutedResult

        override suspend fun setMuted(conversationId: Long, muted: Boolean): ApiResult<Boolean> {
            muteCalls += MuteCall(conversationId, muted)
            return setMutedResult
        }
    }

    private data class SearchCall(val conversationId: Long, val query: String)
    private data class ContextCall(val conversationId: Long, val messageId: Long)
    private data class MediaCall(val conversationId: Long, val type: String, val cursor: String?)
    private data class MuteCall(val conversationId: Long, val muted: Boolean)

    private companion object {
        const val CONVERSATION_ID = 42L
    }
}


private fun message(id: Long) = ConversationToolMessage(
    id = id,
    sender = NovaPostAuthor(id, "user$id", "User $id", ""),
    body = "message $id",
    imageUrl = "",
    audioUrl = "",
    audioDurationMs = null,
    replyToId = null,
    replyPreview = "",
    createdAt = "",
    isMine = false,
    isDeleted = false,
)
