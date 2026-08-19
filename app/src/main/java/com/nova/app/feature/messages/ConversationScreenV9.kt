package com.nova.app.feature.messages

import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.app.appContainer
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.details.model.ConversationMessageContext
import com.nova.app.feature.messages.details.model.ConversationToolMessage
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
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


private enum class V9ToolsTab { Details, Search, Media }


@Composable
fun ConversationScreenV9(
    conversationId: Long,
    username: String,
    displayName: String,
    avatarUrl: String,
    themeLabel: String,
    onOpenTheme: () -> Unit,
    onBack: () -> Unit,
    onConversationRead: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    var showDetails by remember(conversationId) { mutableStateOf(false) }
    var detailsStartTab by remember(conversationId) { mutableStateOf(V9ToolsTab.Details) }
    val identityEndPadding = if (username == "group") 61.dp else 110.dp

    Box(Modifier.fillMaxSize()) {
        ConversationScreenV8(
            conversationId = conversationId,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            onBack = onBack,
            onConversationRead = onConversationRead,
            onSessionExpired = onSessionExpired,
        )

        Surface(
            onClick = {
                detailsStartTab = V9ToolsTab.Details
                showDetails = true
            },
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 58.dp, end = identityEndPadding)
                .height(52.dp),
        ) {
            Box(Modifier.fillMaxSize())
        }
    }

    if (showDetails) {
        V9ConversationDetailsDialog(
            conversationId = conversationId,
            username = username,
            displayName = displayName,
            avatarUrl = avatarUrl,
            initialTab = detailsStartTab,
            themeLabel = themeLabel,
            onOpenTheme = onOpenTheme,
            onDismiss = { showDetails = false },
            onSessionExpired = onSessionExpired,
        )
    }
}


