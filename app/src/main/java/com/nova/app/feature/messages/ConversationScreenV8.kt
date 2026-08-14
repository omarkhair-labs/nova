package com.nova.app.feature.messages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.nova.app.MainActivity
import com.nova.app.app.appContainer
import com.nova.app.core.messaging.NovaActiveConversation
import com.nova.app.core.messaging.NovaConversationPresence
import com.nova.app.core.messaging.NovaRealtimeStatus
import com.nova.app.core.messaging.NovaVoiceDraft
import com.nova.app.core.messaging.NovaVoiceRecorder
import com.nova.app.feature.messages.conversation.ConversationViewModel
import com.nova.app.feature.messages.conversation.PendingMessage
import com.nova.app.feature.messages.conversation.PendingMessageStatus
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaMessageShare
import com.nova.app.feature.messages.domain.model.NovaReplyPreview
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
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt


private val V8OnlineGreen = Color(0xFF35C982)
private val V8ReactionChoices = listOf("❤️", "😂", "😮", "😢", "😡", "👍")
private const val V8MaxVoiceMs = 5 * 60 * 1000L
private const val V8SwipeReplyThresholdPx = 72f
private const val V8CallHistoryClientPrefix = "call:"


@Composable
fun ConversationScreenV8(
    conversationId: Long,
    username: String,
    displayName: String,
    avatarUrl: String,
    onBack: () -> Unit,
    onConversationRead: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val container = context.appContainer
    val realtime = remember(conversationId, container) {
        container.conversationRealtime(conversationId)
    }
    val draftStore = remember(container) { container.conversationDraftStore() }
    val conversationViewModel: ConversationViewModel = viewModel(
        key = "conversation-$conversationId",
        factory = ConversationViewModel.factory(
            conversationId = conversationId,
            username = username,
            repository = container.messagingRepository,
            realtime = realtime,
            draftStore = draftStore,
            currentAuthor = container::currentMessageAuthor,
        ),
    )
    val state = conversationViewModel.state
    val voiceRecorder = remember(context) { NovaVoiceRecorder(appContext) }
    val listState = rememberLazyListState()
    val isGroupConversation = username == "group"
    val messages = state.messages
    val pendingMessages = state.pendingMessages
    val nextCursor = state.nextCursor
    val isLoading = state.isLoading
    val isLoadingEarlier = state.isLoadingEarlier
    val mutatingMessageId = state.mutatingMessageId
    val reactingMessageId = state.reactingMessageId
    val errorMessage = state.errorMessage
    val draft = state.draft
    val replyTarget = state.replyTarget
    val editingTarget = state.editingTarget
    val deleteTarget = state.deleteTarget
    val actionsForMessageId = state.actionsForMessageId
    val isOtherTyping = state.isOtherTyping
    val otherPresence = state.otherPresence
    val realtimeStatus = state.realtimeStatus
    val unreadAnchorMessageId = state.unreadAnchorMessageId
    val unreadCountAtOpen = state.unreadCountAtOpen
    val newMessagesAwayCount = state.newMessagesAwayCount

    var selectedImage by remember(conversationId) { mutableStateOf<Uri?>(null) }
    var voiceDraft by remember(conversationId) { mutableStateOf<NovaVoiceDraft?>(null) }
    var fullScreenPhotoUrl by remember(conversationId) { mutableStateOf<String?>(null) }
    var isRecording by remember(conversationId) { mutableStateOf(false) }
    var recordingStartedAt by remember(conversationId) { mutableLongStateOf(0L) }
    var recordingElapsedMs by remember(conversationId) { mutableLongStateOf(0L) }

    val nearBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total <= 1) true
            else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible >= total - 3
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && voiceDraft == null && !isRecording && editingTarget == null) {
            selectedImage = uri
            conversationViewModel.setErrorMessage(null)
        }
    }

    fun beginVoiceRecording() {
        if (editingTarget != null || selectedImage != null || voiceDraft != null || isRecording) return
        conversationViewModel.setErrorMessage(null)
        voiceRecorder.start()
            .onSuccess {
                isRecording = true
                recordingStartedAt = SystemClock.elapsedRealtime()
                recordingElapsedMs = 0L
                conversationViewModel.onRecordingChanged(true)
            }
            .onFailure {
                conversationViewModel.setErrorMessage("Nova couldn't start the microphone. Try again.")
            }
    }

    fun finishVoiceRecording() {
        if (!isRecording) return
        val result = voiceRecorder.stop()
        isRecording = false
        conversationViewModel.onRecordingChanged(false)
        recordingStartedAt = 0L
        recordingElapsedMs = 0L
        result.onSuccess { recorded ->
            if (recorded.durationMs < 1_000L) {
                recorded.file.delete()
                conversationViewModel.setErrorMessage(
                    "Voice message is too short. Record for at least 1 second."
                )
            } else {
                voiceDraft?.file?.delete()
                voiceDraft = recorded
                conversationViewModel.setErrorMessage(null)
            }
        }.onFailure {
            conversationViewModel.setErrorMessage(
                "Nova couldn't finish that recording. Record it again."
            )
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) beginVoiceRecording()
        else conversationViewModel.setErrorMessage(
            "Microphone permission is required to send voice messages."
        )
    }

    fun requestVoiceRecording() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            beginVoiceRecording()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun send() {
        val voice = voiceDraft
        if (conversationViewModel.send(selectedImage, voice?.file, voice?.durationMs)) {
            selectedImage = null
            voiceDraft = null
        }
    }

    fun startEdit(message: NovaMessage) {
        if (!conversationViewModel.startEdit(message)) return
        if (isRecording) {
            voiceRecorder.cancel()
            isRecording = false
            conversationViewModel.onRecordingChanged(false)
        }
        voiceDraft?.file?.delete()
        voiceDraft = null
        selectedImage = null
    }

    LaunchedEffect(nearBottom) {
        conversationViewModel.onNearBottomChanged(nearBottom)
    }

    LaunchedEffect(isRecording, recordingStartedAt) {
        while (isRecording) {
            recordingElapsedMs = (SystemClock.elapsedRealtime() - recordingStartedAt).coerceAtLeast(0L)
            if (recordingElapsedMs >= V8MaxVoiceMs) {
                finishVoiceRecording()
                break
            }
            delay(250)
        }
    }

    LaunchedEffect(state.scrollRequestVersion) {
        if (state.scrollRequestVersion > 0) {
            delay(30)
            val target = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            if (state.scrollRequestAnimated) listState.animateScrollToItem(target)
            else listState.scrollToItem(target)
            conversationViewModel.onScrollLatestCompleted()
        }
    }

    LaunchedEffect(state.conversationReadVersion) {
        if (state.conversationReadVersion > 0) onConversationRead()
    }

    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    DisposableEffect(conversationId, conversationViewModel) {
        NovaActiveConversation.enter(conversationId)
        conversationViewModel.startRealtime()
        conversationViewModel.loadLatest(showSpinner = true, scrollToBottom = true)

        onDispose {
            voiceRecorder.cancel()
            voiceDraft?.file?.delete()
            NovaActiveConversation.leave(conversationId)
            conversationViewModel.stopRealtime()
        }
    }

    ConversationChromeScaffold(
        topBar = {
            V8Header(
                username = username,
                displayName = displayName,
                avatarUrl = avatarUrl,
                presence = otherPresence,
                realtimeStatus = realtimeStatus,
                isTyping = isOtherTyping,
                onBack = onBack,
                onRefresh = { conversationViewModel.loadLatest(showSpinner = false) },
            )
        },
        bottomBar = {
            V8Composer(
                username = username,
                draft = draft,
                selectedImage = selectedImage,
                voiceDraft = voiceDraft,
                replyTarget = replyTarget,
                editingTarget = editingTarget,
                isMutating = mutatingMessageId != null,
                isRecording = isRecording,
                recordingElapsedMs = recordingElapsedMs,
                errorMessage = errorMessage,
                onDraftChange = conversationViewModel::onDraftChanged,
                onPickPhoto = {
                    if (editingTarget == null && voiceDraft == null && !isRecording) imagePicker.launch("image/*")
                },
                onRemovePhoto = { selectedImage = null },
                onStartRecording = ::requestVoiceRecording,
                onStopRecording = ::finishVoiceRecording,
                onCancelRecording = {
                    voiceRecorder.cancel()
                    isRecording = false
                    conversationViewModel.onRecordingChanged(false)
                    recordingStartedAt = 0L
                    recordingElapsedMs = 0L
                },
                onRemoveVoice = {
                    voiceDraft?.file?.delete()
                    voiceDraft = null
                },
                onCancelReply = conversationViewModel::cancelReply,
                onCancelEdit = conversationViewModel::cancelEdit,
                onSend = {
                    if (editingTarget != null) conversationViewModel.saveEdit() else send()
                },
            )
        },
    ) { innerPadding ->
        when {
            isLoading && messages.isEmpty() && pendingMessages.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = NovaAccent)
            }

            messages.isEmpty() && pendingMessages.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Start the conversation", color = NovaInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isGroupConversation) {
                        "Send a message, photo, or voice note to the group."
                    } else {
                        "Send a message, photo, or voice note to @$username."
                    },
                    color = NovaMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }

            else -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().background(NovaBackground),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (nextCursor != null) {
                        item(key = "load-earlier") {
                            Surface(
                                onClick = { if (!isLoadingEarlier) conversationViewModel.loadEarlier() },
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

                    itemsIndexed(messages, key = { _, message -> "server-${message.id}" }) { index, message ->
                        if (message.id == unreadAnchorMessageId && unreadCountAtOpen > 0) {
                            V8UnreadDivider(unreadCountAtOpen)
                        }

                        val previous = messages.getOrNull(index - 1)
                        val day = messageLocalDateV8(message.createdAt)
                        val previousDay = previous?.let { messageLocalDateV8(it.createdAt) }
                        if (day != null && day != previousDay) V8DateDivider(day)

                        val previousSameSender = previous?.sender?.id == message.sender.id && previousDay == day
                        val next = messages.getOrNull(index + 1)
                        val nextSameSender = next?.sender?.id == message.sender.id && messageLocalDateV8(next.createdAt) == day

                        V8MessageBubble(
                            message = message,
                            compactTop = previousSameSender,
                            compactBottom = nextSameSender,
                            showSenderName = isGroupConversation && !message.isMine && !previousSameSender,
                            showActions = actionsForMessageId == message.id,
                            reactionBusy = reactingMessageId == message.id,
                            mutationBusy = mutatingMessageId == message.id,
                            onToggleActions = {
                                conversationViewModel.toggleActions(message.id)
                            },
                            onReply = { conversationViewModel.startReply(message) },
                            onEdit = { startEdit(message) },
                            onDelete = { conversationViewModel.confirmDelete(message) },
                            onReact = { emoji -> conversationViewModel.setReaction(message, emoji) },
                            onOpenPhoto = { fullScreenPhotoUrl = it },
                            onOpenSharedPost = { postId -> openSharedPostV8(context, postId) },
                            onOpenSharedProfile = { sharedUsername -> openSharedProfileV8(context, sharedUsername) },
                        )
                    }

                    items(pendingMessages, key = { "pending-${it.clientId}" }) { pending ->
                        V8PendingBubble(
                            pending = pending,
                            onRetry = { conversationViewModel.retryPending(pending) },
                        )
                    }
                }

                if (!nearBottom || newMessagesAwayCount > 0) {
                    Surface(
                        onClick = { conversationViewModel.requestScrollLatest(animated = true) },
                        shape = RoundedCornerShape(22.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                        shadowElevation = 5.dp,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    ) {
                        Text(
                            if (newMessagesAwayCount > 0) "↓  $newMessagesAwayCount new" else "↓  Latest",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            color = NovaAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = conversationViewModel::dismissDelete,
            title = { Text("Delete message?", color = NovaInk, fontWeight = FontWeight.Bold) },
            text = { Text("This removes the message for everyone. This can't be undone.", color = NovaMuted) },
            confirmButton = {
                Surface(
                    onClick = {
                        if (mutatingMessageId == null) conversationViewModel.deleteForEveryone()
                    },
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
                    onClick = conversationViewModel::dismissDelete,
                    shape = RoundedCornerShape(14.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Text("Cancel", modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp), color = NovaMuted)
                }
            },
            containerColor = NovaSurface,
        )
    }

    fullScreenPhotoUrl?.let { photoUrl ->
        V8FullScreenPhoto(photoUrl = photoUrl, onDismiss = { fullScreenPhotoUrl = null })
    }
}


@Composable
private fun V8Header(
    username: String,
    displayName: String,
    avatarUrl: String,
    presence: NovaConversationPresence?,
    realtimeStatus: NovaRealtimeStatus,
    isTyping: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    val isGroupConversation = username == "group"
    val online = !isGroupConversation && presence?.isOnline == true
    val subtitle = when {
        realtimeStatus == NovaRealtimeStatus.Live && isTyping -> if (isGroupConversation) "Someone is typing…" else "typing…"
        isGroupConversation && realtimeStatus == NovaRealtimeStatus.Live -> "Group conversation"
        realtimeStatus == NovaRealtimeStatus.Live && online -> "Online"
        realtimeStatus == NovaRealtimeStatus.Live && presence?.lastSeenAt != null -> formatLastSeenV8(presence.lastSeenAt)
        realtimeStatus == NovaRealtimeStatus.Connecting -> "Connecting…"
        realtimeStatus == NovaRealtimeStatus.Reconnecting -> "Reconnecting…"
        realtimeStatus == NovaRealtimeStatus.Offline -> "Offline"
        isGroupConversation -> "Group conversation"
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
                NovaAvatar(source = avatarUrl, fallbackText = displayName.ifBlank { username }, size = 44.dp)
                if (online) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(V8OnlineGreen)
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
private fun V8Composer(
    username: String,
    draft: String,
    selectedImage: Uri?,
    voiceDraft: NovaVoiceDraft?,
    replyTarget: NovaMessage?,
    editingTarget: NovaMessage?,
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
                V8ComposerContextCard(
                    title = "Editing message",
                    preview = replyPreviewTextV8(editingTarget),
                    onClose = onCancelEdit,
                )
            } else if (replyTarget != null) {
                V8ComposerContextCard(
                    title = "Replying to @${replyTarget.sender.username}",
                    preview = replyPreviewTextV8(replyTarget),
                    onClose = onCancelReply,
                )
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
                            Text("Recording ${formatVoiceDurationV8(recordingElapsedMs)}", color = NovaInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Up to 5 minutes", color = NovaMuted, fontSize = 10.sp)
                        }
                        V8SmallChip("Cancel", onCancelRecording)
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
                        Text("It will appear instantly while uploading.", color = NovaMuted, fontSize = 11.sp)
                    }
                    V8SmallChip("Remove", onRemovePhoto)
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
                        Text(formatVoiceDurationV8(voiceDraft.durationMs), color = NovaMuted, fontSize = 11.sp)
                    }
                    V8SmallChip("Remove", onRemoveVoice)
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
                        color = if (voiceDraft == null) NovaAccentSoft else NovaBackground,
                    ) {
                        Text("+", modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = NovaAccent, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isMutating && !isRecording,
                    placeholder = {
                        Text(
                            when {
                                editingTarget != null -> "Edit message"
                                username == "group" -> "Message group"
                                else -> "Message @$username"
                            },
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
                    !isRecording && (draft.isNotBlank() || selectedImage != null || voiceDraft != null)
                }
                Surface(
                    onClick = { if (enabled) onSend() },
                    shape = CircleShape,
                    color = if (enabled) NovaAccent else NovaAccentSoft,
                ) {
                    Text(
                        when {
                            isMutating -> "…"
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
private fun V8ComposerContextCard(title: String, preview: String, onClose: () -> Unit) {
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
                Text(title, color = NovaAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(preview, color = NovaMuted, fontSize = 12.sp, maxLines = 1)
            }
            Surface(onClick = onClose, shape = CircleShape, color = NovaSurface) {
                Text("×", modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = NovaMuted, fontSize = 18.sp)
            }
        }
    }
}


@Composable
private fun V8SmallChip(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = NovaBackground) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = NovaMuted, fontSize = 11.sp)
    }
}


@Composable
private fun V8MessageBubble(
    message: NovaMessage,
    compactTop: Boolean,
    compactBottom: Boolean,
    showSenderName: Boolean,
    showActions: Boolean,
    reactionBusy: Boolean,
    mutationBusy: Boolean,
    onToggleActions: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onOpenPhoto: (String) -> Unit,
    onOpenSharedPost: (Long) -> Unit,
    onOpenSharedProfile: (String) -> Unit,
) {
    var dragX by remember(message.id) { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (compactTop) 0.dp else 5.dp)
            .offset { IntOffset(dragX.roundToInt(), 0) }
            .pointerInput(message.id, message.isDeleted) {
                if (!message.isDeleted) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount ->
                            dragX = (dragX + amount).coerceIn(-120f, 120f)
                        },
                        onDragEnd = {
                            if (abs(dragX) >= V8SwipeReplyThresholdPx) onReply()
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f },
                    )
                }
            },
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
    ) {
        if (abs(dragX) > 24f) {
            Text("↩ Reply", color = NovaAccent, fontSize = 10.sp, modifier = Modifier.padding(bottom = 3.dp))
        }

        Box(contentAlignment = if (message.isMine) Alignment.TopEnd else Alignment.TopStart) {
            Surface(
                modifier = Modifier.combinedClickable(
                    enabled = !message.isDeleted,
                    onClick = {},
                    onLongClick = onToggleActions,
                ),
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
                    if (showSenderName) {
                        Text(
                            text = message.sender.name.ifBlank { "@${message.sender.username}" },
                            color = NovaAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (message.isDeleted) {
                        Text(
                            "Message deleted",
                            color = if (message.isMine) NovaBackground.copy(alpha = 0.78f) else NovaMuted,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                        )
                    } else {
                        message.replyTo?.let { reply -> V8ReplyPreview(reply, message.isMine) }

                        if (message.imageUrl.isNotBlank()) {
                            Surface(onClick = { onOpenPhoto(message.imageUrl) }, shape = RoundedCornerShape(14.dp), color = Color.Transparent) {
                                NovaMediaImage(
                                    source = message.imageUrl,
                                    modifier = Modifier.fillMaxWidth().height(240.dp),
                                    contentDescription = "Message photo",
                                )
                            }
                            if (message.body.isNotBlank() || message.share != null) Spacer(Modifier.height(8.dp))
                        }

                        if (message.audioUrl.isNotBlank()) {
                            V8VoiceNotePlayer(message.audioUrl, message.audioDurationMs, message.isMine)
                            if (message.body.isNotBlank() || message.share != null) Spacer(Modifier.height(7.dp))
                        }

                        message.share?.let { share ->
                            V8SharedContentCard(
                                share = share,
                                mine = message.isMine,
                                onOpenPost = onOpenSharedPost,
                                onOpenProfile = onOpenSharedProfile,
                            )
                        }

                        if (message.share == null && message.body.isNotBlank()) {
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
                        localMessageTimeV8(message.createdAt) + edited + delivery,
                        color = if (message.isMine) NovaBackground.copy(alpha = 0.72f) else NovaMuted,
                        fontSize = 9.sp,
                    )
                }
            }

            DropdownMenu(
                expanded = showActions && !message.isDeleted,
                onDismissRequest = onToggleActions,
                containerColor = NovaSurface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    V8ReactionChoices.forEach { emoji ->
                        Surface(
                            onClick = { if (!reactionBusy && !mutationBusy) onReact(emoji) },
                            shape = CircleShape,
                            color = NovaBackground,
                        ) {
                            Text(emoji, modifier = Modifier.padding(6.dp), fontSize = 16.sp)
                        }
                    }
                }
                DropdownMenuItem(
                    text = { Text("Reply", color = NovaInk) },
                    onClick = { if (!mutationBusy) onReply() },
                    enabled = !mutationBusy,
                )
                if (message.isMine && message.share == null && !message.isCallHistoryV8()) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = NovaInk) },
                        onClick = { if (!mutationBusy) onEdit() },
                        enabled = !mutationBusy,
                    )
                }
                if (message.isMine) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = NovaInk) },
                        onClick = { if (!mutationBusy) onDelete() },
                        enabled = !mutationBusy,
                    )
                }
            }
        }

        if (!message.isDeleted && message.reactions.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                message.reactions.forEach { reaction ->
                    Surface(
                        onClick = { if (!reactionBusy) onReact(reaction.emoji) },
                        shape = RoundedCornerShape(13.dp),
                        color = if (reaction.reactedByMe) NovaAccentSoft else NovaSurface,
                        border = BorderStroke(1.dp, if (reaction.reactedByMe) NovaAccent else NovaBorder),
                    ) {
                        Text("${reaction.emoji} ${reaction.count}", modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = NovaInk, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}


@Composable
private fun V8SharedContentCard(
    share: NovaMessageShare,
    mine: Boolean,
    onOpenPost: (Long) -> Unit,
    onOpenProfile: (String) -> Unit,
) {
    val context = LocalContext.current
    val cardColor = if (mine) NovaBackground.copy(alpha = 0.16f) else NovaBackground
    val borderColor = if (mine) NovaBackground.copy(alpha = 0.25f) else NovaBorder
    val primary = if (mine) NovaBackground else NovaInk
    val secondary = if (mine) NovaBackground.copy(alpha = 0.75f) else NovaMuted

    if (!share.available) {
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = when (share.kind) {
                        "profile" -> "Shared profile"
                        "reel" -> "Shared Reel"
                        else -> "Shared post"
                    },
                    color = primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text("This content is no longer available to you.", color = secondary, fontSize = 11.sp)
            }
        }
        return
    }

    share.post?.let { post ->
        Surface(
            onClick = { onOpenPost(post.id) },
            shape = RoundedCornerShape(15.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NovaAvatar(
                        source = post.author.avatarUrl,
                        fallbackText = post.author.name.ifBlank { post.author.username },
                        size = 34.dp,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = post.author.name.ifBlank { post.author.username },
                            color = primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text("@${post.author.username} · Shared post", color = secondary, fontSize = 9.sp, maxLines = 1)
                    }
                    Text("›", color = secondary, fontSize = 18.sp)
                }
                if (post.imageUrl.isNotBlank()) {
                    NovaMediaImage(
                        source = post.imageUrl,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentDescription = "Shared post photo",
                    )
                }
                if (post.caption.isNotBlank()) {
                    Text(
                        text = post.caption,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        color = primary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                    )
                }
            }
        }
        return
    }

    share.reel?.let { reel ->
        Surface(
            onClick = {
                com.nova.app.core.reels.NovaReelsNavigator.openProfile(
                    context = context,
                    username = reel.author.username,
                    initialReelId = reel.id,
                )
            },
            shape = RoundedCornerShape(15.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    NovaAvatar(
                        source = reel.author.avatarUrl,
                        fallbackText = reel.author.name.ifBlank { reel.author.username },
                        size = 38.dp,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = reel.author.name.ifBlank { reel.author.username },
                            color = primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text("@${reel.author.username} · Reel", color = secondary, fontSize = 9.sp, maxLines = 1)
                    }
                    Text("▶", color = if (mine) NovaBackground else NovaAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                if (reel.caption.isNotBlank()) {
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = reel.caption,
                        color = primary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Watch Reel",
                    color = if (mine) NovaBackground else NovaAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        return
    }

    share.profile?.let { profile ->
        Surface(
            onClick = { onOpenProfile(profile.username) },
            shape = RoundedCornerShape(15.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NovaAvatar(
                    source = profile.avatarUrl,
                    fallbackText = profile.name.ifBlank { profile.username },
                    size = 48.dp,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = profile.name.ifBlank { profile.username },
                        color = primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text("@${profile.username}", color = secondary, fontSize = 10.sp, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text("View profile", color = if (mine) NovaBackground else NovaAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("›", color = secondary, fontSize = 20.sp)
            }
        }
        return
    }

    Surface(
        shape = RoundedCornerShape(15.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Shared content unavailable",
            modifier = Modifier.padding(12.dp),
            color = secondary,
            fontSize = 11.sp,
        )
    }
}


@Composable
private fun V8PendingBubble(pending: PendingMessage, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 5.dp), horizontalAlignment = Alignment.End) {
        Surface(
            onClick = { if (pending.status == PendingMessageStatus.Failed) onRetry() },
            shape = RoundedCornerShape(20.dp, 20.dp, 5.dp, 20.dp),
            color = if (pending.status == PendingMessageStatus.Failed) NovaAccent.copy(alpha = 0.72f) else NovaAccent,
        ) {
            Column(modifier = Modifier.widthIn(max = 292.dp).padding(horizontal = 10.dp, vertical = 9.dp)) {
                pending.replyTo?.let { V8ReplyPreview(it, mine = true) }

                if (pending.imageUri.isNotBlank()) {
                    NovaMediaImage(
                        source = pending.imageUri,
                        modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(14.dp)),
                        contentDescription = "Sending message photo",
                    )
                    if (pending.body.isNotBlank()) Spacer(Modifier.height(8.dp))
                }

                if (pending.audioPath.isNotBlank()) {
                    V8VoiceNotePlayer(pending.audioPath, pending.audioDurationMs, mine = true)
                    if (pending.body.isNotBlank()) Spacer(Modifier.height(7.dp))
                }

                if (pending.body.isNotBlank()) {
                    Text(pending.body, color = NovaBackground, fontSize = 14.sp, lineHeight = 20.sp)
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    when (pending.status) {
                        PendingMessageStatus.Sending -> "${localMessageTimeV8(pending.createdAt)} · Sending…"
                        PendingMessageStatus.Failed -> "${localMessageTimeV8(pending.createdAt)} · Failed · Tap to retry"
                    },
                    color = NovaBackground.copy(alpha = 0.78f),
                    fontSize = 9.sp,
                )
                if (pending.status == PendingMessageStatus.Failed && !pending.error.isNullOrBlank()) {
                    Text(pending.error, color = NovaBackground.copy(alpha = 0.72f), fontSize = 9.sp, maxLines = 2)
                }
            }
        }
    }
}


@Composable
private fun V8ReplyPreview(reply: NovaReplyPreview, mine: Boolean) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = if (mine) NovaBackground.copy(alpha = 0.18f) else NovaAccentSoft,
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(
                "@${reply.sender.username}",
                color = if (mine) NovaBackground else NovaAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                replyPreviewTextV8(reply),
                color = if (mine) NovaBackground.copy(alpha = 0.8f) else NovaMuted,
                fontSize = 11.sp,
                maxLines = 2,
            )
        }
    }
}


@Composable
private fun V8ActionChip(label: String, enabled: Boolean, onClick: () -> Unit) {
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
private fun V8VoiceNotePlayer(audioUrl: String, durationMs: Long?, mine: Boolean) {
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
        }.onFailure { failed = true }

        onDispose { runCatching { player.release() } }
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
                Text(if (failed) "Voice unavailable" else "Voice message", color = if (mine) NovaBackground else NovaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(formatVoiceDurationV8(durationMs ?: 0L), color = if (mine) NovaBackground.copy(alpha = 0.72f) else NovaMuted, fontSize = 10.sp)
            }
        }
    }
}


