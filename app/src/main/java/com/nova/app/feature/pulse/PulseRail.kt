package com.nova.app.feature.pulse

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.app.appContainer
import com.nova.app.feature.publishing.MediaPublishStatus
import com.nova.app.feature.publishing.MediaPublishTarget
import com.nova.app.feature.publishing.MediaPublishWorker
import com.nova.app.feature.publishing.MediaPublishingStateOwner
import com.nova.app.feature.pulse.domain.model.NovaPulse
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMotion
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType
import kotlinx.coroutines.delay


@Composable
fun PulseRail(
    displayName: String,
    username: String,
    avatarUrl: String,
    showCreateCard: Boolean = true,
    onOpenFeed: (() -> Unit)? = null,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.pulseRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { PulseStateOwner(repository, scope) }
    val state = owner.state
    val publishingOwner = remember(context, scope) { MediaPublishingStateOwner(context, scope) }
    val publishingState = publishingOwner.state
    val currentUserId = context.appContainer.currentCachedUserId()
    val pulsePublishes = publishingState.items.filter { it.target == MediaPublishTarget.PULSE }

    var composerVisible by remember { mutableStateOf(false) }
    var pendingMedia by remember { mutableStateOf<Uri?>(null) }
    var selectedPulse by remember { mutableStateOf<NovaPulse?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            pendingMedia = uri
            composerVisible = true
            owner.clearError()
        }
    }

    LaunchedEffect(owner, currentUserId) {
        owner.load(showSpinner = true)
        currentUserId?.let(publishingOwner::enter) ?: publishingOwner.reset()
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }
    LaunchedEffect(state.createdVersion) {
        if (state.createdVersion > 0) {
            composerVisible = false
            pendingMedia = null
        }
    }
    LaunchedEffect(publishingState.pulsePublishedVersion) {
        if (publishingState.pulsePublishedVersion > 0) owner.load()
    }

    Column(
        modifier = Modifier.animateContentSize(
            animationSpec = tween(durationMillis = NovaMotion.standard),
        ),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Pulse",
                    color = NovaInk,
                    style = NovaType.title.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "What’s happening in your orbit",
                    color = NovaMuted,
                    style = NovaType.micro,
                )
            }
            Text(
                text = if (state.error != null) "Retry" else if (onOpenFeed != null) "View all" else "LIVE",
                color = if (state.error != null) NovaAccent else com.nova.app.ui.theme.NovaBaseLive,
                style = NovaType.micro.copy(
                    fontWeight = if (state.error != null) FontWeight.SemiBold else FontWeight.Normal,
                ),
                modifier = if (state.error != null) {
                    Modifier.clickable { owner.load(showSpinner = true) }
                } else if (onOpenFeed != null) {
                    Modifier.clickable(onClick = onOpenFeed)
                } else {
                    Modifier
                },
            )
        }

        if (state.loading && state.pulses.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(132.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = NovaAccent,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (showCreateCard) {
                    item(key = "pulse-create") {
                        PulseCreateCard(
                            displayName = displayName,
                            username = username,
                            avatarUrl = avatarUrl,
                            uploading = state.uploading,
                            onText = {
                                pendingMedia = null
                                composerVisible = true
                                owner.clearError()
                            },
                            onMedia = { picker.launch(arrayOf("image/*", "video/*")) },
                        )
                    }
                }
                items(state.pulses, key = { it.id }) { pulse ->
                    PulseCard(pulse = pulse, onClick = { selectedPulse = pulse })
                }
            }
        }

        state.error?.let { error ->
            Text(
                text = error,
                color = NovaMuted,
                style = NovaType.meta,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (pulsePublishes.isNotEmpty()) {
            MediaPublishStatus(
                items = pulsePublishes,
                modifier = Modifier.fillMaxWidth(),
                onRetry = publishingOwner::retry,
                onCancel = publishingOwner::cancel,
            )
        }
    }

    if (composerVisible) {
        PulseComposerDialog(
            title = "New Pulse",
            subtitle = "Here now. Gone in 12 hours.",
            pendingMedia = pendingMedia,
            uploading = state.uploading,
            error = state.error,
            initialAudience = "followers",
            confirmLabel = "Post Pulse",
            onPickMedia = { picker.launch(arrayOf("image/*", "video/*")) },
            onDismiss = {
                if (!state.uploading) {
                    composerVisible = false
                    pendingMedia = null
                    owner.clearError()
                }
            },
            onSubmit = { note, audience, category ->
                val media = pendingMedia
                if (media == null) {
                    owner.createText(note, audience, category)
                } else {
                    currentUserId?.let { userId ->
                        MediaPublishWorker.enqueue(
                            context = context,
                            target = MediaPublishTarget.PULSE,
                            userId = userId,
                            sourceUri = media,
                            caption = note,
                            audience = audience,
                            category = category,
                        )
                        publishingOwner.enter(userId)
                        composerVisible = false
                        pendingMedia = null
                        owner.clearError()
                    } ?: onSessionExpired()
                }
            },
        )
    }

    selectedPulse?.let { pulse ->
        val viewerOwner = remember(pulse.id, repository, scope) {
            PulseViewerStateOwner(pulse, repository, scope)
        }
        val viewerState = viewerOwner.state

        LaunchedEffect(pulse.id) {
            viewerOwner.loadChain(pulse.id, showSpinner = true)
            viewerOwner.recordView(pulse.id)
            while (true) {
                delay(10_000)
                viewerOwner.loadChain(pulse.id, showSpinner = false)
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
                viewerOwner.replyText(parent.id, note, audience)
            },
            onReplyMedia = { parent, media, note, audience ->
                viewerOwner.replyMedia(parent.id, media, note, audience)
            },
        )
    }
}


