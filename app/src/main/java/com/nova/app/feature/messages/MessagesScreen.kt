package com.nova.app.feature.messages

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.messaging.NovaConversation
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun MessagesScreen(
    onConversationClick: (NovaConversation) -> Unit,
    onHomeClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUnreadCountChanged: (Int) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaMessagingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var conversations by remember { mutableStateOf<List<NovaConversation>>(emptyList()) }
    var unreadCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var requestVersion by remember { mutableStateOf(0) }
    val inboxRefreshVersion = NovaMessagesSignal.inboxRefreshVersion
    val initialInboxRefreshVersion = remember { inboxRefreshVersion }

    fun loadInbox(search: String = query, showSpinner: Boolean = true) {
        requestVersion += 1
        val version = requestVersion
        scope.launch {
            if (showSpinner) isLoading = true
            errorMessage = null
            when (val result = repository.conversations(search)) {
                is ApiResult.Success -> {
                    if (version == requestVersion) {
                        conversations = result.value.conversations
                        unreadCount = result.value.unreadCount
                        onUnreadCountChanged(result.value.unreadCount)
                        isLoading = false
                    }
                }

                is ApiResult.Failure -> {
                    if (version == requestVersion) {
                        isLoading = false
                        if (result.statusCode == 401) {
                            onSessionExpired()
                        } else {
                            errorMessage = result.message
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(query) {
        delay(260)
        loadInbox(query, showSpinner = conversations.isEmpty())
    }

    LaunchedEffect(inboxRefreshVersion) {
        if (inboxRefreshVersion != initialInboxRefreshVersion) {
            loadInbox(query, showSpinner = false)
        }
    }

    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Messages,
                messagesUnreadCount = unreadCount,
                onHomeClick = onHomeClick,
                onPeopleClick = onPeopleClick,
                onMessagesClick = {},
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaBackground)
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Messages",
                        color = NovaInk,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (unreadCount > 0) {
                            "$unreadCount unread ${if (unreadCount == 1) "message" else "messages"}"
                        } else {
                            "Your conversations on Nova."
                        },
                        color = NovaMuted,
                        fontSize = 13.sp,
                    )
                }

                Surface(
                    onClick = { loadInbox(showSpinner = conversations.isEmpty()) },
                    shape = RoundedCornerShape(16.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Text(
                        text = "Refresh",
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        color = NovaInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            NovaTextField(
                value = query,
                onValueChange = { query = it.take(40) },
                label = "Search messages",
                placeholder = "Name or username",
            )

            Spacer(modifier = Modifier.height(14.dp))

            when {
                isLoading && conversations.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = NovaAccent)
                    }
                }

                errorMessage != null && conversations.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = NovaSurface,
                            border = BorderStroke(1.dp, NovaBorder),
                        ) {
                            Column(modifier = Modifier.padding(22.dp)) {
                                Text(
                                    text = "Couldn't load messages",
                                    color = NovaInk,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(7.dp))
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = NovaMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                NovaSecondaryButton(
                                    text = "Try again",
                                    onClick = { loadInbox() },
                                )
                            }
                        }
                    }
                }

                conversations.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            color = NovaSurface,
                            border = BorderStroke(1.dp, NovaBorder),
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "✉",
                                    color = NovaAccent,
                                    fontSize = 34.sp,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (query.isBlank()) "No conversations yet" else "No matching conversations",
                                    color = NovaInk,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(7.dp))
                                Text(
                                    text = if (query.isBlank()) {
                                        "Open someone's profile and tap Message to start talking."
                                    } else {
                                        "Try another name or username."
                                    },
                                    color = NovaMuted,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(conversations, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                onClick = { onConversationClick(conversation) },
                            )
                        }
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }
}


@Composable
private fun ConversationRow(
    conversation: NovaConversation,
    onClick: () -> Unit,
) {
    val other = conversation.otherUser
    val last = conversation.lastMessage

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (conversation.unreadCount > 0) NovaAccentSoft else NovaSurface,
        border = BorderStroke(
            1.dp,
            if (conversation.unreadCount > 0) NovaAccent.copy(alpha = 0.45f) else NovaBorder,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            NovaAvatar(
                source = other.avatarUrl,
                fallbackText = other.name.ifBlank { other.username },
                size = 56.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = other.name.ifBlank { other.username },
                        color = NovaInk,
                        fontSize = 16.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NovaAccent,
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                color = NovaBackground,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Text(
                    text = "@${other.username}",
                    color = NovaMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        last == null -> "Start the conversation"
                        last.isMine -> "You: ${last.body}"
                        else -> last.body
                    },
                    color = if (conversation.unreadCount > 0) NovaInk else NovaMuted,
                    fontSize = 13.sp,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
