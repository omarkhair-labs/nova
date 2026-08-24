package com.nova.app.feature.pulse

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.app.appContainer
import com.nova.app.feature.pulse.domain.model.NovaPulse
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaErrorState
import com.nova.app.ui.components.NovaLoadingState
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBaseLive
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType
import kotlinx.coroutines.delay


@Composable
fun PulseScreen(
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = context.appContainer.pulseRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { PulseStateOwner(repository, scope) }
    val state = owner.state
    var composerVisible by remember { mutableStateOf(false) }
    var pendingMedia by remember { mutableStateOf<Uri?>(null) }
    var selectedPulse by remember { mutableStateOf<NovaPulse?>(null) }
    var selectedCategory by remember { mutableStateOf("all") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingMedia = uri
            composerVisible = true
            owner.clearError()
        }
    }

    LaunchedEffect(owner) { owner.load(showSpinner = true) }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }
    LaunchedEffect(state.createdVersion) {
        if (state.createdVersion > 0) {
            composerVisible = false
            pendingMedia = null
        }
    }

    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = null,
                onHomeClick = onHomeClick,
                onOrbitClick = onOrbitClick,
                onCreateClick = onCreateClick,
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
                    ) {
                        Text("Pulse", color = NovaInk, style = NovaType.pageTitle)
                        Surface(shape = MaterialTheme.shapes.small, color = NovaBaseLive) {
                            Text(
                                "LIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = NovaSurface,
                                style = NovaType.badge,
                            )
                        }
                    }
                    Text(
                        "What’s happening in your orbit",
                        color = NovaMuted,
                        style = NovaType.bodyCompact,
                    )
                }
                Surface(
                    onClick = {
                        pendingMedia = null
                        owner.clearError()
                        composerVisible = true
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = NovaAccent,
                ) {
                    Text(
                        "Create",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = NovaSurface,
                        style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = NovaSpacing.lg, vertical = NovaSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
            ) {
                listOf("all" to "All", "live" to "Live", "music" to "Music", "talks" to "Talks", "vibes" to "Vibes").forEach { (value, label) ->
                    Surface(
                        onClick = { selectedCategory = value },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                        color = if (selectedCategory == value) NovaAccent else NovaSurface,
                        border = BorderStroke(1.dp, if (selectedCategory == value) NovaAccent else NovaBorder),
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                            color = if (selectedCategory == value) NovaSurface else NovaInk,
                            style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }

            val visiblePulses = if (selectedCategory == "all") state.pulses else state.pulses.filter { it.category == selectedCategory }

            when {
                state.loading && state.pulses.isEmpty() -> NovaLoadingState(
                    message = "Finding live moments…",
                    modifier = Modifier.weight(1f),
                )

                state.error != null && state.pulses.isEmpty() -> NovaErrorState(
                    title = "Pulse is quiet right now",
                    message = state.error,
                    onRetry = { owner.load(showSpinner = true) },
                    modifier = Modifier.weight(1f),
                )

                visiblePulses.isEmpty() -> NovaEmptyState(
                    title = "Start the first Pulse",
                    message = "Share a live or short moment with your orbit.",
                    actionLabel = "Create Pulse",
                    onAction = { composerVisible = true },
                    modifier = Modifier.weight(1f),
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = NovaSpacing.lg,
                        end = NovaSpacing.lg,
                        bottom = NovaSpacing.xxl,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(NovaSpacing.md),
                ) {
                    items(visiblePulses, key = { it.id }) { pulse ->
                        PulseFeedCard(pulse = pulse, onClick = { selectedPulse = pulse })
                    }
                }
            }
        }
    }

    if (composerVisible) {
        PulseComposerDialog(
            title = "Create Pulse",
            subtitle = "Live for 12 hours in your orbit.",
            pendingMedia = pendingMedia,
            uploading = state.uploading,
            error = state.error,
            initialAudience = "followers",
            confirmLabel = "Go live",
            onPickMedia = { picker.launch(arrayOf("image/*", "video/*")) },
            onDismiss = {
                if (!state.uploading) {
                    composerVisible = false
                    pendingMedia = null
                    owner.clearError()
                }
            },
            onSubmit = { note, audience, category ->
                pendingMedia?.let { media -> owner.createMedia(media, note, audience, category) }
                    ?: owner.createText(note, audience, category)
            },
        )
    }

    selectedPulse?.let { pulse ->
        val viewerOwner = remember(pulse.id, repository, scope) {
            PulseViewerStateOwner(pulse, repository, scope)
        }
        val viewerState = viewerOwner.state
        LaunchedEffect(pulse.id) {
            viewerOwner.loadChain(pulse.id)
            viewerOwner.recordView(pulse.id)
            while (true) {
                delay(10_000)
                viewerOwner.loadChain(pulse.id)
            }
        }
        LaunchedEffect(viewerState.sessionExpiryVersion) {
            if (viewerState.sessionExpiryVersion > 0) onSessionExpired()
        }
        LaunchedEffect(viewerState.replyCreatedVersion) {
            if (viewerState.replyCreatedVersion > 0) owner.load()
        }
        PulseViewerDialog(
            initialPulse = pulse,
            state = viewerState,
            deletingPulseId = state.deletingPulseId,
            onDismiss = { selectedPulse = null },
            onClearError = viewerOwner::clearError,
            onDelete = { target ->
                owner.delete(target.id)
                selectedPulse = null
            },
            onReaction = { target, enabled -> viewerOwner.setReaction(target.id, enabled) },
            onReplyText = { parent, note, audience ->
                viewerOwner.replyText(parent.id, note, audience, parent.category)
            },
            onReplyMedia = { parent, media, note, audience ->
                viewerOwner.replyMedia(parent.id, media, note, audience, parent.category)
            },
        )
    }
}


@Composable
private fun PulseFeedCard(pulse: NovaPulse, onClick: () -> Unit) {
    val palette = PulseTheme.media
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.82f),
        shape = MaterialTheme.shapes.large,
        color = palette.background,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
        ) {
            when (pulse.mediaType) {
                "image" -> NovaMediaImage(
                    source = pulse.mediaUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "${pulse.author.username} Pulse",
                )
                "video" -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    NovaIcon(
                        asset = NovaIconAsset.Reels,
                        contentDescription = "Play Pulse",
                        tint = palette.ink,
                        modifier = Modifier.size(28.dp),
                    )
                }
                else -> Text(
                    text = pulse.note,
                    modifier = Modifier.padding(20.dp),
                    color = palette.ink,
                    style = NovaType.bodyCompact.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(palette.overlay)
                    .padding(NovaSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
            ) {
                NovaAvatar(
                    source = pulse.author.avatarUrl,
                    fallbackText = pulse.author.name.ifBlank { pulse.author.username },
                    size = 28.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        pulse.author.name.ifBlank { pulse.author.username },
                        color = palette.ink,
                        style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Text("live now", color = NovaBaseLive, style = NovaType.badge)
                }
            }
        }
    }
}
