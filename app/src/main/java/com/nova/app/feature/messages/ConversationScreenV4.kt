package com.nova.app.feature.messages

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.messaging.NovaRealtimeEvent
import com.nova.app.core.messaging.NovaRealtimeStatus
import com.nova.app.core.network.ApiResult
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID


private val OnlineGreen = Color(0xFF35C982)


@Composable
fun ConversationScreenV4(
    conversationId: Long,
    username: String,
    displayName: String,
    avatarUrl: String,
    onBack: () -> Unit,
    onConversationRead: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaMessagingRepository(context.applicationContext)
    }
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
    var errorMessage by remember(conversationId) { mutableStateOf<String?>(null) }
    var draft by remember(conversationId) { mutableStateOf("") }
    var isOtherTyping by remember(conversationId) { mutableStateOf(false) }
    var typingAnnounced by remember(conversationId) { mutableStateOf(false) }
    var otherPresence by remember(conversationId) { mutableStateOf<NovaConversationPresence?>(null) }
    var realtimeStatus by remember(conversationId) {
        mutableStateOf(NovaRealtimeStatus.Connecting)
    }

    fun markConversationRead() {
        scope.launch {
            when (val result = repository.markRead(conversationId)) {
                is ApiResult.Success -> onConversationRead()
                is ApiResult.Failure -> if (result.statusCode == 401) onSessionExpired()
            }
        }
    }

    fun loadLatest(
        showSpinner: Boolean = true,
        scrollToBottom: Boolean = false,
    ) {
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
                    if (scrollToBottom && result.value.messages.isNotEmpty()) {
                        listState.scrollToItem(result.value.messages.lastIndex)
                    }
                }

                is ApiResult.Failure -> {
                    isLoading = false
                    if (result.statusCode == 401) {
                        onSessionExpired()
                    } else if (showSpinner || messages.isEmpty()) {
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    fun loadEarlier() {
        val cursor = nextCursor ?: return
        if (isLoadingEarlier) return

        scope.launch {
            isLoadingEarlier = true
            errorMessage = null
            when (val result = repository.messages(conversationId, cursor)) {
                is ApiResult.Success -> {
                    val existingIds = messages.mapTo(mutableSetOf()) { it.id }
                    messages = result.value.messages.filterNot { it.id in existingIds } + messages
                    nextCursor = result.value.nextCursor
                    isLoadingEarlier = false
                }

                is ApiResult.Failure -> {
                    isLoadingEarlier = false
                    if (result.statusCode == 401) {
                        onSessionExpired()
                    } else {
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    fun send() {
        val body = draft.trim()
        if (body.isBlank() || isSending) return

        if (typingAnnounced) {
            realtimeClient.sendTyping(false)
            typingAnnounced = false
        }

        val clientId = UUID.randomUUID().toString()
        scope.launch {
            isSending = true
            errorMessage = null
            when (
                val result = repository.sendMessage(
                    conversationId = conversationId,
                    body = body,
                    clientId = clientId,
                )
            ) {
                is ApiResult.Success -> {
                    if (messages.none { it.id == result.value.id }) {
                        messages = messages + result.value
                    }
                    draft = ""
                    isSending = false
                    onConversationRead()
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.lastIndex)
                    }
                }

                is ApiResult.Failure -> {
                    isSending = false
                    if (result.statusCode == 401) {
                        onSessionExpired()
                    } else {
                        errorMessage = result.message
                    }
                }
            }
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
            if (typingAnnounced) {
                realtimeClient.sendTyping(false)
                typingAnnounced = false
            }
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
                                scope.launch {
                                    listState.animateScrollToItem(messages.lastIndex)
                                }
                            }
                        }
                    }

                    is NovaRealtimeEvent.MessagesDelivered -> {
                        if (event.deliveredAt.isNotBlank() && event.messageIds.isNotEmpty()) {
                            messages = messages.map { message ->
                                if (message.isMine && message.id in event.messageIds) {
                                    message.copy(deliveredAt = event.deliveredAt)
                                } else {
                                    message
                                }
                            }
                        }
                    }

                    is NovaRealtimeEvent.ConversationRead -> {
                        if (event.readAt.isNotBlank() && event.messageIds.isNotEmpty()) {
                            messages = messages.map { message ->
                                if (message.isMine && message.id in event.messageIds) {
                                    message.copy(
                                        deliveredAt = message.deliveredAt ?: event.readAt,
                                        readAt = event.readAt,
                                    )
                                } else {
                                    message
                                }
                            }
                        }
                    }

                    is NovaRealtimeEvent.Typing -> {
                        isOtherTyping = event.isTyping
                    }
                }
            },
            onStatus = {
                realtimeStatus = it
                if (it != NovaRealtimeStatus.Live) {
                    isOtherTyping = false
                }
            },
            onSessionExpired = onSessionExpired,
            onPresence = { presence ->
                if (presence.username == username) {
                    otherPresence = presence
                    if (!presence.isOnline) isOtherTyping = false
                }
            },
        )

        onDispose {
            if (typingAnnounced) {
                realtimeClient.sendTyping(false)
            }
            NovaActiveConversation.leave(conversationId)
            realtimeClient.stop()
        }
    }

    Scaffold(
        containerColor = NovaBackground,
        topBar = {
            ConversationHeaderV4(
                username = username,
                displayName = displayName,
                avatarUrl = avatarUrl,
                presence = otherPresence,
                isTyping = isOtherTyping,
                realtimeStatus = realtimeStatus,
                onBack = onBack,
                onRefresh = { loadLatest(showSpinner = false) },
            )
        },
        bottomBar = {
            MessageComposerV4(
                draft = draft,
                isSending = isSending,
                errorMessage = errorMessage,
                username = username,
                onDraftChanged = { draft = it.take(2000) },
                onSend = ::send,
            )
        },
    ) { innerPadding ->
        when {
            isLoading && messages.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NovaBackground)
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = NovaAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading messages…", color = NovaMuted, fontSize = 13.sp)
                }
            }

            messages.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NovaBackground)
                        .padding(innerPadding)
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    NovaAvatar(
                        source = avatarUrl,
                        fallbackText = displayName.ifBlank { username },
                        size = 72.dp,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Start the conversation",
                        color = NovaInk,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = "Send a message to @${username}. Your conversation stays synced across Nova.",
                        color = NovaMuted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NovaBackground)
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (nextCursor != null) {
                        item {
                            Surface(
                                onClick = { if (!isLoadingEarlier) loadEarlier() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
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
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(end = 4.dp),
                                        )
                                    }
                                    Text(
                                        text = if (isLoadingEarlier) "Loading earlier…" else "Load earlier messages",
                                        color = NovaMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(messages, key = { _, item -> item.id }) { index, message ->
                        val previous = messages.getOrNull(index - 1)
                        val next = messages.getOrNull(index + 1)
                        val day = dateKey(message.createdAt)
                        val previousDay = previous?.let { dateKey(it.createdAt) }

                        if (day.isNotBlank() && day != previousDay) {
                            DaySeparator(day)
                        }

                        MessageBubbleV4(
                            message = message,
                            joinsPrevious = previous?.isMine == message.isMine && previousDay == day,
                            joinsNext = next?.isMine == message.isMine && dateKey(next.createdAt) == day,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ConversationHeaderV4(
    username: String,
    displayName: String,
    avatarUrl: String,
    presence: NovaConversationPresence?,
    isTyping: Boolean,
    realtimeStatus: NovaRealtimeStatus,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(
        color = NovaSurface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = NovaBackground,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Text(
                    text = "‹",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    color = NovaInk,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Box {
                NovaAvatar(
                    source = avatarUrl,
                    fallbackText = displayName.ifBlank { username },
                    size = 44.dp,
                )
                if (presence?.isOnline == true) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(13.dp),
                        shape = CircleShape,
                        color = OnlineGreen,
                        border = BorderStroke(2.dp, NovaSurface),
                    ) {}
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName.ifBlank { username },
                    color = NovaInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = headerStatus(
                        presence = presence,
                        isTyping = isTyping,
                        realtimeStatus = realtimeStatus,
                    ),
                    color = when {
                        isTyping -> NovaAccent
                        presence?.isOnline == true -> OnlineGreen
                        else -> NovaMuted
                    },
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }

            Surface(
                onClick = onRefresh,
                shape = CircleShape,
                color = NovaBackground,
            ) {
                Text(
                    text = "↻",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    color = NovaAccent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}


@Composable
private fun MessageComposerV4(
    draft: String,
    isSending: Boolean,
    errorMessage: String?,
    username: String,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = NovaSurface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = NovaMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message @$username", color = NovaMuted) },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                        focusedContainerColor = NovaBackground,
                        unfocusedContainerColor = NovaBackground,
                    ),
                )

                Surface(
                    onClick = { if (draft.isNotBlank() && !isSending) onSend() },
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = if (draft.isNotBlank() && !isSending) NovaAccent else NovaAccentSoft,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isSending) "…" else "↑",
                            color = if (draft.isNotBlank() && !isSending) NovaBackground else NovaMuted,
                            fontSize = if (isSending) 18.sp else 23.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun DaySeparator(day: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Text(
                text = readableDay(day),
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                color = NovaMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}


@Composable
private fun MessageBubbleV4(
    message: NovaMessage,
    joinsPrevious: Boolean,
    joinsNext: Boolean,
) {
    val topGap = if (joinsPrevious) 1.dp else 6.dp
    val own = message.isMine
    val shape = RoundedCornerShape(
        topStart = if (!own && joinsPrevious) 8.dp else 20.dp,
        topEnd = if (own && joinsPrevious) 8.dp else 20.dp,
        bottomStart = if (!own && joinsNext) 8.dp else if (!own) 5.dp else 20.dp,
        bottomEnd = if (own && joinsNext) 8.dp else if (own) 5.dp else 20.dp,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topGap),
        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = shape,
            color = if (own) NovaAccent else NovaSurface,
            border = if (own) null else BorderStroke(1.dp, NovaBorder),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text(
                    text = message.body,
                    color = if (own) NovaBackground else NovaInk,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                val deliveryLabel = when {
                    !own -> ""
                    message.readAt != null -> " · Read"
                    message.deliveredAt != null -> " · Delivered"
                    else -> " · Sent"
                }
                Text(
                    text = compactTimeV4(message.createdAt) + deliveryLabel,
                    color = if (own) NovaBackground.copy(alpha = 0.72f) else NovaMuted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}


private fun headerStatus(
    presence: NovaConversationPresence?,
    isTyping: Boolean,
    realtimeStatus: NovaRealtimeStatus,
): String {
    if (isTyping && realtimeStatus == NovaRealtimeStatus.Live) return "typing…"
    if (presence?.isOnline == true) return "Online"
    if (!presence?.lastSeenAt.isNullOrBlank()) return lastSeenLabel(presence?.lastSeenAt.orEmpty())

    return when (realtimeStatus) {
        NovaRealtimeStatus.Connecting -> "Connecting…"
        NovaRealtimeStatus.Reconnecting -> "Reconnecting…"
        NovaRealtimeStatus.Offline -> "Offline"
        NovaRealtimeStatus.Live -> "Active status unavailable"
    }
}


private fun lastSeenLabel(value: String): String {
    val instant = parseInstant(value) ?: return "Offline"
    val now = Instant.now()
    val duration = Duration.between(instant, now).coerceAtLeast(Duration.ZERO)
    val minutes = duration.toMinutes()

    return when {
        minutes < 1 -> "Last seen just now"
        minutes < 60 -> "Last seen ${minutes}m ago"
        minutes < 24 * 60 -> "Last seen ${duration.toHours()}h ago"
        else -> {
            val local = instant.atZone(ZoneId.systemDefault())
            "Last seen " + local.format(DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault()))
        }
    }
}


private fun parseInstant(value: String): Instant? {
    return runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull()
}


private fun dateKey(value: String): String {
    return value.takeIf { it.length >= 10 }?.substring(0, 10).orEmpty()
}


private fun readableDay(value: String): String {
    val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return value
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }
}


private fun compactTimeV4(value: String): String {
    val instant = parseInstant(value)
    if (instant != null) {
        return instant
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    }
    if (value.length >= 16 && value.getOrNull(10) == 'T') {
        return value.substring(11, 16)
    }
    return ""
}
