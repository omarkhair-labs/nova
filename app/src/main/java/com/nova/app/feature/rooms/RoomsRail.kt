package com.nova.app.feature.rooms

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.app.appContainer
import com.nova.app.feature.rooms.domain.model.RoomSummary
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMotion
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType


@Composable
fun RoomsRail(
    onPersonClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.roomRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { RoomsStateOwner(repository, scope) }
    val state = owner.state

    var showAll by remember { mutableStateOf(false) }
    var selectedRoomId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(owner) {
        owner.load(showSpinner = true)
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    Column(
        modifier = Modifier.animateContentSize(
            animationSpec = tween(durationMillis = NovaMotion.standard),
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Rooms",
                    color = NovaInk,
                    style = NovaType.title.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Places you share with your people.",
                    color = NovaMuted,
                    style = NovaType.micro,
                )
            }
            Surface(
                onClick = { showAll = true },
                shape = MaterialTheme.shapes.small,
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Text(
                    text = "All rooms",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    color = NovaAccent,
                    style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        when {
            state.loading && state.rooms.isEmpty() -> {
                Row(
                    modifier = Modifier.fillMaxWidth().height(82.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = NovaAccent,
                        strokeWidth = 2.dp,
                    )
                }
            }

            state.rooms.isEmpty() -> EmptyRoomsCard(onClick = { showAll = true })

            else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.rooms.take(6), key = { it.conversation.id }) { room ->
                    RoomRailCard(
                        room = room,
                        onClick = { selectedRoomId = room.conversation.id },
                    )
                }
            }
        }

        if (!state.error.isNullOrBlank()) {
            Text(
                text = "Couldn't refresh Rooms · tap All rooms to retry",
                color = NovaMuted,
                style = NovaType.micro,
            )
        }
    }

    if (showAll) {
        FullScreenRoomDialog(onDismiss = { showAll = false }) {
            RoomsScreen(
                onBack = { showAll = false },
                onRoomClick = { roomId ->
                    showAll = false
                    selectedRoomId = roomId
                },
                onSessionExpired = {
                    showAll = false
                    onSessionExpired()
                },
            )
        }
    }

    selectedRoomId?.let { roomId ->
        FullScreenRoomDialog(onDismiss = { selectedRoomId = null }) {
            RoomScreen(
                conversationId = roomId,
                onBack = { selectedRoomId = null },
                onPersonClick = { username ->
                    selectedRoomId = null
                    onPersonClick(username)
                },
                onSessionExpired = {
                    selectedRoomId = null
                    onSessionExpired()
                },
            )
        }
    }
}


@Composable
private fun RoomRailCard(
    room: RoomSummary,
    onClick: () -> Unit,
) {
    val conversation = room.conversation
    NovaCard(
        onClick = onClick,
        modifier = Modifier.width(208.dp),
        borderColor = if (conversation.unreadCount > 0) {
            NovaAccent.copy(alpha = 0.35f)
        } else {
            NovaBorder
        },
    ) {
        Row(
            modifier = Modifier.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaAvatar(
                source = conversation.avatarUrl,
                fallbackText = conversation.title,
                size = 46.dp,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    color = NovaInk,
                    style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = room.description.ifBlank {
                        "${conversation.membersCount} people"
                    },
                    color = NovaMuted,
                    style = NovaType.micro,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (conversation.unreadCount > 0) {
                Spacer(modifier = Modifier.width(7.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = NovaAccent,
                ) {
                    Text(
                        text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = NovaType.badge,
                    )
                }
            }
        }
    }
}


@Composable
private fun EmptyRoomsCard(onClick: () -> Unit) {
    NovaCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        containerColor = NovaAccentSoft,
        borderColor = NovaAccent.copy(alpha = 0.18f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌂", color = NovaAccent, style = NovaType.sectionTitle)
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Make a place for your people",
                    color = NovaInk,
                    style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "A Room starts from a Nova group and grows into shared memories, plans and media.",
                    color = NovaMuted,
                    style = NovaType.micro,
                )
            }
        }
    }
}


@Composable
private fun FullScreenRoomDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        content()
    }
}
