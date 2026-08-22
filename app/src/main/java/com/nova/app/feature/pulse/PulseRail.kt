package com.nova.app.feature.pulse

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nova.app.app.appContainer
import com.nova.app.feature.pulse.domain.model.NovaPulse
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


private val PulseViewerBackground = Color(0xFF07090D)
private val PulseViewerInk = Color(0xFFF8F9FB)
private val PulseViewerMuted = Color(0xFFB7BDC8)


@Composable
fun PulseRail(
    displayName: String,
    username: String,
    avatarUrl: String,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.pulseRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { PulseStateOwner(repository, scope) }
    val state = owner.state

    var composerVisible by remember { mutableStateOf(false) }
    var pendingMedia by remember { mutableStateOf<Uri?>(null) }
    var selectedPulse by remember { mutableStateOf<NovaPulse?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingMedia = uri
            composerVisible = true
            owner.clearError()
        }
    }

    LaunchedEffect(Unit) { owner.load(showSpinner = true) }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }
    LaunchedEffect(state.createdVersion) {
        if (state.createdVersion > 0) {
            composerVisible = false
            pendingMedia = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Pulse",
                    color = NovaInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "What’s happening right now?",
                    color = NovaMuted,
                    fontSize = 10.sp,
                )
            }
            Text(
                text = if (state.error != null) "Retry" else "12h • live",
                color = if (state.error != null) NovaAccent else NovaMuted,
                fontSize = 10.sp,
                fontWeight = if (state.error != null) FontWeight.SemiBold else FontWeight.Normal,
                modifier = if (state.error != null) {
                    Modifier.clickable { owner.load(showSpinner = true) }
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
                items(state.pulses, key = { it.id }) { pulse ->
                    PulseCard(pulse = pulse, onClick = { selectedPulse = pulse })
                }
            }
        }

        state.error?.let { error ->
            Text(
                text = error,
                color = NovaMuted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (composerVisible) {
        PulseComposerDialog(
            pendingMedia = pendingMedia,
            uploading = state.uploading,
            error = state.error,
            onDismiss = {
                if (!state.uploading) {
                    composerVisible = false
                    pendingMedia = null
                    owner.clearError()
                }
            },
            onSubmit = { note, audience ->
                val media = pendingMedia
                if (media == null) {
                    owner.createText(note, audience)
                } else {
                    owner.createMedia(media, note, audience)
                }
            },
        )
    }

    selectedPulse?.let { pulse ->
        PulseViewerDialog(
            pulse = pulse,
            deleting = state.deletingPulseId == pulse.id,
            onDismiss = { selectedPulse = null },
            onDelete = {
                owner.delete(pulse.id)
                selectedPulse = null
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
        modifier = Modifier.width(176.dp).height(132.dp),
        shape = RoundedCornerShape(24.dp),
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
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Pulse",
                        color = NovaInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = "right now", color = NovaMuted, fontSize = 9.sp)
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
                        shape = RoundedCornerShape(13.dp),
                        color = NovaSurface,
                    ) {
                        Text(
                            text = "Aa",
                            modifier = Modifier.padding(vertical = 7.dp),
                            color = NovaInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    Surface(
                        onClick = onMedia,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        color = NovaAccent,
                    ) {
                        Text(
                            text = "+ media",
                            modifier = Modifier.padding(vertical = 7.dp),
                            color = NovaBackground,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
    Surface(
        onClick = onClick,
        modifier = Modifier.width(176.dp).height(132.dp),
        shape = RoundedCornerShape(24.dp),
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
                "video" -> Box(
                    modifier = Modifier.fillMaxSize().background(PulseViewerBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "▶",
                        color = PulseViewerInk,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                else -> Box(
                    modifier = Modifier.fillMaxSize().background(NovaAccentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = pulse.note,
                        modifier = Modifier.padding(16.dp),
                        color = NovaInk,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(9.dp),
                shape = RoundedCornerShape(16.dp),
                color = PulseViewerBackground.copy(alpha = if (pulse.mediaType == "text") 0.74f else 0.72f),
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
                        color = PulseViewerInk,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (pulse.mediaType != "text" && pulse.note.isNotBlank()) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(9.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = PulseViewerBackground.copy(alpha = 0.76f),
                ) {
                    Text(
                        text = pulse.note,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = PulseViewerInk,
                        fontSize = 9.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}


@Composable
private fun PulseComposerDialog(
    pendingMedia: Uri?,
    uploading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var note by remember(pendingMedia) { mutableStateOf("") }
    var audience by remember(pendingMedia) { mutableStateOf("followers") }
    val mime = remember(pendingMedia) {
        pendingMedia?.let { context.contentResolver.getType(it).orEmpty().lowercase() }.orEmpty()
    }
    val canSubmit = !uploading && (pendingMedia != null || note.trim().isNotEmpty()) && note.length <= 180

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("New Pulse", color = NovaInk, fontWeight = FontWeight.Bold)
                Text("Here now. Gone in 12 hours.", color = NovaMuted, fontSize = 11.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (pendingMedia != null) {
                    if (mime.startsWith("image/")) {
                        NovaMediaImage(
                            source = pendingMedia.toString(),
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentDescription = "Selected Pulse photo",
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(86.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = NovaAccentSoft,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("▶ Video selected", color = NovaInk, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 180) note = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uploading,
                    label = { Text(if (pendingMedia == null) "What are you doing?" else "Add a note") },
                    supportingText = { Text("${note.length}/180") },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        focusedLabelColor = NovaAccent,
                        cursorColor = NovaAccent,
                    ),
                )

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Audience", color = NovaMuted, fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulseAudienceChoice(
                            label = "Followers",
                            selected = audience == "followers",
                            enabled = !uploading,
                            onClick = { audience = "followers" },
                        )
                        PulseAudienceChoice(
                            label = "Close Friends",
                            selected = audience == "close_friends",
                            enabled = !uploading,
                            onClick = { audience = "close_friends" },
                        )
                    }
                }

                error?.let {
                    Text(text = it, color = NovaMuted, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(note.trim(), audience) },
                enabled = canSubmit,
            ) {
                if (uploading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NovaAccent, strokeWidth = 2.dp)
                } else {
                    Text("Post Pulse", color = NovaAccent, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uploading) {
                Text("Cancel", color = NovaMuted)
            }
        },
        containerColor = NovaSurface,
    )
}


@Composable
private fun PulseAudienceChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(15.dp),
        color = if (selected) NovaAccentSoft else NovaSurface,
        border = BorderStroke(1.dp, if (selected) NovaAccent else NovaBorder),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            color = if (selected) NovaAccent else NovaInk,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}


@Composable
private fun PulseViewerDialog(
    pulse: NovaPulse,
    deleting: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(PulseViewerBackground),
        ) {
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
                        color = PulseViewerInk,
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NovaAvatar(
                    source = pulse.author.avatarUrl,
                    fallbackText = pulse.author.name.ifBlank { pulse.author.username },
                    size = 38.dp,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (pulse.isMine) "Your Pulse" else pulse.author.name.ifBlank { pulse.author.username },
                        color = PulseViewerInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "@${pulse.author.username} • live for 12h",
                        color = PulseViewerMuted,
                        fontSize = 10.sp,
                    )
                }
                Text(
                    text = "✕",
                    color = PulseViewerInk,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
                )
            }

            if (pulse.mediaType != "text" && pulse.note.isNotBlank()) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 74.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = PulseViewerBackground.copy(alpha = 0.78f),
                ) {
                    Text(
                        text = pulse.note,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = PulseViewerInk,
                        fontSize = 14.sp,
                    )
                }
            }

            if (pulse.isMine) {
                TextButton(
                    onClick = onDelete,
                    enabled = !deleting,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
                ) {
                    if (deleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PulseViewerInk,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Delete", color = PulseViewerInk)
                    }
                }
            }
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
