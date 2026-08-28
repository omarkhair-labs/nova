package com.nova.app.feature.stories

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.nova.app.ReelsActivity
import com.nova.app.app.appContainer
import com.nova.app.core.push.NovaPushOpenSignal
import com.nova.app.feature.publishing.MediaPublishStatus
import com.nova.app.feature.publishing.MediaPublishTarget
import com.nova.app.feature.publishing.MediaPublishWorker
import com.nova.app.feature.publishing.MediaPublishingStateOwner
import com.nova.app.feature.stories.domain.model.NovaStory
import com.nova.app.feature.stories.domain.model.NovaStoryGroup
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.components.NovaPlayerSurface
import com.nova.app.ui.components.NovaVideoPlayer
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay


private val StoryBackground = Color(0xFF080A0F)
private val StoryInk = Color(0xFFF7F8FA)
private val StoryMuted = Color(0xFFB8BDC8)
private val StoryReactions = listOf("❤️", "😂", "😮", "😢", "🔥", "👏")
private const val STORY_FRAME_MS = 5_500L
private const val STORY_TICK_MS = 55L


@Composable
fun StoriesRail(
    displayName: String,
    username: String,
    avatarUrl: String,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.storiesRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { StoriesStateOwner(repository, scope) }
    val state = owner.state
    val publishingOwner = remember(context, scope) { MediaPublishingStateOwner(context, scope) }
    val publishingState = publishingOwner.state
    val currentUserId = context.appContainer.currentCachedUserId()
    val storyPublishes = publishingState.items.filter { it.target == MediaPublishTarget.STORY }

    var pendingMedia by remember { mutableStateOf<Uri?>(null) }
    var showTextComposer by remember { mutableStateOf(false) }
    var viewerGroup by remember { mutableStateOf<NovaStoryGroup?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            pendingMedia = uri
            owner.clearError()
        }
    }

    LaunchedEffect(Unit) {
        owner.load(showSpinner = true)
        currentUserId?.let(publishingOwner::enter)
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }
    LaunchedEffect(state.mediaCreatedVersion) {
        if (state.mediaCreatedVersion > 0) pendingMedia = null
    }
    LaunchedEffect(state.textCreatedVersion) {
        if (state.textCreatedVersion > 0) showTextComposer = false
    }
    LaunchedEffect(publishingState.storyPublishedVersion) {
        if (publishingState.storyPublishedVersion > 0) owner.load()
    }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Stories", color = NovaInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (state.error != null) {
                Text(
                    "Retry",
                    color = NovaAccent,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { owner.load(showSpinner = true) },
                )
            } else {
                Text("24h", color = NovaMuted, fontSize = 10.sp)
            }
        }

        if (state.loading && state.groups.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(92.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NovaAccent, strokeWidth = 2.dp)
            }
        } else {
            val myGroup = state.groups.firstOrNull { it.isMine }
            val otherGroups = state.groups.filterNot { it.isMine }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item(key = "story-add-media") {
                    StoryCreateRailItem(
                        label = if (myGroup == null) "Your story" else "Add story",
                        symbol = "+",
                        avatarUrl = if (myGroup == null) avatarUrl else "",
                        fallback = displayName.ifBlank { username },
                        onClick = { picker.launch(arrayOf("image/*", "video/*")) },
                    )
                }
                item(key = "story-add-text") {
                    StoryCreateRailItem(
                        label = "Text",
                        symbol = "Aa",
                        avatarUrl = "",
                        fallback = "Text",
                        onClick = { showTextComposer = true },
                    )
                }
                if (myGroup != null) {
                    item(key = "story-mine") {
                        StoryGroupRailItem(group = myGroup, label = "Your story") {
                            viewerGroup = myGroup
                        }
                    }
                }
                items(otherGroups, key = { it.author.id }) { group ->
                    StoryGroupRailItem(group = group, label = group.author.displayName) {
                        viewerGroup = group
                    }
                }
            }
        }

        state.error?.let {
            Text(it, color = NovaMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (storyPublishes.isNotEmpty()) {
            MediaPublishStatus(
                items = storyPublishes,
                modifier = Modifier.fillMaxWidth(),
                onRetry = publishingOwner::retry,
                onCancel = publishingOwner::cancel,
            )
        }
    }

    pendingMedia?.let { uri ->
        MediaStoryComposer(
            mediaUri = uri,
            uploading = false,
            onDismiss = { pendingMedia = null },
            onPost = { caption, audience ->
                currentUserId?.let { userId ->
                    MediaPublishWorker.enqueue(
                        context = context,
                        target = MediaPublishTarget.STORY,
                        userId = userId,
                        sourceUri = uri,
                        caption = caption,
                        audience = audience,
                    )
                    publishingOwner.enter(userId)
                    pendingMedia = null
                }
            },
        )
    }

    if (showTextComposer) {
        TextStoryComposer(
            uploading = state.uploading,
            onDismiss = { if (!state.uploading) showTextComposer = false },
            onPost = { text, backgroundStyle, audience ->
                owner.createTextStory(text, backgroundStyle, audience)
            },
        )
    }

    viewerGroup?.let { group ->
        StoryViewer(
            initialGroup = group,
            onDismiss = {
                viewerGroup = null
                owner.load()
            },
            onSessionExpired = {
                viewerGroup = null
                onSessionExpired()
            },
        )
    }
}


