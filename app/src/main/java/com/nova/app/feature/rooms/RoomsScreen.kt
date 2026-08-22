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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.nova.app.app.appContainer
import com.nova.app.feature.messages.NewGroupDialog
import com.nova.app.feature.rooms.domain.model.RoomSummary
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


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
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Text(
                        text = "‹",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        color = NovaInk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = "Rooms",
                        color = NovaInk,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Your shared places",
                        color = NovaMuted,
                        fontSize = 10.sp,
                    )
                }
                Surface(
                    onClick = { showCreate = true },
                    shape = RoundedCornerShape(16.dp),
                    color = NovaAccent,
                ) {
                    Text(
                        text = "+ New",
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        color = NovaBackground,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = 6.dp))

            when {
                state.loading && state.rooms.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = NovaAccent)
                    }
                }

                state.rooms.isEmpty() -> {
                    EmptyRoomsScreenCard(onCreate = { showCreate = true })
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
                                Surface(
                                    onClick = { owner.load(showSpinner = false) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = NovaAccentSoft,
                                ) {
                                    Text(
                                        text = "${state.error} · Tap to retry",
                                        modifier = Modifier.padding(12.dp),
                                        color = NovaMuted,
                                        fontSize = 10.sp,
                                    )
                                }
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
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
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
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    color = NovaInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = room.description.ifBlank {
                        "${conversation.membersCount} people · shared place"
                    },
                    color = NovaMuted,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (conversation.unreadCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NovaAccent,
                ) {
                    Text(
                        text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = NovaBackground,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Text("›", color = NovaMuted, fontSize = 22.sp)
            }
        }
    }
}


@Composable
private fun EmptyRoomsScreenCard(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(24.dp),
            color = NovaAccentSoft,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("⌂", color = NovaAccent, fontSize = 30.sp)
            }
        }
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Text(
            text = "Your first Room starts here",
            color = NovaInk,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Choose your people once. Chat, plans, media and memories can live in the same place.",
            modifier = Modifier.padding(horizontal = 30.dp, vertical = 8.dp),
            color = NovaMuted,
            fontSize = 11.sp,
        )
        Surface(
            onClick = onCreate,
            shape = RoundedCornerShape(17.dp),
            color = NovaAccent,
        ) {
            Text(
                text = "Create a Room",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = NovaBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
