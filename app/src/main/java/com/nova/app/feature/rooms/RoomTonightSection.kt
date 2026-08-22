package com.nova.app.feature.rooms

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.app.appContainer
import com.nova.app.feature.rooms.domain.model.RoomTonightRow
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import java.util.TimeZone
import kotlinx.coroutines.delay


private val RoomTonightSurface = Color(0xFF151A27)
private val RoomTonightBorder = Color.White.copy(alpha = 0.08f)
private val RoomTonightInk = Color(0xFFF7F8FC)
private val RoomTonightMuted = Color(0xFFB1B7C5)


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
        verticalArrangement = Arrangement.spacedBy(9.dp),
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
                color = RoomTonightInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (state.loading && state.snapshot == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    color = NovaAccent,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = if (state.error == null) {
                        "${state.snapshot?.momentsCount ?: 0} shared"
                    } else {
                        "tap to retry"
                    },
                    color = if (state.error == null) RoomTonightMuted else NovaAccent,
                    fontSize = 8.sp,
                    modifier = Modifier.then(
                        if (state.error != null) {
                            Modifier
                        } else {
                            Modifier
                        }
                    ),
                )
            }
        }

        when {
            state.error != null && state.snapshot == null -> {
                Surface(
                    onClick = { owner.load(roomUtcOffsetMinutes(), showSpinner = true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = RoomTonightSurface,
                    border = BorderStroke(1.dp, RoomTonightBorder),
                ) {
                    Text(
                        text = "Couldn't refresh live Rooms · tap to retry",
                        modifier = Modifier.padding(11.dp),
                        color = RoomTonightMuted,
                        fontSize = 9.sp,
                    )
                }
            }

            state.snapshot?.rooms.isNullOrEmpty() -> {
                Text(
                    text = "Your Rooms are quiet right now. Add something to a Room and it will wake up here.",
                    color = RoomTonightMuted,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                )
            }

            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val room = row.room.conversation
    Surface(
        onClick = onClick,
        modifier = Modifier.width(196.dp),
        shape = RoundedCornerShape(18.dp),
        color = RoomTonightSurface,
        border = BorderStroke(1.dp, RoomTonightBorder),
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaAvatar(
                source = room.avatarUrl,
                fallbackText = room.title,
                size = 38.dp,
            )
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.title,
                    color = RoomTonightInk,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${row.momentsCount} ${if (row.momentsCount == 1) "thing" else "things"} tonight",
                    color = RoomTonightMuted,
                    fontSize = 8.sp,
                )
                val latest = row.latestItem.title.ifBlank { row.latestItem.body }
                if (latest.isNotBlank()) {
                    Text(
                        text = latest,
                        color = RoomTonightMuted,
                        fontSize = 8.sp,
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