@Composable
private fun StoryCreateRailItem(
    label: String,
    symbol: String,
    avatarUrl: String,
    fallback: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(66.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = NovaAccentSoft,
            border = BorderStroke(1.5.dp, NovaAccent.copy(alpha = 0.55f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (avatarUrl.isNotBlank()) {
                    NovaAvatar(source = avatarUrl, fallbackText = fallback, size = 52.dp)
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).size(21.dp),
                        shape = CircleShape,
                        color = NovaAccent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(symbol, color = NovaBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(symbol, color = NovaAccent, fontSize = if (symbol == "Aa") 18.sp else 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = NovaMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


@Composable
private fun StoryGroupRailItem(group: NovaStoryGroup, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(66.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = NovaSurface,
            border = BorderStroke(
                if (group.hasUnseen) 2.dp else 1.dp,
                if (group.hasUnseen) NovaAccent else NovaBorder,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                NovaAvatar(
                    source = group.author.avatarUrl,
                    fallbackText = group.author.displayName,
                    size = 50.dp,
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = NovaInk, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}


@Composable
private fun MediaStoryComposer(
    mediaUri: Uri,
    uploading: Boolean,
    onDismiss: () -> Unit,
    onPost: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var caption by remember(mediaUri) { mutableStateOf("") }
    var audience by remember(mediaUri) { mutableStateOf("followers") }
    val isVideo = remember(mediaUri) {
        context.contentResolver.getType(mediaUri)?.startsWith("video/") == true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StoryBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("New Story", color = StoryInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Visible for 24 hours", color = StoryMuted, fontSize = 10.sp)
                }
                Surface(
                    onClick = onDismiss,
                    enabled = !uploading,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        NovaIcon(
                            asset = NovaIconAsset.Close,
                            contentDescription = "Close Story composer",
                            modifier = Modifier.size(22.dp),
                            tint = StoryInk,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (isVideo) {
                    NovaVideoPlayer(
                        source = mediaUri.toString(),
                        modifier = Modifier.fillMaxSize(),
                        autoplay = true,
                        repeat = true,
                        muted = true,
                        useController = true,
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                        description = "Selected Story video preview",
                    )
                } else {
                    NovaMediaImage(
                        source = mediaUri.toString(),
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "Selected Story photo preview",
                    )
                }
            }

            Surface(
                color = NovaSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(240) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Caption (optional)", color = NovaMuted) },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !uploading,
                    shape = RoundedCornerShape(16.dp),
                    colors = storyFieldColors(),
                )
                StoryAudienceChooser(audience, !uploading) { audience = it }
                    Surface(
                        onClick = { if (!uploading) onPost(caption, audience) },
                        enabled = !uploading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = NovaAccent,
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(19.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Share Story", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun TextStoryComposer(
    uploading: Boolean,
    onDismiss: () -> Unit,
    onPost: (String, String, String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("midnight") }
    var audience by remember { mutableStateOf("followers") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Text Story", color = NovaInk, fontWeight = FontWeight.Bold)
                Text("Choose a look for your words.", color = NovaMuted, fontSize = 10.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(storyBackgroundBrush(style))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text.ifBlank { "Say something…" },
                        color = StoryInk.copy(alpha = if (text.isBlank()) 0.62f else 1f),
                        fontSize = 23.sp,
                        lineHeight = 29.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(240) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Write your Story", color = NovaMuted) },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !uploading,
                    shape = RoundedCornerShape(16.dp),
                    colors = storyFieldColors(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("midnight", "sunset", "ocean", "forest").forEach { option ->
                        Surface(
                            onClick = { if (!uploading) style = option },
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(if (style == option) 2.dp else 1.dp, if (style == option) NovaAccent else NovaBorder),
                        ) {
                            Box(Modifier.fillMaxSize().background(storyBackgroundBrush(option)))
                        }
                    }
                }
                StoryAudienceChooser(audience, !uploading) { audience = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (!uploading && text.isNotBlank()) onPost(text, style, audience) },
                enabled = !uploading && text.isNotBlank(),
            ) {
                Text(if (uploading) "Posting…" else "Share Story", color = NovaAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uploading) { Text("Cancel") }
        },
    )
}


@Composable
private fun StoryAudienceChooser(
    audience: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StoryAudienceChip(
            label = "Followers",
            selected = audience == "followers",
            enabled = enabled,
            onClick = { onChange("followers") },
        )
        StoryAudienceChip(
            label = "★ Close Friends",
            selected = audience == "close_friends",
            enabled = enabled,
            onClick = { onChange("close_friends") },
        )
    }
}


@Composable
private fun StoryAudienceChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) NovaAccentSoft else NovaSurface,
        border = BorderStroke(1.dp, if (selected) NovaAccent else NovaBorder),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            color = if (selected) NovaAccent else NovaMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}


@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StoryViewer(
    initialGroup: NovaStoryGroup,
    onDismiss: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.storiesRepository
    val scope = rememberCoroutineScope()
    val owner = remember(initialGroup.author.id, repository, scope) {
        StoryViewerStateOwner(initialGroup, repository, scope)
    }
    val state = owner.state
    var progress by remember { mutableFloatStateOf(0f) }
    val imeVisible = WindowInsets.isImeVisible

    val story = state.currentStory
    if (story == null) {
        onDismiss()
        return
    }

    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }
    LaunchedEffect(state.finishedVersion) {
        if (state.finishedVersion > 0) onDismiss()
    }
    LaunchedEffect(state.deletedVersion) {
        if (state.deletedVersion > 0) onDismiss()
    }
    LaunchedEffect(story.id) {
        progress = 0f
        owner.enterCurrentStory()
    }

    LaunchedEffect(story.id) {
        if (story.mediaType == "video") return@LaunchedEffect
        var elapsedMs = 0L
        while (elapsedMs < STORY_FRAME_MS) {
            delay(STORY_TICK_MS)
            if (owner.state.viewersVisible || owner.state.mutationBusy || imeVisible) continue
            elapsedMs += STORY_TICK_MS
            progress = (elapsedMs.toFloat() / STORY_FRAME_MS.toFloat()).coerceIn(0f, 1f)
        }
        owner.advance()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(StoryBackground)) {
            StoryVisual(
                story = story,
                paused = state.viewersVisible || state.mutationBusy || imeVisible,
                onVideoProgress = { progress = it },
                onVideoFinished = owner::advance,
            )

            Column(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.stories.forEachIndexed { storyIndex, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.22f)),
                        ) {
                            val fill = when {
                                storyIndex < state.index -> 1f
                                storyIndex == state.index -> progress
                                else -> 0f
                            }
                            if (fill > 0f) {
                                Box(Modifier.fillMaxWidth(fill.coerceIn(0f, 1f)).height(3.dp).background(Color.White))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NovaAvatar(source = story.author.avatarUrl, fallbackText = story.author.displayName, size = 38.dp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(story.author.displayName, color = StoryInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "@${story.author.username}${if (story.audience == "close_friends") " · ★ Close Friends" else ""}",
                            color = StoryMuted,
                            fontSize = 9.sp,
                        )
                    }
                    Surface(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.35f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            NovaIcon(
                                asset = NovaIconAsset.Close,
                                contentDescription = "Close Story",
                                modifier = Modifier.size(22.dp),
                                tint = StoryInk,
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxSize().padding(top = 86.dp, bottom = 150.dp)) {
                Box(Modifier.weight(1f).fillMaxSize().clickable { owner.previous() })
                Box(Modifier.weight(1f).fillMaxSize().clickable { owner.advance() })
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                if (story.mediaType != "text" && story.caption.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.Black.copy(alpha = 0.42f)) {
                        Text(
                            story.caption,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            color = StoryInk,
                            fontSize = 12.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                story.sharedReel?.let { shared ->
                    Surface(
                        onClick = {
                            context.startActivity(
                                Intent(context, ReelsActivity::class.java)
                                    .putExtra(ReelsActivity.EXTRA_PROFILE_USERNAME, shared.author.username)
                                    .putExtra(ReelsActivity.EXTRA_INITIAL_REEL_ID, shared.id)
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(15.dp),
                        color = Color.Black.copy(alpha = 0.52f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NovaIcon(
                                asset = NovaIconAsset.Play,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = NovaAccent,
                            )
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Watch original Reel", color = StoryInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("@${shared.author.username}", color = StoryMuted, fontSize = 9.sp)
                            }
                            NovaIcon(
                                asset = NovaIconAsset.Play,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = StoryMuted,
                            )
                        }
                    }
                }

                story.sharedPost?.let { shared ->
                    Surface(
                        onClick = {
                            NovaPushOpenSignal.offer(Intent().putExtra("kind", "comment").putExtra("post_id", shared.id))
                            onDismiss()
                        },
                        shape = RoundedCornerShape(15.dp),
                        color = Color.Black.copy(alpha = 0.52f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NovaIcon(
                                asset = NovaIconAsset.Comment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = NovaAccent,
                            )
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("View original post", color = StoryInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("@${shared.author.username}", color = StoryMuted, fontSize = 9.sp)
                            }
                            NovaIcon(
                                asset = NovaIconAsset.Play,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                                tint = StoryMuted,
                            )
                        }
                    }
                }

                if (story.isMine) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = { owner.openViewers() },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.52f),
                        ) {
                            Text(
                                "${story.viewsCount ?: 0} viewers",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                color = StoryInk,
                                fontSize = 10.sp,
                            )
                        }
                        Surface(
                            onClick = {
                                if (!state.mutationBusy) owner.deleteCurrentStory()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.52f),
                        ) {
                            Text(
                                if (state.mutationBusy) "Deleting…" else "Delete",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                color = StoryInk,
                                fontSize = 10.sp,
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StoryReactions.forEach { emoji ->
                            Surface(
                                onClick = {
                                    if (!state.mutationBusy) owner.toggleReaction(emoji)
                                },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = if (story.myReaction == emoji) NovaAccent.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.48f),
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 15.sp) }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.replyBody,
                            onValueChange = owner::setReplyBody,
                            modifier = Modifier.weight(1f),
                            enabled = !state.mutationBusy,
                            placeholder = { Text("Reply to @${story.author.username}", color = StoryMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = StoryInk,
                                unfocusedTextColor = StoryInk,
                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                cursorColor = NovaAccent,
                                focusedContainerColor = Color.Black.copy(alpha = 0.38f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.38f),
                            ),
                        )
                        Surface(
                            onClick = {
                                if (!state.mutationBusy && state.replyBody.isNotBlank()) owner.sendReply()
                            },
                            shape = CircleShape,
                            color = if (state.replyBody.isNotBlank()) NovaAccent else Color.Black.copy(alpha = 0.4f),
                        ) {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                NovaIcon(
                                    asset = NovaIconAsset.Send,
                                    contentDescription = "Send Story reply",
                                    modifier = Modifier.size(21.dp),
                                    tint = StoryInk,
                                )
                            }
                        }
                    }
                }

                state.message?.let {
                    Text(it, color = StoryMuted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 3.dp))
                }
            }
        }
    }

    if (state.viewersVisible) {
        StoryViewersDialog(owner = owner)
    }
}


@Composable
private fun StoryVisual(
    story: NovaStory,
    paused: Boolean,
    onVideoProgress: (Float) -> Unit,
    onVideoFinished: () -> Unit,
) {
    val context = LocalContext.current

    when (story.mediaType) {
        "text" -> {
            Box(
                modifier = Modifier.fillMaxSize().background(storyBackgroundBrush(story.backgroundStyle)).padding(30.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    story.caption,
                    color = StoryInk,
                    fontSize = 31.sp,
                    lineHeight = 38.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        "video" -> {
            val player = remember(story.id) {
                ExoPlayer.Builder(context.applicationContext).build().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        true,
                    )
                    playWhenReady = false
                    repeatMode = Player.REPEAT_MODE_OFF
                }
            }

            DisposableEffect(story.id, player) {
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            onVideoProgress(1f)
                            onVideoFinished()
                        }
                    }

                }

                player.addListener(listener)
                player.setMediaItem(MediaItem.fromUri(story.mediaUrl))
                player.seekTo(0)
                player.prepare()

                onDispose {
                    player.removeListener(listener)
                    player.release()
                }
            }

            LaunchedEffect(paused, player) {
                if (paused) player.pause() else player.play()
            }

            LaunchedEffect(story.id, player) {
                while (true) {
                    val duration = player.duration
                    if (duration > 0L) {
                        onVideoProgress(
                            (player.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                        )
                    }
                    delay(STORY_TICK_MS)
                }
            }

            NovaPlayerSurface(
                player = player,
                thumbnailSource = story.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                useController = false,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                description = "Story video by @${story.author.username}",
            )
        }
        else -> {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                NovaMediaImage(
                    source = story.mediaUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "Story by @${story.author.username}",
                )
            }
        }
    }
}


@Composable
private fun StoryViewersDialog(owner: StoryViewerStateOwner) {
    val state = owner.state

    AlertDialog(
        onDismissRequest = owner::closeViewers,
        title = { Text("Story viewers", color = NovaInk, fontWeight = FontWeight.Bold) },
        text = {
            when {
                state.viewersLoading -> Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NovaAccent)
                }
                state.viewers.isEmpty() -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(state.viewersError ?: "No viewers yet.", color = NovaMuted, fontSize = 12.sp)
                    if (state.viewersError != null) {
                        Surface(
                            onClick = owner::openViewers,
                            shape = RoundedCornerShape(13.dp),
                            color = NovaAccentSoft,
                        ) {
                            Text(
                                "Try again",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                color = NovaAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.viewers, key = { it.user.id }) { viewer ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NovaAvatar(source = viewer.user.avatarUrl, fallbackText = viewer.user.displayName, size = 36.dp)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(viewer.user.displayName, color = NovaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("@${viewer.user.username}", color = NovaMuted, fontSize = 9.sp)
                            }
                            if (viewer.reaction.isNotBlank()) Text(viewer.reaction, fontSize = 18.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = owner::closeViewers) { Text("Done") } },
    )
}


@Composable
private fun storyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NovaAccent,
    unfocusedBorderColor = NovaBorder,
    cursorColor = NovaAccent,
)


private fun storyBackgroundBrush(style: String): Brush = when (style) {
    "sunset" -> Brush.linearGradient(listOf(Color(0xFF5B1A55), Color(0xFFE06A46), Color(0xFFF0B35A)))
    "ocean" -> Brush.linearGradient(listOf(Color(0xFF092B4A), Color(0xFF0D6E8A), Color(0xFF31A6A0)))
    "forest" -> Brush.linearGradient(listOf(Color(0xFF102A24), Color(0xFF1D5943), Color(0xFF5A7D45)))
    else -> Brush.linearGradient(listOf(Color(0xFF0B0E17), Color(0xFF232A45), Color(0xFF4A315D)))
}
