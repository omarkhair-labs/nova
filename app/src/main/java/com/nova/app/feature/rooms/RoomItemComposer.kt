package com.nova.app.feature.rooms

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


private data class RoomComposerKind(
    val value: String,
    val label: String,
)


private val roomComposerKinds = listOf(
    RoomComposerKind("note", "Note"),
    RoomComposerKind("photo", "Photo"),
    RoomComposerKind("video", "Video"),
    RoomComposerKind("music", "Music"),
    RoomComposerKind("plan", "Plan"),
    RoomComposerKind("saved", "Saved"),
)


@Composable
fun RoomItemComposer(
    submitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (
        kind: String,
        title: String,
        body: String,
        url: String,
        mediaUri: Uri?,
    ) -> Unit,
) {
    var kind by remember { mutableStateOf("note") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) mediaUri = uri
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) mediaUri = uri
    }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("Add to Room") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    roomComposerKinds.forEach { option ->
                        val selected = kind == option.value
                        Surface(
                            onClick = {
                                if (!submitting) {
                                    kind = option.value
                                    mediaUri = null
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) NovaAccent else NovaSurface,
                            border = BorderStroke(1.dp, if (selected) NovaAccent else NovaBorder),
                        ) {
                            Text(
                                text = option.label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                color = if (selected) NovaSurface else NovaInk,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (kind != "note") {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 120) title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                when (kind) {
                                    "plan" -> "Plan title"
                                    "music" -> "Song / playlist"
                                    "saved" -> "Saved title"
                                    else -> "Title (optional)"
                                }
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        colors = composerFieldColors(),
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                }

                if (kind in setOf("note", "photo", "video", "plan")) {
                    OutlinedTextField(
                        value = body,
                        onValueChange = { if (it.length <= 500) body = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                when (kind) {
                                    "note" -> "What do you want to leave here?"
                                    "plan" -> "Details (optional)"
                                    else -> "Caption (optional)"
                                }
                            )
                        },
                        minLines = if (kind == "note" || kind == "plan") 3 else 2,
                        maxLines = 5,
                        shape = RoundedCornerShape(15.dp),
                        colors = composerFieldColors(),
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                }

                if (kind == "music" || kind == "saved") {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { if (it.length <= 700) url = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (kind == "music") "Music link" else "Link") },
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        colors = composerFieldColors(),
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                }

                if (kind == "photo" || kind == "video") {
                    Surface(
                        onClick = {
                            if (!submitting) {
                                if (kind == "photo") photoPicker.launch("image/*") else videoPicker.launch("video/*")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = NovaAccentSoft,
                        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.22f)),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (mediaUri == null) {
                                    if (kind == "photo") "Choose photo" else "Choose video"
                                } else {
                                    if (kind == "photo") "Photo selected · tap to change" else "Video selected · tap to change"
                                },
                                color = NovaAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (kind == "photo") "Up to 12 MB" else "Up to 60 MB",
                                color = NovaMuted,
                                fontSize = 8.sp,
                            )
                        }
                    }
                }

                if (!error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = error,
                        color = NovaMuted,
                        fontSize = 9.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(kind, title, body, url, mediaUri) },
                enabled = !submitting,
            ) {
                Text(if (submitting) "Adding…" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text("Cancel")
            }
        },
    )
}


@Composable
private fun composerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NovaAccent,
    unfocusedBorderColor = NovaBorder,
    cursorColor = NovaAccent,
)
