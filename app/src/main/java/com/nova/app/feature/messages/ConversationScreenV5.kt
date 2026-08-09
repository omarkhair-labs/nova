package com.nova.app.feature.messages

import android.net.Uri
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.messaging.NovaActiveConversation
import com.nova.app.core.messaging.NovaConversationPresence
import com.nova.app.core.messaging.NovaConversationRealtimeClient
import com.nova.app.core.messaging.NovaMessage
import com.nova.app.core.messaging.NovaMessageReaction
import com.nova.app.core.messaging.NovaMessageReactionEvent
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.messaging.NovaRealtimeEvent
import com.nova.app.core.messaging.NovaRealtimeStatus
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


private val V5OnlineGreen = Color(0xFF35C982)
private val V5ReactionChoices = listOf("❤️", "😂", "😮", "😢", "😡", "👍")


@Composable
fun ConversationScreenV5(
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
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember(conversationId) { mutableStateOf<List<NovaMessage>>(emptyList()) }
    var nextCursor by remember(conversationId) { mutableStateOf<String?>(null) }
    var isLoading by remember(conversationId) { mutableStateOf(true) }
    var isLoadingEarlier by remember(conversationId) { mutableStateOf(false) }
    var isSending by remember(conversationId) { mutableStateOf(false) }
    var reactingMessageId by remember(conversationId) { mutableStateOf<Long?>(null) }
    var errorMessage by remember(conversationId) { mutableStateOf<String?>(null) }
    var draft by remember(conversationId) { mutableStateOf("") }
    var selectedImage by remember(conversationId) { mutableStateOf<Uri?>(null) }
    var replyTarget by remember(conversationId) { mutableStateOf<NovaMessage?>(null) }
    var actionsForMessageId by remember(conversationId) { mutableStateOf<Long?>(null) }
    var isOtherTyping by remember(conversationId) { mutableStateOf(false) }
    var typingAnnounced by remember(conversationId) { mutableStateOf(false) }
    var otherPresence by remember(conversationId) { mutableStateOf<NovaConversationPresence?>(null) }
    var realtimeStatus by remember(conversationId) { mutableStateOf(NovaRealtimeStatus.Connecting) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedImage = uri
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

    fun send() {
        val body = draft.trim()
        val image = selectedImage
        if ((body.isBlank() && image == null) || isSending) return

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
                )
            ) {
                is ApiResult.Success -> {
                    if (messages.none { it.id == result.value.id }) {
                        messages = messages + result.value
                    }
                    draft = ""
                    selectedImage = null
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

    fun setReaction(message: NovaMessage, emoji: String) {
        if (reactingMessageId != null) return
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
            if (message.id != event.messageId) return@map message

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
            message.copy(reactions = mutable.values.sortedBy { V5ReactionChoices.indexOf(it.emoji).let { index -> if (index < 0) 99 else index } })
        }
    }

    LaunchedEffect(conversationId) {
        loadLatest(showSpinner = true, scrollToBottom = true)
    }

    LaunchedEffect(draft, realtimeStatus) {
        if (realtimeStatus != NovaRealtimeStatus.Live) {
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
        )

        onDispose {
            if (typingAnnounced) realtimeClient.sendTyping(false)
            NovaActiveConversation.leave(conversationId)
            realtimeClient.stop()
        }
    }

    Scaffold(
        containerColor = NovaBackground,
        topBar = {
            V5Header(
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
            V5Composer(
                username = username,
                draft = draft,
                selectedImage = selectedImage,
                replyTarget = replyTarget,
                isSending = isSending,
                errorMessage = errorMessage,
                onDraftChange = { draft = it.take(2000) },
                onPickPhoto = { if (!isSending) imagePicker.launch("image/*") },
                onRemovePhoto = { selectedImage = null },
                onCancelReply = { replyTarget = null },
                onSend = ::send,
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
                    "Send a message or photo to @$username.",
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
                    val day = messageLocalDate(message.createdAt)
                    val previousDay = previous?.let { messageLocalDate(it.createdAt) }
                    if (day != null && day != previousDay) {
                        V5DateDivider(day)
                    }

                    val previousSameSender = previous?.sender?.id == message.sender.id && previousDay == day
                    val next = messages.getOrNull(index + 1)
                    val nextSameSender = next?.sender?.id == message.sender.id && messageLocalDate(next.createdAt) == day

                    V5MessageBubble(
                        message = message,
                        compactTop = previousSameSender,
                        compactBottom = nextSameSender,
                        showActions = actionsForMessageId == message.id,
                        reactionBusy = reactingMessageId == message.id,
                        onToggleActions = {
                            actionsForMessageId = if (actionsForMessageId == message.id) null else message.id
                        },
                        onReply = {
                            replyTarget = message
                            actionsForMessageId = null
                        },
                        onReact = { emoji -> setReaction(message, emoji) },
                    )
                }
            }
        }
    }
}


@Composable
private fun V5Header(
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
        realtimeStatus == NovaRealtimeStatus.Live && presence?.lastSeenAt != null -> formatLastSeen(presence.lastSeenAt)
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
                        modifier = Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(V5OnlineGreen)
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
private fun V5Composer(
    username: String,
    draft: String,
    selectedImage: Uri?,
    replyTarget: NovaMessage?,
    isSending: Boolean,
    errorMessage: String?,
    onDraftChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onCancelReply: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(color = NovaSurface, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            if (replyTarget != null) {
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
                            Text(replyPreviewText(replyTarget), color = NovaMuted, fontSize = 12.sp, maxLines = 1)
                        }
                        Surface(onClick = onCancelReply, shape = CircleShape, color = NovaSurface) {
                            Text("×", modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = NovaMuted, fontSize = 18.sp)
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

            if (errorMessage != null) {
                Text(errorMessage, color = NovaMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(onClick = onPickPhoto, shape = CircleShape, color = NovaAccentSoft) {
                    Text("+", modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = NovaAccent, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isSending,
                    placeholder = { Text("Message @$username", color = NovaMuted) },
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
                val enabled = !isSending && (draft.isNotBlank() || selectedImage != null)
                Surface(
                    onClick = { if (enabled) onSend() },
                    shape = CircleShape,
                    color = if (enabled) NovaAccent else NovaAccentSoft,
                ) {
                    Text(
                        if (isSending) "…" else "↑",
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
private fun V5MessageBubble(
    message: NovaMessage,
    compactTop: Boolean,
    compactBottom: Boolean,
    showActions: Boolean,
    reactionBusy: Boolean,
    onToggleActions: () -> Unit,
    onReply: () -> Unit,
    onReact: (String) -> Unit,
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
                                reply.body.ifBlank { if (reply.imageUrl.isNotBlank()) "📷 Photo" else "Message" },
                                color = if (message.isMine) NovaBackground.copy(alpha = 0.8f) else NovaMuted,
                                fontSize = 11.sp,
                                maxLines = 2,
                            )
                        }
                    }
                }

                if (message.imageUrl.isNotBlank()) {
                    NovaMediaImage(
                        source = message.imageUrl,
                        modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(14.dp)),
                        contentDescription = "Message photo",
                    )
                    if (message.body.isNotBlank()) Spacer(Modifier.height(8.dp))
                }

                if (message.body.isNotBlank()) {
                    Text(
                        message.body,
                        color = if (message.isMine) NovaBackground else NovaInk,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }

                Spacer(Modifier.height(4.dp))
                val delivery = when {
                    !message.isMine -> ""
                    message.readAt != null -> " · Read"
                    message.deliveredAt != null -> " · Delivered"
                    else -> " · Sent"
                }
                Text(
                    localMessageTime(message.createdAt) + delivery,
                    color = if (message.isMine) NovaBackground.copy(alpha = 0.72f) else NovaMuted,
                    fontSize = 9.sp,
                )
            }
        }

        if (message.reactions.isNotEmpty()) {
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

        if (showActions) {
            Row(
                modifier = Modifier.padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(onClick = onReply, shape = RoundedCornerShape(13.dp), color = NovaSurface, border = BorderStroke(1.dp, NovaBorder)) {
                    Text("Reply", modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = NovaMuted, fontSize = 11.sp)
                }
                V5ReactionChoices.forEach { emoji ->
                    Surface(
                        onClick = { if (!reactionBusy) onReact(emoji) },
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


@Composable
private fun V5DateDivider(day: LocalDate) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(shape = RoundedCornerShape(14.dp), color = NovaSurface, border = BorderStroke(1.dp, NovaBorder)) {
            Text(dayLabel(day), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = NovaMuted, fontSize = 10.sp)
        }
    }
}


private fun replyPreviewText(message: NovaMessage): String {
    return message.body.ifBlank { if (message.imageUrl.isNotBlank()) "📷 Photo" else "Message" }
}


private fun parseMessageInstant(value: String): Instant? {
    return runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull()
}


private fun messageLocalDate(value: String): LocalDate? {
    return parseMessageInstant(value)?.atZone(ZoneId.systemDefault())?.toLocalDate()
}


private fun localMessageTime(value: String): String {
    val instant = parseMessageInstant(value) ?: return ""
    return DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        .format(instant.atZone(ZoneId.systemDefault()))
}


private fun dayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }
}


private fun formatLastSeen(value: String): String {
    val instant = parseMessageInstant(value) ?: return "Last seen recently"
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
