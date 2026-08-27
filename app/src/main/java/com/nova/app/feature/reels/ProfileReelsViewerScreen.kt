package com.nova.app.feature.reels

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.nova.app.app.appContainer
import com.nova.app.feature.reels.domain.model.NovaReel
import com.nova.app.feature.sharing.NovaShareDialog
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaImmersiveAction
import com.nova.app.ui.components.NovaLikeBurst
import com.nova.app.ui.components.NovaPlayerSurface
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


private val ProfileViewerBackground = Color(0xFF050608)
private val ProfileViewerInk = Color(0xFFF8F8FA)
private val ProfileViewerMuted = Color(0xFFC3C5CA)


@Composable
fun ProfileReelsViewerScreen(
    username: String,
    initialReelId: Long,
    onFinish: () -> Unit,
    onPersonClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = context.appContainer
    val profileRepository = appContainer.profileReelsRepository
    val interactionRepository = appContainer.reelsRepository
    val scope = rememberCoroutineScope()
    val owner = remember(
        username,
        initialReelId,
        profileRepository,
        interactionRepository,
        scope,
    ) {
        ProfileReelsViewerStateOwner(
            username = username,
            initialReelId = initialReelId,
            profileRepository = profileRepository,
            interactionRepository = interactionRepository,
            scope = scope,
        )
    }
    val state = owner.state
    val reels = state.reels
    val nextCursor = state.nextCursor
    val loading = state.loading
    val loadingMore = state.loadingMore
    val error = state.error
    val likingId = state.likingId
    val repostingId = state.repostingId
    val deletingId = state.deletingId
    val playerPool = remember(context) { ReelPlayerPool(context.applicationContext) }

    var commentsReel by remember(username) { mutableStateOf<NovaReel?>(null) }
    var shareReelTarget by remember(username) { mutableStateOf<NovaReel?>(null) }
    var deleteReelTarget by remember(username) { mutableStateOf<NovaReel?>(null) }

    val overlayOpen = commentsReel != null || shareReelTarget != null || deleteReelTarget != null

    DisposableEffect(playerPool) {
        onDispose { playerPool.releaseAll() }
    }

    LaunchedEffect(owner) {
        owner.loadInitial()
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onFinish()
    }
    LaunchedEffect(state.deletedVersion) {
        if (state.deletedVersion > 0) {
            val deletedId = deleteReelTarget?.id
            if (deletedId != null) {
                if (commentsReel?.id == deletedId) commentsReel = null
                if (shareReelTarget?.id == deletedId) shareReelTarget = null
            }
            deleteReelTarget = null
        }
    }
    LaunchedEffect(reels) {
        commentsReel = commentsReel?.let { target ->
            reels.firstOrNull { it.id == target.id } ?: target
        }
        shareReelTarget = shareReelTarget?.let { target ->
            reels.firstOrNull { it.id == target.id } ?: target
        }
    }

    fun replaceOverlayReel(updated: NovaReel) {
        owner.replaceReel(updated)
        if (commentsReel?.id == updated.id) commentsReel = updated
        if (shareReelTarget?.id == updated.id) shareReelTarget = updated
    }

    fun shareReelOutsideNova(reel: NovaReel) {
        val shareText = buildString {
            append("Watch @${reel.author.username} on Nova")
            if (reel.caption.isNotBlank()) append("\n\n${reel.caption}")
            if (reel.videoUrl.isNotBlank()) append("\n\n${reel.videoUrl}")
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                },
                "Share Reel",
            )
        )
    }

    when {
        loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ProfileViewerBackground),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NovaAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Opening @$username's Reels…",
                        color = ProfileViewerMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        reels.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ProfileViewerBackground)
                    .statusBarsPadding()
                    .padding(22.dp),
            ) {
                Surface(
                    onClick = onFinish,
                    modifier = Modifier.align(Alignment.TopStart).size(48.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        NovaIcon(
                            asset = NovaIconAsset.Back,
                            contentDescription = "Back",
                            modifier = Modifier.size(23.dp),
                            tint = ProfileViewerInk,
                        )
                    }
                }
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No Reels available",
                        color = ProfileViewerInk,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = error ?: "These Reels may no longer be visible.",
                        color = ProfileViewerMuted,
                        fontSize = 12.sp,
                    )
                    if (error != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            onClick = owner::loadInitial,
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = "Try again",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                color = ProfileViewerInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        else -> {
            val initialIndex = reels.indexOfFirst { it.id == initialReelId }.coerceAtLeast(0)
            val pagerState = rememberPagerState(
                initialPage = initialIndex,
                pageCount = { reels.size },
            )
            val reelIdentity = reels.map { it.id to it.videoUrl }

            LaunchedEffect(pagerState.currentPage, reelIdentity, overlayOpen) {
                val center = pagerState.currentPage.coerceIn(0, reels.lastIndex)
                playerPool.retainAround(reels, center)
                playerPool.pauseAllExcept(reels.getOrNull(center)?.id?.takeUnless { overlayOpen })
            }

            LaunchedEffect(pagerState.currentPage, reels.size, nextCursor, loadingMore) {
                if (
                    pagerState.currentPage >= reels.lastIndex - 2 &&
                    nextCursor != null &&
                    !loadingMore
                ) {
                    owner.loadMore()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ProfileViewerBackground),
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { index -> reels[index].id },
                ) { page ->
                    val reel = reels[page]
                    val player = remember(reel.id, reel.videoUrl) { playerPool.playerFor(reel) }
                    ProfileViewerReelPage(
                        reel = reel,
                        player = player,
                        isActive = pagerState.currentPage == page && !overlayOpen,
                        isLiking = likingId == reel.id,
                        isReposting = repostingId == reel.id,
                        isDeleting = deletingId == reel.id,
                        onLike = { owner.toggleLike(reel) },
                        onComments = { commentsReel = reel },
                        onRepost = { owner.toggleRepost(reel) },
                        onShare = { shareReelTarget = reel },
                        onDelete = { deleteReelTarget = reel },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        onClick = onFinish,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            NovaIcon(
                                asset = NovaIconAsset.Back,
                                contentDescription = "Back",
                                modifier = Modifier.size(23.dp),
                                tint = ProfileViewerInk,
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.44f),
                    ) {
                        Text(
                            text = "@$username · Reels",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = ProfileViewerInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                if (loadingMore) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .size(22.dp),
                        color = NovaAccent,
                        strokeWidth = 2.dp,
                    )
                }

                error?.let { message ->
                    Surface(
                        onClick = owner::clearError,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 60.dp, start = 20.dp, end = 20.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                            color = ProfileViewerInk,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }

    commentsReel?.let { reel ->
        ThreadedReelCommentsSheet(
            reel = reel,
            repository = interactionRepository,
            onDismiss = { commentsReel = null },
            onReelUpdated = ::replaceOverlayReel,
            onPersonClick = onPersonClick,
            onSessionExpired = onFinish,
        )
    }

    shareReelTarget?.let { reel ->
        NovaShareDialog(
            title = "Share this Reel",
            reelId = reel.id,
            onExternalShare = { shareReelOutsideNova(reel) },
            onDismiss = { shareReelTarget = null },
        )
    }

    deleteReelTarget?.let { reel ->
        ProfileReelDeleteDialog(
            deleting = deletingId == reel.id,
            onDismiss = { if (deletingId == null) deleteReelTarget = null },
            onDelete = { owner.deleteReel(reel) },
        )
    }
}


@Composable
private fun ProfileViewerReelPage(
    reel: NovaReel,
    player: ExoPlayer,
    isActive: Boolean,
    isLiking: Boolean,
    isReposting: Boolean,
    isDeleting: Boolean,
    onLike: () -> Unit,
    onComments: () -> Unit,
    onRepost: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var pausedByUser by remember(reel.id) { mutableStateOf(false) }
    var muted by remember(reel.id) { mutableStateOf(false) }
    var likeBurstTrigger by remember(reel.id) { mutableIntStateOf(0) }

    LaunchedEffect(isActive, pausedByUser) {
        if (isActive && !pausedByUser) player.play() else player.pause()
    }
    LaunchedEffect(muted) {
        player.volume = if (muted) 0f else 1f
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileViewerBackground)
            .combinedClickable(
                onClick = { pausedByUser = !pausedByUser },
                onDoubleClick = {
                    // Double-tap always means Like. Repeating it never unlikes the Reel.
                    if (!isLiking) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        likeBurstTrigger += 1
                        if (!reel.isLiked) onLike()
                    }
                },
            ),
    ) {
        NovaPlayerSurface(
            player = player,
            thumbnailSource = reel.thumbnailUrl,
            modifier = Modifier.fillMaxSize(),
            useController = false,
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            description = "Reel by ${reel.author.username}",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.06f)),
        )

        NovaLikeBurst(
            trigger = likeBurstTrigger,
            modifier = Modifier.fillMaxSize(),
        )

        if (pausedByUser) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(62.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.48f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NovaIcon(
                        asset = NovaIconAsset.Play,
                        contentDescription = "Resume Reel",
                        modifier = Modifier.size(28.dp),
                        tint = ProfileViewerInk,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 27.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NovaImmersiveAction(
                icon = if (reel.isLiked) NovaIconAsset.LikeFilled else NovaIconAsset.Like,
                contentDescription = if (reel.isLiked) "Unlike Reel" else "Like Reel",
                label = reel.likesCount.toString(),
                active = reel.isLiked,
                busy = isLiking,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLike()
                },
            )
            NovaImmersiveAction(
                icon = NovaIconAsset.Comment,
                contentDescription = "Open Reel comments",
                label = reel.commentsCount.toString(),
                onClick = onComments,
            )
            NovaImmersiveAction(
                icon = NovaIconAsset.Repost,
                contentDescription = if (reel.isReposted) "Remove Reel repost" else "Repost Reel",
                label = reel.repostsCount.toString(),
                active = reel.isReposted,
                busy = isReposting,
                onClick = onRepost,
            )
            NovaImmersiveAction(
                icon = NovaIconAsset.Share,
                contentDescription = "Share Reel",
                label = "Share",
                onClick = onShare,
            )
            if (reel.isMine) {
                NovaImmersiveAction(
                    icon = NovaIconAsset.Delete,
                    contentDescription = "Delete Reel",
                    label = "Delete",
                    busy = isDeleting,
                    onClick = onDelete,
                )
            }
            NovaImmersiveAction(
                icon = if (muted) NovaIconAsset.VolumeOff else NovaIconAsset.VolumeOn,
                contentDescription = if (muted) "Unmute Reel" else "Mute Reel",
                label = if (muted) "Muted" else "Sound",
                active = !muted,
                onClick = { muted = !muted },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, end = 10.dp, bottom = 28.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                NovaAvatar(
                    source = reel.author.avatarUrl,
                    fallbackText = reel.author.displayName,
                    size = 34.dp,
                )
                Column {
                    Text(
                        text = reel.author.displayName,
                        color = ProfileViewerInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = "@${reel.author.username}",
                        color = ProfileViewerMuted,
                        fontSize = 10.sp,
                    )
                }
            }
            if (reel.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(9.dp))
                Text(
                    text = reel.caption,
                    color = ProfileViewerInk,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


@Composable
private fun ProfileReelDeleteDialog(
    deleting: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(onDismissRequest = { if (!deleting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Delete Reel?", color = NovaInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    "This Reel and its interactions will be permanently removed from Nova.",
                    color = NovaMuted,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        onClick = onDismiss,
                        enabled = !deleting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(15.dp),
                        color = NovaBackground,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Text(
                            "Cancel",
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = NovaInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    Surface(
                        onClick = onDelete,
                        enabled = !deleting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(15.dp),
                        color = NovaAccent,
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (deleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Delete", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
