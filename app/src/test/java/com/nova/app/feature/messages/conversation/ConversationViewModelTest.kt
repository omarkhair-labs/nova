package com.nova.app.feature.messages.conversation

import com.nova.app.core.messaging.NovaConversationPresence
import com.nova.app.core.messaging.NovaMessageDeletedEvent
import com.nova.app.core.messaging.NovaMessageReactionEvent
import com.nova.app.core.messaging.NovaMessageUpdatedEvent
import com.nova.app.core.messaging.NovaRealtimeEvent
import com.nova.app.core.messaging.NovaRealtimeStatus
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.messages.data.FakeMessagesRepository
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaMessagePage
import com.nova.app.feature.messages.domain.model.NovaMessageReaction
import com.nova.app.feature.messages.domain.model.NovaReplyPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class ConversationViewModelTest {
    @Test
    fun latestLoadCapturesUnreadMarksReadAndPagesWithoutDuplicates() = runBlocking {
        val repository = FakeMessagesRepository().apply {
            messagesResult = page(
                messages = listOf(message(1L, isMine = false), message(2L, isMine = true)),
                nextCursor = "earlier",
            )
            markReadResult = ApiResult.Success(1)
        }
        val viewModel = viewModel(repository)

        viewModel.loadLatestNow(showSpinner = true, scrollToBottom = true)
        yield()

        assertEquals(listOf(1L, 2L), viewModel.state.messages.map { it.id })
        assertEquals(1L, viewModel.state.unreadAnchorMessageId)
        assertEquals(1, viewModel.state.unreadCountAtOpen)
        assertEquals(1, viewModel.state.conversationReadVersion)
        assertEquals(1, viewModel.state.scrollRequestVersion)
        assertFalse(viewModel.state.scrollRequestAnimated)

        repository.messagesResult = page(
            messages = listOf(message(0L, isMine = false), message(1L, isMine = false)),
            nextCursor = null,
        )
        viewModel.loadEarlierNow()

        assertEquals(listOf(0L, 1L, 2L), viewModel.state.messages.map { it.id })
        assertEquals(
            listOf(
                FakeMessagesRepository.MessagesCall(CONVERSATION_ID, null),
                FakeMessagesRepository.MessagesCall(CONVERSATION_ID, "earlier"),
            ),
            repository.messagesCalls,
        )
    }

    @Test
    fun optimisticFailureCanRetryWithTheSameClientId() = runBlocking {
        val repository = FakeMessagesRepository().apply {
            sendMessageResult = ApiResult.Failure("offline")
        }
        val viewModel = viewModel(repository)
        viewModel.onDraftChanged("  hello  ")

        assertTrue(viewModel.send(imageUri = null, audioFile = null, audioDurationMs = null))
        assertEquals(PendingMessageStatus.Sending, viewModel.state.pendingMessages.single().status)
        yield()

        val failed = viewModel.state.pendingMessages.single()
        assertEquals(PendingMessageStatus.Failed, failed.status)
        assertEquals("offline", failed.error)
        repository.sendMessageResult = ApiResult.Success(
            message(id = 9L, isMine = true, clientId = failed.clientId, body = "hello")
        )

        viewModel.retryPending(failed)
        yield()

        assertTrue(viewModel.state.pendingMessages.isEmpty())
        assertEquals(listOf(9L), viewModel.state.messages.map { it.id })
        assertEquals(listOf("fixed-client", "fixed-client"), repository.sendMessageCalls.map { it.clientId })
        assertEquals(listOf("hello", "hello"), repository.sendMessageCalls.map { it.body })
    }

    @Test
    fun editDeleteAndReactionMutationsReconcileOwnedState() = runBlocking {
        val first = message(1L, isMine = true, body = "before")
        val second = message(2L, isMine = true, body = "second")
        val repository = FakeMessagesRepository().apply {
            messagesResult = page(listOf(first, second))
            markReadResult = ApiResult.Success(0)
        }
        val viewModel = viewModel(repository)
        viewModel.loadLatestNow(showSpinner = true, scrollToBottom = false)
        yield()

        viewModel.startEdit(first)
        viewModel.onDraftChanged("after")
        repository.editMessageResult = ApiResult.Success(first.copy(body = "after"))
        viewModel.saveEditNow(first, "after")

        assertEquals("after", viewModel.state.messages.first().body)
        assertNull(viewModel.state.editingTarget)

        repository.setReactionResult = ApiResult.Success(
            listOf(NovaMessageReaction("❤️", count = 1, reactedByMe = true))
        )
        viewModel.setReactionNow(second.id, "❤️")
        assertTrue(viewModel.state.messages.last().reactions.single().reactedByMe)

        viewModel.confirmDelete(second)
        repository.deleteMessageResult = ApiResult.Success("2026-08-14T01:00:00Z")
        viewModel.deleteForEveryoneNow(second)

        assertTrue(viewModel.state.messages.last().isDeleted)
        assertTrue(viewModel.state.messages.last().reactions.isEmpty())
        assertEquals(listOf(second.id), repository.deletedMessageIds)
    }

    @Test
    fun realtimeReconcilesIncomingReceiptsUpdatesDeletionAndReactions() = runBlocking {
        val original = message(1L, isMine = true, body = "before")
        val reply = message(
            id = 2L,
            isMine = true,
            replyTo = preview(original),
        )
        val repository = FakeMessagesRepository().apply {
            messagesResult = page(listOf(original, reply))
            markReadResult = ApiResult.Success(0)
        }
        val realtime = FakeConversationRealtime()
        val viewModel = viewModel(repository, realtime)
        viewModel.loadLatestNow(showSpinner = true, scrollToBottom = false)
        yield()
        viewModel.startEdit(original)

        viewModel.applyMessageUpdate(
            NovaMessageUpdatedEvent(original.id, body = "edited", editedAt = "edited-at")
        )
        viewModel.applyReactionEvent(
            NovaMessageReactionEvent(original.id, "😂", count = 2, active = true, isMine = true)
        )

        assertEquals("edited", viewModel.state.messages.first().body)
        assertEquals("edited", viewModel.state.messages.last().replyTo?.body)
        assertEquals("😂", viewModel.state.messages.first().reactions.single().emoji)
        assertEquals("edited", viewModel.state.editingTarget?.body)

        viewModel.applyMessageDelete(
            NovaMessageDeletedEvent(original.id, deletedAt = "deleted-at")
        )

        assertTrue(viewModel.state.messages.first().isDeleted)
        assertTrue(viewModel.state.messages.last().replyTo?.isDeleted == true)
        assertNull(viewModel.state.editingTarget)

        val incoming = message(3L, isMine = false)
        viewModel.onNearBottomChanged(false)
        viewModel.onRealtimeEvent(NovaRealtimeEvent.MessageCreated(incoming))
        yield()
        assertEquals(1, viewModel.state.newMessagesAwayCount)

        viewModel.onRealtimeEvent(
            NovaRealtimeEvent.MessagesDelivered(7L, "delivered-at", setOf(reply.id))
        )
        viewModel.onRealtimeEvent(
            NovaRealtimeEvent.ConversationRead(7L, "read-at", setOf(reply.id))
        )
        assertEquals("delivered-at", viewModel.state.messages[1].deliveredAt)
        assertEquals("read-at", viewModel.state.messages[1].readAt)
    }

    @Test
    fun realtimeLifecycleOwnsPresenceTypingDraftsAndSessionEffect() = runBlocking {
        val repository = FakeMessagesRepository()
        val realtime = FakeConversationRealtime()
        val drafts = InMemoryDraftStore(mutableMapOf(CONVERSATION_ID to "saved"))
        val viewModel = viewModel(
            repository = repository,
            realtime = realtime,
            drafts = drafts,
            draftDebounceMillis = 0L,
            typingWindowMillis = 0L,
        )

        assertEquals("saved", viewModel.state.draft)
        viewModel.startRealtime()
        realtime.emitStatus(NovaRealtimeStatus.Live)
        realtime.emitPresence(NovaConversationPresence(7L, USERNAME, true, null))
        viewModel.onDraftChanged("hello")
        repeat(3) { yield() }

        assertEquals(listOf(true, false), realtime.typing)
        assertEquals("hello", drafts.values[CONVERSATION_ID])
        assertTrue(viewModel.state.otherPresence?.isOnline == true)

        realtime.emitSessionExpired()
        assertEquals(1, viewModel.state.sessionExpiryVersion)
        viewModel.stopRealtime()
        assertEquals(1, realtime.stopCount)
    }

    @Test
    fun terminalLoadFailureEmitsSessionExpiryWithoutInlineError() = runBlocking {
        val repository = FakeMessagesRepository().apply {
            messagesResult = ApiResult.Failure("expired", statusCode = 401)
        }
        val viewModel = viewModel(repository)

        viewModel.loadLatestNow(showSpinner = true, scrollToBottom = false)

        assertEquals(1, viewModel.state.sessionExpiryVersion)
        assertNull(viewModel.state.errorMessage)
        assertFalse(viewModel.state.isLoading)
    }

    private fun CoroutineScope.viewModel(
        repository: FakeMessagesRepository,
        realtime: FakeConversationRealtime = FakeConversationRealtime(),
        drafts: InMemoryDraftStore = InMemoryDraftStore(),
        draftDebounceMillis: Long = 0L,
        typingWindowMillis: Long = 0L,
    ) = ConversationViewModel(
        conversationId = CONVERSATION_ID,
        username = USERNAME,
        repository = repository,
        realtime = realtime,
        draftStore = drafts,
        currentAuthor = { author(id = 99L, username = "me") },
        workScope = this,
        clientIdFactory = { "fixed-client" },
        localIdFactory = { -1L },
        now = { "2026-08-14T00:00:00Z" },
        draftDebounceMillis = draftDebounceMillis,
        typingWindowMillis = typingWindowMillis,
    )

    private fun page(
        messages: List<NovaMessage>,
        nextCursor: String? = null,
    ) = ApiResult.Success(NovaMessagePage(messages, nextCursor))

    private fun message(
        id: Long,
        isMine: Boolean,
        clientId: String = "client-$id",
        body: String = "message-$id",
        replyTo: NovaReplyPreview? = null,
    ) = NovaMessage(
        id = id,
        clientId = clientId,
        sender = if (isMine) author(99L, "me") else author(7L, USERNAME),
        body = body,
        imageUrl = "",
        replyTo = replyTo,
        reactions = emptyList(),
        createdAt = "2026-08-14T00:00:00Z",
        deliveredAt = null,
        readAt = null,
        isMine = isMine,
    )

    private fun preview(message: NovaMessage) = NovaReplyPreview(
        id = message.id,
        sender = message.sender,
        body = message.body,
        imageUrl = message.imageUrl,
        audioUrl = message.audioUrl,
        audioDurationMs = message.audioDurationMs,
        isDeleted = message.isDeleted,
    )

    private fun author(id: Long, username: String) = NovaPostAuthor(id, username, username, "")

    private class InMemoryDraftStore(
        val values: MutableMap<Long, String> = mutableMapOf(),
    ) : ConversationDraftStore {
        override fun load(conversationId: Long): String = values[conversationId].orEmpty()

        override fun save(conversationId: Long, draft: String) {
            values[conversationId] = draft
        }

        override fun remove(conversationId: Long) {
            values.remove(conversationId)
        }
    }

    private class FakeConversationRealtime : ConversationRealtime {
        private var onEvent: (NovaRealtimeEvent) -> Unit = {}
        private var onStatus: (NovaRealtimeStatus) -> Unit = {}
        private var onSessionExpired: () -> Unit = {}
        private var onPresence: (NovaConversationPresence) -> Unit = {}
        private var onReaction: (NovaMessageReactionEvent) -> Unit = {}
        private var onMessageUpdated: (NovaMessageUpdatedEvent) -> Unit = {}
        private var onMessageDeleted: (NovaMessageDeletedEvent) -> Unit = {}

        val typing = mutableListOf<Boolean>()
        var stopCount = 0

        override fun start(
            scope: CoroutineScope,
            onEvent: (NovaRealtimeEvent) -> Unit,
            onStatus: (NovaRealtimeStatus) -> Unit,
            onSessionExpired: () -> Unit,
            onPresence: (NovaConversationPresence) -> Unit,
            onReaction: (NovaMessageReactionEvent) -> Unit,
            onMessageUpdated: (NovaMessageUpdatedEvent) -> Unit,
            onMessageDeleted: (NovaMessageDeletedEvent) -> Unit,
        ) {
            this.onEvent = onEvent
            this.onStatus = onStatus
            this.onSessionExpired = onSessionExpired
            this.onPresence = onPresence
            this.onReaction = onReaction
            this.onMessageUpdated = onMessageUpdated
            this.onMessageDeleted = onMessageDeleted
        }

        override fun stop() {
            stopCount += 1
        }

        override fun sendTyping(isTyping: Boolean) {
            typing += isTyping
        }

        fun emitStatus(status: NovaRealtimeStatus) = onStatus(status)

        fun emitPresence(presence: NovaConversationPresence) = onPresence(presence)

        fun emitSessionExpired() = onSessionExpired()
    }

    private companion object {
        const val CONVERSATION_ID = 42L
        const val USERNAME = "alice"
    }
}
