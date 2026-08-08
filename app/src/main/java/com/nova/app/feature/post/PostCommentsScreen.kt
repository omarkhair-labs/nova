package com.nova.app.feature.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.nova.app.core.network.NovaComment
import com.nova.app.core.network.NovaPost
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.theme.NovaAccent
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
    errorMessage: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSend: (String) -> Unit,
    onDelete: (NovaComment) -> Unit,
    onAuthorClick: (String) -> Unit,
) {
    var draft by remember(post?.id) { mutableStateOf("") }
    var wasSending by remember(post?.id) { mutableStateOf(false) }

    LaunchedEffect(isSending, errorMessage) {
        if (wasSending && !isSending && errorMessage == null) {
            draft = ""
        }
        wasSending = isSending
    }

    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            if (post != null) {
                Surface(
                    color = NovaSurface,
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(300) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add a comment…", color = NovaMuted) },
                            maxLines = 3,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NovaAccent,
                                unfocusedBorderColor = NovaBorder,
                                cursorColor = NovaAccent,
                                focusedContainerColor = NovaSurface,
                                unfocusedContainerColor = NovaSurface,
                            ),
                        )
                        Button(
                            onClick = {
                                val text = draft.trim()
                                if (text.isNotBlank() && !isSending) {
                                    onSend(text)
                                }
                            },
                            enabled = draft.isNotBlank() && !isSending,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NovaAccent),
                            modifier = Modifier.size(width = 72.dp, height = 54.dp),
                        ) {
                            Text(
                                text = if (isSending) "…" else "Send",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NovaBackground)
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 22.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                NovaHeader(
                    title = "Comments",
                    subtitle = if (post == null) {
                        "A conversation around this moment."
                    } else {
                        "${post.commentsCount} ${if (post.commentsCount == 1) "comment" else "comments"} on @${post.author.username}'s post."
                    },
                    onBack = onBack,
                )
            }

            if (post != null) {
                item { PostContext(post = post, onAuthorClick = onAuthorClick) }
            }

            when {
                post == null && !isLoading -> {
                    item {
                        MessageCard(
                            title = "This post isn't available",
                            message = errorMessage ?: "It may have been deleted or is no longer in your feed.",
                        )
                    }
                }

                isLoading && comments.isEmpty() -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 52.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(color = NovaAccent)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Loading comments…", color = NovaMuted, fontSize = 13.sp)
                        }
                    }
                }

                errorMessage != null && comments.isEmpty() -> {
                    item {
                        MessageCard(
                            title = "Couldn't load comments",
                            message = errorMessage,
                        )
                    }
                    item {
                        NovaSecondaryButton(text = "Try again", onClick = onRetry)
                    }
                }

                comments.isEmpty() -> {
                    item {
                        MessageCard(
                            title = "Start the conversation",
                            message = "No comments yet. Say something worth keeping.",
                        )
                    }
                }

                else -> {
                    item {
                        Text(
                            text = "Conversation",
                            color = NovaInk,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    items(comments, key = { it.id }) { comment ->
                        CommentRow(
                            comment = comment,
                            isDeleting = deletingCommentId == comment.id,
                            onAuthorClick = { onAuthorClick(comment.author.username) },
                            onDelete = { onDelete(comment) },
                        )
                    }
                }
            }

            if (errorMessage != null && comments.isNotEmpty()) {
                item {
                    Text(
                        text = errorMessage,
                        color = NovaMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
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
        shape = RoundedCornerShape(24.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaMediaImage(
                source = post.imageUrl,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentDescription = "Post preview",
            )

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { onAuthorClick(post.author.username) },
                    color = NovaSurface,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        NovaAvatar(
                            source = post.author.avatarUrl,
                            fallbackText = post.author.name.ifBlank { post.author.username },
                            size = 32.dp,
                        )
                        Column {
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
                }

                if (post.caption.isNotBlank()) {
                    Text(
                        text = post.caption,
                        color = NovaInk,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }

                Text(
                    text = "${post.likesCount} likes · ${post.commentsCount} comments",
                    color = NovaMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: NovaComment,
    isDeleting: Boolean,
    onAuthorClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(onClick = onAuthorClick, color = NovaSurface) {
                NovaAvatar(
                    source = comment.author.avatarUrl,
                    fallbackText = comment.author.name.ifBlank { comment.author.username },
                    size = 40.dp,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = comment.author.name.ifBlank { comment.author.username },
                        color = NovaInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = friendlyCommentDate(comment.createdAt),
                        color = NovaMuted,
                        fontSize = 10.sp,
                    )
                }
                Text(
                    text = "@${comment.author.username}",
                    color = NovaMuted,
                    fontSize = 11.sp,
                )
                Text(
                    text = comment.body,
                    color = NovaInk,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 7.dp),
                )

                if (comment.isMine) {
                    Surface(
                        onClick = { if (!isDeleting) onDelete() },
                        shape = RoundedCornerShape(12.dp),
                        color = NovaBackground,
                        border = BorderStroke(1.dp, NovaBorder),
                        modifier = Modifier.padding(top = 9.dp),
                    ) {
                        Text(
                            text = if (isDeleting) "Deleting…" else "Delete",
                            color = NovaMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    message: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
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
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private fun friendlyCommentDate(raw: String): String {
    if (raw.isBlank()) return "now"
    return runCatching {
        OffsetDateTime.parse(raw).format(DateTimeFormatter.ofPattern("MMM d · h:mm a"))
    }.getOrDefault("now")
}
