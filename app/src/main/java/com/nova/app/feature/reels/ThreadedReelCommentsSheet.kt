package com.nova.app.feature.reels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.reels.data.ReelsRepository
import com.nova.app.feature.reels.domain.model.NovaReel
import com.nova.app.feature.reels.domain.model.NovaReelComment
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
internal fun ThreadedReelCommentsSheet(
    reel: NovaReel,
    repository: ReelsRepository,
    onDismiss: () -> Unit,
    onReelUpdated: (NovaReel) -> Unit,
    onPersonClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val owner = remember(reel.id, repository, scope) {
        ReelCommentsStateOwner(
            reelId = reel.id,
            repository = repository,
            scope = scope,
        )
    }
    val state = owner.state

    LaunchedEffect(owner) {
        owner.loadComments()
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }
    LaunchedEffect(state.reelUpdatedVersion) {
        if (state.reelUpdatedVersion > 0) {
            state.updatedReel?.let(onReelUpdated)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NovaSurface,
        contentColor = NovaInk,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp),
        ) {
            Text(
                text = "Comments · ${reel.commentsCount}",
                color = NovaInk,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = NovaAccent)
                    }
                }
                state.comments.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.error ?: "No comments yet. Start the conversation.",
                            color = NovaMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.comments, key = { "comment-${it.id}" }) { comment ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ReelCommentRow(
                                    comment = comment,
                                    isDeleting = state.deletingCommentId == comment.id,
                                    onPersonClick = onPersonClick,
                                    onReply = { owner.beginReply(comment) },
                                    onDelete = if (comment.isMine) {
                                        { owner.deleteComment(comment) }
                                    } else null,
                                )
                                comment.replies.forEach { reply ->
                                    ReelCommentRow(
                                        comment = reply,
                                        modifier = Modifier.padding(start = 42.dp, top = 3.dp),
                                        isReply = true,
                                        isDeleting = state.deletingReplyId == reply.id,
                                        onPersonClick = onPersonClick,
                                        onReply = { owner.beginReply(comment) },
                                        onDelete = if (reply.isMine) {
                                            { owner.deleteReply(reply) }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            state.error?.let {
                Text(
                    text = it,
                    color = NovaMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            state.replyingTo?.let { target ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Replying to @${target.author.username}",
                        color = NovaMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Surface(
                        onClick = owner::cancelReply,
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            NovaIcon(
                                asset = NovaIconAsset.Close,
                                contentDescription = "Cancel reply",
                                modifier = Modifier.size(18.dp),
                                tint = NovaMuted,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.body,
                    onValueChange = owner::setBody,
                    modifier = Modifier.weight(1f),
                    enabled = !state.sending,
                    placeholder = {
                        Text(
                            if (state.replyingTo == null) "Add a comment…" else "Write a reply…",
                            color = NovaMuted,
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                    ),
                )
                Surface(
                    onClick = owner::send,
                    enabled = !state.sending && state.body.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = if (state.body.isNotBlank()) NovaAccent else NovaBorder,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (state.sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            NovaIcon(
                                asset = NovaIconAsset.Send,
                                contentDescription = "Send Reel comment",
                                modifier = Modifier.size(22.dp),
                                tint = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ReelCommentRow(
    comment: NovaReelComment,
    onPersonClick: (String) -> Unit,
    onReply: () -> Unit,
    modifier: Modifier = Modifier,
    isReply: Boolean = false,
    isDeleting: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        NovaAvatar(
            source = comment.author.avatarUrl,
            fallbackText = comment.author.displayName,
            size = if (isReply) 29.dp else 34.dp,
            modifier = Modifier.clickable { onPersonClick(comment.author.username) },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "@${comment.author.username}",
                color = NovaInk,
                fontSize = if (isReply) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onPersonClick(comment.author.username) },
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.body,
                color = NovaInk,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(onClick = onReply, color = Color.Transparent) {
                    Text("Reply", color = NovaMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                if (onDelete != null) {
                    Surface(
                        onClick = { if (!isDeleting) onDelete() },
                        color = Color.Transparent,
                        border = if (isDeleting) BorderStroke(0.dp, Color.Transparent) else null,
                    ) {
                        Text(
                            if (isDeleting) "Deleting…" else "Delete",
                            color = NovaMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
