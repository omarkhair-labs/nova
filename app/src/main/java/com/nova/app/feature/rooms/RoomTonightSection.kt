package com.nova.app.feature.rooms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.app.appContainer
import com.nova.app.feature.rooms.domain.model.RoomTonightRow
import com.nova.app.feature.tonight.TonightTheme
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaPresenceIndicator
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType
import java.util.TimeZone
import kotlinx.coroutines.delay


@Composable
fun RoomTonightSection(
    onPersonClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.roomRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { RoomTonightStateOwner(repository, scope) }
    val state = owner.state
    val palette = TonightTheme.live
    var selectedRoomId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(owner) {
        while (true) {
            owner.loadNow(
                utcOffsetMinutes = roomUtcOffsetMinutes(),
                showSpinner = owner.state.snapshot == null,
            )
            delay(90_000L)
        }
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    state.snapshot?.roomsCount == 1 -> "1 Room is alive"
                    (state.snapshot?.roomsCount ?: 0) > 1 -> "${state.snapshot?.roomsCount} Rooms are alive"
                    else -> "Rooms tonight"
                },
                color = palette.ink,
                style = NovaType.label.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (state.loading && state.snapshot == null) {
                NovaPresenceIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text(
                    text = if (state.error == null) {
                        "${state.snapshot?.momentsCount ?: 0} shared"
                    } else {
                        "tap to retry"
                    },
                    color = if (state.error == null) palette.muted else NovaAccent,
                    style = NovaType.micro,
                    modifier = if (state.error != null) {
                        Modifier.clickable {
                            owner.load(roomUtcOffsetMinutes(), showSpinner = state.snapshot == null)
                        }
                    } else {
                        Modifier
                    },
                )
            }
        }

        when {
            state.error != null && state.snapshot == null -> {
                Surface(
                    onClick = { owner.load(roomUtcOffsetMinutes(), showSpinner = true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.divider),
                ) {
                    Text(
                        text = "Couldn't refresh live Rooms · tap to retry",
                        modifier = Modifier.padding(NovaSpacing.md),
                        color = palette.muted,
                        style = NovaType.micro,
                    )
                }
            }

            state.snapshot?.rooms.isNullOrEmpty() -> {
                Text(
                    text = "Your Rooms are quiet right now. Add something to a Room and it will wake up here.",
                    color = palette.muted,
                    style = NovaType.micro,
                )
            }

            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
                    items(
                        state.snapshot?.rooms.orEmpty(),
                        key = { it.room.conversation.id },
                    ) { row ->
                        RoomTonightCard(
                            row = row,
                            onClick = { selectedRoomId = row.room.conversation.id },
                        )
                    }
                }
            }
        }
    }

    selectedRoomId?.let { roomId ->
        Dialog(
            onDismissRequest = { selectedRoomId = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
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
private fun RoomTonightCard(
    row: RoomTonightRow,
    onClick: () -> Unit,
) {
    val palette = TonightTheme.live
    val room = row.room.conversation
    Surface(
        onClick = onClick,
        modifier = Modifier.width(196.dp),
        shape = MaterialTheme.shapes.large,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.divider),
    ) {
        Row(
            modifier = Modifier.padding(NovaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaAvatar(
                source = room.avatarUrl,
                fallbackText = room.title,
                size = 38.dp,
            )
            Spacer(modifier = Modifier.width(NovaSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.title,
                    color = palette.ink,
                    style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${row.momentsCount} ${if (row.momentsCount == 1) "thing" else "things"} tonight",
                    color = palette.muted,
                    style = NovaType.badge,
                )
                val latest = row.latestItem.title.ifBlank { row.latestItem.body }
                if (latest.isNotBlank()) {
                    Text(
                        text = latest,
                        color = palette.muted,
                        style = NovaType.badge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}


private fun roomUtcOffsetMinutes(): Int =
    TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