@Composable
private fun V9ConversationDetailsDialog(
    conversationId: Long,
    username: String,
    displayName: String,
    avatarUrl: String,
    initialTab: V9ToolsTab,
    themeLabel: String,
    onOpenTheme: () -> Unit,
    onDismiss: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { context.appContainer.conversationToolsRepository }

    var tab by remember(initialTab) { mutableStateOf(initialTab) }
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ConversationToolMessage>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var mediaType by remember { mutableStateOf("all") }
    var mediaItems by remember { mutableStateOf<List<ConversationToolMessage>>(emptyList()) }
    var mediaCursor by remember { mutableStateOf<String?>(null) }
    var mediaLoading by remember { mutableStateOf(false) }
    var mediaLoadedFor by remember { mutableStateOf<String?>(null) }
    var mediaError by remember { mutableStateOf<String?>(null) }

    var muted by remember { mutableStateOf(false) }
    var muteLoading by remember { mutableStateOf(true) }
    var muteSaving by remember { mutableStateOf(false) }
    var muteError by remember { mutableStateOf<String?>(null) }

    var contextTargetId by remember { mutableStateOf<Long?>(null) }
    var messageContext by remember { mutableStateOf<ConversationMessageContext?>(null) }
    var contextLoading by remember { mutableStateOf(false) }
    var contextError by remember { mutableStateOf<String?>(null) }
    var fullPhotoUrl by remember { mutableStateOf<String?>(null) }

    var activeVoiceUrl by remember { mutableStateOf<String?>(null) }
    var voicePlaying by remember { mutableStateOf(false) }
    var voicePreparing by remember { mutableStateOf(false) }
    var voiceFailedUrl by remember { mutableStateOf<String?>(null) }
    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(mediaPlayer) {
        mediaPlayer.setOnPreparedListener {
            voicePreparing = false
            voiceFailedUrl = null
            runCatching {
                it.start()
                voicePlaying = true
            }.onFailure {
                voicePlaying = false
                voiceFailedUrl = activeVoiceUrl
            }
        }
        mediaPlayer.setOnCompletionListener {
            voicePlaying = false
        }
        mediaPlayer.setOnErrorListener { _, _, _ ->
            voicePreparing = false
            voicePlaying = false
            voiceFailedUrl = activeVoiceUrl
            true
        }
        onDispose {
            runCatching { mediaPlayer.release() }
        }
    }

    fun toggleVoice(url: String) {
        if (url.isBlank()) return
        if (activeVoiceUrl == url && !voicePreparing) {
            runCatching {
                if (voicePlaying) {
                    mediaPlayer.pause()
                    voicePlaying = false
                } else {
                    mediaPlayer.start()
                    voicePlaying = true
                }
            }.onFailure {
                voicePlaying = false
                voiceFailedUrl = url
            }
            return
        }

        activeVoiceUrl = url
        voicePlaying = false
        voicePreparing = true
        voiceFailedUrl = null
        runCatching {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepareAsync()
        }.onFailure {
            voicePreparing = false
            voiceFailedUrl = url
        }
    }

    LaunchedEffect(Unit) {
        when (val result = repository.isMuted(conversationId)) {
            is ApiResult.Success -> {
                muted = result.value
                muteLoading = false
            }
            is ApiResult.Failure -> {
                muteLoading = false
                if (result.statusCode == 401) onSessionExpired() else muteError = result.message
            }
        }
    }

    LaunchedEffect(query) {
        val clean = query.trim()
        if (clean.isBlank()) {
            searchResults = emptyList()
            searchError = null
            searchLoading = false
            return@LaunchedEffect
        }
        delay(320)
        searchLoading = true
        searchError = null
        when (val result = repository.searchMessages(conversationId, clean)) {
            is ApiResult.Success -> searchResults = result.value
            is ApiResult.Failure -> {
                if (result.statusCode == 401) onSessionExpired() else searchError = result.message
            }
        }
        searchLoading = false
    }

    LaunchedEffect(tab, mediaType) {
        if (tab != V9ToolsTab.Media || mediaLoadedFor == mediaType) return@LaunchedEffect
        mediaLoading = true
        mediaError = null
        when (val result = repository.sharedMedia(conversationId, mediaType)) {
            is ApiResult.Success -> {
                mediaItems = result.value.items
                mediaCursor = result.value.nextCursor
                mediaLoadedFor = mediaType
            }
            is ApiResult.Failure -> {
                if (result.statusCode == 401) onSessionExpired() else mediaError = result.message
            }
        }
        mediaLoading = false
    }

    LaunchedEffect(contextTargetId) {
        val target = contextTargetId ?: run {
            messageContext = null
            contextError = null
            return@LaunchedEffect
        }
        contextLoading = true
        contextError = null
        when (val result = repository.messageContext(conversationId, target)) {
            is ApiResult.Success -> messageContext = result.value
            is ApiResult.Failure -> {
                if (result.statusCode == 401) onSessionExpired() else contextError = result.message
            }
        }
        contextLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NovaBackground,
        ) {
            if (contextTargetId != null) {
                V9ContextView(
                    username = username,
                    context = messageContext,
                    loading = contextLoading,
                    error = contextError,
                    activeVoiceUrl = activeVoiceUrl,
                    voicePlaying = voicePlaying,
                    voicePreparing = voicePreparing,
                    voiceFailedUrl = voiceFailedUrl,
                    onBack = { contextTargetId = null },
                    onJumpReply = { contextTargetId = it },
                    onOpenPhoto = { fullPhotoUrl = it },
                    onVoice = ::toggleVoice,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                ) {
                    V9DetailsHeader(onDismiss = onDismiss)
                    V9IdentitySummary(
                        username = username,
                        displayName = displayName,
                        avatarUrl = avatarUrl,
                    )
                    V9Tabs(tab = tab, onSelect = { tab = it })

                    when (tab) {
                        V9ToolsTab.Details -> V9DetailsView(
                            themeLabel = themeLabel,
                            onOpenTheme = onOpenTheme,
                            muted = muted,
                            loading = muteLoading || muteSaving,
                            error = muteError,
                            onToggleMute = {
                                if (!muteSaving) {
                                    muteSaving = true
                                    muteError = null
                                }
                            },
                        )
                        V9ToolsTab.Search -> V9SearchView(
                            query = query,
                            results = searchResults,
                            loading = searchLoading,
                            error = searchError,
                            onQueryChange = { query = it.take(200) },
                            onOpen = { contextTargetId = it.id },
                        )
                        V9ToolsTab.Media -> V9MediaView(
                            type = mediaType,
                            items = mediaItems,
                            nextCursor = mediaCursor,
                            loading = mediaLoading,
                            error = mediaError,
                            activeVoiceUrl = activeVoiceUrl,
                            voicePlaying = voicePlaying,
                            voicePreparing = voicePreparing,
                            voiceFailedUrl = voiceFailedUrl,
                            onType = {
                                mediaType = it
                                mediaLoadedFor = null
                                mediaItems = emptyList()
                                mediaCursor = null
                            },
                            onOpenContext = { contextTargetId = it.id },
                            onOpenPhoto = { fullPhotoUrl = it },
                            onVoice = ::toggleVoice,
                            onLoadMore = {
                                val cursor = mediaCursor ?: return@V9MediaView
                                if (!mediaLoading) {
                                    mediaLoading = true
                                    mediaError = null
                                    mediaLoadedFor = null
                                    mediaCursor = cursor
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(muteSaving) {
        if (!muteSaving) return@LaunchedEffect
        val desired = !muted
        when (val result = repository.setMuted(conversationId, desired)) {
            is ApiResult.Success -> muted = result.value
            is ApiResult.Failure -> {
                if (result.statusCode == 401) onSessionExpired() else muteError = result.message
            }
        }
        muteSaving = false
    }

    if (fullPhotoUrl != null) {
        Dialog(
            onDismissRequest = { fullPhotoUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                NovaMediaImage(
                    source = fullPhotoUrl.orEmpty(),
                    modifier = Modifier.fillMaxSize().padding(22.dp),
                    contentDescription = "Shared conversation photo",
                )
                Surface(
                    onClick = { fullPhotoUrl = null },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(18.dp),
                ) {
                    Text("×", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontSize = 24.sp)
                }
            }
        }
    }
}


@Composable
private fun V9DetailsHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(onClick = onDismiss, shape = CircleShape, color = NovaSurface, border = BorderStroke(1.dp, NovaBorder)) {
            Text("‹", modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp), color = NovaInk, fontSize = 27.sp)
        }
        Column(Modifier.weight(1f)) {
            Text("Conversation details", color = NovaInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Appearance, search, media and notifications", color = NovaMuted, fontSize = 11.sp)
        }
    }
}


@Composable
private fun V9IdentitySummary(username: String, displayName: String, avatarUrl: String) {
    val isGroup = username == "group"
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NovaAvatar(
                source = avatarUrl,
                fallbackText = displayName.ifBlank { if (isGroup) "Group" else username },
                size = 58.dp,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    displayName.ifBlank { if (isGroup) "Group conversation" else username },
                    color = NovaInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    if (isGroup) "Group conversation" else "@$username",
                    color = NovaMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}


@Composable
private fun V9Tabs(tab: V9ToolsTab, onSelect: (V9ToolsTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        V9TabChip("Details", tab == V9ToolsTab.Details) { onSelect(V9ToolsTab.Details) }
        V9TabChip("Search", tab == V9ToolsTab.Search) { onSelect(V9ToolsTab.Search) }
        V9TabChip("Media", tab == V9ToolsTab.Media) { onSelect(V9ToolsTab.Media) }
    }
}


@Composable
private fun V9TabChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = if (active) NovaAccent else NovaSurface,
        border = if (active) null else BorderStroke(1.dp, NovaBorder),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (active) NovaBackground else NovaMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}


@Composable
private fun V9SearchView(
    query: String,
    results: List<ConversationToolMessage>,
    loading: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onOpen: (ConversationToolMessage) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search messages", color = NovaMuted) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NovaAccent,
                unfocusedBorderColor = NovaBorder,
                cursorColor = NovaAccent,
                focusedContainerColor = NovaSurface,
                unfocusedContainerColor = NovaSurface,
            ),
        )
        Spacer(Modifier.height(10.dp))
        when {
            loading -> V9CenteredLoading()
            error != null -> V9CenteredText(error)
            query.isBlank() -> V9CenteredText("Search the text inside this conversation.")
            results.isEmpty() -> V9CenteredText("No messages found.")
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = { it.id }) { item ->
                    V9ResultCard(item = item, onClick = { onOpen(item) })
                }
            }
        }
    }
}


