package com.nova.app.feature.reels

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
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
import com.nova.app.core.reels.NovaReel
import com.nova.app.core.reels.NovaReelComment
import com.nova.app.core.reels.NovaReelsRepository
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.launch


private val ReelBackground = Color(0xFF050608)
private val ReelInk = Color(0xFFF8F8FA)
private val ReelMuted = Color(0xFFC3C5CA)


@Composable
fun ReelsScreen(
    onFinish: () -> Unit,
    onHomeClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onProfileClick: () -> Unit,
    onPersonClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { NovaReelsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var reels by remember { mutableStateOf<List<NovaReel>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var likingId by remember { mutableStateOf<Long?>(null) }
    var repostingId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingVideo by remember { mutableStateOf<Uri?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var commentsReel by remember { mutableStateOf<NovaReel?>(null) }

    fun replaceReel(updated: NovaReel) {
        reels = reels.map { if (it.id == updated.id) updated else it }
        if (commentsReel?.id == updated.id) commentsReel = updated
    }

    fun load(reset: Boolean) {
        if (reset) {
            if (loading && reels.isNotEmpty()) return
        } else if (loadingMore || nextCursor == null) {
            return
        }
        val cursor = if (reset) null else nextCursor
        scope.launch {
            if (reset) loading = true else loadingMore = true
            error = null
            when (val result = repository.reels(cursor)) {
                is ApiResult.Success -> {
                    reels = if (reset) {
                        result.value.reels
                    } else {
                        val existing = reels.mapTo(mutableSetOf()) { it.id }
                        reels + result.value.reels.filterNot { it.id in existing }
                    }
                    nextCursor = result.value.nextCursor
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onFinish() else error = result.message
                }
            }
            loading = false
            loadingMore = false
        }
    }

    fun toggleLike(reel: NovaReel) {
        if (likingId != null) return
        scope.launch {
            likingId = reel.id
            when (val result = repository.setLiked(reel.id, !reel.isLiked)) {
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
            when (val result = repository.setReposted(reel.id, !reel.isReposted)) {
                is ApiResult.Success -> replaceReel(result.value)
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onFinish() else error = result.message
                }
            }
            repostingId = null
        }
    }

    fun shareReel(reel: NovaReel) {
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
            pendingVideo = uri
            error = null
        }
    }

    LaunchedEffect(Unit) { load(reset = true) }

    Scaffold(
        containerColor = ReelBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Reels,
                onHomeClick = onHomeClick,
                onPeopleClick = onPeopleClick,
                onProfileClick = onProfileClick,
                onReelsClick = {},
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
                            text = error ?: "Nova V3 starts here. Share the first Reel.",
                            color = ReelMuted,
                            fontSize = 14.sp,
                        )
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
                                onClick = { load(reset = true) },
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

                LaunchedEffect(pagerState.currentPage, reels.size, nextCursor, loadingMore) {
                    if (
                        pagerState.currentPage >= reels.lastIndex - 3 &&
                        nextCursor != null &&
                        !loadingMore
                    ) {
                        load(reset = false)
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
                        ReelPage(
                            reel = reel,
                            isActive = pagerState.currentPage == page,
                            isLiking = likingId == reel.id,
                            isReposting = repostingId == reel.id,
                            onLike = { toggleLike(reel) },
                            onComments = { commentsReel = reel },
                            onRepost = { toggleRepost(reel) },
                            onShare = { shareReel(reel) },
                            onAuthor = { onPersonClick(reel.author.username) },
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
                            Text(
                                text = if (uploading) "Posting…" else "+ Create",
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                color = ReelInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
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
                            onClick = { error = null },
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
                }
            }
        }
    }

    pendingVideo?.let { uri ->
        ReelComposerDialog(
            videoUri = uri,
            uploading = uploading,
            onDismiss = { if (!uploading) pendingVideo = null },
            onPost = { caption ->
                scope.launch {
                    uploading = true
                    error = null
                    when (val result = repository.createReel(uri, caption)) {
                        is ApiResult.Success -> {
                            reels = listOf(result.value) + reels.filterNot { it.id == result.value.id }
                            pendingVideo = null
                        }
                        is ApiResult.Failure -> {
                            if (result.statusCode == 401) onFinish() else error = result.message
                        }
                    }
                    uploading = false
                }
            },
        )
    }

    commentsReel?.let { reel ->
        ThreadedReelCommentsSheet(
            reel = reel,
            repository = repository,
            onDismiss = { commentsReel = null },
            onReelUpdated = ::replaceReel,
            onPersonClick = onPersonClick,
            onSessionExpired = onFinish,
        )
    }
}


@Composable
private fun ReelPage(
    reel: NovaReel,
    isActive: Boolean,
    isLiking: Boolean,
    isReposting: Boolean,
    onLike: () -> Unit,
    onComments: () -> Unit,
    onRepost: () -> Unit,
    onShare: () -> Unit,
    onAuthor: () -> Unit,
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
            .background(ReelBackground)
            .clickable { pausedByUser = !pausedByUser },
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
                color = Color.Black.copy(alpha = 0.46f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("▶", color = ReelInk, fontSize = 26.sp)
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
            ReelAction(
                symbol = if (reel.isLiked) "♥" else "♡",
                label = reel.likesCount.toString(),
                active = reel.isLiked,
                busy = isLiking,
                onClick = onLike,
            )
            ReelAction(
                symbol = "◌",
                label = reel.commentsCount.toString(),
                onClick = onComments,
            )
            ReelAction(
                symbol = "↻",
                label = reel.repostsCount.toString(),
                active = reel.isReposted,
                busy = isReposting,
                onClick = onRepost,
            )
            ReelAction(
                symbol = "↗",
                label = "Share",
                onClick = onShare,
            )
            ReelAction(
                symbol = if (muted) "×" else "♪",
                label = if (muted) "Muted" else "Sound",
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
                Text(
                    text = "↻ @${reposter.username} reposted",
                    color = ReelMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
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
private fun ReelAction(
    symbol: String,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    busy: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            enabled = !busy,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.38f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ReelInk,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = symbol,
                        color = if (active) NovaAccent else ReelInk,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = ReelInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
                    Text("V3", color = NovaAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ReelBackground),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            VideoView(viewContext).apply {
                                setVideoURI(videoUri)
                                setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = true
                                    start()
                                }
                            }
                        },
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
                            "Cancel",
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
                                Text("Post Reel", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ReelCommentsSheet(
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
    var body by remember(reel.id) { mutableStateOf("") }
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
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                NovaAvatar(
                                    source = comment.author.avatarUrl,
                                    fallbackText = comment.author.displayName,
                                    size = 34.dp,
                                    modifier = Modifier.clickable {
                                        onPersonClick(comment.author.username)
                                    },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "@${comment.author.username}",
                                        color = NovaInk,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            onPersonClick(comment.author.username)
                                        },
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = comment.body,
                                        color = NovaInk,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
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
                    placeholder = { Text("Add a comment…", color = NovaMuted) },
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
                            scope.launch {
                                sending = true
                                error = null
                                when (val result = repository.addComment(reel.id, body)) {
                                    is ApiResult.Success -> {
                                        comments = comments + result.value.comment
                                        onReelUpdated(result.value.reel)
                                        body = ""
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