@Composable
private fun V8UnreadDivider(count: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
        Surface(shape = RoundedCornerShape(14.dp), color = NovaAccentSoft, border = BorderStroke(1.dp, NovaAccent)) {
            Text(
                if (count == 1) "1 unread message" else "$count unread messages",
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                color = NovaAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}


@Composable
private fun V8DateDivider(day: LocalDate) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
        Surface(shape = RoundedCornerShape(14.dp), color = NovaSurface, border = BorderStroke(1.dp, NovaBorder)) {
            Text(dayLabelV8(day), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = NovaMuted, fontSize = 10.sp)
        }
    }
}


@Composable
private fun V8FullScreenPhoto(photoUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
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


private fun openSharedPostV8(context: android.content.Context, postId: Long) {
    context.startActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("kind", "comment")
            putExtra("post_id", postId.toString())
        }
    )
}

private fun openSharedProfileV8(context: android.content.Context, username: String) {
    if (username.isBlank()) return
    context.startActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("kind", "follow")
            putExtra("actor_username", username)
        }
    )
}

private fun NovaMessage.isCallHistoryV8(): Boolean = clientId.startsWith(V8CallHistoryClientPrefix)

private fun replyPreviewTextV8(message: NovaMessage): String = when {
    message.isDeleted -> "Message deleted"
    message.share?.kind == "post" -> "↗ Shared post"
    message.share?.kind == "profile" -> "↗ Shared profile"
    message.share?.kind == "reel" -> "↗ Shared Reel"
    message.body.isNotBlank() -> message.body
    message.audioUrl.isNotBlank() -> "🎤 Voice message"
    message.imageUrl.isNotBlank() -> "📷 Photo"
    else -> "Message"
}

private fun replyPreviewTextV8(reply: NovaReplyPreview): String = when {
    reply.isDeleted -> "Message deleted"
    reply.body.isNotBlank() -> reply.body
    reply.audioUrl.isNotBlank() -> "🎤 Voice message"
    reply.imageUrl.isNotBlank() -> "📷 Photo"
    else -> "Message"
}

private fun formatVoiceDurationV8(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L).coerceAtMost(5 * 60L)
    return "%d:%02d".format(Locale.US, totalSeconds / 60L, totalSeconds % 60L)
}

private fun parseMessageInstantV8(value: String): Instant? {
    return runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull()
}

private fun messageLocalDateV8(value: String): LocalDate? {
    return parseMessageInstantV8(value)?.atZone(ZoneId.systemDefault())?.toLocalDate()
}

private fun localMessageTimeV8(value: String): String {
    val instant = parseMessageInstantV8(value) ?: return ""
    return DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(instant.atZone(ZoneId.systemDefault()))
}

private fun dayLabelV8(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }
}

private fun formatLastSeenV8(value: String): String {
    val instant = parseMessageInstantV8(value) ?: return "Last seen recently"
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
