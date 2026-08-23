package com.nova.app.feature.rooms

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import com.nova.app.app.appContainer
import com.nova.app.feature.messages.NewGroupDialog
import com.nova.app.feature.rooms.domain.model.RoomSummary
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBackButton
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaInlineRetry
import com.nova.app.ui.components.NovaLoadingState
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType


@Composable
fun RoomsScreen(
    onBack: () -> Unit,
    onRoomClick: (Long) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.roomRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { RoomsStateOwner(repository, scope) }
    val state = owner.state
    var showCreate by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)
    LaunchedEffect(owner) {
        owner.load(showSpinner = true)
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NovaBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = NovaSpacing.xl, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NovaBackButton(onClick = onBack)
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = "Rooms",
                        color = NovaInk,
                        style = NovaType.screenTitle,
                    )
                    Text(
                        text = "Your shared places",
                        color = NovaMuted,
                        style = NovaType.micro,
                    )
                }
                Surface(
                    onClick = { showCreate = true },
                    shape = MaterialTheme.shapes.medium,
                    color = NovaAccent,
                ) {
                    Text(
                        text = "+ New",
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        color = NovaBackground,
                        style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = NovaSpacing.sm))

            when {
                state.loading && state.rooms.isEmpty() -> {
                    NovaLoadingState(
                        message = "Loading your Rooms…",
                        modifier = Modifier.weight(1f),
                    )
                }

                state.rooms.isEmpty() -> {
                    NovaEmptyState(
                        title = "Your first Room starts here",
                        message = "Choose your people once. Chat, plans, media and memories can live in the same place.",
                        modifier = Modifier.weight(1f),
                        actionLabel = "Create a Room",
                        onAction = { showCreate = true },
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NovaBackground),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (!state.error.isNullOrBlank()) {
                            item {
                                NovaInlineRetry(
                                    message = state.error ?: "Couldn't refresh Rooms.",
                                    onRetry = { owner.load(showSpinner = false) },
                                )
                            }
                        }

                        items(state.rooms, key = { it.conversation.id }) { room ->
                            RoomListCard(room = room, onClick = { onRoomClick(room.conversation.id) })
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        NewGroupDialog(
            onDismiss = { showCreate = false },
            onConversationReady = { conversation ->
                showCreate = false
                owner.load(showSpinner = false)
                onRoomClick(conversation.id)
            },
            onSessionExpired = {
                showCreate = false
                onSessionExpired()
            },
        )
    }
}


@Composable
private fun RoomListCard(
    room: RoomSummary,
    onClick: () -> Unit,
) {
    val conversation = room.conversation
    NovaCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaAvatar(
                source = conversation.avatarUrl,
                fallbackText = conversation.title,
                size = 54.dp,
            )
            Spacer(modifier = Modifier.width(NovaSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    color = NovaInk,
                    style = NovaType.bodyCompact.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = room.description.ifBlank {
                        "${conversation.membersCount} people · shared place"
                    },
                    color = NovaMuted,
                    style = NovaType.micro,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (conversation.unreadCount > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = NovaAccent,
                ) {
                    Text(
                        text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = NovaBackground,
                        style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                    )
                }
            } else {
                Text("›", color = NovaMuted, style = NovaType.sectionTitle)
            }
        }
    }
}
