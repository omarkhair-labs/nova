package com.nova.app.feature.pulse

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nova.app.feature.pulse.domain.model.NovaPulse
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaMediaImage
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


@Composable
fun PulseViewerDialog(
    initialPulse: NovaPulse,
    state: PulseViewerUiState,
    deletingPulseId: Long?,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onDelete: (NovaPulse) -> Unit,
    onReplyText: (NovaPulse, String, String) -> Unit,
    onReplyMedia: (NovaPulse, Uri, String, String) -> Unit,
) {
    var activePulseId by remember(initialPulse.id) { mutableStateOf(initialPulse.id) }
    var replyComposerVisible by remember(initialPulse.id) { mutableStateOf(false) }
    var replyMedia by remember(initialPulse.id) { mutableStateOf<Uri?>(null) }

    val palette = PulseTheme.media
    val chain = state.chain.ifEmpty { listOf(initialPulse) }
    val activePulse = chain.firstOrNull { it.id == activePulseId } ?: initialPulse
    val replyPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            replyMedia = uri
            replyComposerVisible = true
            onClearError()
        }
    }

    LaunchedEffect(state.replyCreatedVersion) {
        if (state.replyCreatedVersion > 0) {
            replyComposerVisible = false
            replyMedia = null
            activePulseId = state.chain.lastOrNull()?.id ?: activePulseId
        }
    }

    Dialog(
        onDismissRequest = { if (!state.replying) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(palette.background),
        ) {
            PulseViewerMedia(pulse = activePulse)

            Row(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NovaAvatar(
                    source = activePulse.author.avatarUrl,
                    fallbackText = activePulse.author.name.ifBlank { activePulse.author.username },
                    size = 38.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activePulse.isMine) {
                            "Your Pulse"
                        } else {
                            activePulse.author.name.ifBlank { activePulse.author.username }
                        },
                        color = palette.ink,
                        style = NovaType.bodyCompact.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = if (activePulse.replyToId == null) {
                            "@${activePulse.author.username} • live for 12h"
                        } else {
                            "@${activePulse.author.username} • moment reply • 12h"
                        },
                        color = palette.muted,
                        style = NovaType.micro,
                    )
                }
                Text(
                    text = "✕",
                    color = palette.ink,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable(enabled = !state.replying, onClick = onDismiss)
                        .padding(NovaSpacing.sm),
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 18.dp)
                    .animateContentSize(animationSpec = tween(durationMillis = NovaMotion.standard)),
                shape = MaterialTheme.shapes.large,
                color = palette.background.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, palette.panelBorder),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = NovaSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (activePulse.mediaType != "text" && activePulse.note.isNotBlank()) {
                        Text(
                            text = activePulse.note,
                            color = palette.ink,
                            style = NovaType.bodyCompact,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (chain.size <= 1) "Start a moment chain" else "Moment chain • ${chain.size}",
                            color = palette.muted,
                            style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                        )
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = NovaAccent,
                                strokeWidth = 2.dp,
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(chain, key = { it.id }) { pulse ->
                            PulseChainChip(
                                pulse = pulse,
                                selected = pulse.id == activePulse.id,
                                onClick = {
                                    activePulseId = pulse.id
                                    onClearError()
                                },
                            )
                        }
                    }

                    state.error?.let { error ->
                        Text(
                            text = error,
                            color = palette.muted,
                            style = NovaType.micro,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = {
                                replyMedia = null
                                replyComposerVisible = true
                                onClearError()
                            },
                            enabled = !state.replying,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            color = NovaAccent,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = 9.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (state.replying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = NovaBackground,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text(
                                        text = "↳ Reply with a moment",
                                        color = NovaBackground,
                                        style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }

                        if (activePulse.isMine) {
                            TextButton(
                                onClick = { onDelete(activePulse) },
                                enabled = deletingPulseId != activePulse.id && !state.replying,
                            ) {
                                if (deletingPulseId == activePulse.id) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = palette.ink,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("Delete", color = palette.ink, style = NovaType.meta)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (replyComposerVisible) {
        PulseComposerDialog(
            title = "Reply with a moment",
            subtitle = "Your reply becomes its own 12-hour Pulse.",
            pendingMedia = replyMedia,
            uploading = state.replying,
            error = state.error,
            initialAudience = activePulse.audience,
            initialCategory = activePulse.category,
            showCategory = false,
            confirmLabel = "Reply",
            onPickMedia = { replyPicker.launch(arrayOf("image/*", "video/*")) },
            onDismiss = {
                if (!state.replying) {
                    replyComposerVisible = false
                    replyMedia = null
                    onClearError()
                }
            },
            onSubmit = { note, audience, _ ->
                val media = replyMedia
                if (media == null) {
                    onReplyText(activePulse, note, audience)
                } else {
                    onReplyMedia(activePulse, media, note, audience)
                }
            },
        )
    }
}


@Composable
private fun PulseChainChip(
    pulse: NovaPulse,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) NovaAccentSoft else NovaSurface.copy(alpha = 0.9f),
        border = BorderStroke(
            1.dp,
            if (selected) NovaAccent else NovaBorder.copy(alpha = 0.8f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovaSpacing.sm, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaAvatar(
                source = pulse.author.avatarUrl,
                fallbackText = pulse.author.name.ifBlank { pulse.author.username },
                size = 24.dp,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = if (pulse.isMine) "You" else pulse.author.name.ifBlank { pulse.author.username },
                    color = if (selected) NovaAccent else NovaInk,
                    style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (pulse.replyToId == null) "start" else "reply",
                    color = NovaMuted,
                    style = NovaType.badge,
                )
            }
        }
    }
}


@Composable
private fun PulseViewerMedia(pulse: NovaPulse) {
    val palette = PulseTheme.media
    when (pulse.mediaType) {
        "image" -> NovaMediaImage(
            source = pulse.mediaUrl,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "${pulse.author.username} Pulse",
        )
        "video" -> PulseVideoPlayer(source = pulse.mediaUrl)
        else -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = pulse.note,
                modifier = Modifier.padding(horizontal = 34.dp),
                color = palette.ink,
                style = NovaType.screenTitle.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}


@Composable
private fun PulseVideoPlayer(source: String) {
    val context = LocalContext.current
    val player = remember(source) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            volume = 1f
            setMediaItem(MediaItem.fromUri(source))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
