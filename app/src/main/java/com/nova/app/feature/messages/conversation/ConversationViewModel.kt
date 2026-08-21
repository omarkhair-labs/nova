package com.nova.app.feature.messages.conversation

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.app.core.auth.shouldExpireNovaSession
import com.nova.app.core.messaging.NovaConversationPresence
import com.nova.app.core.messaging.NovaMessageDeletedEvent
import com.nova.app.core.messaging.NovaMessageReactionEvent
import com.nova.app.core.messaging.NovaMessageUpdatedEvent
import com.nova.app.core.messaging.NovaRealtimeEvent
import com.nova.app.core.messaging.NovaRealtimeStatus
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.data.MessagesRepository
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaMessageReaction
import com.nova.app.feature.messages.domain.model.NovaReplyPreview
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/** Lifecycle-aware owner for conversation data, mutations, drafts, and realtime reconciliation. */
class ConversationViewModel internal constructor(
    private val conversationId: Long,
    private val username: String,
    private val repository: MessagesRepository,
    private val realtime: ConversationRealtime,
    private val draftStore: ConversationDraftStore,
    private val currentAuthor: () -> NovaPostAuthor,
    private val workScope: CoroutineScope? = null,
    private val clientIdFactory: () -> String = { UUID.randomUUID().toString() },
    private val localIdFactory: () -> Long = { -System.nanoTime() },
    private val now: () -> String = { Instant.now().toString() },
    private val draftDebounceMillis: Long = DRAFT_DEBOUNCE_MS,
    private val typingWindowMillis: Long = TYPING_WINDOW_MS,
) : ViewModel() {
    var state by mutableStateOf(
        ConversationUiState(draft = draftStore.load(conversationId))
    )
        private set

    private val baseScope: CoroutineScope
        get() = workScope ?: viewModelScope
    private var activeJob: Job? = null
    private var activeScope: CoroutineScope? = null
    private var draftJob: Job? = null
    private var typingJob: Job? = null
    private var typingAnnounced = false
    private var recording = false
    private var nearBottom = true

    fun startRealtime() {
        if (activeJob?.isActive == true) return
        val job = SupervisorJob(baseScope.coroutineContext[Job])
        val scope = CoroutineScope(baseScope.coroutineContext + job)
        activeJob = job
        activeScope = scope
        realtime.start(
            scope = scope,
            onEvent = ::onRealtimeEvent,
            onStatus = ::onRealtimeStatus,
            onSessionExpired = ::emitSessionExpiry,
            onPresence = ::onPresence,
            onReaction = ::applyReactionEvent,
            onMessageUpdated = ::applyMessageUpdate,
            onMessageDeleted = ::applyMessageDelete,
        )
    }

    fun stopRealtime() {
        stopTyping()
        realtime.stop()
        activeJob?.cancel()
        activeJob = null
        activeScope = null
        draftJob = null
        typingJob = null
        state.pendingMessages.forEach(::deletePendingAudio)
        recording = false
        nearBottom = true
        state = ConversationUiState(draft = draftStore.load(conversationId))
    }

    fun loadLatest(
        showSpinner: Boolean = true,
        scrollToBottom: Boolean = false,
    ) {
        scope().launch { loadLatestNow(showSpinner, scrollToBottom) }
    }

    internal suspend fun loadLatestNow(
        showSpinner: Boolean,
        scrollToBottom: Boolean,
    ) {
        if (showSpinner) {
            state = state.copy(isLoading = true, errorMessage = null)
        }
        when (val result = repository.messages(conversationId)) {
            is ApiResult.Success -> {
                val unread = if (!state.initialUnreadCaptured && username != GROUP_USERNAME) {
                    result.value.messages.filter { !it.isMine && it.readAt == null }
                } else {
                    emptyList()
                }
                state = state.copy(
                    messages = result.value.messages,
                    nextCursor = result.value.nextCursor,
                    isLoading = false,
                    initialUnreadCaptured = true,
                    unreadAnchorMessageId = if (state.initialUnreadCaptured) {
                        state.unreadAnchorMessageId
                    } else {
                        unread.firstOrNull()?.id
                    },
                    unreadCountAtOpen = if (state.initialUnreadCaptured) {
                        state.unreadCountAtOpen
                    } else {
                        unread.size
                    },
                )
                markConversationRead()
                if (scrollToBottom && state.messages.isNotEmpty()) requestScrollLatest(animated = false)
            }

            is ApiResult.Failure -> {
                state = state.copy(isLoading = false)
                if (shouldExpireNovaSession(result.statusCode)) {
                    emitSessionExpiry()
                } else if (showSpinner || state.messages.isEmpty()) {
                    state = state.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun loadEarlier() {
        if (state.nextCursor == null || state.isLoadingEarlier) return
        scope().launch { loadEarlierNow() }
    }

    internal suspend fun loadEarlierNow() {
        val cursor = state.nextCursor ?: return
        if (state.isLoadingEarlier) return
        state = state.copy(isLoadingEarlier = true)
        when (val result = repository.messages(conversationId, cursor)) {
            is ApiResult.Success -> {
                val existingIds = state.messages.mapTo(mutableSetOf()) { it.id }
                state = state.copy(
                    messages = result.value.messages.filterNot { it.id in existingIds } + state.messages,
                    nextCursor = result.value.nextCursor,
                )
            }

            is ApiResult.Failure -> {
                if (shouldExpireNovaSession(result.statusCode)) emitSessionExpiry()
                else state = state.copy(errorMessage = result.message)
            }
        }
        state = state.copy(isLoadingEarlier = false)
    }

    fun onDraftChanged(value: String) {
        state = state.copy(draft = value.take(MAX_DRAFT_LENGTH))
        scheduleDraftPersistence()
        scheduleTyping()
    }

    fun onRecordingChanged(isRecording: Boolean) {
        recording = isRecording
        if (isRecording) stopTyping() else scheduleTyping()
    }

    fun startReply(message: NovaMessage) {
        state = if (state.editingTarget == null) {
            state.copy(replyTarget = message, actionsForMessageId = null)
        } else {
            state.copy(
                draft = state.draftBeforeEdit,
                draftBeforeEdit = "",
                replyTarget = message,
                editingTarget = null,
                actionsForMessageId = null,
                errorMessage = null,
            )
        }
        scheduleDraftPersistence()
        scheduleTyping()
    }

    fun cancelReply() {
        state = state.copy(replyTarget = null)
    }

    fun startEdit(message: NovaMessage): Boolean {
        if (
            !message.isMine || message.isDeleted || message.share != null ||
            message.isCallHistory() || state.mutatingMessageId != null
        ) return false
        stopTyping()
        state = state.copy(
            draftBeforeEdit = state.draft,
            draft = message.body,
            replyTarget = null,
            editingTarget = message,
            actionsForMessageId = null,
            errorMessage = null,
        )
        draftJob?.cancel()
        return true
    }

    fun cancelEdit() {
        state = state.copy(
            draft = state.draftBeforeEdit,
            draftBeforeEdit = "",
            editingTarget = null,
            errorMessage = null,
        )
        scheduleDraftPersistence()
        scheduleTyping()
    }

    fun saveEdit() {
        val target = state.editingTarget ?: return
        if (
            target.isDeleted || target.share != null || target.isCallHistory() ||
            state.mutatingMessageId != null
        ) return
        val body = state.draft.trim()
        if (body == target.body) {
            cancelEdit()
            return
        }
        if (body.isBlank() && target.imageUrl.isBlank() && target.audioUrl.isBlank()) return
        scope().launch { saveEditNow(target, body) }
    }

    internal suspend fun saveEditNow(target: NovaMessage, body: String) {
        state = state.copy(mutatingMessageId = target.id, errorMessage = null)
        when (val result = repository.editMessage(target.id, body)) {
            is ApiResult.Success -> {
                state = state.copy(
                    messages = state.messages.map { if (it.id == target.id) result.value else it },
                    draft = state.draftBeforeEdit,
                    draftBeforeEdit = "",
                    editingTarget = null,
                )
                scheduleDraftPersistence()
                scheduleTyping()
            }

            is ApiResult.Failure -> {
                if (shouldExpireNovaSession(result.statusCode)) emitSessionExpiry()
                else state = state.copy(errorMessage = result.message)
            }
        }
        state = state.copy(mutatingMessageId = null)
    }

    fun confirmDelete(message: NovaMessage) {
        if (!message.isMine || message.isDeleted || state.mutatingMessageId != null) return
        state = state.copy(deleteTarget = message, actionsForMessageId = null)
    }

    fun dismissDelete() {
        if (state.mutatingMessageId == null) state = state.copy(deleteTarget = null)
    }

    fun deleteForEveryone() {
        val target = state.deleteTarget ?: return
        if (state.mutatingMessageId != null) return
        scope().launch { deleteForEveryoneNow(target) }
    }

    internal suspend fun deleteForEveryoneNow(target: NovaMessage) {
        state = state.copy(mutatingMessageId = target.id, errorMessage = null)
        when (val result = repository.deleteMessage(target.id)) {
            is ApiResult.Success -> applyMessageDelete(
                NovaMessageDeletedEvent(messageId = target.id, deletedAt = result.value)
            )

            is ApiResult.Failure -> {
                if (shouldExpireNovaSession(result.statusCode)) emitSessionExpiry()
                else state = state.copy(errorMessage = result.message)
            }
        }
        state = state.copy(deleteTarget = null, mutatingMessageId = null)
    }

    fun setReaction(message: NovaMessage, emoji: String) {
        if (state.reactingMessageId != null || message.isDeleted) return
        val current = message.reactions.firstOrNull { it.reactedByMe }?.emoji
        val desired = if (current == emoji) null else emoji
        scope().launch { setReactionNow(message.id, desired) }
    }

    internal suspend fun setReactionNow(messageId: Long, emoji: String?) {
        state = state.copy(reactingMessageId = messageId)
        when (val result = repository.setReaction(messageId, emoji)) {
            is ApiResult.Success -> state = state.copy(
                messages = state.messages.map {
                    if (it.id == messageId) it.copy(reactions = result.value) else it
                },
                actionsForMessageId = null,
            )

            is ApiResult.Failure -> {
                if (shouldExpireNovaSession(result.statusCode)) emitSessionExpiry()
                else state = state.copy(errorMessage = result.message)
            }
        }
        state = state.copy(reactingMessageId = null)
    }

    fun send(
        imageUri: Uri?,
        audioFile: File?,
        audioDurationMs: Long?,
    ): Boolean {
        val body = state.draft.trim()
        if (
            (body.isBlank() && imageUri == null && audioFile == null) ||
            recording || state.editingTarget != null
        ) return false

        stopTyping()
        val pending = PendingMessage(
            localId = localIdFactory(),
            clientId = clientIdFactory(),
            sender = currentAuthor(),
            body = body,
            imageUri = imageUri?.toString().orEmpty(),
            audioPath = audioFile?.absolutePath.orEmpty(),
            audioDurationMs = audioDurationMs,
            replyTo = state.replyTarget?.let(::replyPreview),
            createdAt = now(),
            status = PendingMessageStatus.Sending,
        )
        state = state.copy(
            pendingMessages = state.pendingMessages + pending,
            draft = "",
            replyTarget = null,
            actionsForMessageId = null,
            errorMessage = null,
        )
        draftStore.remove(conversationId)
        requestScrollLatest(animated = true)
        sendPending(pending)
        return true
    }

    internal fun retryPending(pending: PendingMessage) {
        sendPending(pending)
    }

    fun toggleActions(messageId: Long) {
        val message = state.messages.firstOrNull { it.id == messageId } ?: return
        if (message.isDeleted) return
        state = state.copy(
            actionsForMessageId = if (state.actionsForMessageId == messageId) null else messageId
        )
    }

    fun setErrorMessage(message: String?) {
        state = state.copy(errorMessage = message)
    }

    fun onNearBottomChanged(value: Boolean) {
        nearBottom = value
        if (value && state.newMessagesAwayCount != 0) {
            state = state.copy(newMessagesAwayCount = 0)
        }
    }

    fun requestScrollLatest(animated: Boolean) {
        state = state.copy(
            scrollRequestVersion = state.scrollRequestVersion + 1,
            scrollRequestAnimated = animated,
        )
    }

    fun onScrollLatestCompleted() {
        if (state.newMessagesAwayCount != 0) state = state.copy(newMessagesAwayCount = 0)
    }

    internal fun onRealtimeEvent(event: NovaRealtimeEvent) {
        when (event) {
            is NovaRealtimeEvent.MessageCreated -> {
                val message = event.message
                if (state.pendingMessages.any { it.clientId == message.clientId }) {
                    completePending(message.clientId, message)
                } else if (state.messages.none { it.id == message.id }) {
                    val shouldFollow = nearBottom
                    state = state.copy(
                        messages = state.messages + message,
                        isOtherTyping = if (message.isMine) state.isOtherTyping else false,
                        newMessagesAwayCount = if (!message.isMine && !shouldFollow) {
                            state.newMessagesAwayCount + 1
                        } else {
                            state.newMessagesAwayCount
                        },
                    )
                    if (!message.isMine) markConversationRead()
                    if (shouldFollow) requestScrollLatest(animated = true)
                }
            }

            is NovaRealtimeEvent.MessagesDelivered -> {
                if (event.deliveredAt.isNotBlank()) {
                    state = state.copy(messages = state.messages.map { message ->
                        if (message.isMine && message.id in event.messageIds) {
                            message.copy(deliveredAt = event.deliveredAt)
                        } else {
                            message
                        }
                    })
                }
            }

            is NovaRealtimeEvent.ConversationRead -> {
                if (event.readAt.isNotBlank()) {
                    state = state.copy(messages = state.messages.map { message ->
                        if (message.isMine && message.id in event.messageIds) {
                            message.copy(
                                deliveredAt = message.deliveredAt ?: event.readAt,
                                readAt = event.readAt,
                            )
                        } else {
                            message
                        }
                    })
                }
            }

            is NovaRealtimeEvent.Typing -> state = state.copy(isOtherTyping = event.isTyping)
        }
    }

    internal fun applyReactionEvent(event: NovaMessageReactionEvent) {
        state = state.copy(messages = state.messages.map { message ->
            if (message.id != event.messageId || message.isDeleted) return@map message
            val reactions = message.reactions.associateBy { it.emoji }.toMutableMap()
            if (event.isMine && event.active) {
                reactions.keys.toList().forEach { key ->
                    reactions[key]?.let { reactions[key] = it.copy(reactedByMe = false) }
                }
            }
            if (event.count <= 0) {
                reactions.remove(event.emoji)
            } else {
                val existing = reactions[event.emoji]
                reactions[event.emoji] = NovaMessageReaction(
                    emoji = event.emoji,
                    count = event.count,
                    reactedByMe = if (event.isMine) event.active else existing?.reactedByMe == true,
                )
            }
            message.copy(reactions = reactions.values.sortedBy { reactionOrder(it.emoji) })
        })
    }

    internal fun applyMessageUpdate(event: NovaMessageUpdatedEvent) {
        state = state.copy(
            messages = state.messages.map { message ->
                val reply = message.replyTo?.let {
                    if (it.id == event.messageId && !it.isDeleted) it.copy(body = event.body) else it
                }
                if (message.id == event.messageId && !message.isDeleted) {
                    message.copy(body = event.body, editedAt = event.editedAt, replyTo = reply)
                } else {
                    message.copy(replyTo = reply)
                }
            },
            editingTarget = state.editingTarget?.let {
                if (it.id == event.messageId) it.copy(body = event.body, editedAt = event.editedAt) else it
            },
            replyTarget = state.replyTarget?.let {
                if (it.id == event.messageId) it.copy(body = event.body, editedAt = event.editedAt) else it
            },
        )
    }

    internal fun applyMessageDelete(event: NovaMessageDeletedEvent) {
        val editingWasDeleted = state.editingTarget?.id == event.messageId
        state = state.copy(
            messages = state.messages.map { message ->
                val reply = message.replyTo?.let {
                    if (it.id == event.messageId) {
                        it.copy(
                            body = "Message deleted",
                            imageUrl = "",
                            audioUrl = "",
                            audioDurationMs = null,
                            isDeleted = true,
                        )
                    } else {
                        it
                    }
                }
                if (message.id == event.messageId) {
                    message.copy(
                        body = "",
                        imageUrl = "",
                        audioUrl = "",
                        audioDurationMs = null,
                        replyTo = null,
                        reactions = emptyList(),
                        editedAt = null,
                        deletedAt = event.deletedAt,
                        share = null,
                    )
                } else {
                    message.copy(replyTo = reply)
                }
            },
            draft = if (editingWasDeleted) state.draftBeforeEdit else state.draft,
            draftBeforeEdit = if (editingWasDeleted) "" else state.draftBeforeEdit,
            replyTarget = state.replyTarget.takeUnless { it?.id == event.messageId },
            editingTarget = state.editingTarget.takeUnless { it?.id == event.messageId },
            actionsForMessageId = state.actionsForMessageId.takeUnless { it == event.messageId },
            deleteTarget = state.deleteTarget.takeUnless { it?.id == event.messageId },
        )
        if (editingWasDeleted) {
            scheduleDraftPersistence()
            scheduleTyping()
        }
    }

    private fun sendPending(pending: PendingMessage) {
        scope().launch {
            state = state.copy(pendingMessages = state.pendingMessages.map {
                if (it.clientId == pending.clientId) {
                    it.copy(status = PendingMessageStatus.Sending, error = null)
                } else {
                    it
                }
            })
            when (
                val result = repository.sendMessage(
                    conversationId = conversationId,
                    body = pending.body,
                    clientId = pending.clientId,
                    replyToId = pending.replyTo?.id,
                    imageUri = pending.imageUri.takeIf { it.isNotBlank() }?.let(Uri::parse),
                    audioFile = pending.audioPath.takeIf { it.isNotBlank() }?.let(::File),
                    audioDurationMs = pending.audioDurationMs,
                )
            ) {
                is ApiResult.Success -> completePending(pending.clientId, result.value)
                is ApiResult.Failure -> failPending(pending.clientId, result)
            }
        }
    }

    private fun completePending(clientId: String, message: NovaMessage) {
        val pending = state.pendingMessages.firstOrNull { it.clientId == clientId }
        state = state.copy(
            pendingMessages = state.pendingMessages.filterNot { it.clientId == clientId },
            messages = if (state.messages.none { it.id == message.id }) {
                state.messages + message
            } else {
                state.messages
            },
        )
        pending?.let(::deletePendingAudio)
    }

    private fun failPending(clientId: String, failure: ApiResult.Failure) {
        if (shouldExpireNovaSession(failure.statusCode)) {
            emitSessionExpiry()
            return
        }
        state = state.copy(pendingMessages = state.pendingMessages.map {
            if (it.clientId == clientId) {
                it.copy(status = PendingMessageStatus.Failed, error = failure.message)
            } else {
                it
            }
        })
    }

    private fun markConversationRead() {
        scope().launch {
            when (val result = repository.markRead(conversationId)) {
                is ApiResult.Success -> state = state.copy(
                    conversationReadVersion = state.conversationReadVersion + 1
                )

                is ApiResult.Failure -> {
                    if (shouldExpireNovaSession(result.statusCode)) emitSessionExpiry()
                }
            }
        }
    }

    private fun onRealtimeStatus(status: NovaRealtimeStatus) {
        state = state.copy(
            realtimeStatus = status,
            isOtherTyping = if (status == NovaRealtimeStatus.Live) state.isOtherTyping else false,
        )
        scheduleTyping()
    }

    private fun onPresence(presence: NovaConversationPresence) {
        if (username != GROUP_USERNAME && presence.username == username) {
            state = state.copy(
                otherPresence = presence,
                isOtherTyping = if (presence.isOnline) state.isOtherTyping else false,
            )
        }
    }

    private fun scheduleDraftPersistence() {
        draftJob?.cancel()
        if (state.editingTarget != null) return
        draftJob = scope().launch {
            delay(draftDebounceMillis)
            if (state.draft.isBlank()) draftStore.remove(conversationId)
            else draftStore.save(conversationId, state.draft)
        }
    }

    private fun scheduleTyping() {
        typingJob?.cancel()
        typingJob = scope().launch {
            if (
                state.realtimeStatus != NovaRealtimeStatus.Live ||
                state.editingTarget != null || recording || state.draft.isBlank()
            ) {
                stopTyping()
                return@launch
            }
            if (!typingAnnounced) {
                realtime.sendTyping(true)
                typingAnnounced = true
            }
            delay(typingWindowMillis)
            stopTyping()
        }
    }

    private fun stopTyping() {
        typingJob?.cancel()
        typingJob = null
        if (typingAnnounced) realtime.sendTyping(false)
        typingAnnounced = false
    }

    private fun emitSessionExpiry() {
        state = state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
    }

    private fun scope(): CoroutineScope = activeScope ?: baseScope

    private fun replyPreview(message: NovaMessage) = NovaReplyPreview(
        id = message.id,
        sender = message.sender,
        body = message.body,
        imageUrl = message.imageUrl,
        audioUrl = message.audioUrl,
        audioDurationMs = message.audioDurationMs,
        isDeleted = message.isDeleted,
    )

    private fun deletePendingAudio(pending: PendingMessage) {
        pending.audioPath.takeIf { it.isNotBlank() }?.let { File(it).delete() }
    }

    private fun NovaMessage.isCallHistory(): Boolean = clientId.startsWith(CALL_HISTORY_CLIENT_PREFIX)

    private fun reactionOrder(emoji: String): Int =
        REACTION_CHOICES.indexOf(emoji).let { if (it < 0) 99 else it }

    override fun onCleared() {
        stopRealtime()
        super.onCleared()
    }

    companion object {
        private const val GROUP_USERNAME = "group"
        private const val MAX_DRAFT_LENGTH = 2_000
        private const val DRAFT_DEBOUNCE_MS = 250L
        private const val TYPING_WINDOW_MS = 1_400L
        private const val CALL_HISTORY_CLIENT_PREFIX = "call:"
        private val REACTION_CHOICES = listOf("❤️", "😂", "😮", "😢", "😡", "👍")

        fun factory(
            conversationId: Long,
            username: String,
            repository: MessagesRepository,
            realtime: ConversationRealtime,
            draftStore: ConversationDraftStore,
            currentAuthor: () -> NovaPostAuthor,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ConversationViewModel::class.java))
                return ConversationViewModel(
                    conversationId = conversationId,
                    username = username,
                    repository = repository,
                    realtime = realtime,
                    draftStore = draftStore,
                    currentAuthor = currentAuthor,
                ) as T
            }
        }
    }
}
