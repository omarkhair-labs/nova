package com.nova.app.feature.reels

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.nova.app.feature.publishing.MediaPublishStatus
import com.nova.app.feature.publishing.MediaPublishTarget
import com.nova.app.feature.publishing.MediaPublishWorker
import com.nova.app.feature.publishing.MediaPublishingStateOwner
import com.nova.app.feature.sharing.NovaShareDialog
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaLikeBurst
import com.nova.app.ui.components.NovaImmersiveAction
import com.nova.app.ui.components.NovaPlayerSurface
import com.nova.app.ui.components.NovaVideoPlayer
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay


private val ReelBackground = Color(0xFF050608)
private val ReelInk = Color(0xFFF8F8FA)
private val ReelMuted = Color(0xFFC3C5CA)


@Composable
fun ReelsScreen(
    onFinish: () -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
    onPersonClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val appContainer = context.appContainer
    val repository = appContainer.reelsRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, appContainer.reelWatchRepository, scope) {
        ReelsStateOwner(
            repository = repository,
            watchRepository = appContainer.reelWatchRepository,
            scope = scope,
        )
    }
    val state = owner.state
    val publishingOwner = remember(context, scope) { MediaPublishingStateOwner(context, scope) }
    val publishingState = publishingOwner.state
    val currentUserId = appContainer.currentCachedUserId()
    val reels = state.reels
    val nextCursor = state.nextCursor
    val loading = state.loading
    val loadingMore = state.loadingMore
    val likingId = state.likingId
    val repostingId = state.repostingId
    val deletingId = state.deletingId
    val error = state.error
    val uploading = state.uploading
    val playerPool = remember(context) { ReelPlayerPool(context.applicationContext) }

    var pendingVideo by remember { mutableStateOf<Uri?>(null) }
    var commentsReel by remember { mutableStateOf<NovaReel?>(null) }
    var shareReelTarget by remember { mutableStateOf<NovaReel?>(null) }
    var deleteReelTarget by remember { mutableStateOf<NovaReel?>(null) }

    val overlayOpen = pendingVideo != null || commentsReel != null || shareReelTarget != null || deleteReelTarget != null

    DisposableEffect(playerPool) {
        onDispose { playerPool.releaseAll() }
    }

    LaunchedEffect(Unit) {
        owner.load(reset = true)
        currentUserId?.let(publishingOwner::enter)
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onFinish()
    }
    LaunchedEffect(state.createdVersion) {
        if (state.createdVersion > 0) pendingVideo = null
    }
    LaunchedEffect(publishingState.reelPublishedVersion) {
        if (publishingState.reelPublishedVersion > 0) owner.load(reset = true)
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
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Reel"))
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            pendingVideo = uri
            owner.clearError()
        }
    }

    Scaffold(
        containerColor = ReelBackground,
        bottomBar = {
            NovaBottomBar(
                selected = null,
                onHomeClick = onHomeClick,
                onOrbitClick = onOrbitClick,
                onCreateClick = onCreateClick,
                onProfileClick = onProfileClick,
                containerColor = ReelBackground,
                inactiveContentColor = ReelMuted,
            )
        },
    ) { innerPadding ->
        when {
            loading && reels.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ReelBackground)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NovaAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading Reels…", color = ReelMuted, fontSize = 12.sp)
                    }
                }
            }

            reels.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ReelBackground)
                        .padding(innerPadding)
                        .statusBarsPadding()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Reels", color = ReelInk, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "No Reels yet. Share the first one.",
                            color = ReelMuted,
                            fontSize = 14.sp,
                        )
                        if (publishingState.items.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            MediaPublishStatus(
                                items = publishingState.items,
                                modifier = Modifier.fillMaxWidth(),
                                onRetry = publishingOwner::retry,
                                onCancel = publishingOwner::cancel,
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Surface(
                            onClick = { picker.launch(arrayOf("video/*")) },
                            shape = RoundedCornerShape(18.dp),
                            color = NovaAccent,
                        ) {
                            Text(
                                text = "Create Reel",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (error != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                onClick = { owner.load(reset = true) },
                                color = Color.Transparent,
                            ) {
                                Text("Try again", color = NovaAccent, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            else -> {
                val pagerState = rememberPagerState(pageCount = { reels.size })
                val reelIdentity = reels.map { it.id to it.videoUrl }

                LaunchedEffect(pagerState.currentPage, reelIdentity, overlayOpen) {
                    if (reels.isEmpty()) return@LaunchedEffect
                    val center = pagerState.currentPage.coerceIn(0, reels.lastIndex)
                    playerPool.retainAround(reels, center)
                    playerPool.pauseAllExcept(
                        reels.getOrNull(center)?.id?.takeUnless { overlayOpen }
                    )
                }

                LaunchedEffect(pagerState.currentPage, reels.size, nextCursor, loadingMore) {
                    if (
                        pagerState.currentPage >= reels.lastIndex - 3 &&
                        nextCursor != null &&
                        !loadingMore
                    ) {
                        owner.load(reset = false)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ReelBackground)
                        .padding(innerPadding),
                ) {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        key = { index -> reels[index].id },
                    ) { page ->
                        val reel = reels[page]
                        val player = remember(reel.id, reel.videoUrl) {
                            playerPool.playerFor(reel)
                        }
                        ReelPage(
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
                            onAuthor = { onPersonClick(reel.author.username) },
                            onWatchSession = { snapshot -> owner.recordWatch(reel, snapshot) },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Reels",
                            color = ReelInk,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(
                            onClick = { if (!uploading) picker.launch(arrayOf("video/*")) },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.42f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        ) {
                            Row(
                                modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                if (uploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = ReelInk,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    NovaIcon(
                                        asset = NovaIconAsset.Create,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = ReelInk,
                                    )
                                }
                                Text(
                                    text = if (uploading) "Posting…" else "Create",
                                    color = ReelInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    if (loadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 14.dp)
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
                                .padding(top = 58.dp, start = 20.dp, end = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = ReelInk,
                                fontSize = 12.sp,
                            )
                        }
                    }

                    if (publishingState.items.isNotEmpty()) {
                        MediaPublishStatus(
                            items = publishingState.items,
                            onRetry = publishingOwner::retry,
                            onCancel = publishingOwner::cancel,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 58.dp, start = 20.dp, end = 20.dp),
                        )
                    }
                }
            }
        }
    }

    pendingVideo?.let { uri ->
        ReelComposerDialog(
            videoUri = uri,
            uploading = false,
            onDismiss = { pendingVideo = null },
            onPost = { caption ->
                currentUserId?.let { userId ->
                    MediaPublishWorker.enqueue(
                        context = context,
                        target = MediaPublishTarget.REEL,
                        userId = userId,
                        sourceUri = uri,
                        caption = caption,
                    )
                    publishingOwner.enter(userId)
                    pendingVideo = null
                }
            },
        )
    }

    commentsReel?.let { reel ->
        ThreadedReelCommentsSheet(
            reel = reel,
            repository = repository,
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
        ReelDeleteDialog(
            reel = reel,
            deleting = deletingId == reel.id,
            onDismiss = { if (deletingId == null) deleteReelTarget = null },
            onDelete = { owner.deleteReel(reel) },
        )
    }
}


@Composable
private fun ReelPage(
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
    onAuthor: () -> Unit,
    onWatchSession: (ReelWatchSnapshot) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var pausedByUser by remember(reel.id) { mutableStateOf(false) }
    var muted by remember(reel.id) { mutableStateOf(false) }
    var likeBurstTrigger by remember(reel.id) { mutableIntStateOf(0) }

    LaunchedEffect(isActive, pausedByUser, player) {
        if (isActive && !pausedByUser) player.play() else player.pause()
    }
    LaunchedEffect(muted, player) {
        player.volume = if (muted) 0f else 1f
    }
    LaunchedEffect(isActive, reel.id, player) {
        if (!isActive) return@LaunchedEffect
        val watchSession = ReelWatchSession()
        try {
            while (true) {
                watchSession.sample(player)
                delay(250)
            }
        } finally {
            val snapshot = watchSession.finish(player)
            if (snapshot.watchedMs >= 250L) {
                onWatchSession(snapshot)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBackground)
            .combinedClickable(
                onClick = { pausedByUser = !pausedByUser },
                onDoubleClick = {
                    // The burst is visual feedback only; double-tap never unlikes.
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
                color = Color.Black.copy(alpha = 0.46f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NovaIcon(
                        asset = NovaIconAsset.Play,
                        contentDescription = "Resume Reel",
                        modifier = Modifier.size(28.dp),
                        tint = ReelInk,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 24.dp),
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
                .padding(start = 16.dp, end = 10.dp, bottom = 24.dp),
        ) {
            reel.repostedBy?.let { reposter ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    NovaIcon(
                        asset = NovaIconAsset.Repost,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ReelMuted,
                    )
                    Text(
                        text = "@${reposter.username} reposted",
                        color = ReelMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.clickable(onClick = onAuthor),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NovaAvatar(
                    source = reel.author.avatarUrl,
                    fallbackText = reel.author.displayName,
                    size = 38.dp,
                )
                Text(
                    text = "@${reel.author.username}",
                    color = ReelInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (reel.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(9.dp))
                Text(
                    text = reel.caption,
                    color = ReelInk,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


@Composable
private fun ReelDeleteDialog(
    reel: NovaReel,
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
                    text = if (reel.caption.isBlank()) {
                        "This Reel will be permanently removed from Nova."
                    } else {
                        "This Reel and its interactions will be permanently removed from Nova."
                    },
                    color = NovaMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
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


@Composable
private fun ReelComposerDialog(
    videoUri: Uri,
    uploading: Boolean,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit,
) {
    var caption by remember(videoUri) { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("New Reel", color = NovaInk, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    NovaIcon(
                        asset = NovaIconAsset.Reels,
                        contentDescription = null,
                        tint = NovaAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ReelBackground),
                ) {
                    NovaVideoPlayer(
                        source = videoUri.toString(),
                        modifier = Modifier.fillMaxSize(),
                        autoplay = true,
                        repeat = true,
                        muted = true,
                        useController = true,
                        description = "Selected Reel video preview",
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uploading,
                    placeholder = { Text("Write a caption…", color = NovaMuted) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                    ),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        onClick = onDismiss,
                        enabled = !uploading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = NovaBackground,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Text(
                            text = "Cancel",
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = NovaInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    Surface(
                        onClick = { if (!uploading) onPost(caption) },
                        enabled = !uploading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = NovaAccent,
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    "Post Reel",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
