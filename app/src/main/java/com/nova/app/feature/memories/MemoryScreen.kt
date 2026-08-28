package com.nova.app.feature.memories

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.app.appContainer
import com.nova.app.feature.memories.domain.model.MemoryHighlight
import com.nova.app.feature.memories.domain.model.MemoryDraft
import com.nova.app.feature.memories.domain.model.MemoryStats
import com.nova.app.feature.memories.domain.model.WeeklyMemory
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBackButton
import com.nova.app.ui.components.NovaIconButton
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.components.NovaPrimaryButton
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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import kotlinx.coroutines.delay


@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    onPersonClick: (String) -> Unit,
    onRoomClick: (Long) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.memoryRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { MemoryStateOwner(repository, scope) }
    val state = owner.state
    var showDraftComposer by remember { mutableStateOf(false) }
    var draftMedia by remember { mutableStateOf<Uri?>(null) }
    var selectedDraft by remember { mutableStateOf<MemoryDraft?>(null) }
    var awaitingDraftSave by remember { mutableStateOf(false) }
    var sawDraftSaving by remember { mutableStateOf(false) }
    val draftMediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        draftMedia = it
        if (it != null) showDraftComposer = true
    }

    BackHandler(onBack = onBack)
    LaunchedEffect(owner) {
        owner.load(
            utcOffsetMinutes = memoryUtcOffsetMinutes(),
            weeksAgo = 0,
            showSpinner = true,
        )
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }
    LaunchedEffect(state.savingDraft, state.error, awaitingDraftSave) {
        if (awaitingDraftSave && state.savingDraft) sawDraftSaving = true
        if (awaitingDraftSave && sawDraftSaving && !state.savingDraft) {
            if (state.error == null) {
                showDraftComposer = false
                draftMedia = null
                selectedDraft = null
            }
            awaitingDraftSave = false
            sawDraftSaving = false
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = NovaBackground) {
        when {
            state.loading && state.memory == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = NovaAccent)
            }

            state.memory == null -> MemoryError(
                message = state.error ?: "Nova couldn't build this memory.",
                onBack = onBack,
                onRetry = {
                    owner.load(memoryUtcOffsetMinutes(), state.weeksAgo, showSpinner = true)
                },
            )

            else -> MemoryContent(
                memory = state.memory,
                loading = state.loading,
                error = state.error,
                onBack = onBack,
                onOlder = {
                    owner.load(
                        memoryUtcOffsetMinutes(),
                        (state.weeksAgo + 1).coerceAtMost(51),
                        showSpinner = true,
                    )
                },
                onNewer = if (state.weeksAgo > 0) {
                    {
                        owner.load(
                            memoryUtcOffsetMinutes(),
                            state.weeksAgo - 1,
                            showSpinner = true,
                        )
                    }
                } else {
                    null
                },
                onRetry = {
                    owner.load(memoryUtcOffsetMinutes(), state.weeksAgo, showSpinner = false)
                },
                onPersonClick = onPersonClick,
                onRoomClick = onRoomClick,
                onShare = { shareMemory(context, state.memory) },
                drafts = state.drafts,
                savingDraft = state.savingDraft,
                deletingDraftId = state.deletingDraftId,
                onNewDraft = { showDraftComposer = true },
                onEditDraft = { draft ->
                    selectedDraft = draft
                    draftMedia = null
                    showDraftComposer = true
                },
                onDeleteDraft = owner::deleteDraft,
            )
        }
    }

    if (showDraftComposer) {
        MemoryDraftDialog(
            initialDraft = selectedDraft,
            mediaUri = draftMedia,
            saving = state.savingDraft,
            error = state.error,
            onPickMedia = { draftMediaPicker.launch(arrayOf("image/*", "video/*")) },
            onDismiss = {
                if (!state.savingDraft) {
                    showDraftComposer = false
                    draftMedia = null
                    selectedDraft = null
                    awaitingDraftSave = false
                    sawDraftSaving = false
                }
            },
            onSave = { kind, title, note ->
                awaitingDraftSave = true
                val draft = selectedDraft
                if (draft == null) owner.createDraft(kind, title, note, draftMedia)
                else owner.updateDraft(draft.id, kind, title, note, draftMedia)
            },
            onAutoSave = { kind, title, note ->
                selectedDraft?.let { owner.updateDraft(it.id, kind, title, note, draftMedia) }
            },
        )
    }
}


