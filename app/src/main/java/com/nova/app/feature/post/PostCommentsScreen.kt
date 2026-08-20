package com.nova.app.feature.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaConfirmDeleteDialog
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@Composable
fun PostCommentsScreen(
    post: NovaPost?,
    comments: List<NovaComment>,
    isLoading: Boolean,
    isSending: Boolean,
    deletingCommentId: Long?,
    isReplySending: Boolean,
    deletingReplyId: Long?,
    errorMessage: String?,
    replyErrorMessage: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSend: (String) -> Unit,
    onDelete: (NovaComment) -> Unit,
    onSendReply: (NovaComment, String) -> Unit,
    onDeleteReply: (NovaComment) -> Unit,
    onAuthorClick: (String) -> Unit,
) {
    var draft by remember(post?.id) { mutableStateOf("") }
    var wasSending by remember(post?.id) { mutableStateOf(false) }
    var wasReplySending by remember(post?.id) { mutableStateOf(false) }
    var replyingTo by remember(post?.id) { mutableStateOf<NovaComment?>(null) }

    LaunchedEffect(isSending, errorMessage) {
        if (wasSending && !isSending && errorMessage == null) {
            draft = ""
        }
        wasSending = isSending
    }

    LaunchedEffect(isReplySending, replyErrorMessage) {
        if (wasReplySending && !isReplySending && replyErrorMessage == null) {
            draft = ""
            replyingTo = null
        }
        wasReplySending = isReplySending
    }

    val composerBusy = isSending || isReplySending
    val visibleError = replyErrorMessage ?: errorMessage

    Scaffold(
        containerColor = NovaBackground,
        topBar = {
            CommentsTopBar(
                count = post?.commentsCount,
                onBack = onBack,
            )
        },
        bottomBar = {
            if (post != null) {
                CommentComposer(
                    draft = draft,
                    isSending = composerBusy,
                    replyingTo = replyingTo,
                    onCancelReply = { replyingTo = null },
                    onDraftChange = { draft = it.take(300) },
                    onSend = {
                        val clean = draft.trim()
                        val parent = replyingTo
                        if (clean.isBlank() || composerBusy) return@CommentComposer

                        if (parent == null) {
                            onSend(clean)
                        } else {
                            onSendReply(parent, clean)
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaBackground)
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 12.dp,
                bottom = 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (post != null) {
                item {
                    PostContext(
                        post = post,
                        onAuthorClick = onAuthorClick,
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            when {
                post == null && !isLoading -> {
                    item {
                        EmptyCommentsState(
                            title = "Post unavailable",
                            message = visibleError ?: "This post may have been deleted or is no longer available.",
                        )
                    }
                }

                isLoading && comments.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = NovaAccent)
                        }
                    }
                }

                visibleError != null && comments.isEmpty() -> {
                    item {
                        EmptyCommentsState(
                            title = "Couldn't load comments",
                            message = visibleError,
                        )
                    }
                    item {
                        NovaSecondaryButton(text = "Try again", onClick = onRetry)
                    }
                }

                comments.isEmpty() -> {
                    item {
                        EmptyCommentsState(
                            title = "No comments yet",
                            message = "Be the first to join the conversation.",
                        )
                    }
                }

                else -> {
                    items(comments, key = { it.id }) { comment ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CommentRow(
                                comment = comment,
                                isDeleting = deletingCommentId == comment.id,
                                onAuthorClick = { onAuthorClick(comment.author.username) },
                                onReply = { replyingTo = comment },
                                onDelete = { onDelete(comment) },
                            )

                            comment.replies.forEach { reply ->
                                CommentRow(
                                    comment = reply,
                                    modifier = Modifier.padding(start = 42.dp),
                                    isReply = true,
                                    isDeleting = deletingReplyId == reply.id,
                                    onAuthorClick = { onAuthorClick(reply.author.username) },
                                    onReply = { replyingTo = comment },
                                    onDelete = { onDeleteReply(reply) },
                                )
                            }
                        }
                    }
                }
            }

            if (visibleError != null && comments.isNotEmpty()) {
                item {
                    Text(
                        text = visibleError,
                        color = NovaMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 54.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}


@Composable
private fun CommentsTopBar(
    count: Int?,
    onBack: () -> Unit,
) {
    Surface(
        color = NovaSurface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onBack,
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = NovaBackground,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "‹",
                        color = NovaInk,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = "Comments",
                    color = NovaInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (count != null) {
                    Text(
                        text = "$count ${if (count == 1) "comment" else "comments"}",
                        color = NovaMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}


@Composable
private fun CommentComposer(
    draft: String,
    isSending: Boolean,
    replyingTo: NovaComment?,
    onCancelReply: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = NovaSurface,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            replyingTo?.let { target ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 12.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Replying to @${target.author.username}",
                        color = NovaMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Surface(onClick = onCancelReply, color = NovaSurface) {
                        Text("×", color = NovaMuted, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (replyingTo == null) "Add a comment…" else "Write a reply…",
                            color = NovaMuted,
                        )
                    },
                    enabled = !isSending,
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                        focusedContainerColor = NovaBackground,
                        unfocusedContainerColor = NovaBackground,
                    ),
                )

                val enabled = draft.isNotBlank() && !isSending
                Surface(
                    onClick = { if (enabled) onSend() },
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = if (enabled) NovaAccent else NovaAccentSoft,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NovaMuted,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "↑",
                                color = if (enabled) NovaBackground else NovaMuted,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun PostContext(
    post: NovaPost,
    onAuthorClick: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaMediaImage(
                source = post.imageUrl,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(13.dp)),
                contentDescription = "Post preview",
            )
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { onAuthorClick(post.author.username) },
                    color = NovaSurface,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        NovaAvatar(
                            source = post.author.avatarUrl,
                            fallbackText = post.author.name.ifBlank { post.author.username },
                            size = 27.dp,
                        )
                        Text(
                            text = post.author.name.ifBlank { post.author.username },
                            color = NovaInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "@${post.author.username}",
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
                if (post.caption.isNotBlank()) {
                    Text(
                        text = post.caption,
                        color = NovaInk,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
        }
    }
}


@Composable
private fun CommentRow(
    comment: NovaComment,
    isDeleting: Boolean,
    onAuthorClick: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isReply: Boolean = false,
) {
    var showDeleteConfirm by remember(comment.id, comment.parentId) { mutableStateOf(false) }

    if (showDeleteConfirm) {
        NovaConfirmDeleteDialog(
            title = if (isReply) "Delete this reply?" else "Delete this comment?",
            message = if (isReply) {
                "This reply will be removed. This can't be undone."
            } else {
                "This comment and its replies will be removed. This can't be undone."
            },
            isBusy = isDeleting,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = if (isReply) 6.dp else 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            onClick = onAuthorClick,
            shape = CircleShape,
            color = NovaBackground,
        ) {
            NovaAvatar(
                source = comment.author.avatarUrl,
                fallbackText = comment.author.name.ifBlank { comment.author.username },
                size = if (isReply) 32.dp else 40.dp,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = comment.author.name.ifBlank { comment.author.username },
                    color = NovaInk,
                    fontSize = if (isReply) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "@${comment.author.username}",
                    color = NovaMuted,
                    fontSize = 10.sp,
                )
            }
            Text(
                text = comment.body,
                color = NovaInk,
                fontSize = if (isReply) 13.sp else 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = friendlyCommentDate(comment.createdAt),
                    color = NovaMuted,
                    fontSize = 10.sp,
                )
                Surface(
                    onClick = onReply,
                    color = NovaBackground,
                ) {
                    Text(
                        text = "Reply",
                        color = NovaMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (comment.isMine) {
                    Surface(
                        onClick = { if (!isDeleting) showDeleteConfirm = true },
                        color = NovaBackground,
                    ) {
                        Text(
                            text = if (isDeleting) "Deleting…" else "Delete",
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


@Composable
private fun EmptyCommentsState(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = NovaInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = message,
            color = NovaMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}


private fun friendlyCommentDate(raw: String): String {
    if (raw.isBlank()) return "now"
    return runCatching {
        OffsetDateTime.parse(raw).format(DateTimeFormatter.ofPattern("MMM d · h:mm a"))
    }.getOrDefault("now")
}
