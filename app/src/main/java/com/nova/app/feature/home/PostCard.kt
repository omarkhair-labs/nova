package com.nova.app.feature.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.sharing.NovaShareDialog
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaConfirmDeleteDialog
import com.nova.app.ui.components.NovaLikeBurst
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.components.NovaSocialAction
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@Composable
fun NovaPostCard(
    post: NovaPost,
    isDeleting: Boolean,
    isLiking: Boolean,
    isReposting: Boolean,
    actionErrorMessage: String?,
    onAuthorClick: () -> Unit,
    onReposterClick: (String) -> Unit,
    onOpenPost: () -> Unit,
    onLikeToggle: () -> Unit,
    onCommentsClick: () -> Unit,
    onRepostToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showDeleteConfirm by remember(post.id) { mutableStateOf(false) }
    var showShare by remember(post.id) { mutableStateOf(false) }
    var likeBurstTrigger by remember(post.id) { mutableIntStateOf(0) }

    if (showDeleteConfirm) {
        NovaConfirmDeleteDialog(
            title = "Delete this post?",
            message = "This removes the post and its comments from Nova. This can't be undone.",
            isBusy = isDeleting,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
        )
    }

    if (showShare) {
        NovaShareDialog(
            title = "Send this moment",
            postId = post.id,
            onExternalShare = {
                val shareText = buildString {
                    append("See @${post.author.username}'s moment on Nova")
                    if (post.caption.isNotBlank()) append("\n\n${post.caption}")
                }
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        },
                        "Share post",
                    )
                )
            },
            onDismiss = { showShare = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        post.repostedBy?.let { reposter ->
            Surface(
                onClick = { onReposterClick(reposter.username) },
                color = MaterialTheme.colorScheme.background,
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = NovaSpacing.md,
                        end = NovaSpacing.md,
                        bottom = NovaSpacing.sm,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
                ) {
                    NovaIcon(
                        asset = NovaIconAsset.Repost,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NovaAccent,
                    )
                    Text(
                        text = "${reposter.name.ifBlank { "@${reposter.username}" }} moved this into your orbit",
                        color = NovaMuted,
                        style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
        ) {
            Surface(onClick = onAuthorClick, shape = CircleShape, color = MaterialTheme.colorScheme.background) {
                NovaAvatar(
                    source = post.author.avatarUrl,
                    fallbackText = post.author.name.ifBlank { post.author.username },
                    size = 42.dp,
                )
            }
            Surface(
                onClick = onAuthorClick,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column {
                    Text(
                        post.author.name.ifBlank { post.author.username },
                        color = NovaInk,
                        style = NovaType.bodyCompact.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "@${post.author.username} · ${friendlyDate(post.createdAt)}",
                        color = NovaMuted,
                        style = NovaType.micro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (post.isMine) {
                Surface(
                    onClick = { if (!isDeleting) showDeleteConfirm = true },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        NovaIcon(
                            asset = NovaIconAsset.More,
                            contentDescription = if (isDeleting) "Deleting post" else "Post options",
                            modifier = Modifier.size(22.dp),
                            tint = NovaMuted,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = onOpenPost,
                    onDoubleClick = {
                        if (!isLiking) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            likeBurstTrigger += 1
                            if (!post.isLiked) onLikeToggle()
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            NovaMediaImage(
                source = post.imageUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
                contentDescription = "Post by ${post.author.username}",
            )
            NovaLikeBurst(
                trigger = likeBurstTrigger,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NovaSocialAction(
                icon = if (post.isLiked) NovaIconAsset.LikeFilled else NovaIconAsset.Like,
                contentDescription = if (post.isLiked) "Unlike post" else "Like post",
                count = post.likesCount,
                active = post.isLiked,
                busy = isLiking,
                activeColor = NovaAccent,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLikeToggle()
                },
            )
            NovaSocialAction(
                icon = NovaIconAsset.Comment,
                contentDescription = "Open ${post.commentsCount} comments",
                count = post.commentsCount,
                onClick = onCommentsClick,
            )
            NovaSocialAction(
                icon = NovaIconAsset.Repost,
                contentDescription = if (post.isReposted) "Remove repost" else "Repost",
                count = post.repostsCount,
                active = post.isReposted,
                busy = isReposting,
                onClick = onRepostToggle,
            )
            NovaSocialAction(
                icon = NovaIconAsset.Share,
                contentDescription = "Share post",
                onClick = { showShare = true },
            )
        }

        if (post.caption.isNotBlank()) {
            Text(
                text = post.caption,
                modifier = Modifier.padding(horizontal = NovaSpacing.md),
                color = NovaInk,
                style = NovaType.bodyCompact,
            )
        }

        if (post.commentsCount > 0) {
            Surface(onClick = onCommentsClick, color = MaterialTheme.colorScheme.background) {
                Text(
                    text = "Join ${post.commentsCount} ${if (post.commentsCount == 1) "comment" else "comments"}",
                    modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
                    color = NovaMuted,
                    style = NovaType.meta,
                )
            }
        } else {
            Spacer(modifier = Modifier.padding(top = NovaSpacing.sm))
        }

        actionErrorMessage?.let {
            Surface(
                modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
                shape = MaterialTheme.shapes.small,
                color = NovaAccentSoft,
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
                    color = NovaInk,
                    style = NovaType.meta,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = NovaSpacing.md),
            color = NovaBorder.copy(alpha = 0.7f),
        )
    }
}


private fun friendlyDate(raw: String): String {
    if (raw.isBlank()) return "now"
    return runCatching {
        OffsetDateTime.parse(raw).format(DateTimeFormatter.ofPattern("MMM d"))
    }.getOrDefault("now")
}
