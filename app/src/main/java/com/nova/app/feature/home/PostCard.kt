package com.nova.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.core.sharing.NovaRepostState
import com.nova.app.core.sharing.NovaSharingRepository
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.sharing.NovaShareDialog
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaConfirmDeleteDialog
import com.nova.app.ui.components.NovaLikeBurst
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch


@Composable
fun NovaPostCard(
    post: NovaPost,
    isDeleting: Boolean,
    isLiking: Boolean,
    onAuthorClick: () -> Unit,
    onLikeToggle: () -> Unit,
    onCommentsClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val sharingRepository = remember(context) {
        NovaSharingRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var showDeleteConfirm by remember(post.id) { mutableStateOf(false) }
    var showShare by remember(post.id) { mutableStateOf(false) }
    var repostState by remember(post.id) { mutableStateOf<NovaRepostState?>(null) }
    var repostBusy by remember(post.id) { mutableStateOf(false) }
    var repostError by remember(post.id) { mutableStateOf<String?>(null) }
    var hiddenFromFeed by remember(post.id) { mutableStateOf(false) }
    var likeBurstTrigger by remember(post.id) { mutableIntStateOf(0) }

    LaunchedEffect(post.id) {
        when (val result = sharingRepository.repostState(post.id)) {
            is ApiResult.Success -> repostState = result.value
            is ApiResult.Failure -> Unit
        }
    }

    fun toggleRepost() {
        if (repostBusy) return
        scope.launch {
            repostBusy = true
            repostError = null
            val current = when (val known = repostState) {
                null -> when (val loaded = sharingRepository.repostState(post.id)) {
                    is ApiResult.Success -> loaded.value.also { repostState = it }
                    is ApiResult.Failure -> {
                        repostBusy = false
                        repostError = loaded.message
                        return@launch
                    }
                }
                else -> known
            }

            when (
                val result = sharingRepository.setReposted(
                    postId = post.id,
                    reposted = !current.isReposted,
                )
            ) {
                is ApiResult.Success -> {
                    repostState = result.value
                    hiddenFromFeed = !result.value.stillInFeed
                }
                is ApiResult.Failure -> repostError = result.message
            }
            repostBusy = false
        }
    }

    if (hiddenFromFeed) return

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
            title = "Share this post",
            postId = post.id,
            onDismiss = { showShare = false },
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column {
            repostState?.feedRepostedBy?.let { reposter ->
                Text(
                    text = "↻ @${reposter.username} reposted",
                    modifier = Modifier.padding(
                        start = NovaSpacing.lg,
                        end = NovaSpacing.lg,
                        top = NovaSpacing.md,
                    ),
                    color = NovaMuted,
                    style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(onClick = onAuthorClick, shape = CircleShape, color = NovaSurface) {
                    NovaAvatar(
                        source = post.author.avatarUrl,
                        fallbackText = post.author.name.ifBlank { post.author.username },
                        size = 38.dp,
                    )
                }
                Surface(
                    onClick = onAuthorClick,
                    modifier = Modifier.weight(1f),
                    color = NovaSurface,
                ) {
                    Column {
                        Text(
                            post.author.name.ifBlank { post.author.username },
                            color = NovaInk,
                            style = NovaType.meta.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${friendlyDate(post.createdAt)} · @${post.author.username}",
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
                        shape = CircleShape,
                        color = NovaSurface,
                    ) {
                        Text(
                            if (isDeleting) "…" else "•••",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            color = NovaMuted,
                            style = NovaType.label,
                        )
                    }
                }
            }

            if (post.caption.isNotBlank()) {
                Text(
                    text = post.caption,
                    modifier = Modifier.padding(
                        start = NovaSpacing.lg,
                        end = NovaSpacing.lg,
                        bottom = NovaSpacing.md,
                    ),
                    color = NovaInk,
                    style = NovaType.bodyCompact,
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = NovaSpacing.md)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium)
                    .pointerInput(post.id, post.isLiked, isLiking) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!isLiking) {
                                    likeBurstTrigger += 1
                                    if (!post.isLiked) onLikeToggle()
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                NovaMediaImage(
                    source = post.imageUrl,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentDescription = "Post by ${post.author.username}",
                )
                NovaLikeBurst(
                    trigger = likeBurstTrigger,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PostAction(
                    label = when {
                        isLiking -> "…"
                        post.isLiked -> "♥ ${post.likesCount}"
                        else -> "♡ ${post.likesCount}"
                    },
                    active = post.isLiked,
                    onClick = { if (!isLiking) onLikeToggle() },
                )
                PostAction(
                    label = "◯ ${post.commentsCount}",
                    onClick = onCommentsClick,
                )
                val currentRepost = repostState
                PostAction(
                    label = when {
                        repostBusy -> "↻ …"
                        currentRepost == null -> "↻"
                        else -> "↻ ${currentRepost.repostsCount}"
                    },
                    active = currentRepost?.isReposted == true,
                    onClick = ::toggleRepost,
                )
                PostAction(label = "↗", onClick = { showShare = true })
                Spacer(modifier = Modifier.weight(1f))
                Text("▱", color = NovaMuted, style = NovaType.title)
            }

            if (!repostError.isNullOrBlank()) {
                Text(
                    text = repostError.orEmpty(),
                    modifier = Modifier.padding(
                        start = NovaSpacing.lg,
                        end = NovaSpacing.lg,
                        bottom = NovaSpacing.md,
                    ),
                    color = NovaMuted,
                    style = NovaType.micro,
                )
            } else {
                Spacer(modifier = Modifier.height(NovaSpacing.xs))
            }
        }
    }
}


@Composable
private fun PostAction(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (active) NovaAccentSoft else NovaSurface,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            color = if (active) NovaAccent else NovaMuted,
            style = NovaType.meta.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}


private fun friendlyDate(raw: String): String {
    if (raw.isBlank()) return "now"
    return runCatching {
        val date = OffsetDateTime.parse(raw)
        date.format(DateTimeFormatter.ofPattern("MMM d"))
    }.getOrDefault("now")
}
