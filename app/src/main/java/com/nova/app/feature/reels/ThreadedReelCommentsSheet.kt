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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.ApiResult
import com.nova.app.core.reels.NovaReel
import com.nova.app.core.reels.NovaReelComment
import com.nova.app.core.reels.NovaReelsRepository
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.launch


@Composable
internal fun ThreadedReelCommentsSheet(
    reel: NovaReel,
    repository: NovaReelsRepository,
    onDismiss: () -> Unit,
    onReelUpdated: (NovaReel) -> Unit,
    onPersonClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var comments by remember(reel.id) { mutableStateOf<List<NovaReelComment>>(emptyList()) }
    var loading by remember(reel.id) { mutableStateOf(true) }
    var sending by remember(reel.id) { mutableStateOf(false) }
    var deletingReplyId by remember(reel.id) { mutableStateOf<Long?>(null) }
    var body by remember(reel.id) { mutableStateOf("") }
    var replyingTo by remember(reel.id) { mutableStateOf<NovaReelComment?>(null) }
    var error by remember(reel.id) { mutableStateOf<String?>(null) }

    fun loadComments() {
        scope.launch {
            loading = true
            error = null
            when (val result = repository.comments(reel.id)) {
                is ApiResult.Success -> comments = result.value
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
            loading = false
        }
    }

    fun appendComment(comment: NovaReelComment) {
        val parentId = comment.parentId
        comments = if (parentId == null) {
            comments + comment
        } else {
            comments.map { parent ->
                if (parent.id != parentId) {
                    parent
                } else {
                    val existing = parent.replies.filterNot { it.id == comment.id }
                    parent.copy(
                        replies = existing + comment,
                        repliesCount = existing.size + 1,
                    )
                }
            }
        }
    }

    fun removeReply(reply: NovaReelComment) {
        val parentId = reply.parentId ?: return
        comments = comments.map { parent ->
            if (parent.id != parentId) {
                parent
            } else {
                val remaining = parent.replies.filterNot { it.id == reply.id }
                parent.copy(replies = remaining, repliesCount = remaining.size)
            }
        }
    }

    LaunchedEffect(reel.id) { loadComments() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NovaSurface,
        contentColor = NovaInk,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
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
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = NovaAccent)
                    }
                }
                comments.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = error ?: "No comments yet. Start the conversation.",
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
                        items(comments, key = { "comment-${it.id}" }) { comment ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ReelCommentRow(
                                    comment = comment,
                                    onPersonClick = onPersonClick,
                                    onReply = {
                                        replyingTo = comment
                                        error = null
                                    },
                                )
                                comment.replies.forEach { reply ->
                                    ReelCommentRow(
                                        comment = reply,
                                        modifier = Modifier.padding(start = 42.dp, top = 3.dp),
                                        isReply = true,
                                        isDeleting = deletingReplyId == reply.id,
                                        onPersonClick = onPersonClick,
                                        onReply = {
                                            replyingTo = comment
                                            error = null
                                        },
                                        onDelete = if (reply.isMine) {
                                            {
                                                if (deletingReplyId == null) {
                                                    scope.launch {
                                                        deletingReplyId = reply.id
                                                        error = null
                                                        when (val result = repository.deleteCommentReply(reply.id)) {
                                                            is ApiResult.Success -> {
                                                                removeReply(reply)
                                                                onReelUpdated(result.value)
                                                            }
                                                            is ApiResult.Failure -> {
                                                                if (result.statusCode == 401) {
                                                                    onSessionExpired()
                                                                } else {
                                                                    error = result.message
                                                                }
                                                            }
                                                        }
                                                        deletingReplyId = null
                                                    }
                                                }
                                            }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = NovaMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            replyingTo?.let { target ->
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
                        onClick = { replyingTo = null },
                        color = Color.Transparent,
                    ) {
                        Text("×", color = NovaMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    value = body,
                    onValueChange = { body = it.take(300) },
                    modifier = Modifier.weight(1f),
                    enabled = !sending,
                    placeholder = {
                        Text(
                            if (replyingTo == null) "Add a comment…" else "Write a reply…",
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
                    onClick = {
                        if (!sending && body.isNotBlank()) {
                            val parentId = replyingTo?.id
                            scope.launch {
                                sending = true
                                error = null
                                when (val result = repository.addComment(reel.id, body, parentId)) {
                                    is ApiResult.Success -> {
                                        appendComment(result.value.comment)
                                        onReelUpdated(result.value.reel)
                                        body = ""
                                        replyingTo = null
                                    }
                                    is ApiResult.Failure -> {
                                        if (result.statusCode == 401) onSessionExpired() else error = result.message
                                    }
                                }
                                sending = false
                            }
                        }
                    },
                    enabled = !sending && body.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = if (body.isNotBlank()) NovaAccent else NovaBorder,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("↑", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