@Composable
private fun MemoryContent(
    memory: WeeklyMemory,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onOlder: () -> Unit,
    onNewer: (() -> Unit)?,
    onRetry: () -> Unit,
    onPersonClick: (String) -> Unit,
    onRoomClick: (Long) -> Unit,
    onShare: () -> Unit,
    drafts: List<MemoryDraft>,
    savingDraft: Boolean,
    deletingDraftId: Long?,
    onNewDraft: () -> Unit,
    onEditDraft: (MemoryDraft) -> Unit,
    onDeleteDraft: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 14.dp,
            bottom = 36.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            MemoryHeader(
                memory = memory,
                loading = loading,
                onBack = onBack,
                onShare = onShare,
            )
        }

        item { StatsCard(memory.stats) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle("Recent drafts")
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        onClick = onNewDraft,
                        enabled = !savingDraft,
                        shape = RoundedCornerShape(16.dp),
                        color = NovaAccent,
                    ) {
                        Text(
                            "+ New Memory",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = NovaBackground,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (drafts.isEmpty()) {
                    Text("Start a recap or film and Nova will autosave it here.", color = NovaMuted, fontSize = 10.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(drafts, key = { it.id }) { draft ->
                            Surface(
                                onClick = { onEditDraft(draft) },
                                modifier = Modifier.width(190.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = NovaSurface,
                                border = BorderStroke(1.dp, NovaBorder),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(draft.title, color = NovaInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(draft.kind.uppercase(), color = NovaAccent, fontSize = 8.sp)
                                    if (draft.note.isNotBlank()) {
                                        Text(draft.note, color = NovaMuted, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    TextButton(
                                        onClick = { onDeleteDraft(draft.id) },
                                        enabled = deletingDraftId == null,
                                    ) {
                                        Text(if (deletingDraftId == draft.id) "Deleting…" else "Delete draft", fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (memory.people.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Your people")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(memory.people, key = { it.person.id }) { row ->
                            Surface(
                                onClick = { onPersonClick(row.person.username) },
                                modifier = Modifier.width(110.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = NovaSurface,
                                border = BorderStroke(1.dp, NovaBorder),
                            ) {
                                Column(
                                    modifier = Modifier.padding(11.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    NovaAvatar(
                                        source = row.person.avatarUrl,
                                        fallbackText = row.person.name.ifBlank { row.person.username },
                                        size = 42.dp,
                                    )
                                    Spacer(modifier = Modifier.height(7.dp))
                                    Text(
                                        text = row.person.name.ifBlank { row.person.username },
                                        color = NovaInk,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${row.sharedCount} shared",
                                        color = NovaMuted,
                                        fontSize = 8.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (memory.rooms.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Rooms you lived in")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(memory.rooms, key = { it.room.id }) { row ->
                            Surface(
                                onClick = { onRoomClick(row.room.id) },
                                modifier = Modifier.width(190.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = NovaSurface,
                                border = BorderStroke(1.dp, NovaBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    NovaAvatar(
                                        source = row.room.avatarUrl,
                                        fallbackText = row.room.title,
                                        size = 40.dp,
                                    )
                                    Spacer(modifier = Modifier.width(9.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = row.room.title,
                                            color = NovaInk,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${row.sharedCount} shared things",
                                            color = NovaMuted,
                                            fontSize = 8.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle("Your week") }

        if (memory.highlights.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        NovaIcon(
                            asset = NovaIconAsset.Memory,
                            contentDescription = null,
                            tint = NovaAccent,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A quiet week is still a week.",
                            color = NovaInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Nova didn't find shared moments to replay here.",
                            color = NovaMuted,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        } else {
            items(
                items = memory.highlights,
                key = { "${it.source}-${it.id}" },
            ) { highlight ->
                MemoryHighlightCard(
                    highlight = highlight,
                    utcOffsetMinutes = memory.utcOffsetMinutes,
                    onPersonClick = onPersonClick,
                )
            }
        }

        if (!error.isNullOrBlank()) {
            item {
                Surface(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = NovaAccentSoft,
                ) {
                    Text(
                        text = "$error · Tap to retry",
                        modifier = Modifier.padding(12.dp),
                        color = NovaMuted,
                        fontSize = 9.sp,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onNewer != null) {
                    MemoryNavButton(
                        text = "Newer week",
                        onClick = onNewer,
                        modifier = Modifier.weight(1f),
                    )
                }
                MemoryNavButton(
                    text = "Older week",
                    onClick = onOlder,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}


@Composable
private fun MemoryHeader(
    memory: WeeklyMemory,
    loading: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NovaBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (memory.weeksAgo == 0) "Your week" else "Your week · ${memory.weeksAgo} ago",
                    color = NovaInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = memoryDateRange(memory),
                    color = NovaMuted,
                    fontSize = 10.sp,
                )
            }
            NovaIconButton(
                asset = NovaIconAsset.Share,
                contentDescription = "Share Your week",
                onClick = onShare,
                size = 48.dp,
                containerColor = NovaAccentSoft,
                contentColor = NovaAccent,
                borderColor = NovaAccent.copy(alpha = 0.22f),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF10131B),
            border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.26f)),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (memory.stats.highlights > 0) "Your week is ready." else "A quiet week, remembered.",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = memoryHeroLine(memory.stats),
                    color = Color(0xFFB6BCC9),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
                if (loading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = NovaAccent,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}


@Composable
private fun StatsCard(stats: MemoryStats) {
    val chips = listOf(
        "${stats.highlights} moments",
        "${stats.nights} nights",
        "${stats.people} people",
        "${stats.rooms} rooms",
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(chips) { label ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = NovaInk,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}


@Composable
private fun MemoryHighlightCard(
    highlight: MemoryHighlight,
    utcOffsetMinutes: Int,
    onPersonClick: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column {
            if (highlight.mediaType == "image" && highlight.mediaUrl.isNotBlank()) {
                NovaMediaImage(
                    source = highlight.mediaUrl,
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentDescription = "Memory image",
                )
            } else if (highlight.mediaType == "video" && highlight.mediaUrl.isNotBlank()) {
                NovaVideoPlayer(
                    source = highlight.mediaUrl,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    autoplay = false,
                    useController = true,
                    description = "Memory video",
                )
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = memorySourceLabel(highlight.source),
                        color = NovaAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = memoryOccurredAt(highlight.occurredAt, utcOffsetMinutes),
                        color = NovaMuted,
                        fontSize = 8.sp,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    highlight.room?.let { room ->
                        Text(
                            text = room.title,
                            color = NovaMuted,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (highlight.title.isNotBlank()) {
                    Text(
                        text = highlight.title,
                        color = NovaInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (highlight.text.isNotBlank()) {
                    Text(
                        text = highlight.text,
                        color = NovaInk,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
                if (highlight.url.isNotBlank()) {
                    Text(
                        text = highlight.url,
                        color = NovaAccent,
                        fontSize = 9.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                highlight.person?.takeIf { it.username.isNotBlank() }?.let { person ->
                    Surface(
                        onClick = { onPersonClick(person.username) },
                        shape = RoundedCornerShape(14.dp),
                        color = NovaAccentSoft,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NovaAvatar(
                                source = person.avatarUrl,
                                fallbackText = person.name.ifBlank { person.username },
                                size = 24.dp,
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = person.name.ifBlank { "@${person.username}" },
                                color = NovaInk,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = NovaInk,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}


@Composable
private fun MemoryNavButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = NovaAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}


@Composable
private fun MemoryDraftDialog(
    initialDraft: MemoryDraft?,
    mediaUri: Uri?,
    saving: Boolean,
    error: String?,
    onPickMedia: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onAutoSave: (String, String, String) -> Unit,
) {
    var kind by remember(initialDraft?.id) { mutableStateOf(initialDraft?.kind ?: "recap") }
    var title by remember(initialDraft?.id) { mutableStateOf(initialDraft?.title.orEmpty()) }
    var note by remember(initialDraft?.id) { mutableStateOf(initialDraft?.note.orEmpty()) }
    LaunchedEffect(initialDraft?.id, kind, title, note, mediaUri) {
        if (initialDraft != null && title.isNotBlank()) {
            delay(750)
            onAutoSave(kind, title, note)
        }
    }
    val previewSource = mediaUri?.toString() ?: initialDraft?.mediaUrl.orEmpty()
    val previewType = if (mediaUri != null) {
        LocalContext.current.contentResolver.getType(mediaUri).orEmpty()
    } else {
        initialDraft?.mediaType.orEmpty()
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NovaBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NovaBackButton(onClick = onDismiss)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (initialDraft == null) "New Memory" else "Edit Memory",
                            color = NovaInk,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Keep the part of the week that mattered to you.",
                            color = NovaMuted,
                            fontSize = 10.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("recap" to "Recap", "film" to "Film").forEach { (value, label) ->
                        Surface(
                            onClick = { kind = value },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (kind == value) NovaAccent else NovaAccentSoft,
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(14.dp),
                                color = if (kind == value) NovaBackground else NovaAccent,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    placeholder = { Text("Seoul nights") },
                    supportingText = { Text("${title.length}/120") },
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 500) note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What should this Memory keep?") },
                    minLines = 3,
                    maxLines = 7,
                    supportingText = { Text("${note.length}/500") },
                )
                if (previewSource.isNotBlank()) {
                    if (previewType.startsWith("video") || initialDraft?.mediaType == "video") {
                        NovaVideoPlayer(
                            source = previewSource,
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            autoplay = false,
                            useController = true,
                            description = "Selected Memory video",
                        )
                    } else {
                        NovaMediaImage(
                            source = previewSource,
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                            contentDescription = "Selected Memory photo",
                        )
                    }
                }
                Surface(
                    onClick = onPickMedia,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = NovaAccentSoft,
                ) {
                    Text(
                        if (previewSource.isBlank()) "Add a photo or video" else "Change selected media",
                        modifier = Modifier.padding(14.dp),
                        color = NovaAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        color = NovaMuted,
                        fontSize = 10.sp,
                    )
                }
                }
                Spacer(modifier = Modifier.height(12.dp))
                NovaPrimaryButton(
                text = if (saving) "Saving…" else if (initialDraft == null) "Save draft" else "Done",
                onClick = { onSave(kind, title, note) },
                enabled = title.isNotBlank() && !saving,
                )
            }
        }
    }
}


@Composable
private fun MemoryError(
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = NovaMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = onBack, shape = RoundedCornerShape(16.dp), color = NovaSurface) {
                Text("Back", modifier = Modifier.padding(12.dp), color = NovaInk)
            }
            Surface(onClick = onRetry, shape = RoundedCornerShape(16.dp), color = NovaAccent) {
                Text("Retry", modifier = Modifier.padding(12.dp), color = NovaBackground)
            }
        }
    }
}


private fun memoryDateRange(memory: WeeklyMemory): String {
    val start = parseMemoryTime(memory.startsAt)?.plusMinutes(memory.utcOffsetMinutes.toLong())
    val end = parseMemoryTime(memory.endsAt)
        ?.plusMinutes(memory.utcOffsetMinutes.toLong())
        ?.minusDays(1)
    if (start == null || end == null) return "A completed week"
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    return "${start.format(formatter)} – ${end.format(formatter)}"
}


private fun memoryOccurredAt(raw: String, utcOffsetMinutes: Int): String {
    val value = parseMemoryTime(raw)?.plusMinutes(utcOffsetMinutes.toLong()) ?: return ""
    return value.format(DateTimeFormatter.ofPattern("EEE · h:mm a"))
}


private fun parseMemoryTime(raw: String): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(raw) }.getOrNull()


private fun memorySourceLabel(source: String): String = when (source) {
    "pulse" -> "PULSE"
    "post" -> "POST"
    "room_item" -> "ROOM"
    else -> "MEMORY"
}


private fun memoryHeroLine(stats: MemoryStats): String = when {
    stats.highlights == 0 -> "Nothing had to happen for the week to belong to you."
    stats.people > 0 && stats.nights > 0 -> "${stats.highlights} things · ${stats.people} people · ${stats.nights} nights"
    stats.people > 0 -> "${stats.highlights} things · ${stats.people} people"
    else -> "${stats.highlights} things worth keeping"
}


private fun shareMemory(context: android.content.Context, memory: WeeklyMemory) {
    val text = buildString {
        append("My Nova week · ${memoryDateRange(memory)}\n")
        append("${memory.stats.highlights} moments")
        if (memory.stats.nights > 0) append(" · ${memory.stats.nights} nights")
        if (memory.stats.people > 0) append(" · ${memory.stats.people} people")
        if (memory.stats.rooms > 0) append(" · ${memory.stats.rooms} rooms")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share your Nova week"))
}


private fun memoryUtcOffsetMinutes(): Int =
    TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
