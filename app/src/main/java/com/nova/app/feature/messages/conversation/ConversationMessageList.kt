package com.nova.app.feature.messages.conversation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
internal fun ConversationMessageList(
    state: ConversationUiState,
    username: String,
    isGroupConversation: Boolean,
    listState: LazyListState,
    nearBottom: Boolean,
    modifier: Modifier = Modifier,
    onLoadEarlier: () -> Unit,
    onToggleActions: (Long) -> Unit,
    onReply: (NovaMessage) -> Unit,
    onEdit: (NovaMessage) -> Unit,
    onDelete: (NovaMessage) -> Unit,
    onReact: (NovaMessage, String) -> Unit,
    onOpenPhoto: (String) -> Unit,
    onOpenSharedPost: (Long) -> Unit,
    onOpenSharedProfile: (String) -> Unit,
    onOpenSharedReel: (String, Long) -> Unit,
    onRetryPending: (PendingMessage) -> Unit,
    onScrollLatest: () -> Unit,
) {
    when {
        state.isLoading && state.messages.isEmpty() && state.pendingMessages.isEmpty() -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = NovaAccent)
        }

        state.messages.isEmpty() && state.pendingMessages.isEmpty() -> Column(
            modifier = modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Start the conversation", color = NovaInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(8.dp))
            Text(
                if (isGroupConversation) {
                    "Send a message, photo, or voice note to the group."
                } else {
                    "Send a message, photo, or voice note to @$username."
                },
                color = NovaMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }

        else -> Box(modifier = modifier) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().background(NovaBackground),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (state.nextCursor != null) {
                    item(key = "load-earlier") {
                        LoadEarlierRow(
                            loading = state.isLoadingEarlier,
                            onClick = onLoadEarlier,
                        )
                    }
                }

                itemsIndexed(state.messages, key = { _, message -> "server-${message.id}" }) { index, message ->
                    val row = messageRowContext(
                        messages = state.messages,
                        index = index,
                        isGroupConversation = isGroupConversation,
                        unreadAnchorMessageId = state.unreadAnchorMessageId,
                        unreadCountAtOpen = state.unreadCountAtOpen,
                    )
                    if (row.showUnreadDivider) ConversationUnreadDivider(state.unreadCountAtOpen)
                    if (row.showDateDivider && row.day != null) ConversationDateDivider(row.day)

                    ConversationMessageRow(
                        message = message,
                        compactTop = row.compactTop,
                        compactBottom = row.compactBottom,
                        showSenderName = row.showSenderName,
                        showActions = state.actionsForMessageId == message.id,
                        reactionBusy = state.reactingMessageId == message.id,
                        mutationBusy = state.mutatingMessageId == message.id,
                        onToggleActions = { onToggleActions(message.id) },
                        onReply = { onReply(message) },
                        onEdit = { onEdit(message) },
                        onDelete = { onDelete(message) },
                        onReact = { emoji -> onReact(message, emoji) },
                        onOpenPhoto = onOpenPhoto,
                        onOpenSharedPost = onOpenSharedPost,
                        onOpenSharedProfile = onOpenSharedProfile,
                        onOpenSharedReel = onOpenSharedReel,
                    )
                }

                items(state.pendingMessages, key = { "pending-${it.clientId}" }) { pending ->
                    PendingMessageRow(pending = pending, onRetry = { onRetryPending(pending) })
                }
            }

            if (!nearBottom || state.newMessagesAwayCount > 0) {
                Surface(
                    onClick = onScrollLatest,
                    shape = RoundedCornerShape(22.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                    shadowElevation = 5.dp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        NovaIcon(
                            asset = NovaIconAsset.Back,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).rotate(-90f),
                            tint = NovaAccent,
                        )
                        Text(
                            if (state.newMessagesAwayCount > 0) {
                                "${state.newMessagesAwayCount} new"
                            } else {
                                "Latest"
                            },
                            color = NovaAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}


internal data class MessageRowContext(
    val showUnreadDivider: Boolean,
    val day: LocalDate?,
    val showDateDivider: Boolean,
    val compactTop: Boolean,
    val compactBottom: Boolean,
    val showSenderName: Boolean,
)


internal fun messageRowContext(
    messages: List<NovaMessage>,
    index: Int,
    isGroupConversation: Boolean,
    unreadAnchorMessageId: Long?,
    unreadCountAtOpen: Int,
    zoneId: ZoneId = ZoneId.systemDefault(),
): MessageRowContext {
    val message = messages[index]
    val day = messageLocalDate(message.createdAt, zoneId)
    val previous = messages.getOrNull(index - 1)
    val previousDay = previous?.let { messageLocalDate(it.createdAt, zoneId) }
    val previousSameSender = previous?.sender?.id == message.sender.id && previousDay == day
    val next = messages.getOrNull(index + 1)
    val nextSameSender = next?.sender?.id == message.sender.id &&
        messageLocalDate(next.createdAt, zoneId) == day
    return MessageRowContext(
        showUnreadDivider = message.id == unreadAnchorMessageId && unreadCountAtOpen > 0,
        day = day,
        showDateDivider = day != null && day != previousDay,
        compactTop = previousSameSender,
        compactBottom = nextSameSender,
        showSenderName = isGroupConversation && !message.isMine && !previousSameSender,
    )
}


@Composable
private fun LoadEarlierRow(loading: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = { if (!loading) onClick() },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = NovaAccent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(
                if (loading) "Loading earlier…" else "Load earlier messages",
                color = NovaMuted,
                fontSize = 12.sp,
            )
        }
    }
}


@Composable
private fun ConversationUnreadDivider(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = NovaAccentSoft,
            border = BorderStroke(1.dp, NovaAccent),
        ) {
            Text(
                if (count == 1) "1 unread message" else "$count unread messages",
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                color = NovaAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}


@Composable
private fun ConversationDateDivider(day: LocalDate) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Text(
                dayLabel(day),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                color = NovaMuted,
                fontSize = 10.sp,
            )
        }
    }
}


internal fun parseMessageInstant(value: String): Instant? =
    runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull()


internal fun messageLocalDate(value: String, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate? =
    parseMessageInstant(value)?.atZone(zoneId)?.toLocalDate()


internal fun localMessageTime(value: String, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val instant = parseMessageInstant(value) ?: return ""
    return DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(instant.atZone(zoneId))
}


internal fun dayLabel(day: LocalDate, today: LocalDate = LocalDate.now()): String = when (day) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> day.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}
