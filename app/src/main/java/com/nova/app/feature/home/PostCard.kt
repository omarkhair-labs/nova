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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.nova.app.ui.theme.NovaSurface
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
        shape = RoundedCornerShape(26.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column {
            repostState?.feedRepostedBy?.let { reposter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = "↻",
                        color = NovaAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "@${reposter.username} reposted",
                        color = NovaMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Surface(
                    onClick = onAuthorClick,
                    shape = RoundedCornerShape(24.dp),
                    color = NovaSurface,
                ) {
                    NovaAvatar(
                        source = post.author.avatarUrl,
                        fallbackText = post.author.name.ifBlank { post.author.username },
                        size = 42.dp,
                    )
                }

                Surface(
                    onClick = onAuthorClick,
                    modifier = Modifier.weight(1f),
                    color = NovaSurface,
                ) {
                    Column {
                        Text(
                            text = post.author.name.ifBlank { post.author.username },
                            color = NovaInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "@${post.author.username} · ${friendlyDate(post.createdAt)}",
                            color = NovaMuted,
                            fontSize = 11.sp,
                        )
                    }
                }

                if (post.isMine) {
                    Surface(
                        onClick = {
                            if (!isDeleting) showDeleteConfirm = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = NovaAccentSoft,
                    ) {
                        Text(
                            text = if (isDeleting) "…" else "Delete",
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            color = NovaAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(post.id, post.isLiked, isLiking) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!isLiking) {
                                    likeBurstTrigger += 1
                                    if (!post.isLiked) onLikeToggle()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                NovaMediaImage(
                    source = post.imageUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(0.dp)),
                    contentDescription = "Post by ${post.author.username}",
                )
                NovaLikeBurst(
                    trigger = likeBurstTrigger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = { if (!isLiking) onLikeToggle() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (post.isLiked) NovaAccentSoft else NovaSurface,
                    border = BorderStroke(1.dp, if (post.isLiked) NovaAccent.copy(alpha = 0.22f) else NovaBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isLiking) "…" else if (post.isLiked) "♥ ${post.likesCount}" else "♡ ${post.likesCount}",
                            color = if (post.isLiked) NovaAccent else NovaInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Surface(
                    onClick = onCommentsClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Text(
                        text = "Comment · ${post.commentsCount}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        color = NovaMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val state = repostState
                Surface(
                    onClick = ::toggleRepost,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (state?.isReposted == true) NovaAccentSoft else NovaSurface,
                    border = BorderStroke(
                        1.dp,
                        if (state?.isReposted == true) NovaAccent.copy(alpha = 0.28f) else NovaBorder,
                    ),
                ) {
                    Text(
                        text = when {
                            repostBusy -> "↻ Updating…"
                            state == null -> "↻ Repost"
                            state.isReposted -> "↻ Reposted · ${state.repostsCount}"
                            else -> "↻ Repost · ${state.repostsCount}"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        color = if (state?.isReposted == true) NovaAccent else NovaMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Surface(
                    onClick = { showShare = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Text(
                        text = "↗ Share",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        color = NovaMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (!repostError.isNullOrBlank()) {
                Text(
                    text = repostError.orEmpty(),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                    color = NovaMuted,
                    fontSize = 10.sp,
                )
            }

            if (post.caption.isNotBlank()) {
                Text(
                    text = post.caption,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 15.dp),
                    color = NovaInk,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}

private fun friendlyDate(raw: String): String {
    if (raw.isBlank()) return "now"
    return runCatching {
        val date = OffsetDateTime.parse(raw)
        date.format(DateTimeFormatter.ofPattern("MMM d"))
    }.getOrDefault("now")
}
