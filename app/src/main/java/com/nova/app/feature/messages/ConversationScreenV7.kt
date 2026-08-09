package com.nova.app.feature.messages

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nova.app.core.messaging.NovaActiveConversation
import com.nova.app.core.messaging.NovaConversationPresence
import com.nova.app.core.messaging.NovaConversationRealtimeClient
import com.nova.app.core.messaging.NovaMessage
import com.nova.app.core.messaging.NovaMessageDeletedEvent
import com.nova.app.core.messaging.NovaMessageReaction
import com.nova.app.core.messaging.NovaMessageReactionEvent
import com.nova.app.core.messaging.NovaMessageUpdatedEvent
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.messaging.NovaRealtimeEvent
import com.nova.app.core.messaging.NovaRealtimeStatus
import com.nova.app.core.messaging.NovaVoiceDraft
import com.nova.app.core.messaging.NovaVoiceRecorder
import com.nova.app.core.network.ApiResult
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID


private val V7OnlineGreen = Color(0xFF35C982)
private val V7ReactionChoices = listOf("❤️", "😂", "😮", "😢", "😡", "👍")
private const val V7MaxVoiceMs = 5 * 60 * 1000L


@Composable
fun ConversationScreenV7(
    conversationId: Long,
    username: String,
    displayName: String,
    avatarUrl: String,
    onBack: () -> Unit,
    onConversationRead: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { NovaMessagingRepository(context.applicationContext) }
    val realtimeClient = remember(conversationId, repository) {
        NovaConversationRealtimeClient(
            context = context.applicationContext,
            conversationId = conversationId,
            repository = repository,
        )
    }
    val voiceRecorder = remember(context) { NovaVoiceRecorder(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember(conversationId) { mutableStateOf<List<NovaMessage>>(emptyList()) }
    var nextCursor by remember(conversationId) { mutableStateOf<String?>(null) }
    var isLoading by remember(conversationId) { mutableStateOf(true) }
    var isLoadingEarlier by remember(conversationId) { mutableStateOf(false) }
    var isSending by remember(conversationId) { mutableStateOf(false) }
    var mutatingMessageId by remember(conversationId) { mutableStateOf<Long?>(null) }
    var reactingMessageId by remember(conversationId) { mutableStateOf<Long?>(null) }
    var errorMessage by remember(conversationId) { mutableStateOf<String?>(null) }
    var draft by remember(conversationId) { mutableStateOf("") }
    var selectedImage by remember(conversationId) { mutableStateOf<Uri?>(null) }
    var voiceDraft by remember(conversationId) { mutableStateOf<NovaVoiceDraft?>(null) }
    var replyTarget by remember(conversationId) { mutableStateOf<NovaMessage?>(null) }
    var editingTarget by remember(conversationId) { mutableStateOf<NovaMessage?>(null) }
    var deleteTarget by remember(conversationId) { mutableStateOf<NovaMessage?>(null) }
    var actionsForMessageId by remember(conversationId) { mutableStateOf<Long?>(null) }
    var fullScreenPhotoUrl by remember(conversationId) { mutableStateOf<String?>(null) }
    var isRecording by remember(conversationId) { mutableStateOf(false) }
    var recordingStartedAt by remember(conversationId) { mutableLongStateOf(0L) }
    var recordingElapsedMs by remember(conversationId) { mutableLongStateOf(0L) }
    var isOtherTyping by remember(conversationId) { mutableStateOf(false) }
    var typingAnnounced by remember(conversationId) { mutableStateOf(false) }
    var otherPresence by remember(conversationId) { mutableStateOf<NovaConversationPresence?>(null) }
    var realtimeStatus by remember(conversationId) { mutableStateOf(NovaRealtimeStatus.Connecting) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && voiceDraft == null && !isRecording && editingTarget == null) {
            selectedImage = uri
            errorMessage = null
        }
    }

    fun beginVoiceRecording() {
        if (isSending || editingTarget != null || selectedImage != null || voiceDraft != null || isRecording) return
        errorMessage = null
        voiceRecorder.start()
            .onSuccess {
                isRecording = true
                recordingStartedAt = SystemClock.elapsedRealtime()
                recordingElapsedMs = 0L
                if (typingAnnounced) {
                    realtimeClient.sendTyping(false)
                    typingAnnounced = false
                }
            }
            .onFailure {
                errorMessage = "Nova couldn't start the microphone. Try again."
            }
    }

    fun finishVoiceRecording() {
        if (!isRecording) return
        val result = voiceRecorder.stop()
        isRecording = false
        recordingStartedAt = 0L
        recordingElapsedMs = 0L
        result.onSuccess { recorded ->
            if (recorded.durationMs < 1_000L) {
                recorded.file.delete()
                errorMessage = "Voice message is too short. Record for at least 1 second."
            } else {
                voiceDraft?.file?.delete()
                voiceDraft = recorded
                errorMessage = null
            }
        }.onFailure {
            errorMessage = "Nova couldn't finish that recording. Record it again."
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) beginVoiceRecording()
        else errorMessage = "Microphone permission is required to send voice messages."
    }

    fun requestVoiceRecording() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            beginVoiceRecording()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun markConversationRead() {
        scope.launch {
            when (val result = repository.markRead(conversationId)) {
                is ApiResult.Success -> onConversationRead()
                is ApiResult.Failure -> if (result.statusCode == 401) onSessionExpired()
            }
        }
    }

    fun loadLatest(showSpinner: Boolean = true, scrollToBottom: Boolean = false) {
        scope.launch {
            if (showSpinner) {
                isLoading = true
                errorMessage = null
            }
            when (val result = repository.messages(conversationId)) {
                is ApiResult.Success -> {
                    messages = result.value.messages
                    nextCursor = result.value.nextCursor
                    isLoading = false
                    markConversationRead()
                    if (scrollToBottom && messages.isNotEmpty()) {
                        listState.scrollToItem(messages.lastIndex)
                    }
                }
                is ApiResult.Failure -> {
                    isLoading = false
                    if (result.statusCode == 401) onSessionExpired()
                    else if (showSpinner || messages.isEmpty()) errorMessage = result.message
                }
            }
        }
    }

    fun loadEarlier() {
        val cursor = nextCursor ?: return
        if (isLoadingEarlier) return
        scope.launch {
            isLoadingEarlier = true
            when (val result = repository.messages(conversationId, cursor)) {
                is ApiResult.Success -> {
                    val existingIds = messages.mapTo(mutableSetOf()) { it.id }
                    messages = result.value.messages.filterNot { it.id in existingIds } + messages
                    nextCursor = result.value.nextCursor
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired()
                    else errorMessage = result.message
                }
            }
            isLoadingEarlier = false
        }
    }

    fun applyMessageUpdate(event: NovaMessageUpdatedEvent) {
        messages = messages.map { message ->
            val reply = message.replyTo?.let {
                if (it.id == event.messageId && !it.isDeleted) it.copy(body = event.body) else it
            }
            if (message.id == event.messageId && !message.isDeleted) {
                message.copy(body = event.body, editedAt = event.editedAt, replyTo = reply)
            } else {
                message.copy(replyTo = reply)
            }
        }
        editingTarget = editingTarget?.let {
            if (it.id == event.messageId) it.copy(body = event.body, editedAt = event.editedAt) else it
        }
        replyTarget = replyTarget?.let {
            if (it.id == event.messageId) it.copy(body = event.body, editedAt = event.editedAt) else it
        }
    }

    fun applyMessageDelete(event: NovaMessageDeletedEvent) {
        messages = messages.map { message ->
            val reply = message.replyTo?.let {
                if (it.id == event.messageId) {
                    it.copy(
                        body = "Message deleted",
                        imageUrl = "",
                        audioUrl = "",
                        audioDurationMs = null,
                        isDeleted = true,
                    )
                } else it
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
                )
            } else {
                message.copy(replyTo = reply)
            }
        }
        if (replyTarget?.id == event.messageId) replyTarget = null
        if (editingTarget?.id == event.messageId) {
            editingTarget = null
            draft = ""
        }
        if (actionsForMessageId == event.messageId) actionsForMessageId = null
        if (deleteTarget?.id == event.messageId) deleteTarget = null
    }

    fun send() {
        val body = draft.trim()
        val image = selectedImage
        val voice = voiceDraft
        if ((body.isBlank() && image == null && voice == null) || isSending || isRecording || editingTarget != null) return

        if (typingAnnounced) {
            realtimeClient.sendTyping(false)
            typingAnnounced = false
        }

        val replyId = replyTarget?.id
        val clientId = UUID.randomUUID().toString()
        scope.launch {
            isSending = true
            errorMessage = null
            when (
                val result = repository.sendMessage(
                    conversationId = conversationId,
                    body = body,
                    clientId = clientId,
                    replyToId = replyId,
                    imageUri = image,
                    audioFile = voice?.file,
                    audioDurationMs = voice?.durationMs,
                )
            ) {
                is ApiResult.Success -> {
                    if (messages.none { it.id == result.value.id }) {
                        messages = messages + result.value
                    }
                    draft = ""
                    selectedImage = null
                    voiceDraft?.file?.delete()
                    voiceDraft = null
                    replyTarget = null
                    actionsForMessageId = null
                    onConversationRead()
                    if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired()
                    else errorMessage = result.message
                }
            }
            isSending = false
        }
    }

    fun startEdit(message: NovaMessage) {
        if (!message.isMine || message.isDeleted || mutatingMessageId != null) return
        if (isRecording) {
            voiceRecorder.cancel()
            isRecording = false
        }
        voiceDraft?.file?.delete()
        voiceDraft = null
        selectedImage = null
        replyTarget = null
        editingTarget = message
        draft = message.body
        actionsForMessageId = null
        errorMessage = null
        if (typingAnnounced) {
            realtimeClient.sendTyping(false)
            typingAnnounced = false
        }
    }

    fun cancelEdit() {
        editingTarget = null
        draft = ""
        errorMessage = null
    }

    fun saveEdit() {
        val target = editingTarget ?: return
        if (target.isDeleted || mutatingMessageId != null) return
        val body = draft.trim()
        if (body == target.body) {
            cancelEdit()
            return
        }
        if (body.isBlank() && target.imageUrl.isBlank() && target.audioUrl.isBlank()) return

        scope.launch {
            mutatingMessageId = target.id
            errorMessage = null
            when (val result = repository.editMessage(target.id, body)) {
                is ApiResult.Success -> {
                    messages = messages.map { if (it.id == target.id) result.value else it }
                    editingTarget = null
                    draft = ""
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired()
                    else errorMessage = result.message
                }
            }
            mutatingMessageId = null
        }
    }

    fun confirmDelete(message: NovaMessage) {
        if (!message.isMine || message.isDeleted || mutatingMessageId != null) return
        deleteTarget = message
        actionsForMessageId = null
    }

    fun deleteForEveryone() {
        val target = deleteTarget ?: return
        if (mutatingMessageId != null) return
        scope.launch {
            mutatingMessageId = target.id
            errorMessage = null
            when (val result = repository.deleteMessage(target.id)) {
                is ApiResult.Success -> {
                    applyMessageDelete(
                        NovaMessageDeletedEvent(
                            messageId = target.id,
                            deletedAt = result.value,
                        )
                    )
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired()
                    else errorMessage = result.message
                }
            }
            deleteTarget = null
            mutatingMessageId = null
        }
    }

    fun setReaction(message: NovaMessage, emoji: String) {
        if (reactingMessageId != null || message.isDeleted) return
        val current = message.reactions.firstOrNull { it.reactedByMe }?.emoji
        val desired = if (current == emoji) null else emoji
        scope.launch {
            reactingMessageId = message.id
            when (val result = repository.setReaction(message.id, desired)) {
                is ApiResult.Success -> {
                    messages = messages.map {
                        if (it.id == message.id) it.copy(reactions = result.value) else it
                    }
                    actionsForMessageId = null
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired()
                    else errorMessage = result.message
                }
            }
            reactingMessageId = null
        }
    }

    fun applyReactionEvent(event: NovaMessageReactionEvent) {
        messages = messages.map { message ->
            if (message.id != event.messageId || message.isDeleted) return@map message

            val mutable = message.reactions.associateBy { it.emoji }.toMutableMap()
            if (event.isMine && event.active) {
                mutable.keys.toList().forEach { key ->
                    mutable[key]?.let { mutable[key] = it.copy(reactedByMe = false) }
                }
            }

            if (event.count <= 0) {
                mutable.remove(event.emoji)
            } else {
                val existing = mutable[event.emoji]
                mutable[event.emoji] = NovaMessageReaction(
                    emoji = event.emoji,
                    count = event.count,
                    reactedByMe = if (event.isMine) event.active else existing?.reactedByMe == true,
                )
            }
            message.copy(
                reactions = mutable.values.sortedBy {
                    V7ReactionChoices.indexOf(it.emoji).let { index -> if (index < 0) 99 else index }
                }
            )
        }
    }

    LaunchedEffect(conversationId) {
        loadLatest(showSpinner = true, scrollToBottom = true)
    }

    LaunchedEffect(isRecording, recordingStartedAt) {
        while (isRecording) {
            recordingElapsedMs = (SystemClock.elapsedRealtime() - recordingStartedAt).coerceAtLeast(0L)
            if (recordingElapsedMs >= V7MaxVoiceMs) {
                finishVoiceRecording()
                break
            }
            delay(250)
        }
    }

    LaunchedEffect(draft, realtimeStatus, editingTarget, isRecording) {
        if (realtimeStatus != NovaRealtimeStatus.Live || editingTarget != null || isRecording) {
            if (typingAnnounced) realtimeClient.sendTyping(false)
            typingAnnounced = false
            return@LaunchedEffect
        }
        if (draft.isBlank()) {
            if (typingAnnounced) realtimeClient.sendTyping(false)
            typingAnnounced = false
            return@LaunchedEffect
        }
        if (!typingAnnounced) {
            realtimeClient.sendTyping(true)
            typingAnnounced = true
        }
        delay(1_400)
        realtimeClient.sendTyping(false)
        typingAnnounced = false
    }

    DisposableEffect(conversationId, realtimeClient) {
        NovaActiveConversation.enter(conversationId)
        realtimeClient.start(
            scope = scope,
            onEvent = { event ->
                when (event) {
                    is NovaRealtimeEvent.MessageCreated -> {
                        val message = event.message
                        if (messages.none { it.id == message.id }) {
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                            val wasNearBottom = messages.isEmpty() || lastVisible >= messages.lastIndex - 2
                            messages = messages + message
                            if (!message.isMine) {
                                isOtherTyping = false
                                markConversationRead()
                            }
                            if (wasNearBottom && messages.isNotEmpty()) {
                                scope.launch { listState.animateScrollToItem(messages.lastIndex) }
                            }
                        }
                    }
                    is NovaRealtimeEvent.MessagesDelivered -> {
                        if (event.deliveredAt.isNotBlank()) {
                            messages = messages.map { message ->
                                if (message.isMine && message.id in event.messageIds) {
                                    message.copy(deliveredAt = event.deliveredAt)
                                } else message
                            }
                        }
                    }
                    is NovaRealtimeEvent.ConversationRead -> {
                        if (event.readAt.isNotBlank()) {
                            messages = messages.map { message ->
                                if (message.isMine && message.id in event.messageIds) {
                                    message.copy(
                                        deliveredAt = message.deliveredAt ?: event.readAt,
                                        readAt = event.readAt,
                                    )
                                } else message
                            }
                        }
                    }
                    is NovaRealtimeEvent.Typing -> isOtherTyping = event.isTyping
                }
            },
            onStatus = {
                realtimeStatus = it
                if (it != NovaRealtimeStatus.Live) isOtherTyping = false
            },
            onSessionExpired = onSessionExpired,
            onPresence = { presence ->
                if (presence.username == username) {
                    otherPresence = presence
                    if (!presence.isOnline) isOtherTyping = false
                }
            },
            onReaction = ::applyReactionEvent,
            onMessageUpdated = ::applyMessageUpdate,
            onMessageDeleted = ::applyMessageDelete,
        )

        onDispose {
            if (typingAnnounced) realtimeClient.sendTyping(false)
            voiceRecorder.cancel()
            NovaActiveConversation.leave(conversationId)
            realtimeClient.stop()
        }
    }

    Scaffold(
        containerColor = NovaBackground,
        topBar = {
            V7Header(
                username = username,
                displayName = displayName,
                avatarUrl = avatarUrl,
                presence = otherPresence,
                realtimeStatus = realtimeStatus,
                isTyping = isOtherTyping,
                onBack = onBack,
                onRefresh = { loadLatest(showSpinner = false) },
            )
        },
        bottomBar = {
            V7Composer(
                username = username,
                draft = draft,
                selectedImage = selectedImage,
                voiceDraft = voiceDraft,
                replyTarget = replyTarget,
                editingTarget = editingTarget,
                isSending = isSending,
                isMutating = mutatingMessageId != null,
                isRecording = isRecording,
                recordingElapsedMs = recordingElapsedMs,
                errorMessage = errorMessage,
                onDraftChange = { draft = it.take(2000) },
                onPickPhoto = {
                    if (!isSending && editingTarget == null && voiceDraft == null && !isRecording) {
                        imagePicker.launch("image/*")
                    }
                },
                onRemovePhoto = { selectedImage = null },
                onStartRecording = ::requestVoiceRecording,
                onStopRecording = ::finishVoiceRecording,
                onCancelRecording = {
                    voiceRecorder.cancel()
                    isRecording = false
                    recordingStartedAt = 0L
                    recordingElapsedMs = 0L
                },
                onRemoveVoice = {
                    voiceDraft?.file?.delete()
                    voiceDraft = null
                },
                onCancelReply = { replyTarget = null },
                onCancelEdit = ::cancelEdit,
                onSend = { if (editingTarget != null) saveEdit() else send() },
            )
        },
    ) { innerPadding ->
        when {
            isLoading && messages.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = NovaAccent)
            }

            messages.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Start the conversation", color = NovaInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Send a message, photo, or voice note to @$username.",
                    color = NovaMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().background(NovaBackground).padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (nextCursor != null) {
                    item {
                        Surface(
                            onClick = { if (!isLoadingEarlier) loadEarlier() },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = NovaSurface,
                            border = BorderStroke(1.dp, NovaBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(11.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (isLoadingEarlier) {
                                    CircularProgressIndicator(
                                        color = NovaAccent,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.size(8.dp))
                                }
                                Text(
                                    if (isLoadingEarlier) "Loading earlier…" else "Load earlier messages",
                                    color = NovaMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }

                itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                    val previous = messages.getOrNull(index - 1)
                    val day = messageLocalDateV7(message.createdAt)
                    val previousDay = previous?.let { messageLocalDateV7(it.createdAt) }
                    if (day != null && day != previousDay) {
                        V7DateDivider(day)
                    }

                    val previousSameSender = previous?.sender?.id == message.sender.id && previousDay == day
                    val next = messages.getOrNull(index + 1)
                    val nextSameSender = next?.sender?.id == message.sender.id && messageLocalDateV7(next.createdAt) == day

                    V7MessageBubble(
                        message = message,
                        compactTop = previousSameSender,
                        compactBottom = nextSameSender,
                        showActions = actionsForMessageId == message.id,
                        reactionBusy = reactingMessageId == message.id,
                        mutationBusy = mutatingMessageId == message.id,
                        onToggleActions = {
                            if (!message.isDeleted) {
                                actionsForMessageId = if (actionsForMessageId == message.id) null else message.id
                            }
                        },
                        onReply = {
                            replyTarget = message
                            editingTarget = null
                            draft = ""
                            actionsForMessageId = null
                        },
                        onEdit = { startEdit(message) },
                        onDelete = { confirmDelete(message) },
                        onReact = { emoji -> setReaction(message, emoji) },
                        onOpenPhoto = { fullScreenPhotoUrl = it },
                    )
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (mutatingMessageId == null) deleteTarget = null },
            title = { Text("Delete message?", color = NovaInk, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This removes the message for both people. This can't be undone.",
                    color = NovaMuted,
                )
            },
            confirmButton = {
                Surface(
                    onClick = { if (mutatingMessageId == null) deleteForEveryone() },
                    shape = RoundedCornerShape(14.dp),
                    color = NovaAccent,
                ) {
                    Text(
                        if (mutatingMessageId == target.id) "Deleting…" else "Delete",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        color = NovaBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                Surface(
                    onClick = { if (mutatingMessageId == null) deleteTarget = null },
                    shape = RoundedCornerShape(14.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Text(
                        "Cancel",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        color = NovaMuted,
                    )
                }
            },
            containerColor = NovaSurface,
        )
    }

    fullScreenPhotoUrl?.let { photoUrl ->
        V7FullScreenPhoto(
            photoUrl = photoUrl,
            onDismiss = { fullScreenPhotoUrl = null },
        )
    }
}


@Composable
private fun V7Header(
    username: String,
    displayName: String,
    avatarUrl: String,
    presence: NovaConversationPresence?,
    realtimeStatus: NovaRealtimeStatus,
    isTyping: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    val online = presence?.isOnline == true
    val subtitle = when {
        realtimeStatus == NovaRealtimeStatus.Live && isTyping -> "typing…"
        realtimeStatus == NovaRealtimeStatus.Live && online -> "Online"
        realtimeStatus == NovaRealtimeStatus.Live && presence?.lastSeenAt != null -> formatLastSeenV7(presence.lastSeenAt)
        realtimeStatus == NovaRealtimeStatus.Connecting -> "Connecting…"
        realtimeStatus == NovaRealtimeStatus.Reconnecting -> "Reconnecting…"
        realtimeStatus == NovaRealtimeStatus.Offline -> "Offline"
        else -> "@$username"
    }

    Surface(color = NovaSurface, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Surface(onClick = onBack, shape = CircleShape, color = NovaBackground) {
                Text("‹", modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp), fontSize = 28.sp, color = NovaInk)
            }
            Box {
                NovaAvatar(
                    source = avatarUrl,
                    fallbackText = displayName.ifBlank { username },
                    size = 44.dp,
                )
                if (online) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(V7OnlineGreen)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(displayName.ifBlank { username }, color = NovaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text(subtitle, color = if (online || isTyping) NovaAccent else NovaMuted, fontSize = 12.sp, maxLines = 1)
            }
            Surface(onClick = onRefresh, shape = CircleShape, color = NovaBackground) {
                Text("↻", modifier = Modifier.padding(10.dp), color = NovaAccent, fontSize = 17.sp)
            }
        }
    }
}


@Composable
private fun V7Composer(
    username: String,
    draft: String,
    selectedImage: Uri?,
    voiceDraft: NovaVoiceDraft?,
    replyTarget: NovaMessage?,
    editingTarget: NovaMessage?,
    isSending: Boolean,
    isMutating: Boolean,
    isRecording: Boolean,
    recordingElapsedMs: Long,
    errorMessage: String?,
    onDraftChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onRemoveVoice: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(color = NovaSurface, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            if (editingTarget != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NovaAccentSoft,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Editing message", color = NovaAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(replyPreviewTextV7(editingTarget), color = NovaMuted, fontSize = 12.sp, maxLines = 1)
                        }
                        Surface(onClick = onCancelEdit, shape = CircleShape, color = NovaSurface) {
                            Text("×", modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = NovaMuted, fontSize = 18.sp)
                        }
                    }
                }
            } else if (replyTarget != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NovaAccentSoft,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Replying to @${replyTarget.sender.username}", color = NovaAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(replyPreviewTextV7(replyTarget), color = NovaMuted, fontSize = 12.sp, maxLines = 1)
                        }
                        Surface(onClick = onCancelReply, shape = CircleShape, color = NovaSurface) {
                            Text("×", modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = NovaMuted, fontSize = 18.sp)
                        }
                    }
                }
            }

            if (isRecording) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = NovaAccentSoft,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("●", color = NovaAccent, fontSize = 16.sp)
                        Column(Modifier.weight(1f)) {
                            Text("Recording ${formatVoiceDuration(recordingElapsedMs)}", color = NovaInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Up to 5 minutes", color = NovaMuted, fontSize = 10.sp)
                        }
                        Surface(onClick = onCancelRecording, shape = RoundedCornerShape(12.dp), color = NovaSurface) {
                            Text("Cancel", modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = NovaMuted, fontSize = 11.sp)
                        }
                        Surface(onClick = onStopRecording, shape = CircleShape, color = NovaAccent) {
                            Text("■", modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = NovaBackground, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (selectedImage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NovaMediaImage(
                        source = selectedImage.toString(),
                        modifier = Modifier.size(70.dp).clip(RoundedCornerShape(15.dp)),
                        contentDescription = "Selected message photo",
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Photo ready", color = NovaInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Add a caption or send it as-is.", color = NovaMuted, fontSize = 11.sp)
                    }
                    Surface(onClick = onRemovePhoto, shape = RoundedCornerShape(12.dp), color = NovaBackground) {
                        Text("Remove", modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = NovaMuted, fontSize = 11.sp)
                    }
                }
            }

            if (voiceDraft != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(shape = CircleShape, color = NovaAccentSoft) {
                        Text("🎤", modifier = Modifier.padding(12.dp), fontSize = 18.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Voice message ready", color = NovaInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatVoiceDuration(voiceDraft.durationMs), color = NovaMuted, fontSize = 11.sp)
                    }
                    Surface(onClick = onRemoveVoice, shape = RoundedCornerShape(12.dp), color = NovaBackground) {
                        Text("Remove", modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = NovaMuted, fontSize = 11.sp)
                    }
                }
            }

            if (errorMessage != null) {
                Text(errorMessage, color = NovaMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (editingTarget == null && !isRecording) {
                    Surface(
                        onClick = onPickPhoto,
                        shape = CircleShape,
                        color = if (voiceDraft == null && !isSending) NovaAccentSoft else NovaBackground,
                    ) {
                        Text("+", modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = NovaAccent, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isSending && !isMutating && !isRecording,
                    placeholder = {
                        Text(
                            if (editingTarget != null) "Edit message" else "Message @$username",
                            color = NovaMuted,
                        )
                    },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                        focusedContainerColor = NovaBackground,
                        unfocusedContainerColor = NovaBackground,
                    ),
                )

                if (editingTarget == null && !isRecording && selectedImage == null && voiceDraft == null && draft.isBlank()) {
                    Surface(onClick = onStartRecording, shape = CircleShape, color = NovaAccentSoft) {
                        Text("🎤", modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp), fontSize = 16.sp)
                    }
                }

                val enabled = if (editingTarget != null) {
                    !isMutating && !editingTarget.isDeleted &&
                        draft.trim() != editingTarget.body &&
                        (draft.isNotBlank() || editingTarget.imageUrl.isNotBlank() || editingTarget.audioUrl.isNotBlank())
                } else {
                    !isSending && !isRecording && (draft.isNotBlank() || selectedImage != null || voiceDraft != null)
                }
                Surface(
                    onClick = { if (enabled) onSend() },
                    shape = CircleShape,
                    color = if (enabled) NovaAccent else NovaAccentSoft,
                ) {
                    Text(
                        when {
                            isSending || isMutating -> "…"
                            editingTarget != null -> "✓"
                            else -> "↑"
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = if (enabled) NovaBackground else NovaMuted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}


@Composable
private fun V7MessageBubble(
    message: NovaMessage,
    compactTop: Boolean,
    compactBottom: Boolean,
    showActions: Boolean,
    reactionBusy: Boolean,
    mutationBusy: Boolean,
    onToggleActions: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onOpenPhoto: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = if (compactTop) 0.dp else 5.dp),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
    ) {
        Surface(
            onClick = onToggleActions,
            shape = RoundedCornerShape(
                topStart = if (!message.isMine && compactTop) 8.dp else 20.dp,
                topEnd = if (message.isMine && compactTop) 8.dp else 20.dp,
                bottomStart = if (!message.isMine && !compactBottom) 5.dp else 20.dp,
                bottomEnd = if (message.isMine && !compactBottom) 5.dp else 20.dp,
            ),
            color = if (message.isMine) NovaAccent else NovaSurface,
            border = if (message.isMine) null else BorderStroke(1.dp, NovaBorder),
        ) {
            Column(modifier = Modifier.widthIn(max = 292.dp).padding(horizontal = 10.dp, vertical = 9.dp)) {
                if (message.isDeleted) {
                    Text(
                        "Message deleted",
                        color = if (message.isMine) NovaBackground.copy(alpha = 0.78f) else NovaMuted,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                    )
                } else {
                    message.replyTo?.let { reply ->
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = if (message.isMine) NovaBackground.copy(alpha = 0.18f) else NovaAccentSoft,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                Text(
                                    "@${reply.sender.username}",
                                    color = if (message.isMine) NovaBackground else NovaAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    replyPreviewTextV7(reply),
                                    color = if (message.isMine) NovaBackground.copy(alpha = 0.8f) else NovaMuted,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                )
                            }
                        }
                    }

                    if (message.imageUrl.isNotBlank()) {
                        Surface(
                            onClick = { onOpenPhoto(message.imageUrl) },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Transparent,
                        ) {
                            NovaMediaImage(
                                source = message.imageUrl,
                                modifier = Modifier.fillMaxWidth().height(240.dp),
                                contentDescription = "Message photo",
                            )
                        }
                        if (message.body.isNotBlank()) Spacer(Modifier.height(8.dp))
                    }

                    if (message.audioUrl.isNotBlank()) {
                        V7VoiceNotePlayer(
                            audioUrl = message.audioUrl,
                            durationMs = message.audioDurationMs,
                            mine = message.isMine,
                        )
                        if (message.body.isNotBlank()) Spacer(Modifier.height(7.dp))
                    }

                    if (message.body.isNotBlank()) {
                        Text(
                            message.body,
                            color = if (message.isMine) NovaBackground else NovaInk,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                val delivery = when {
                    !message.isMine -> ""
                    message.readAt != null -> " · Read"
                    message.deliveredAt != null -> " · Delivered"
                    else -> " · Sent"
                }
                val edited = if (!message.isDeleted && message.editedAt != null) " · Edited" else ""
                Text(
                    localMessageTimeV7(message.createdAt) + edited + delivery,
                    color = if (message.isMine) NovaBackground.copy(alpha = 0.72f) else NovaMuted,
                    fontSize = 9.sp,
                )
            }
        }

        if (!message.isDeleted && message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                message.reactions.forEach { reaction ->
                    Surface(
                        onClick = { if (!reactionBusy) onReact(reaction.emoji) },
                        shape = RoundedCornerShape(13.dp),
                        color = if (reaction.reactedByMe) NovaAccentSoft else NovaSurface,
                        border = BorderStroke(1.dp, if (reaction.reactedByMe) NovaAccent else NovaBorder),
                    ) {
                        Text(
                            "${reaction.emoji} ${reaction.count}",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            color = NovaInk,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }

        if (showActions && !message.isDeleted) {
            Column(
                modifier = Modifier.padding(top = 5.dp),
                horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    V7ActionChip("Reply", enabled = !mutationBusy, onClick = onReply)
                    if (message.isMine) {
                        V7ActionChip("Edit", enabled = !mutationBusy, onClick = onEdit)
                        V7ActionChip("Delete", enabled = !mutationBusy, onClick = onDelete)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    V7ReactionChoices.forEach { emoji ->
                        Surface(
                            onClick = { if (!reactionBusy && !mutationBusy) onReact(emoji) },
                            shape = CircleShape,
                            color = NovaSurface,
                            border = BorderStroke(1.dp, NovaBorder),
                        ) {
                            Text(emoji, modifier = Modifier.padding(6.dp), fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun V7ActionChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(13.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            color = if (enabled) NovaMuted else NovaMuted.copy(alpha = 0.45f),
            fontSize = 11.sp,
        )
    }
}


@Composable
private fun V7VoiceNotePlayer(
    audioUrl: String,
    durationMs: Long?,
    mine: Boolean,
) {
    var prepared by remember(audioUrl) { mutableStateOf(false) }
    var playing by remember(audioUrl) { mutableStateOf(false) }
    var failed by remember(audioUrl) { mutableStateOf(false) }
    val player = remember(audioUrl) { MediaPlayer() }

    DisposableEffect(player, audioUrl) {
        player.setOnPreparedListener {
            prepared = true
            failed = false
        }
        player.setOnCompletionListener { playing = false }
        player.setOnErrorListener { _, _, _ ->
            playing = false
            failed = true
            true
        }
        runCatching {
            player.setDataSource(audioUrl)
            player.prepareAsync()
        }.onFailure {
            failed = true
        }

        onDispose {
            runCatching { player.release() }
        }
    }

    Surface(
        onClick = {
            if (prepared && !failed) {
                runCatching {
                    if (playing) {
                        player.pause()
                        playing = false
                    } else {
                        player.start()
                        playing = true
                    }
                }.onFailure { failed = true }
            }
        },
        shape = RoundedCornerShape(16.dp),
        color = if (mine) NovaBackground.copy(alpha = 0.17f) else NovaAccentSoft,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                when {
                    failed -> "!"
                    !prepared -> "…"
                    playing -> "❚❚"
                    else -> "▶"
                },
                color = if (mine) NovaBackground else NovaAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (failed) "Voice unavailable" else "Voice message",
                    color = if (mine) NovaBackground else NovaInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatVoiceDuration(durationMs ?: 0L),
                    color = if (mine) NovaBackground.copy(alpha = 0.72f) else NovaMuted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}


@Composable
private fun V7FullScreenPhoto(
    photoUrl: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Full screen message photo",
                modifier = Modifier.fillMaxSize().padding(vertical = 60.dp),
                contentScale = ContentScale.Fit,
            )
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(18.dp),
            ) {
                Text("×", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontSize = 24.sp)
            }
        }
    }
}


@Composable
private fun V7DateDivider(day: LocalDate) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(shape = RoundedCornerShape(14.dp), color = NovaSurface, border = BorderStroke(1.dp, NovaBorder)) {
            Text(dayLabelV7(day), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = NovaMuted, fontSize = 10.sp)
        }
    }
}


private fun replyPreviewTextV7(message: NovaMessage): String {
    return when {
        message.isDeleted -> "Message deleted"
        message.body.isNotBlank() -> message.body
        message.audioUrl.isNotBlank() -> "🎤 Voice message"
        message.imageUrl.isNotBlank() -> "📷 Photo"
        else -> "Message"
    }
}


private fun replyPreviewTextV7(reply: com.nova.app.core.messaging.NovaReplyPreview): String {
    return when {
        reply.isDeleted -> "Message deleted"
        reply.body.isNotBlank() -> reply.body
        reply.audioUrl.isNotBlank() -> "🎤 Voice message"
        reply.imageUrl.isNotBlank() -> "📷 Photo"
        else -> "Message"
    }
}


private fun formatVoiceDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L).coerceAtMost(5 * 60L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(Locale.US, minutes, seconds)
}


private fun parseMessageInstantV7(value: String): Instant? {
    return runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull()
}


private fun messageLocalDateV7(value: String): LocalDate? {
    return parseMessageInstantV7(value)?.atZone(ZoneId.systemDefault())?.toLocalDate()
}


private fun localMessageTimeV7(value: String): String {
    val instant = parseMessageInstantV7(value) ?: return ""
    return DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        .format(instant.atZone(ZoneId.systemDefault()))
}


private fun dayLabelV7(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }
}


private fun formatLastSeenV7(value: String): String {
    val instant = parseMessageInstantV7(value) ?: return "Last seen recently"
    val zone = instant.atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    val date = zone.toLocalDate()
    val time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(zone)
    return when (date) {
        today -> "Last seen today at $time"
        today.minusDays(1) -> "Last seen yesterday at $time"
        else -> "Last seen ${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))}"
    }
}
