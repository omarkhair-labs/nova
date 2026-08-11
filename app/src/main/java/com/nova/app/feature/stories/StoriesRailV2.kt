package com.nova.app.feature.stories

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.ReelsActivity
import com.nova.app.core.network.ApiResult
import com.nova.app.core.push.NovaPushOpenSignal
import com.nova.app.core.stories.NovaStoriesRepository
import com.nova.app.core.stories.NovaStory
import com.nova.app.core.stories.NovaStoryGroup
import com.nova.app.core.stories.NovaStoryViewer
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private val StoryV2Background = Color(0xFF080A0F)
private val StoryV2Ink = Color(0xFFF7F8FA)
private val StoryV2Muted = Color(0xFFB8BDC8)
private val StoryReactions = listOf("❤️", "😂", "😮", "😢", "🔥", "👏")
private const val STORY_FRAME_MS = 5_500L
private const val STORY_TICK_MS = 55L


@Composable
fun StoriesRailV2(
    displayName: String,
    username: String,
    avatarUrl: String,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { NovaStoriesRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var groups by remember { mutableStateOf<List<NovaStoryGroup>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingMedia by remember { mutableStateOf<Uri?>(null) }
    var showTextComposer by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var viewerGroup by remember { mutableStateOf<NovaStoryGroup?>(null) }

    fun loadStories(showSpinner: Boolean = false) {
        scope.launch {
            if (showSpinner) loading = true
            when (val result = repository.stories()) {
                is ApiResult.Success -> {
                    groups = result.value
                    error = null
                }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) onSessionExpired() else error = result.message
                }
            }
            loading = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingMedia = uri
            error = null
        }
    }

    LaunchedEffect(Unit) { loadStories(showSpinner = true) }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Stories", color = NovaInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (error != null) {
                Text(
                    "Retry",
                    color = NovaAccent,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { loadStories(showSpinner = true) },
                )
            } else {
                Text("24h", color = NovaMuted, fontSize = 10.sp)
            }
        }

        if (loading && groups.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(92.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NovaAccent, strokeWidth = 2.dp)
            }
        } else {
            val myGroup = groups.firstOrNull { it.isMine }
            val otherGroups = groups.filterNot { it.isMine }
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

        error?.let {
            Text(it, color = NovaMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }

    pendingMedia?.let { uri ->
        MediaStoryComposerV2(
            mediaUri = uri,
            uploading = uploading,
            onDismiss = { if (!uploading) pendingMedia = null },
            onPost = { caption, audience ->
                scope.launch {
                    uploading = true
                    when (val result = repository.createStory(uri, caption, audience)) {
                        is ApiResult.Success -> {
                            pendingMedia = null
                            loadStories()
                        }
                        is ApiResult.Failure -> {
                            if (result.statusCode == 401) onSessionExpired() else error = result.message
                        }
                    }
                    uploading = false
                }
            },
        )
    }

    if (showTextComposer) {
        TextStoryComposerV2(
            uploading = uploading,
            onDismiss = { if (!uploading) showTextComposer = false },
            onPost = { text, backgroundStyle, audience ->
                scope.launch {
                    uploading = true
                    when (val result = repository.createTextStory(text, backgroundStyle, audience)) {
                        is ApiResult.Success -> {
                            showTextComposer = false
                            loadStories()
                        }
                        is ApiResult.Failure -> {
                            if (result.statusCode == 401) onSessionExpired() else error = result.message
                        }
                    }
                    uploading = false
                }
            },
        )
    }

    viewerGroup?.let { group ->
        StoryViewerV2(
            initialGroup = group,
            repository = repository,
            onDismiss = {
                viewerGroup = null
                loadStories()
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
private fun MediaStoryComposerV2(
    mediaUri: Uri,
    uploading: Boolean,
    onDismiss: () -> Unit,
    onPost: (String, String) -> Unit,
) {
    var caption by remember(mediaUri) { mutableStateOf("") }
    var audience by remember(mediaUri) { mutableStateOf("followers") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Story", color = NovaInk, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NovaAccentSoft,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Photo / video ready", color = NovaInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Add a caption or post it as-is.", color = NovaMuted, fontSize = 10.sp)
                    }
                }
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
                StoryAudienceChooserV2(audience, !uploading) { audience = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!uploading) onPost(caption, audience) }, enabled = !uploading) {
                Text(if (uploading) "Posting…" else "Share Story", color = NovaAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uploading) { Text("Cancel") }
        },
    )
}


@Composable
private fun TextStoryComposerV2(
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
                Text("Aa · Nova V3", color = NovaMuted, fontSize = 10.sp)
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
                        color = StoryV2Ink.copy(alpha = if (text.isBlank()) 0.62f else 1f),
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
                            modifier = Modifier.size(34.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(if (style == option) 2.dp else 1.dp, if (style == option) NovaAccent else NovaBorder),
                        ) {
                            Box(Modifier.fillMaxSize().background(storyBackgroundBrush(option)))
                        }
                    }
                }
                StoryAudienceChooserV2(audience, !uploading) { audience = it }
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
private fun StoryAudienceChooserV2(
    audience: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StoryAudienceChipV2(
            label = "Followers",
            selected = audience == "followers",
            enabled = enabled,
            onClick = { onChange("followers") },
        )
        StoryAudienceChipV2(
            label = "★ Close Friends",
            selected = audience == "close_friends",
            enabled = enabled,
            onClick = { onChange("close_friends") },
        )
    }
}


