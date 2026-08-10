package com.nova.app.feature.stories

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.network.ApiResult
import com.nova.app.core.stories.NovaStoriesRepository
import com.nova.app.core.stories.NovaStory
import com.nova.app.core.stories.NovaStoryAuthor
import com.nova.app.core.stories.NovaStoryGroup
import com.nova.app.core.stories.NovaStoryViewer
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

private val StoryViewerBackground = Color(0xFF090B10)
private val StoryViewerInk = Color(0xFFF7F8FB)
private val StoryViewerMuted = Color(0xFFB8BDC8)
private val StoryReactionChoices = listOf("❤️", "😂", "😮", "😢", "🔥", "👏")


@Composable
fun StoriesRail(
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
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingMedia by remember { mutableStateOf<Uri?>(null) }
    var viewerGroupIndex by remember { mutableStateOf<Int?>(null) }

    fun reload(showSpinner: Boolean = false) {
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

    LaunchedEffect(Unit) { reload(showSpinner = true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Stories",
                color = NovaInk,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            when {
                uploading -> Text("Posting…", color = NovaMuted, fontSize = 11.sp)
                loading -> CircularProgressIndicator(
                    color = NovaAccent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                else -> Text(
                    text = "24h",
                    color = NovaMuted,
                    fontSize = 11.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val ownGroupIndex = groups.indexOfFirst { it.isMine }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = "your-story") {
                StoryCircle(
                    author = NovaStoryAuthor(
                        id = 0,
                        username = username,
                        name = displayName,
                        avatarUrl = avatarUrl,
                    ),
                    label = "Your story",
                    unseen = ownGroupIndex >= 0 && groups[ownGroupIndex].hasUnseen,
                    showAdd = true,
                    onClick = {
                        if (ownGroupIndex >= 0 && groups[ownGroupIndex].stories.isNotEmpty()) {
                            viewerGroupIndex = ownGroupIndex
                        } else if (!uploading) {
                            picker.launch(arrayOf("image/*", "video/*"))
                        }
                    },
                    onAdd = {
                        if (!uploading) picker.launch(arrayOf("image/*", "video/*"))
                    },
                )
            }

            items(
                items = groups.withIndex().filter { !it.value.isMine },
                key = { it.value.author.id },
            ) { indexed ->
                StoryCircle(
                    author = indexed.value.author,
                    label = indexed.value.author.displayName,
                    unseen = indexed.value.hasUnseen,
                    showAdd = false,
                    onClick = { viewerGroupIndex = indexed.index },
                    onAdd = {},
                )
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = { reload(showSpinner = true) },
                shape = RoundedCornerShape(12.dp),
                color = NovaAccentSoft,
            ) {
                Text(
                    text = "${error}  ·  Try again",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    color = NovaMuted,
                    fontSize = 11.sp,
                )
            }
        }
    }

    pendingMedia?.let { uri ->
        StoryComposerDialog(
            mediaUri = uri,
            uploading = uploading,
            onDismiss = { if (!uploading) pendingMedia = null },
            onPost = { caption ->
                scope.launch {
                    uploading = true
                    error = null
                    when (val result = repository.createStory(uri, caption)) {
                        is ApiResult.Success -> {
                            pendingMedia = null
                            reload()
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

    viewerGroupIndex?.let { startIndex ->
        if (startIndex in groups.indices) {
            StoryViewerDialog(
                groups = groups,
                startGroupIndex = startIndex,
                repository = repository,
                onDismiss = {
                    viewerGroupIndex = null
                    reload()
                },
                onStoriesChanged = { reload() },
                onSessionExpired = {
                    viewerGroupIndex = null
                    onSessionExpired()
                },
            )
        }
    }
}


@Composable
private fun StoryCircle(
    author: NovaStoryAuthor,
    label: String,
    unseen: Boolean,
    showAdd: Boolean,
    onClick: () -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .border(
                        width = if (unseen) 3.dp else 1.dp,
                        color = if (unseen) NovaAccent else NovaBorder,
                        shape = CircleShape,
                    )
                    .padding(4.dp)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                NovaAvatar(
                    source = author.avatarUrl,
                    fallbackText = author.displayName,
                    size = 56.dp,
                )
            }

            if (showAdd) {
                Surface(
                    onClick = onAdd,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(23.dp),
                    shape = CircleShape,
                    color = NovaAccent,
                    border = BorderStroke(2.dp, NovaBackground),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+",
                            color = NovaBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            color = NovaInk,
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}


@Composable
private fun StoryComposerDialog(
    mediaUri: Uri,
    uploading: Boolean,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit,
) {
    val context = LocalContext.current
    val mimeType = remember(mediaUri) { context.contentResolver.getType(mediaUri).orEmpty() }
    var caption by remember(mediaUri) { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "New story",
                    color = NovaInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(StoryViewerBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    if (mimeType.startsWith("video/")) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { viewContext ->
                                VideoView(viewContext).apply {
                                    setVideoURI(mediaUri)
                                    setOnPreparedListener { player ->
                                        player.isLooping = true
                                        start()
                                    }
                                }
                            },
                        )
                    } else {
                        AsyncImage(
                            model = mediaUri,
                            contentDescription = "Story preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(240) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add a caption…", color = NovaMuted) },
                    enabled = !uploading,
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                    ),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        onClick = { if (!uploading) onDismiss() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(15.dp),
                        color = NovaBackground,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Text(
                            text = "Cancel",
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            color = NovaInk,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Surface(
                        onClick = { if (!uploading) onPost(caption) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(15.dp),
                        color = NovaAccent,
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (uploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = NovaBackground,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (uploading) "Posting…" else "Share story",
                                color = NovaBackground,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun StoryViewerDialog(
    groups: List<NovaStoryGroup>,
    startGroupIndex: Int,
    repository: NovaStoriesRepository,
    onDismiss: () -> Unit,
    onStoriesChanged: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var localGroups by remember(groups) { mutableStateOf(groups) }
    var groupIndex by remember { mutableStateOf(startGroupIndex.coerceIn(localGroups.indices)) }
    var storyIndex by remember {
        mutableStateOf(
            localGroups.getOrNull(startGroupIndex)?.stories?.indexOfFirst { !it.isViewed }
                ?.takeIf { it >= 0 } ?: 0
        )
    }
    var reply by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var showViewers by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun currentStory(): NovaStory? = localGroups.getOrNull(groupIndex)?.stories?.getOrNull(storyIndex)

    fun moveNext() {
        val group = localGroups.getOrNull(groupIndex) ?: return onDismiss()
        if (storyIndex + 1 < group.stories.size) {
            storyIndex += 1
            return
        }
        if (groupIndex + 1 < localGroups.size) {
            groupIndex += 1
            storyIndex = localGroups[groupIndex].stories.indexOfFirst { !it.isViewed }.takeIf { it >= 0 } ?: 0
        } else {
            onDismiss()
        }
    }

    fun movePrevious() {
        if (storyIndex > 0) {
            storyIndex -= 1
            return
        }
        if (groupIndex > 0) {
            groupIndex -= 1
            storyIndex = (localGroups[groupIndex].stories.size - 1).coerceAtLeast(0)
        }
    }

    val story = currentStory() ?: return
    val group = localGroups[groupIndex]

    LaunchedEffect(story.id) {
        reply = ""
        feedback = null
        if (!story.isMine) {
            when (val result = repository.markViewed(story.id)) {
                is ApiResult.Success -> {
                    localGroups = localGroups.map { candidate ->
                        if (candidate.author.id != story.author.id) return@map candidate
                        val updatedStories = candidate.stories.map { item ->
                            if (item.id == story.id) item.copy(isViewed = true) else item
                        }
                        candidate.copy(
                            stories = updatedStories,
                            hasUnseen = updatedStories.any { !it.isViewed },
                        )
                    }
                    onStoriesChanged()
                }
                is ApiResult.Failure -> if (result.statusCode == 401) onSessionExpired()
            }
        }
    }

    LaunchedEffect(story.id, showViewers, confirmDelete) {
        if (story.mediaType == "image" && !showViewers && !confirmDelete) {
            delay(5_000)
            moveNext()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StoryViewerBackground),
        ) {
            StoryMedia(
                story = story,
                onVideoComplete = ::moveNext,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .align(Alignment.Center),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = ::movePrevious),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = ::moveNext),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    group.stories.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    if (index <= storyIndex) StoryViewerInk else StoryViewerInk.copy(alpha = 0.3f)
                                ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NovaAvatar(
                        source = story.author.avatarUrl,
                        fallbackText = story.author.displayName,
                        size = 38.dp,
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = story.author.displayName,
                            color = StoryViewerInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = storyAge(story.createdAt),
                            color = StoryViewerMuted,
                            fontSize = 10.sp,
                        )
                    }
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.35f),
                    ) {
                        Text(
                            text = "×",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = StoryViewerInk,
                            fontSize = 22.sp,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
                if (story.caption.isNotBlank()) {
                    Text(
                        text = story.caption,
                        color = StoryViewerInk,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                feedback?.let {
                    Text(it, color = StoryViewerMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(7.dp))
                }

                if (story.isMine) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ViewerAction(
                            text = "${story.viewsCount ?: 0} viewers",
                            modifier = Modifier.weight(1f),
                            onClick = { showViewers = true },
                        )
                        ViewerAction(
                            text = "Delete",
                            modifier = Modifier.weight(1f),
                            onClick = { confirmDelete = true },
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StoryReactionChoices.forEach { emoji ->
                            Surface(
                                onClick = {
                                    if (busy) return@Surface
                                    val removing = story.myReaction == emoji
                                    localGroups = updateStory(localGroups, story.id) {
                                        it.copy(myReaction = if (removing) "" else emoji, isViewed = true)
                                    }
                                    scope.launch {
                                        busy = true
                                        val result = if (removing) {
                                            repository.removeReaction(story.id)
                                        } else {
                                            repository.react(story.id, emoji).let { response ->
                                                when (response) {
                                                    is ApiResult.Success -> ApiResult.Success(Unit)
                                                    is ApiResult.Failure -> response
                                                }
                                            }
                                        }
                                        if (result is ApiResult.Failure) {
                                            if (result.statusCode == 401) onSessionExpired() else feedback = result.message
                                        }
                                        busy = false
                                    }
                                },
                                shape = CircleShape,
                                color = if (story.myReaction == emoji) NovaAccent else Color.Black.copy(alpha = 0.28f),
                                border = BorderStroke(1.dp, StoryViewerInk.copy(alpha = 0.28f)),
                            ) {
                                Text(
                                    text = emoji,
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 18.sp,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = reply,
                            onValueChange = { reply = it.take(1000) },
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            placeholder = { Text("Reply to story…", color = StoryViewerMuted) },
                            maxLines = 3,
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = StoryViewerInk,
                                unfocusedTextColor = StoryViewerInk,
                                focusedBorderColor = NovaAccent,
                                unfocusedBorderColor = StoryViewerInk.copy(alpha = 0.28f),
                                cursorColor = NovaAccent,
                            ),
                        )
                        Surface(
                            onClick = {
                                if (busy || reply.isBlank()) return@Surface
                                val body = reply
                                scope.launch {
                                    busy = true
                                    when (val result = repository.reply(story.id, body)) {
                                        is ApiResult.Success -> {
                                            reply = ""
                                            feedback = "Reply sent to Messages."
                                        }
                                        is ApiResult.Failure -> {
                                            if (result.statusCode == 401) onSessionExpired() else feedback = result.message
                                        }
                                    }
                                    busy = false
                                }
                            },
                            modifier = Modifier.size(50.dp),
                            shape = CircleShape,
                            color = if (reply.isBlank() || busy) NovaAccent.copy(alpha = 0.45f) else NovaAccent,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (busy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = StoryViewerInk,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("↑", color = StoryViewerInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (feedback == "Reply sent to Messages.") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                onDismiss()
                                NovaMessagingNavigator.openInbox(context)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, StoryViewerInk.copy(alpha = 0.25f)),
                        ) {
                            Text(
                                text = "Open Messages",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = StoryViewerInk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showViewers) {
        StoryViewersDialog(
            story = story,
            repository = repository,
            onDismiss = { showViewers = false },
            onSessionExpired = onSessionExpired,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = false },
            title = { Text("Delete story?") },
            text = { Text("This story will disappear immediately for everyone.") },
            confirmButton = {
                Text(
                    text = if (busy) "Deleting…" else "Delete",
                    modifier = Modifier.clickable(enabled = !busy) {
                        scope.launch {
                            busy = true
                            when (val result = repository.deleteStory(story.id)) {
                                is ApiResult.Success -> {
                                    confirmDelete = false
                                    onStoriesChanged()
                                    onDismiss()
                                }
                                is ApiResult.Failure -> {
                                    if (result.statusCode == 401) onSessionExpired() else feedback = result.message
                                    confirmDelete = false
                                }
                            }
                            busy = false
                        }
                    },
                    color = NovaAccent,
                    fontWeight = FontWeight.Bold,
                )
            },
            dismissButton = {
                Text(
                    text = "Cancel",
                    modifier = Modifier.clickable(enabled = !busy) { confirmDelete = false },
                    color = NovaMuted,
                )
            },
        )
    }
}


@Composable
private fun StoryMedia(
    story: NovaStory,
    onVideoComplete: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (story.mediaType == "video") {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    VideoView(context).apply {
                        setVideoURI(Uri.parse(story.mediaUrl))
                        setOnPreparedListener { start() }
                        setOnCompletionListener { onVideoComplete() }
                    }
                },
                update = { view ->
                    if (!view.isPlaying) view.start()
                },
            )
        } else {
            AsyncImage(
                model = story.mediaUrl,
                contentDescription = "Story by ${story.author.displayName}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}


@Composable
private fun ViewerAction(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, StoryViewerInk.copy(alpha = 0.24f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 11.dp),
            textAlign = TextAlign.Center,
            color = StoryViewerInk,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}


@Composable
private fun StoryViewersDialog(
    story: NovaStory,
    repository: NovaStoriesRepository,
    onDismiss: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    var viewers by remember(story.id) { mutableStateOf<List<NovaStoryViewer>>(emptyList()) }
    var loading by remember(story.id) { mutableStateOf(true) }
    var error by remember(story.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(story.id) {
        when (val result = repository.viewers(story.id)) {
            is ApiResult.Success -> viewers = result.value
            is ApiResult.Failure -> {
                if (result.statusCode == 401) onSessionExpired() else error = result.message
            }
        }
        loading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp),
            shape = RoundedCornerShape(26.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Story viewers", color = NovaInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("${viewers.size} people", color = NovaMuted, fontSize = 11.sp)
                    }
                    Surface(onClick = onDismiss, shape = CircleShape, color = NovaBackground) {
                        Text("×", modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp), color = NovaInk, fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NovaAccent)
                    }
                    error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error.orEmpty(), color = NovaMuted, textAlign = TextAlign.Center)
                    }
                    viewers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No views yet.", color = NovaMuted)
                    }
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(viewers, key = { it.user.id }) { viewer ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = NovaBackground,
                                border = BorderStroke(1.dp, NovaBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    NovaAvatar(
                                        source = viewer.user.avatarUrl,
                                        fallbackText = viewer.user.displayName,
                                        size = 42.dp,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(viewer.user.displayName, color = NovaInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("@${viewer.user.username}", color = NovaMuted, fontSize = 10.sp)
                                    }
                                    if (viewer.reaction.isNotBlank()) {
                                        Text(viewer.reaction, fontSize = 21.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun updateStory(
    groups: List<NovaStoryGroup>,
    storyId: Long,
    update: (NovaStory) -> NovaStory,
): List<NovaStoryGroup> {
    return groups.map { group ->
        val stories = group.stories.map { story ->
            if (story.id == storyId) update(story) else story
        }
        group.copy(stories = stories, hasUnseen = stories.any { !it.isViewed })
    }
}


private fun storyAge(raw: String): String {
    val created = runCatching { OffsetDateTime.parse(raw).toInstant() }
        .recoverCatching { Instant.parse(raw) }
        .getOrNull() ?: return "now"
    val duration = Duration.between(created, Instant.now()).coerceAtLeast(Duration.ZERO)
    return when {
        duration.toMinutes() < 1 -> "now"
        duration.toHours() < 1 -> "${duration.toMinutes()}m"
        else -> "${duration.toHours()}h"
    }
}
