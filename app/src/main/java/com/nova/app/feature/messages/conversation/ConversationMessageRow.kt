package com.nova.app.feature.messages.conversation

import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaMessageShare
import com.nova.app.feature.messages.domain.model.NovaReplyPreview
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt


private val ReactionChoices = listOf("❤️", "😂", "😮", "😢", "😡", "👍")
private const val SwipeReplyThresholdPx = 72f
private const val CallHistoryClientPrefix = "call:"


@Composable
internal fun ConversationMessageRow(
    message: NovaMessage,
    compactTop: Boolean,
    compactBottom: Boolean,
    showSenderName: Boolean,
    showActions: Boolean,
    reactionBusy: Boolean,
    mutationBusy: Boolean,
    onToggleActions: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onOpenPhoto: (String) -> Unit,
    onOpenSharedPost: (Long) -> Unit,
    onOpenSharedProfile: (String) -> Unit,
    onOpenSharedReel: (String, Long) -> Unit,
) {
    var dragX by remember(message.id) { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (compactTop) 0.dp else 5.dp)
            .offset { IntOffset(dragX.roundToInt(), 0) }
            .pointerInput(message.id, message.isDeleted) {
                if (!message.isDeleted) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, amount ->
                            dragX = (dragX + amount).coerceIn(-120f, 120f)
                        },
                        onDragEnd = {
                            if (abs(dragX) >= SwipeReplyThresholdPx) onReply()
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f },
                    )
                }
            },
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
    ) {
        if (abs(dragX) > 24f) {
            Text("↩ Reply", color = NovaAccent, fontSize = 10.sp, modifier = Modifier.padding(bottom = 3.dp))
        }

        Box(contentAlignment = if (message.isMine) Alignment.TopEnd else Alignment.TopStart) {
            Surface(
                modifier = Modifier.combinedClickable(
                    enabled = !message.isDeleted,
                    onClick = {},
                    onLongClick = onToggleActions,
                ),
                shape = RoundedCornerShape(
                    topStart = if (!message.isMine && compactTop) 8.dp else 20.dp,
                    topEnd = if (message.isMine && compactTop) 8.dp else 20.dp,
                    bottomStart = if (!message.isMine && !compactBottom) 5.dp else 20.dp,
                    bottomEnd = if (message.isMine && !compactBottom) 5.dp else 20.dp,
                ),
                color = if (message.isMine) NovaAccent else NovaSurface,
                border = if (message.isMine) null else BorderStroke(1.dp, NovaBorder),
            ) {
                Column(modifier = Modifier.widthIn(max = 292.dp).padding(horizontal = 10.dp, vertical = 9.dp)) {
                    if (showSenderName) {
                        Text(
                            text = message.sender.name.ifBlank { "@${message.sender.username}" },
                            color = NovaAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (message.isDeleted) {
                        Text(
                            "Message deleted",
                            color = if (message.isMine) NovaBackground.copy(alpha = 0.78f) else NovaMuted,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                        )
                    } else {
                        message.replyTo?.let { reply -> ConversationReplyPreview(reply, message.isMine) }

                        if (message.imageUrl.isNotBlank()) {
                            Surface(
                                onClick = { onOpenPhoto(message.imageUrl) },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Transparent,
                            ) {
                                NovaMediaImage(
                                    source = message.imageUrl,
                                    modifier = Modifier.fillMaxWidth().height(240.dp),
                                    contentDescription = "Message photo",
                                )
                            }
                            if (message.body.isNotBlank() || message.share != null) Spacer(Modifier.height(8.dp))
                        }

                        if (message.audioUrl.isNotBlank()) {
                            ConversationVoiceNotePlayer(message.audioUrl, message.audioDurationMs, message.isMine)
                            if (message.body.isNotBlank() || message.share != null) Spacer(Modifier.height(7.dp))
                        }

                        message.share?.let { share ->
                            ConversationSharedContentCard(
                                share = share,
                                mine = message.isMine,
                                onOpenPost = onOpenSharedPost,
                                onOpenProfile = onOpenSharedProfile,
                                onOpenReel = onOpenSharedReel,
                            )
                        }

                        if (message.share == null && message.body.isNotBlank()) {
                            Text(
                                message.body,
                                color = if (message.isMine) NovaBackground else NovaInk,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    val delivery = when {
                        !message.isMine -> ""
                        message.readAt != null -> " · Read"
                        message.deliveredAt != null -> " · Delivered"
                        else -> " · Sent"
                    }
                    val edited = if (!message.isDeleted && message.editedAt != null) " · Edited" else ""
                    Text(
                        localMessageTime(message.createdAt) + edited + delivery,
                        color = if (message.isMine) NovaBackground.copy(alpha = 0.72f) else NovaMuted,
                        fontSize = 9.sp,
                    )
                }
            }

            DropdownMenu(
                expanded = showActions && !message.isDeleted,
                onDismissRequest = onToggleActions,
                containerColor = NovaSurface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReactionChoices.forEach { emoji ->
                        Surface(
                            onClick = { if (!reactionBusy && !mutationBusy) onReact(emoji) },
                            shape = CircleShape,
                            color = NovaBackground,
                        ) {
                            Text(emoji, modifier = Modifier.padding(6.dp), fontSize = 16.sp)
                        }
                    }
                }
                DropdownMenuItem(
                    text = { Text("Reply", color = NovaInk) },
                    onClick = { if (!mutationBusy) onReply() },
                    enabled = !mutationBusy,
                )
                if (message.isMine && message.share == null && !message.isCallHistory()) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = NovaInk) },
                        onClick = { if (!mutationBusy) onEdit() },
                        enabled = !mutationBusy,
                    )
                }
                if (message.isMine) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = NovaInk) },
                        onClick = { if (!mutationBusy) onDelete() },
                        enabled = !mutationBusy,
                    )
                }
            }
        }

        if (!message.isDeleted && message.reactions.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                message.reactions.forEach { reaction ->
                    Surface(
                        onClick = { if (!reactionBusy) onReact(reaction.emoji) },
                        shape = RoundedCornerShape(13.dp),
                        color = if (reaction.reactedByMe) NovaAccentSoft else NovaSurface,
                        border = BorderStroke(1.dp, if (reaction.reactedByMe) NovaAccent else NovaBorder),
                    ) {
                        Text(
                            "${reaction.emoji} ${reaction.count}",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            color = NovaInk,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ConversationSharedContentCard(
    share: NovaMessageShare,
    mine: Boolean,
    onOpenPost: (Long) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenReel: (String, Long) -> Unit,
) {
    val cardColor = if (mine) NovaBackground.copy(alpha = 0.16f) else NovaBackground
    val borderColor = if (mine) NovaBackground.copy(alpha = 0.25f) else NovaBorder
    val primary = if (mine) NovaBackground else NovaInk
    val secondary = if (mine) NovaBackground.copy(alpha = 0.75f) else NovaMuted

    if (!share.available) {
        Surface(
            shape = RoundedCornerShape(15.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = when (share.kind) {
                        "profile" -> "Shared profile"
                        "reel" -> "Shared Reel"
                        else -> "Shared post"
                    },
                    color = primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text("This content is no longer available to you.", color = secondary, fontSize = 11.sp)
            }
        }
        return
    }

    share.post?.let { post ->
        Surface(
            onClick = { onOpenPost(post.id) },
            shape = RoundedCornerShape(15.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NovaAvatar(
                        source = post.author.avatarUrl,
                        fallbackText = post.author.name.ifBlank { post.author.username },
                        size = 34.dp,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = post.author.name.ifBlank { post.author.username },
                            color = primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text("@${post.author.username} · Shared post", color = secondary, fontSize = 9.sp, maxLines = 1)
                    }
                    Text("›", color = secondary, fontSize = 18.sp)
                }
                if (post.imageUrl.isNotBlank()) {
                    NovaMediaImage(
                        source = post.imageUrl,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentDescription = "Shared post photo",
                    )
                }
                if (post.caption.isNotBlank()) {
                    Text(
                        text = post.caption,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        color = primary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                    )
                }
            }
        }
        return
    }

    share.reel?.let { reel ->
        Surface(
            onClick = { onOpenReel(reel.author.username, reel.id) },
            shape = RoundedCornerShape(15.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    NovaAvatar(
                        source = reel.author.avatarUrl,
                        fallbackText = reel.author.name.ifBlank { reel.author.username },
                        size = 38.dp,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = reel.author.name.ifBlank { reel.author.username },
                            color = primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text("@${reel.author.username} · Reel", color = secondary, fontSize = 9.sp, maxLines = 1)
                    }
                    Text(
                        "▶",
                        color = if (mine) NovaBackground else NovaAccent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (reel.caption.isNotBlank()) {
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = reel.caption,
                        color = primary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Watch Reel",
                    color = if (mine) NovaBackground else NovaAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        return
    }

    share.profile?.let { profile ->
        Surface(
            onClick = { onOpenProfile(profile.username) },
            shape = RoundedCornerShape(15.dp),
            color = cardColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NovaAvatar(
                    source = profile.avatarUrl,
                    fallbackText = profile.name.ifBlank { profile.username },
                    size = 48.dp,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = profile.name.ifBlank { profile.username },
                        color = primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text("@${profile.username}", color = secondary, fontSize = 10.sp, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "View profile",
                        color = if (mine) NovaBackground else NovaAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text("›", color = secondary, fontSize = 20.sp)
            }
        }
        return
    }

    Surface(
        shape = RoundedCornerShape(15.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Shared content unavailable",
            modifier = Modifier.padding(12.dp),
            color = secondary,
            fontSize = 11.sp,
        )
    }
}


@Composable
internal fun PendingMessageRow(pending: PendingMessage, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 5.dp), horizontalAlignment = Alignment.End) {
        Surface(
            onClick = { if (pending.status == PendingMessageStatus.Failed) onRetry() },
            shape = RoundedCornerShape(20.dp, 20.dp, 5.dp, 20.dp),
            color = if (pending.status == PendingMessageStatus.Failed) {
                NovaAccent.copy(alpha = 0.72f)
            } else {
                NovaAccent
            },
        ) {
            Column(modifier = Modifier.widthIn(max = 292.dp).padding(horizontal = 10.dp, vertical = 9.dp)) {
                pending.replyTo?.let { ConversationReplyPreview(it, mine = true) }

                if (pending.imageUri.isNotBlank()) {
                    NovaMediaImage(
                        source = pending.imageUri,
                        modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(14.dp)),
                        contentDescription = "Sending message photo",
                    )
                    if (pending.body.isNotBlank()) Spacer(Modifier.height(8.dp))
                }

                if (pending.audioPath.isNotBlank()) {
                    ConversationVoiceNotePlayer(pending.audioPath, pending.audioDurationMs, mine = true)
                    if (pending.body.isNotBlank()) Spacer(Modifier.height(7.dp))
                }

                if (pending.body.isNotBlank()) {
                    Text(pending.body, color = NovaBackground, fontSize = 14.sp, lineHeight = 20.sp)
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    when (pending.status) {
                        PendingMessageStatus.Sending -> "${localMessageTime(pending.createdAt)} · Sending…"
                        PendingMessageStatus.Failed -> "${localMessageTime(pending.createdAt)} · Failed · Tap to retry"
                    },
                    color = NovaBackground.copy(alpha = 0.78f),
                    fontSize = 9.sp,
                )
                if (pending.status == PendingMessageStatus.Failed && !pending.error.isNullOrBlank()) {
                    Text(
                        pending.error,
                        color = NovaBackground.copy(alpha = 0.72f),
                        fontSize = 9.sp,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}


@Composable
private fun ConversationReplyPreview(reply: NovaReplyPreview, mine: Boolean) {
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = if (mine) NovaBackground.copy(alpha = 0.18f) else NovaAccentSoft,
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(
                "@${reply.sender.username}",
                color = if (mine) NovaBackground else NovaAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                replyPreviewText(reply),
                color = if (mine) NovaBackground.copy(alpha = 0.8f) else NovaMuted,
                fontSize = 11.sp,
                maxLines = 2,
            )
        }
    }
}


@Composable
private fun ConversationVoiceNotePlayer(audioUrl: String, durationMs: Long?, mine: Boolean) {
    var prepared by remember(audioUrl) { mutableStateOf(false) }
    var playing by remember(audioUrl) { mutableStateOf(false) }
    var failed by remember(audioUrl) { mutableStateOf(false) }
    val player = remember(audioUrl) { MediaPlayer() }

    DisposableEffect(player, audioUrl) {
        player.setOnPreparedListener {
            prepared = true
            failed = false
        }
        player.setOnCompletionListener { playing = false }
        player.setOnErrorListener { _, _, _ ->
            playing = false
            failed = true
            true
        }
        runCatching {
            player.setDataSource(audioUrl)
            player.prepareAsync()
        }.onFailure { failed = true }

        onDispose { runCatching { player.release() } }
    }

    Surface(
        onClick = {
            if (prepared && !failed) {
                runCatching {
                    if (playing) {
                        player.pause()
                        playing = false
                    } else {
                        player.start()
                        playing = true
                    }
                }.onFailure { failed = true }
            }
        },
        shape = RoundedCornerShape(16.dp),
        color = if (mine) NovaBackground.copy(alpha = 0.17f) else NovaAccentSoft,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                when {
                    failed -> "!"
                    !prepared -> "…"
                    playing -> "❚❚"
                    else -> "▶"
                },
                color = if (mine) NovaBackground else NovaAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (failed) "Voice unavailable" else "Voice message",
                    color = if (mine) NovaBackground else NovaInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatVoiceDuration(durationMs ?: 0L),
                    color = if (mine) NovaBackground.copy(alpha = 0.72f) else NovaMuted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}


@Composable
internal fun ConversationFullScreenPhoto(photoUrl: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Full screen message photo",
                modifier = Modifier.fillMaxSize().padding(vertical = 60.dp),
                contentScale = ContentScale.Fit,
            )
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(18.dp),
            ) {
                Text(
                    "×",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 24.sp,
                )
            }
        }
    }
}


internal fun replyPreviewText(reply: NovaReplyPreview): String = when {
    reply.isDeleted -> "Message deleted"
    reply.body.isNotBlank() -> reply.body
    reply.audioUrl.isNotBlank() -> "🎤 Voice message"
    reply.imageUrl.isNotBlank() -> "📷 Photo"
    else -> "Message"
}


internal fun formatVoiceDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L).coerceAtMost(5 * 60L)
    return "%d:%02d".format(Locale.US, totalSeconds / 60L, totalSeconds % 60L)
}


private fun NovaMessage.isCallHistory(): Boolean = clientId.startsWith(CallHistoryClientPrefix)