@Composable
private fun StoryAudienceChipV2(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
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
private fun StoryViewerV2(
    initialGroup: NovaStoryGroup,
    repository: NovaStoriesRepository,
    onDismiss: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stories by remember(initialGroup.author.id) { mutableStateOf(initialGroup.stories) }
    var index by remember(initialGroup.author.id) {
        mutableStateOf(initialGroup.stories.indexOfFirst { !it.isViewed }.let { if (it < 0) 0 else it })
    }
    var progress by remember { mutableFloatStateOf(0f) }
    var replyBody by remember { mutableStateOf("") }
    var mutationBusy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showViewers by remember { mutableStateOf(false) }

    val story = stories.getOrNull(index)
    if (story == null) {
        onDismiss()
        return
    }

    fun advance() {
        if (index < stories.lastIndex) {
            index += 1
        } else {
            onDismiss()
        }
    }

    fun previous() {
        if (index > 0) index -= 1
    }

    LaunchedEffect(story.id) {
        progress = 0f
        message = null
        if (!story.isMine && !story.isViewed) {
            when (val result = repository.markViewed(story.id)) {
                is ApiResult.Success -> {
                    stories = stories.map { if (it.id == story.id) it.copy(isViewed = true) else it }
                }
                is ApiResult.Failure -> if (result.statusCode == 401) onSessionExpired()
            }
        }
        val steps = (STORY_FRAME_MS / STORY_TICK_MS).toInt().coerceAtLeast(1)
        repeat(steps) { step ->
            delay(STORY_TICK_MS)
            progress = (step + 1f) / steps
        }
        advance()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(StoryV2Background),
        ) {
            StoryVisualV2(story = story, onVideoFinished = ::advance)

            Column(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    stories.forEachIndexed { storyIndex, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.22f)),
                        ) {
                            val fill = when {
                                storyIndex < index -> 1f
                                storyIndex == index -> progress
                                else -> 0f
                            }
                            if (fill > 0f) {
                                Box(
                                    Modifier.fillMaxWidth(fill.coerceIn(0f, 1f)).height(3.dp).background(Color.White)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NovaAvatar(
                        source = story.author.avatarUrl,
                        fallbackText = story.author.displayName,
                        size = 38.dp,
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(story.author.displayName, color = StoryV2Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "@${story.author.username}${if (story.audience == "close_friends") " · ★ Close Friends" else ""}",
                            color = StoryV2Muted,
                            fontSize = 9.sp,
                        )
                    }
                    Surface(onClick = onDismiss, shape = CircleShape, color = Color.Black.copy(alpha = 0.35f)) {
                        Text("×", modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = StoryV2Ink, fontSize = 20.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxSize().padding(top = 86.dp, bottom = 150.dp),
            ) {
                Box(Modifier.weight(1f).fillMaxSize().clickable { previous() })
                Box(Modifier.weight(1f).fillMaxSize().clickable { advance() })
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
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.42f),
                    ) {
                        Text(
                            story.caption,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            color = StoryV2Ink,
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
                            Text("▶", color = NovaAccent, fontSize = 15.sp)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Watch original Reel", color = StoryV2Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("@${shared.author.username}", color = StoryV2Muted, fontSize = 9.sp)
                            }
                            Text("›", color = StoryV2Muted, fontSize = 18.sp)
                        }
                    }
                }

                story.sharedPost?.let { shared ->
                    Surface(
                        onClick = {
                            NovaPushOpenSignal.offer(
                                Intent()
                                    .putExtra("kind", "comment")
                                    .putExtra("post_id", shared.id)
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
                            Text("▣", color = NovaAccent, fontSize = 15.sp)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("View original post", color = StoryV2Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("@${shared.author.username}", color = StoryV2Muted, fontSize = 9.sp)
                            }
                            Text("›", color = StoryV2Muted, fontSize = 18.sp)
                        }
                    }
                }

                if (story.isMine) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = { showViewers = true },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.52f),
                        ) {
                            Text(
                                "◉ ${story.viewsCount ?: 0} viewers",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                color = StoryV2Ink,
                                fontSize = 10.sp,
                            )
                        }
                        Surface(
                            onClick = {
                                if (!mutationBusy) {
                                    scope.launch {
                                        mutationBusy = true
                                        when (val result = repository.deleteStory(story.id)) {
                                            is ApiResult.Success -> onDismiss()
                                            is ApiResult.Failure -> {
                                                if (result.statusCode == 401) onSessionExpired() else message = result.message
                                            }
                                        }
                                        mutationBusy = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.52f),
                        ) {
                            Text(
                                if (mutationBusy) "Deleting…" else "Delete",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                color = StoryV2Ink,
                                fontSize = 10.sp,
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StoryReactions.forEach { emoji ->
                            Surface(
                                onClick = {
                                    if (!mutationBusy) {
                                        scope.launch {
                                            mutationBusy = true
                                            val result = if (story.myReaction == emoji) {
                                                repository.removeReaction(story.id)
                                            } else {
                                                repository.react(story.id, emoji)
                                            }
                                            when (result) {
                                                is ApiResult.Success -> {
                                                    val nextReaction = if (story.myReaction == emoji) "" else emoji
                                                    stories = stories.map {
                                                        if (it.id == story.id) it.copy(myReaction = nextReaction) else it
                                                    }
                                                }
                                                is ApiResult.Failure -> {
                                                    if (result.statusCode == 401) onSessionExpired() else message = result.message
                                                }
                                            }
                                            mutationBusy = false
                                        }
                                    }
                                },
                                modifier = Modifier.size(37.dp),
                                shape = CircleShape,
                                color = if (story.myReaction == emoji) NovaAccent.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.48f),
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 15.sp) }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = replyBody,
                            onValueChange = { replyBody = it.take(1000) },
                            modifier = Modifier.weight(1f),
                            enabled = !mutationBusy,
                            placeholder = { Text("Reply to @${story.author.username}", color = StoryV2Muted) },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = StoryV2Ink,
                                unfocusedTextColor = StoryV2Ink,
                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                cursorColor = NovaAccent,
                                focusedContainerColor = Color.Black.copy(alpha = 0.38f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.38f),
                            ),
                        )
                        Surface(
                            onClick = {
                                if (!mutationBusy && replyBody.isNotBlank()) {
                                    scope.launch {
                                        mutationBusy = true
                                        when (val result = repository.reply(story.id, replyBody)) {
                                            is ApiResult.Success -> {
                                                replyBody = ""
                                                message = "Reply sent to Messages."
                                            }
                                            is ApiResult.Failure -> {
                                                if (result.statusCode == 401) onSessionExpired() else message = result.message
                                            }
                                        }
                                        mutationBusy = false
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = if (replyBody.isNotBlank()) NovaAccent else Color.Black.copy(alpha = 0.4f),
                        ) {
                            Text("↑", modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = StoryV2Ink, fontSize = 18.sp)
                        }
                    }
                }

                message?.let {
                    Text(it, color = StoryV2Muted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 3.dp))
                }
            }
        }
    }

    if (showViewers) {
        StoryViewersDialogV2(
            storyId = story.id,
            repository = repository,
            onDismiss = { showViewers = false },
            onSessionExpired = onSessionExpired,
        )
    }
}