@Composable
private fun PulseCreateCard(
    displayName: String,
    username: String,
    avatarUrl: String,
    uploading: Boolean,
    onText: () -> Unit,
    onMedia: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(148.dp).height(112.dp),
        shape = MaterialTheme.shapes.large,
        color = NovaAccentSoft,
        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.34f)),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NovaAvatar(
                    source = avatarUrl,
                    fallbackText = displayName.ifBlank { username },
                    size = 34.dp,
                )
                Spacer(modifier = Modifier.width(NovaSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Pulse",
                        color = NovaInk,
                        style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(text = "right now", color = NovaMuted, style = NovaType.micro)
                }
            }

            if (uploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp).align(Alignment.CenterHorizontally),
                    color = NovaAccent,
                    strokeWidth = 2.dp,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Surface(
                        onClick = onText,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        color = NovaSurface,
                    ) {
                        Text(
                            text = "Aa",
                            modifier = Modifier.padding(vertical = 7.dp),
                            color = NovaInk,
                            style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Surface(
                        onClick = onMedia,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        color = NovaAccent,
                    ) {
                        Text(
                            text = "+ media",
                            modifier = Modifier.padding(vertical = 7.dp),
                            color = NovaBackground,
                            style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun PulseCard(
    pulse: NovaPulse,
    onClick: () -> Unit,
) {
    val mediaPalette = PulseTheme.media
    Surface(
        onClick = onClick,
        modifier = Modifier.width(124.dp).height(104.dp),
        shape = MaterialTheme.shapes.large,
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (pulse.mediaType) {
                "image" -> NovaMediaImage(
                    source = pulse.mediaUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "${pulse.author.username} Pulse",
                )
                "video" -> Box(modifier = Modifier.fillMaxSize()) {
                    NovaMediaImage(
                        source = pulse.thumbnailUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "${pulse.author.username} Pulse video thumbnail",
                    )
                    NovaIcon(
                        asset = NovaIconAsset.Play,
                        contentDescription = "Play ${pulse.author.username} Pulse video",
                        modifier = Modifier.align(Alignment.Center),
                        tint = mediaPalette.ink,
                    )
                }
                else -> Box(
                    modifier = Modifier.fillMaxSize().background(NovaAccentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = pulse.note,
                        modifier = Modifier.padding(NovaSpacing.md),
                        color = NovaInk,
                        style = NovaType.bodyCompact.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(9.dp),
                shape = MaterialTheme.shapes.medium,
                color = mediaPalette.overlay,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NovaAvatar(
                        source = pulse.author.avatarUrl,
                        fallbackText = pulse.author.name.ifBlank { pulse.author.username },
                        size = 24.dp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pulse.isMine) "You" else pulse.author.name.ifBlank { pulse.author.username },
                        color = mediaPalette.ink,
                        style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (pulse.replyToId != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(9.dp),
                    shape = MaterialTheme.shapes.small,
                    color = mediaPalette.overlay,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NovaIcon(
                            asset = NovaIconAsset.Send,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = mediaPalette.ink,
                        )
                        Text(
                            text = "reply",
                            color = mediaPalette.ink,
                            style = NovaType.badge,
                        )
                    }
                }
            }

            if (pulse.mediaType != "text" && pulse.note.isNotBlank()) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(9.dp),
                    shape = MaterialTheme.shapes.small,
                    color = mediaPalette.overlay,
                ) {
                    Text(
                        text = pulse.note,
                        modifier = Modifier.padding(horizontal = NovaSpacing.sm, vertical = 5.dp),
                        color = mediaPalette.ink,
                        style = NovaType.micro,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
