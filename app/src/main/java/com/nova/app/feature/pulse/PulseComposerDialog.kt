package com.nova.app.feature.pulse

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.components.NovaVideoPlayer
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun PulseComposerDialog(
    title: String,
    subtitle: String,
    pendingMedia: Uri?,
    uploading: Boolean,
    error: String?,
    initialAudience: String,
    initialCategory: String = "vibes",
    showCategory: Boolean = true,
    confirmLabel: String,
    onPickMedia: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit,
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }
    var audience by remember(initialAudience) {
        mutableStateOf(
            initialAudience.takeIf { it == "followers" || it == "close_friends" }
                ?: "followers",
        )
    }
    var category by remember(initialCategory) {
        mutableStateOf(initialCategory.takeIf { it in setOf("live", "music", "talks", "vibes") } ?: "vibes")
    }
    val mime = remember(pendingMedia) {
        pendingMedia?.let { context.contentResolver.getType(it).orEmpty().lowercase() }.orEmpty()
    }
    val canSubmit = !uploading &&
        (pendingMedia != null || note.trim().isNotEmpty()) &&
        note.length <= 180

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(title, color = NovaInk, fontWeight = FontWeight.Bold)
                Text(subtitle, color = NovaMuted, fontSize = 11.sp)
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
                        NovaVideoPlayer(
                            source = pendingMedia.toString(),
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            autoplay = true,
                            repeat = true,
                            muted = true,
                            useController = false,
                            description = "Selected Pulse video preview",
                        )
                    }
                    TextButton(
                        onClick = onPickMedia,
                        enabled = !uploading,
                    ) {
                        Text("Change photo / video", color = NovaAccent)
                    }
                } else {
                    Surface(
                        onClick = onPickMedia,
                        enabled = !uploading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = NovaAccentSoft,
                        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.32f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "+ Add photo or video",
                                color = NovaAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 180) note = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uploading,
                    label = {
                        Text(if (pendingMedia == null) "What are you doing?" else "Add a note")
                    },
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

                if (showCategory) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Category", color = NovaMuted, fontSize = 10.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("live" to "Live", "music" to "Music", "talks" to "Talks", "vibes" to "Vibes").forEach { (value, label) ->
                                PulseAudienceChoice(
                                    label = label,
                                    selected = category == value,
                                    enabled = !uploading,
                                    onClick = { category = value },
                                )
                            }
                        }
                    }
                }

                error?.let {
                    Text(text = it, color = NovaMuted, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(note.trim(), audience, category) },
                enabled = canSubmit,
            ) {
                if (uploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = NovaAccent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(confirmLabel, color = NovaAccent, fontWeight = FontWeight.Bold)
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