@Composable
private fun V9MediaView(
    type: String,
    items: List<ConversationToolMessage>,
    nextCursor: String?,
    loading: Boolean,
    error: String?,
    activeVoiceUrl: String?,
    voicePlaying: Boolean,
    voicePreparing: Boolean,
    voiceFailedUrl: String?,
    onType: (String) -> Unit,
    onOpenContext: (ConversationToolMessage) -> Unit,
    onOpenPhoto: (String) -> Unit,
    onVoice: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            V9MiniChip("All", type == "all") { onType("all") }
            V9MiniChip("Photos", type == "image") { onType("image") }
            V9MiniChip("Voice", type == "audio") { onType("audio") }
        }
        Spacer(Modifier.height(10.dp))

        when {
            loading && items.isEmpty() -> V9CenteredLoading()
            error != null && items.isEmpty() -> V9CenteredText(error)
            items.isEmpty() -> V9CenteredText("No shared media here yet.")
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    V9MediaCard(
                        item = item,
                        activeVoiceUrl = activeVoiceUrl,
                        voicePlaying = voicePlaying,
                        voicePreparing = voicePreparing,
                        voiceFailedUrl = voiceFailedUrl,
                        onOpenContext = { onOpenContext(item) },
                        onOpenPhoto = onOpenPhoto,
                        onVoice = onVoice,
                    )
                }
                if (nextCursor != null) {
                    item {
                        Surface(
                            onClick = onLoadMore,
                            shape = RoundedCornerShape(16.dp),
                            color = NovaSurface,
                            border = BorderStroke(1.dp, NovaBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (loading) "Loading…" else "Load more",
                                modifier = Modifier.padding(12.dp),
                                color = NovaAccent,
                                textAlign = TextAlign.Center,
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
private fun V9DetailsView(
    themeLabel: String,
    onOpenTheme: () -> Unit,
    muted: Boolean,
    loading: Boolean,
    error: String?,
    onToggleMute: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Appearance", color = NovaInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Surface(
            onClick = onOpenTheme,
            shape = RoundedCornerShape(20.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Chat theme", color = NovaInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Choose the colors and mood for this conversation.",
                        color = NovaMuted,
                        fontSize = 11.sp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = NovaAccentSoft,
                ) {
                    Text(
                        themeLabel,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        color = NovaAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Text("Notifications", color = NovaInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Surface(
            onClick = { if (!loading) onToggleMute() },
            shape = RoundedCornerShape(20.dp),
            color = NovaSurface,
            border = BorderStroke(1.dp, NovaBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Mute messages", color = NovaInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        if (muted) "Push notifications are muted. Messages still arrive normally." else "Receive push notifications for new messages.",
                        color = NovaMuted,
                        fontSize = 11.sp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = if (muted) NovaAccent else NovaAccentSoft,
                ) {
                    Text(
                        when {
                            loading -> "…"
                            muted -> "Muted"
                            else -> "On"
                        },
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        color = if (muted) NovaBackground else NovaAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (error != null) Text(error, color = NovaMuted, fontSize = 11.sp)
    }
}


@Composable
private fun V9ContextView(
    username: String,
    context: ConversationMessageContext?,
    loading: Boolean,
    error: String?,
    activeVoiceUrl: String?,
    voicePlaying: Boolean,
    voicePreparing: Boolean,
    voiceFailedUrl: String?,
    onBack: () -> Unit,
    onJumpReply: (Long) -> Unit,
    onOpenPhoto: (String) -> Unit,
    onVoice: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(onClick = onBack, shape = CircleShape, color = NovaSurface, border = BorderStroke(1.dp, NovaBorder)) {
                Text("‹", modifier = Modifier.padding(horizontal = 13.dp, vertical = 5.dp), color = NovaInk, fontSize = 27.sp)
            }
            Column {
                Text("Message context", color = NovaInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("@$username", color = NovaMuted, fontSize = 11.sp)
            }
        }

        when {
            loading -> V9CenteredLoading()
            error != null -> V9CenteredText(error)
            context == null -> V9CenteredText("Message unavailable.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (context.hasEarlier) {
                    item { V9ContextEdge("Earlier messages exist above this context") }
                }
                items(context.items, key = { it.id }) { item ->
                    V9ContextMessageCard(
                        item = item,
                        highlighted = item.id == context.targetMessageId,
                        activeVoiceUrl = activeVoiceUrl,
                        voicePlaying = voicePlaying,
                        voicePreparing = voicePreparing,
                        voiceFailedUrl = voiceFailedUrl,
                        onJumpReply = onJumpReply,
                        onOpenPhoto = onOpenPhoto,
                        onVoice = onVoice,
                    )
                }
                if (context.hasLater) {
                    item { V9ContextEdge("Later messages exist below this context") }
                }
            }
        }
    }
}


@Composable
private fun V9ResultCard(item: ConversationToolMessage, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("@${item.sender.username}", color = NovaAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(formatV9Timestamp(item.createdAt), color = NovaMuted, fontSize = 9.sp)
            }
            Spacer(Modifier.height(5.dp))
            Text(v9Preview(item), color = NovaInk, fontSize = 13.sp, maxLines = 3)
            Text("Open message context →", color = NovaMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}


@Composable
private fun V9MediaCard(
    item: ConversationToolMessage,
    activeVoiceUrl: String?,
    voicePlaying: Boolean,
    voicePreparing: Boolean,
    voiceFailedUrl: String?,
    onOpenContext: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onVoice: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(19.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("@${item.sender.username}", color = NovaAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(formatV9Timestamp(item.createdAt), color = NovaMuted, fontSize = 9.sp)
            }
            if (item.imageUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(onClick = { onOpenPhoto(item.imageUrl) }, shape = RoundedCornerShape(15.dp), color = NovaBackground) {
                    NovaMediaImage(
                        source = item.imageUrl,
                        modifier = Modifier.fillMaxWidth().height(210.dp),
                        contentDescription = "Shared photo",
                    )
                }
            }
            if (item.audioUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                V9VoiceRow(
                    url = item.audioUrl,
                    durationMs = item.audioDurationMs,
                    activeVoiceUrl = activeVoiceUrl,
                    voicePlaying = voicePlaying,
                    voicePreparing = voicePreparing,
                    voiceFailedUrl = voiceFailedUrl,
                    onVoice = onVoice,
                )
            }
            if (item.body.isNotBlank()) {
                Text(item.body, color = NovaInk, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp), maxLines = 3)
            }
            Surface(onClick = onOpenContext, shape = RoundedCornerShape(12.dp), color = NovaAccentSoft, modifier = Modifier.padding(top = 9.dp)) {
                Text("View in context", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = NovaAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
private fun V9ContextMessageCard(
    item: ConversationToolMessage,
    highlighted: Boolean,
    activeVoiceUrl: String?,
    voicePlaying: Boolean,
    voicePreparing: Boolean,
    voiceFailedUrl: String?,
    onJumpReply: (Long) -> Unit,
    onOpenPhoto: (String) -> Unit,
    onVoice: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (highlighted) NovaAccentSoft else NovaSurface,
        border = BorderStroke(1.dp, if (highlighted) NovaAccent else NovaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("@${item.sender.username}", color = NovaAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (highlighted) Text("TARGET", color = NovaAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            if (item.replyToId != null) {
                Surface(
                    onClick = { onJumpReply(item.replyToId) },
                    shape = RoundedCornerShape(11.dp),
                    color = NovaBackground,
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                ) {
                    Column(Modifier.padding(9.dp)) {
                        Text("Reply · tap to jump", color = NovaAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(item.replyPreview, color = NovaMuted, fontSize = 10.sp, maxLines = 2)
                    }
                }
            }
            if (item.imageUrl.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Surface(onClick = { onOpenPhoto(item.imageUrl) }, shape = RoundedCornerShape(14.dp), color = NovaBackground) {
                    NovaMediaImage(
                        source = item.imageUrl,
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                        contentDescription = "Message photo",
                    )
                }
            }
            if (item.audioUrl.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                V9VoiceRow(
                    url = item.audioUrl,
                    durationMs = item.audioDurationMs,
                    activeVoiceUrl = activeVoiceUrl,
                    voicePlaying = voicePlaying,
                    voicePreparing = voicePreparing,
                    voiceFailedUrl = voiceFailedUrl,
                    onVoice = onVoice,
                )
            }
            Text(
                if (item.isDeleted) "Message deleted" else item.body.ifBlank { v9Preview(item) },
                color = if (item.isDeleted) NovaMuted else NovaInk,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 7.dp),
            )
            Text(formatV9Timestamp(item.createdAt), color = NovaMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}


@Composable
private fun V9VoiceRow(
    url: String,
    durationMs: Long?,
    activeVoiceUrl: String?,
    voicePlaying: Boolean,
    voicePreparing: Boolean,
    voiceFailedUrl: String?,
    onVoice: (String) -> Unit,
) {
    val active = activeVoiceUrl == url
    Surface(
        onClick = { onVoice(url) },
        shape = RoundedCornerShape(15.dp),
        color = NovaAccentSoft,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                when {
                    voiceFailedUrl == url -> "!"
                    active && voicePreparing -> "…"
                    active && voicePlaying -> "❚❚"
                    else -> "▶"
                },
                color = NovaAccent,
                fontWeight = FontWeight.Bold,
            )
            Column {
                Text("Voice message", color = NovaInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(formatV9Duration(durationMs ?: 0L), color = NovaMuted, fontSize = 10.sp)
            }
        }
    }
}


@Composable
private fun V9MiniChip(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = if (active) NovaAccentSoft else NovaSurface,
        border = BorderStroke(1.dp, if (active) NovaAccent else NovaBorder),
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = if (active) NovaAccent else NovaMuted, fontSize = 11.sp)
    }
}


@Composable
private fun V9CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = NovaAccent)
    }
}


@Composable
private fun V9CenteredText(text: String) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(text, color = NovaMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}


@Composable
private fun V9ContextEdge(text: String) {
    Text(text, modifier = Modifier.fillMaxWidth().padding(8.dp), color = NovaMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
}


private fun v9Preview(item: ConversationToolMessage): String = when {
    item.isDeleted -> "Message deleted"
    item.body.isNotBlank() -> item.body
    item.audioUrl.isNotBlank() -> "🎤 Voice message"
    item.imageUrl.isNotBlank() -> "📷 Photo"
    else -> "Message"
}

private fun formatV9Duration(durationMs: Long): String {
    val seconds = (durationMs.coerceAtLeast(0L) / 1000L).coerceAtMost(5 * 60L)
    return "%d:%02d".format(Locale.US, seconds / 60L, seconds % 60L)
}

private fun formatV9Timestamp(value: String): String {
    val instant = runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { Instant.parse(value) }
        .getOrNull() ?: return ""
    return DateTimeFormatter.ofPattern("MMM d · h:mm a", Locale.getDefault())
        .format(instant.atZone(ZoneId.systemDefault()))
}
