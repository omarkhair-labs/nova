package com.nova.app.feature.messages.conversation

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.nova.app.feature.messages.ConversationChromeScaffold
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
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


private val ConversationOnlineGreen = Color(0xFF35C982)


/** Stable live conversation content: header, message viewport, composer, and mutation dialogs. */
@Composable
fun ConversationContent(
    conversationId: Long,
    username: String,
    displayName: String,
    avatarUrl: String,
    onBack: () -> Unit,
    onConversationRead: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
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
    val composerState = rememberConversationComposerState(
        conversationId = conversationId,
        onRecordingChanged = conversationViewModel::onRecordingChanged,
        onErrorMessage = conversationViewModel::setErrorMessage,
    )
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

    var fullScreenPhotoUrl by remember(conversationId) { mutableStateOf<String?>(null) }

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

    fun send() {
        val voice = composerState.voiceDraft
        if (conversationViewModel.send(composerState.selectedImage, voice?.file, voice?.durationMs)) {
            composerState.clearAfterSend()
        }
    }

    fun startEdit(message: NovaMessage) {
        if (!conversationViewModel.startEdit(message)) return
        composerState.prepareForEdit()
    }

    LaunchedEffect(nearBottom) {
        conversationViewModel.onNearBottomChanged(nearBottom)
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
            NovaActiveConversation.leave(conversationId)
            conversationViewModel.stopRealtime()
        }
    }

    ConversationChromeScaffold(
        topBar = {
            ConversationHeader(
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
            ConversationComposer(
                username = username,
                draft = draft,
                state = composerState,
                replyTarget = replyTarget,
                editingTarget = editingTarget,
                isMutating = mutatingMessageId != null,
                errorMessage = errorMessage,
                onDraftChange = conversationViewModel::onDraftChanged,
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
            onOpenSharedPost = { postId -> openSharedPost(context, postId) },
            onOpenSharedProfile = { sharedUsername -> openSharedProfile(context, sharedUsername) },
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
private fun ConversationHeader(
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
        realtimeStatus == NovaRealtimeStatus.Live && presence?.lastSeenAt != null -> formatConversationLastSeen(presence.lastSeenAt)
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
                        modifier = Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(ConversationOnlineGreen)
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


private fun openSharedPost(context: android.content.Context, postId: Long) {
    context.startActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("kind", "comment")
            putExtra("post_id", postId.toString())
        }
    )
}

private fun openSharedProfile(context: android.content.Context, username: String) {
    if (username.isBlank()) return
    context.startActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("kind", "follow")
            putExtra("actor_username", username)
        }
    )
}

private fun parseConversationLastSeenInstant(value: String): Instant? {
    return runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull()
}

private fun formatConversationLastSeen(value: String): String {
    val instant = parseConversationLastSeenInstant(value) ?: return "Last seen recently"
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
