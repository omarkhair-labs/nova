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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
            ) {
                Text(
                    text = "Messages",
                    color = NovaInk,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        unreadCount > 0 -> "$unreadCount unread ${if (unreadCount == 1) "conversation" else "conversations"}"
                        conversations.isNotEmpty() -> "Pick up where you left off."
                        else -> "Private conversations on Nova."
                    },
                    color = NovaMuted,
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search conversations", color = NovaMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = NovaMuted,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = if (query.isNotBlank()) {
                    {
                        Surface(
                            onClick = { query = "" },
                            shape = CircleShape,
                            color = NovaAccentSoft,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = NovaAccent,
                                modifier = Modifier.padding(7.dp).size(16.dp),
                            )
                        }
                    }
                } else null,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaAccent,
                    unfocusedBorderColor = NovaBorder,
                    cursorColor = NovaAccent,
                    focusedContainerColor = NovaSurface,
                    unfocusedContainerColor = NovaSurface,
                ),
            )

            Spacer(modifier = Modifier.height(18.dp))

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
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NovaAccentSoft,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = NovaAccent,
                                    modifier = Modifier.padding(16.dp).size(24.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(15.dp))
                            Text(
                                text = if (query.isBlank()) "No conversations yet" else "No matches",
                                color = NovaInk,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(7.dp))
                            Text(
                                text = if (query.isBlank()) {
                                    "Open someone's profile and start a conversation when you want to connect."
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

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (query.isBlank()) "Recent" else "Results",
                            color = NovaInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${conversations.size} ${if (conversations.size == 1) "chat" else "chats"}",
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
    val hasUnread = conversation.unreadCount > 0
    val preview = when {
        last == null -> "Start the conversation"
        last.isMine -> "You: ${last.body}"
        else -> last.body
    }.ifBlank {
        when {
            last?.imageUrl?.isNotBlank() == true -> if (last.isMine) "You sent a photo" else "Sent a photo"
            last?.audioUrl?.isNotBlank() == true -> if (last.isMine) "You sent a voice note" else "Sent a voice note"
            else -> "Open conversation"
        }
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = if (hasUnread) NovaAccentSoft else NovaSurface,
        border = BorderStroke(
            1.dp,
            if (hasUnread) NovaAccent.copy(alpha = 0.34f) else NovaBorder,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box {
                NovaAvatar(
                    source = other.avatarUrl,
                    fallbackText = other.name.ifBlank { other.username },
                    size = 54.dp,
                )
                if (hasUnread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                            .background(NovaAccent, CircleShape),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = other.name.ifBlank { other.username },
                        color = NovaInk,
                        fontSize = 15.sp,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "@${other.username}",
                        color = NovaMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasUnread) {
                        Surface(
                            shape = RoundedCornerShape(11.dp),
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
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = preview,
                    color = if (hasUnread) NovaInk else NovaMuted,
                    fontSize = 13.sp,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
