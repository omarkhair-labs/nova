package com.nova.app.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.messaging.NovaActiveConversation
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
import java.util.UUID


@Composable
fun ConversationScreen(
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
            if (showSpinner) isLoading = true
            if (showSpinner) errorMessage = null

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
            Surface(
                color = NovaSurface,
                shadowElevation = 3.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        color = NovaBackground,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Text(
                            text = "‹",
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 5.dp),
                            color = NovaInk,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    NovaAvatar(
                        source = avatarUrl,
                        fallbackText = displayName.ifBlank { username },
                        size = 44.dp,
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName.ifBlank { username },
                            color = NovaInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = when {
                                realtimeStatus == NovaRealtimeStatus.Live && isOtherTyping -> "@$username · typing…"
                                realtimeStatus == NovaRealtimeStatus.Live -> "@$username · Live"
                                realtimeStatus == NovaRealtimeStatus.Connecting -> "@$username · Connecting…"
                                realtimeStatus == NovaRealtimeStatus.Reconnecting -> "@$username · Reconnecting…"
                                else -> "@$username · Offline"
                            },
                            color = if (
                                realtimeStatus == NovaRealtimeStatus.Live
                            ) NovaAccent else NovaMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }

                    Surface(
                        onClick = { loadLatest(showSpinner = false) },
                        shape = RoundedCornerShape(14.dp),
                        color = NovaBackground,
                    ) {
                        Text(
                            text = "↻",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = NovaAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = NovaSurface,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = NovaMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(2000) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message @$username", color = NovaMuted) },
                            minLines = 1,
                            maxLines = 4,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NovaAccent,
                                unfocusedBorderColor = NovaBorder,
                                cursorColor = NovaAccent,
                                focusedContainerColor = NovaBackground,
                                unfocusedContainerColor = NovaBackground,
                            ),
                        )

                        Surface(
                            onClick = { if (draft.isNotBlank() && !isSending) send() },
                            shape = RoundedCornerShape(18.dp),
                            color = if (draft.isNotBlank() && !isSending) NovaAccent else NovaAccentSoft,
                        ) {
                            Text(
                                text = if (isSending) "…" else "Send",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                                color = if (draft.isNotBlank() && !isSending) NovaBackground else NovaMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
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
                    Text(
                        text = "Say hello 👋",
                        color = NovaInk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This is the beginning of your conversation with @${username}.",
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
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 14.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    if (nextCursor != null) {
                        item {
                            Surface(
                                onClick = { if (!isLoadingEarlier) loadEarlier() },
                                modifier = Modifier.fillMaxWidth(),
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
                                            modifier = Modifier.padding(end = 8.dp),
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

                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
            }
        }
    }
}


@Composable
private fun MessageBubble(message: NovaMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isMine) 20.dp else 6.dp,
                bottomEnd = if (message.isMine) 6.dp else 20.dp,
            ),
            color = if (message.isMine) NovaAccent else NovaSurface,
            border = if (message.isMine) null else BorderStroke(1.dp, NovaBorder),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message.body,
                    color = if (message.isMine) NovaBackground else NovaInk,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                val deliveryLabel = when {
                    !message.isMine -> ""
                    message.readAt != null -> " · Read"
                    message.deliveredAt != null -> " · Delivered"
                    else -> " · Sent"
                }
                Text(
                    text = compactTime(message.createdAt) + deliveryLabel,
                    color = if (message.isMine) NovaBackground.copy(alpha = 0.75f) else NovaMuted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}


private fun compactTime(value: String): String {
    if (value.length >= 16 && value.getOrNull(10) == 'T') {
        return value.substring(11, 16)
    }
    return ""
}
