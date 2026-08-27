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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaErrorState
import com.nova.app.ui.components.NovaIconButton
import com.nova.app.ui.components.NovaInlineRetry
import com.nova.app.ui.components.NovaLoadingState
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
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
                NovaIconButton(
                    asset = NovaIconAsset.Create,
                    contentDescription = "Create a Room",
                    onClick = { showCreate = true },
                    size = 48.dp,
                    containerColor = NovaAccent,
                    contentColor = NovaBackground,
                    borderColor = NovaAccent,
                )
            }

            Spacer(modifier = Modifier.padding(top = NovaSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = NovaSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
            ) {
                listOf("mine" to "My Rooms", "discover" to "Discover", "following" to "Following").forEach { (key, label) ->
                    val selected = state.selectedList == key
                    Surface(
                        onClick = { owner.selectList(key) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected) NovaAccent else NovaAccentSoft,
                        border = BorderStroke(1.dp, if (selected) NovaAccent else NovaBorder),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp),
                            color = if (selected) NovaBackground else NovaAccent,
                            style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                        )
                    }
                }
            }

            when {
                state.loading && state.rooms.isEmpty() -> {
                    NovaLoadingState(
                        message = "Loading your Rooms…",
                        modifier = Modifier.weight(1f),
                    )
                }

                !state.error.isNullOrBlank() && state.rooms.isEmpty() -> {
                    NovaErrorState(
                        title = "Rooms couldn't load",
                        message = state.error ?: "Check your connection and try again.",
                        onRetry = { owner.load(showSpinner = true) },
                        modifier = Modifier.weight(1f),
                    )
                }

                state.rooms.isEmpty() -> {
                    NovaEmptyState(
                        title = when (state.selectedList) {
                            "discover" -> "No public Rooms to discover yet"
                            "following" -> "You aren't following a Room yet"
                            else -> "Your first Room starts here"
                        },
                        message = when (state.selectedList) {
                            "discover" -> "Public Rooms created by people outside your blocked network will appear here."
                            "following" -> "Follow a public Room to keep it close without joining."
                            else -> "Choose your people once. Chat, plans, media and memories can live in the same place."
                        },
                        modifier = Modifier.weight(1f),
                        actionLabel = if (state.selectedList == "mine") "Create a Room" else null,
                        onAction = if (state.selectedList == "mine") ({ showCreate = true }) else null,
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
                            RoomListRow(
                                room = room,
                                busy = state.busyRoomId == room.conversation.id,
                                onClick = {
                                    if (room.isMember) onRoomClick(room.conversation.id)
                                    else owner.join(room, onRoomClick)
                                },
                                onFollow = { owner.toggleFollow(room) },
                            )
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
private fun RoomListRow(
    room: RoomSummary,
    busy: Boolean,
    onClick: () -> Unit,
    onFollow: () -> Unit,
) {
    val conversation = room.conversation
    Surface(
        onClick = onClick,
        enabled = room.isMember,
        modifier = Modifier.fillMaxWidth(),
        color = NovaBackground,
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
                        "A shared place for ${conversation.membersCount} people"
                    },
                    color = NovaMuted,
                    style = NovaType.micro,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoomTruthLabel(if (room.isPublic) "Public" else "Private")
                    RoomTruthLabel("${conversation.membersCount} members")
                    if (room.isMember && conversation.currentUserRole.isNotBlank()) {
                        RoomTruthLabel(conversation.currentUserRole.replaceFirstChar(Char::titlecase))
                    }
                }
                if (room.topics.isNotEmpty()) {
                    Text(
                        text = room.topics.take(3).joinToString("  ·  ") { "#$it" },
                        modifier = Modifier.padding(top = 4.dp),
                        color = NovaAccent,
                        style = NovaType.micro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
                Column(horizontalAlignment = Alignment.End) {
                    if (room.isPublic && !room.isMember) {
                        Surface(
                            onClick = onClick,
                            enabled = !busy,
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = MaterialTheme.shapes.small,
                            color = NovaAccent,
                        ) {
                            Text(
                                if (busy) "Working…" else "Join",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                color = NovaBackground,
                                style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                        Surface(
                            onClick = onFollow,
                            enabled = !busy,
                            modifier = Modifier.heightIn(min = 48.dp),
                            color = NovaBackground,
                        ) {
                            Text(
                                if (room.isFollowing) "Following" else "Follow",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
                                color = NovaAccent,
                                style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                    } else {
                        NovaIcon(
                            asset = NovaIconAsset.Room,
                            contentDescription = null,
                            tint = NovaMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun RoomTruthLabel(label: String) {
    Text(
        text = label,
        color = NovaMuted,
        style = NovaType.micro.copy(fontWeight = FontWeight.Medium),
        maxLines = 1,
    )
}
