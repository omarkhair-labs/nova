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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nova.app.core.network.ApiResult
import com.nova.app.core.reels.NovaProfileReelsRepository
import com.nova.app.core.reels.NovaReel
import com.nova.app.core.reels.NovaReelsRepository
import com.nova.app.feature.sharing.NovaShareDialog
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.launch


private val ProfileViewerBackground = Color(0xFF050608)
private val ProfileViewerInk = Color(0xFFF8F8FA)
private val ProfileViewerMuted = Color(0xFFC3C5CA)


@Composable
fun ProfileReelsViewerScreen(
    username: String,
    initialReelId: Long,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val profileRepository = remember(context) {
        NovaProfileReelsRepository(context.applicationContext)
    }
    val interactionRepository = remember(context) {
        NovaReelsRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var reels by remember(username) { mutableStateOf<List<NovaReel>>(emptyList()) }
    var nextCursor by remember(username) { mutableStateOf<String?>(null) }
    var loading by remember(username, initialReelId) { mutableStateOf(true) }
    var loadingMore by remember(username) { mutableStateOf(false) }
    var error by remember(username) { mutableStateOf<String?>(null) }
    var likingId by remember(username) { mutableStateOf<Long?>(null) }
    var repostingId by remember(username) { mutableStateOf<Long?>(null) }
    var deletingId by remember(username) { mutableStateOf<Long?>(null) }
    var commentsReel by remember(username) { mutableStateOf<NovaReel?>(null) }
    var shareReelTarget by remember(username) { mutableStateOf<NovaReel?>(null) }
    var deleteReelTarget by remember(username) { mutableStateOf<NovaReel?>(null) }

    val overlayOpen = commentsReel != null || shareReelTarget != null || deleteReelTarget != null

    fun replaceReel(updated: NovaReel) {
        reels = reels.map { existing -> if (existing.id == updated.id) updated else existing }
        if (commentsReel?.id == updated.id) commentsReel = updated
        if (shareReelTarget?.id == updated.id) shareReelTarget = updated
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (loadingMore) return
        scope.launch {
            loadingMore = true
            error = null
            when (val result = profileRepository.reels(username, cursor)) {
                is ApiResult.Success -> {
                    val existingIds = reels.mapTo(mutableSetOf()) { it.id }
                    reels = reels + result.value.reels.filterNot { it.id in existingIds }
                    nextCursor = result.value.nextCursor
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onFinish() else error = result.message
                }
            }
            loadingMore = false
        }
    }

    fun toggleLike(reel: NovaReel) {
        if (likingId != null) return
        scope.launch {
            likingId = reel.id
            when (val result = interactionRepository.setLiked(reel.id, !reel.isLiked)) {
                is ApiResult.Success -> replaceReel(result.value)
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onFinish() else error = result.message
                }
            }
            likingId = null
        }
    }

    fun toggleRepost(reel: NovaReel) {
        if (repostingId != null) return
        scope.launch {
            repostingId = reel.id
            when (val result = interactionRepository.setReposted(reel.id, !reel.isReposted)) {
                is ApiResult.Success -> replaceReel(result.value)
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onFinish() else error = result.message
                }
            }
            repostingId = null
        }
    }

    fun deleteReel(reel: NovaReel) {
        if (!reel.isMine || deletingId != null) return
        scope.launch {
            deletingId = reel.id
            error = null
            when (val result = interactionRepository.deleteReel(reel.id)) {
                is ApiResult.Success -> {
                    reels = reels.filterNot { it.id == reel.id }
                    if (commentsReel?.id == reel.id) commentsReel = null
                    if (shareReelTarget?.id == reel.id) shareReelTarget = null
                    deleteReelTarget = null
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onFinish() else error = result.message
                }
            }
            deletingId = null
        }
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

    LaunchedEffect(username, initialReelId) {
        loading = true
        error = null
        reels = emptyList()
        nextCursor = null

        var cursor: String? = null
        var loadedPages = 0
        var aggregate = emptyList<NovaReel>()

        while (loadedPages < MAX_INITIAL_LOOKUP_PAGES) {
            when (val result = profileRepository.reels(username, cursor)) {
                is ApiResult.Success -> {
                    val existingIds = aggregate.mapTo(mutableSetOf()) { it.id }
                    aggregate = aggregate + result.value.reels.filterNot { it.id in existingIds }
                    reels = aggregate
                    nextCursor = result.value.nextCursor
                    cursor = result.value.nextCursor
                    loadedPages += 1
                    if (aggregate.any { it.id == initialReelId } || cursor == null) break
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) {
                        onFinish()
                    } else {
                        error = result.message
                    }
                    break
                }
            }
        }
        loading = false
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
                    modifier = Modifier.align(Alignment.TopStart),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                ) {
                    Text(
                        text = "‹",
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 7.dp),
                        color = ProfileViewerInk,
                        fontSize = 27.sp,
                    )
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
                }
            }
        }

        else -> {
            val initialIndex = reels.indexOfFirst { it.id == initialReelId }.coerceAtLeast(0)
            val pagerState = rememberPagerState(
                initialPage = initialIndex,
                pageCount = { reels.size },
            )

            LaunchedEffect(pagerState.currentPage, reels.size, nextCursor, loadingMore) {
                if (
                    pagerState.currentPage >= reels.lastIndex - 2 &&
                    nextCursor != null &&
                    !loadingMore
                ) {
                    loadMore()
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
                    ProfileViewerReelPage(
                        reel = reel,
                        isActive = pagerState.currentPage == page && !overlayOpen,
                        isLiking = likingId == reel.id,
                        isReposting = repostingId == reel.id,
                        isDeleting = deletingId == reel.id,
                        onLike = { toggleLike(reel) },
                        onComments = { commentsReel = reel },
                        onRepost = { toggleRepost(reel) },
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
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        Text(
                            text = "‹",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            color = ProfileViewerInk,
                            fontSize = 27.sp,
                        )
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
                        onClick = { error = null },
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
            onReelUpdated = ::replaceReel,
            onPersonClick = {},
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
            onDelete = { deleteReel(reel) },
        )
    }
}


@Composable
private fun ProfileViewerReelPage(
    reel: NovaReel,
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
    val context = LocalContext.current
    var pausedByUser by remember(reel.id) { mutableStateOf(false) }
    var muted by remember(reel.id) { mutableStateOf(false) }
    val player = remember(reel.id, reel.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(reel.videoUrl))
            prepare()
        }
    }

    LaunchedEffect(isActive, pausedByUser) {
        if (isActive && !pausedByUser) player.play() else player.pause()
    }
    LaunchedEffect(muted) {
        player.volume = if (muted) 0f else 1f
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProfileViewerBackground)
            .combinedClickable(
                onClick = { pausedByUser = !pausedByUser },
                onDoubleClick = {
                    // Double-tap always means Like. Repeating it never unlikes the Reel.
                    if (!reel.isLiked && !isLiking) onLike()
                },
            ),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.player = player
                }
            },
            update = { it.player = player },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.06f)),
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
                    Text("▶", color = ProfileViewerInk, fontSize = 25.sp)
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
            ProfileViewerAction(
                symbol = if (reel.isLiked) "♥" else "♡",
                label = reel.likesCount.toString(),
                active = reel.isLiked,
                busy = isLiking,
                onClick = onLike,
            )
            ProfileViewerAction(
                symbol = "◌",
                label = reel.commentsCount.toString(),
                onClick = onComments,
            )
            ProfileViewerAction(
                symbol = "↻",
                label = reel.repostsCount.toString(),
                active = reel.isReposted,
                busy = isReposting,
                onClick = onRepost,
            )
            ProfileViewerAction(
                symbol = "↗",
                label = "Share",
                onClick = onShare,
            )
            if (reel.isMine) {
                ProfileViewerAction(
                    symbol = "×",
                    label = "Delete",
                    busy = isDeleting,
                    onClick = onDelete,
                )
            }
            ProfileViewerAction(
                symbol = if (muted) "×" else "♪",
                label = if (muted) "Muted" else "Sound",
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
private fun ProfileViewerAction(
    symbol: String,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    busy: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = { if (!busy) onClick() },
            modifier = Modifier.size(45.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.44f),
            border = BorderStroke(
                1.dp,
                if (active) NovaAccent.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.14f),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ProfileViewerInk,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = symbol,
                        color = if (active) NovaAccent else ProfileViewerInk,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = ProfileViewerInk,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
        )
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


private const val MAX_INITIAL_LOOKUP_PAGES = 20