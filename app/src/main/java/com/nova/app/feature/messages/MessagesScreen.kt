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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nova.app.app.appContainer
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.feature.messages.inbox.InboxViewModel
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


@Composable
fun MessagesScreen(
    onConversationClick: (NovaConversation) -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUnreadCountChanged: (Int) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val inboxViewModel: InboxViewModel = viewModel(
        factory = InboxViewModel.factory(context.appContainer.inboxRepository),
    )
    val state = inboxViewModel.state
    val query = state.query
    val conversations = state.conversations
    val unreadCount = state.unreadCount
    val nextCursor = state.nextCursor
    val isLoading = state.isLoading
    val isLoadingMore = state.isLoadingMore
    val errorMessage = state.errorMessage
    val inboxRefreshVersion = NovaMessagesSignal.inboxRefreshVersion
    val initialInboxRefreshVersion = remember { inboxRefreshVersion }

    LaunchedEffect(inboxRefreshVersion) {
        if (inboxRefreshVersion != initialInboxRefreshVersion) {
            inboxViewModel.refresh()
        }
    }

    LaunchedEffect(state.unreadUpdateVersion) {
        if (state.unreadUpdateVersion > 0) {
            onUnreadCountChanged(state.unreadCount)
        }
    }

    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) {
            onSessionExpired()
        }
    }

    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Inbox,
                messagesUnreadCount = unreadCount,
                onHomeClick = onHomeClick,
                onOrbitClick = onOrbitClick,
                onCreateClick = onCreateClick,
                onInboxClick = {},
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
                    text = "Inbox",
                    color = NovaInk,
                    style = com.nova.app.ui.theme.NovaType.pageTitle,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        unreadCount > 0 -> "$unreadCount unread ${if (unreadCount == 1) "conversation" else "conversations"}"
                        conversations.isNotEmpty() -> "Pick up where you left off."
                        else -> "All your conversations, together."
                    },
                    color = NovaMuted,
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = query,
                onValueChange = inboxViewModel::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search conversations or groups", color = NovaMuted) },
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
                            onClick = { inboxViewModel.onQueryChanged("") },
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
                                    onClick = inboxViewModel::retry,
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
                                    "Start a private message or create a group when you want to connect."
                                } else {
                                    "Try another name, username, or group name."
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
                            text = "${conversations.size} loaded",
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
                        if (errorMessage != null) {
                            item {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = NovaMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                )
                            }
                        }
                        if (nextCursor != null) {
                            item {
                                NovaSecondaryButton(
                                    text = if (isLoadingMore) "Loading more…" else "Load more conversations",
                                    onClick = {
                                        if (!isLoadingMore) {
                                            inboxViewModel.loadMore()
                                        }
                                    },
                                )
                            }
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
    val last = conversation.lastMessage
    val hasUnread = conversation.unreadCount > 0
    val preview = when {
        last == null -> if (conversation.isGroup) "Start the group conversation" else "Start the conversation"
        last.isMine -> "You: ${last.body}"
        conversation.isGroup -> "${last.sender.name.ifBlank { last.sender.username }}: ${last.body}"
        else -> last.body
    }.ifBlank {
        when {
            last?.imageUrl?.isNotBlank() == true -> when {
                last.isMine -> "You sent a photo"
                conversation.isGroup -> "${last.sender.name.ifBlank { last.sender.username }} sent a photo"
                else -> "Sent a photo"
            }
            last?.audioUrl?.isNotBlank() == true -> when {
                last.isMine -> "You sent a voice note"
                conversation.isGroup -> "${last.sender.name.ifBlank { last.sender.username }} sent a voice note"
                else -> "Sent a voice note"
            }
            else -> "Open conversation"
        }
    }
    val avatarSource = if (conversation.isGroup) {
        conversation.membersPreview.firstOrNull()?.avatarUrl.orEmpty()
    } else {
        conversation.otherUser.avatarUrl
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
                    source = avatarSource,
                    fallbackText = conversation.displayName,
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
                        text = conversation.displayName,
                        color = NovaInk,
                        fontSize = 15.sp,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = conversation.displaySubtitle,
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