@Composable
private fun StoryVisualV2(story: NovaStory, onVideoFinished: () -> Unit) {
    when (story.mediaType) {
        "text" -> {
            Box(
                modifier = Modifier.fillMaxSize().background(storyBackgroundBrush(story.backgroundStyle)).padding(30.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    story.caption,
                    color = StoryV2Ink,
                    fontSize = 31.sp,
                    lineHeight = 38.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        "video" -> {
            AndroidView(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                factory = { viewContext ->
                    VideoView(viewContext).apply {
                        setVideoURI(Uri.parse(story.mediaUrl))
                        setOnPreparedListener { player ->
                            player.isLooping = false
                            start()
                        }
                        setOnCompletionListener { onVideoFinished() }
                    }
                },
                update = { view ->
                    if (!view.isPlaying) view.start()
                },
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
private fun StoryViewersDialogV2(
    storyId: Long,
    repository: NovaStoriesRepository,
    onDismiss: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    var loading by remember(storyId) { mutableStateOf(true) }
    var viewers by remember(storyId) { mutableStateOf<List<NovaStoryViewer>>(emptyList()) }
    var error by remember(storyId) { mutableStateOf<String?>(null) }

    LaunchedEffect(storyId) {
        when (val result = repository.viewers(storyId)) {
            is ApiResult.Success -> viewers = result.value
            is ApiResult.Failure -> {
                if (result.statusCode == 401) onSessionExpired() else error = result.message
            }
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Story viewers", color = NovaInk, fontWeight = FontWeight.Bold) },
        text = {
            when {
                loading -> Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NovaAccent)
                }
                viewers.isEmpty() -> Text(error ?: "No viewers yet.", color = NovaMuted, fontSize = 12.sp)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(viewers, key = { it.user.id }) { viewer ->
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}


@Composable
private fun storyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NovaAccent,
    unfocusedBorderColor = NovaBorder,
    cursorColor = NovaAccent,
)


private fun storyBackgroundBrush(style: String): Brush {
    return when (style) {
        "sunset" -> Brush.linearGradient(listOf(Color(0xFF5B1A55), Color(0xFFE06A46), Color(0xFFF0B35A)))
        "ocean" -> Brush.linearGradient(listOf(Color(0xFF092B4A), Color(0xFF0D6E8A), Color(0xFF31A6A0)))
        "forest" -> Brush.linearGradient(listOf(Color(0xFF102A24), Color(0xFF1D5943), Color(0xFF5A7D45)))
        else -> Brush.linearGradient(listOf(Color(0xFF0B0E17), Color(0xFF232A45), Color(0xFF4A315D)))
    }
}
