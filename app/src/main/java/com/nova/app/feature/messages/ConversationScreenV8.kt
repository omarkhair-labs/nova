package com.nova.app.feature.messages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nova.app.MainActivity
import com.nova.app.app.appContainer
import com.nova.app.core.messaging.NovaActiveConversation
import com.nova.app.core.messaging.NovaConversationPresence
import com.nova.app.core.messaging.NovaRealtimeStatus
import com.nova.app.core.messaging.NovaVoiceDraft
import com.nova.app.core.messaging.NovaVoiceRecorder
import com.nova.app.feature.messages.conversation.ConversationFullScreenPhoto
import com.nova.app.feature.messages.conversation.ConversationMessageList
import com.nova.app.feature.messages.conversation.ConversationViewModel
import com.nova.app.feature.messages.domain.model.NovaMessage
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


private val V8OnlineGreen = Color(0xFF35C982)
private const val V8MaxVoiceMs = 5 * 60 * 1000L


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
    val mutatingMessageId = state.mutatingMessageId
    val errorMessage = state.errorMessage
    val draft = state.draft
    val replyTarget = state.replyTarget
    val editingTarget = state.editingTarget
    val deleteTarget = state.deleteTarget
    val isOtherTyping = state.isOtherTyping
    val otherPresence = state.otherPresence
    val realtimeStatus = state.realtimeStatus

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
        ConversationMessageList(
            state = state,
            username = username,
            isGroupConversation = isGroupConversation,
            listState = listState,
            nearBottom = nearBottom,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            onLoadEarlier = conversationViewModel::loadEarlier,
            onToggleActions = conversationViewModel::toggleActions,
            onReply = conversationViewModel::startReply,
            onEdit = ::startEdit,
            onDelete = conversationViewModel::confirmDelete,
            onReact = conversationViewModel::setReaction,
            onOpenPhoto = { fullScreenPhotoUrl = it },
            onOpenSharedPost = { postId -> openSharedPostV8(context, postId) },
            onOpenSharedProfile = { sharedUsername -> openSharedProfileV8(context, sharedUsername) },
            onOpenSharedReel = { reelUsername, reelId ->
                com.nova.app.core.reels.NovaReelsNavigator.openProfile(
                    context = context,
                    username = reelUsername,
                    initialReelId = reelId,
                )
            },
            onRetryPending = conversationViewModel::retryPending,
            onScrollLatest = { conversationViewModel.requestScrollLatest(animated = true) },
        )
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
        ConversationFullScreenPhoto(photoUrl = photoUrl, onDismiss = { fullScreenPhotoUrl = null })
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

private fun formatVoiceDurationV8(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L).coerceAtMost(5 * 60L)
    return "%d:%02d".format(Locale.US, totalSeconds / 60L, totalSeconds % 60L)
}

private fun parseMessageInstantV8(value: String): Instant? {
    return runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull()
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
