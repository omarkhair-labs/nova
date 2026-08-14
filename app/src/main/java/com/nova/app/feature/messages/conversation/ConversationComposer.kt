package com.nova.app.feature.messages.conversation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.messaging.NovaVoiceDraft
import com.nova.app.core.messaging.NovaVoiceRecorder
import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.delay
import java.util.Locale


internal const val ConversationMinVoiceDurationMs = 1_000L
internal const val ConversationMaxVoiceDurationMs = 5 * 60 * 1_000L


internal interface ConversationVoiceRecorder {
    fun start(): Result<Unit>
    fun stop(): Result<NovaVoiceDraft>
    fun cancel()
}


private class NovaConversationVoiceRecorder(context: Context) : ConversationVoiceRecorder {
    private val delegate = NovaVoiceRecorder(context)

    override fun start(): Result<Unit> = delegate.start()

    override fun stop(): Result<NovaVoiceDraft> = delegate.stop()

    override fun cancel() = delegate.cancel()
}


/** Owns photo and voice drafts whose lifetime is limited to one conversation composer. */
@Stable
internal class ConversationComposerState(
    private val recorder: ConversationVoiceRecorder,
    private val elapsedRealtime: () -> Long,
    private val onRecordingChanged: (Boolean) -> Unit,
    private val onErrorMessage: (String?) -> Unit,
) {
    var selectedImage by mutableStateOf<Uri?>(null)
        private set

    var voiceDraft by mutableStateOf<NovaVoiceDraft?>(null)
        private set

    var isRecording by mutableStateOf(false)
        private set

    var recordingElapsedMs by mutableLongStateOf(0L)
        private set

    private var recordingStartedAtMs = 0L

    fun selectImage(uri: Uri, isEditing: Boolean) {
        if (isEditing || voiceDraft != null || isRecording) return
        selectedImage = uri
        onErrorMessage(null)
    }

    fun removeImage() {
        selectedImage = null
    }

    fun onMicrophonePermissionDenied() {
        onErrorMessage("Microphone permission is required to send voice messages.")
    }

    fun beginVoiceRecording(isEditing: Boolean) {
        if (isEditing || selectedImage != null || voiceDraft != null || isRecording) return
        onErrorMessage(null)
        recorder.start()
            .onSuccess {
                isRecording = true
                recordingStartedAtMs = elapsedRealtime()
                recordingElapsedMs = 0L
                onRecordingChanged(true)
            }
            .onFailure {
                onErrorMessage("Nova couldn't start the microphone. Try again.")
            }
    }

    fun finishVoiceRecording() {
        if (!isRecording) return
        val result = recorder.stop()
        stopTrackingRecording()
        result.onSuccess { recorded ->
            if (recorded.durationMs < ConversationMinVoiceDurationMs) {
                recorded.file.delete()
                onErrorMessage("Voice message is too short. Record for at least 1 second.")
            } else {
                voiceDraft?.file?.delete()
                voiceDraft = recorded
                onErrorMessage(null)
            }
        }.onFailure {
            onErrorMessage("Nova couldn't finish that recording. Record it again.")
        }
    }

    fun updateRecordingElapsed() {
        if (!isRecording) return
        recordingElapsedMs = (elapsedRealtime() - recordingStartedAtMs).coerceAtLeast(0L)
        if (recordingElapsedMs >= ConversationMaxVoiceDurationMs) finishVoiceRecording()
    }

    fun cancelRecording() {
        if (!isRecording) return
        recorder.cancel()
        stopTrackingRecording()
    }

    fun removeVoiceDraft() {
        voiceDraft?.file?.delete()
        voiceDraft = null
    }

    fun clearAfterSend() {
        selectedImage = null
        voiceDraft = null
    }

    fun prepareForEdit() {
        if (isRecording) {
            recorder.cancel()
            stopTrackingRecording()
        }
        removeVoiceDraft()
        selectedImage = null
    }

    fun dispose() {
        recorder.cancel()
        if (isRecording) onRecordingChanged(false)
        isRecording = false
        recordingStartedAtMs = 0L
        recordingElapsedMs = 0L
        removeVoiceDraft()
        selectedImage = null
    }

    private fun stopTrackingRecording() {
        isRecording = false
        recordingStartedAtMs = 0L
        recordingElapsedMs = 0L
        onRecordingChanged(false)
    }
}


@Composable
internal fun rememberConversationComposerState(
    conversationId: Long,
    onRecordingChanged: (Boolean) -> Unit,
    onErrorMessage: (String?) -> Unit,
): ConversationComposerState {
    val context = LocalContext.current.applicationContext
    val currentOnRecordingChanged by rememberUpdatedState(onRecordingChanged)
    val currentOnErrorMessage by rememberUpdatedState(onErrorMessage)
    val state = remember(context, conversationId) {
        ConversationComposerState(
            recorder = NovaConversationVoiceRecorder(context),
            elapsedRealtime = SystemClock::elapsedRealtime,
            onRecordingChanged = { currentOnRecordingChanged(it) },
            onErrorMessage = { currentOnErrorMessage(it) },
        )
    }

    DisposableEffect(state) {
        onDispose(state::dispose)
    }
    return state
}


@Composable
internal fun ConversationComposer(
    username: String,
    draft: String,
    state: ConversationComposerState,
    replyTarget: NovaMessage?,
    editingTarget: NovaMessage?,
    isMutating: Boolean,
    errorMessage: String?,
    onDraftChange: (String) -> Unit,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    val currentEditingTarget by rememberUpdatedState(editingTarget)
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) state.selectImage(uri, isEditing = currentEditingTarget != null)
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) state.beginVoiceRecording(isEditing = currentEditingTarget != null)
        else state.onMicrophonePermissionDenied()
    }

    LaunchedEffect(state, state.isRecording) {
        while (state.isRecording) {
            state.updateRecordingElapsed()
            if (state.isRecording) delay(250)
        }
    }

    fun requestVoiceRecording() {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            state.beginVoiceRecording(isEditing = editingTarget != null)
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Surface(color = NovaSurface, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            if (editingTarget != null) {
                ComposerContextCard(
                    title = "Editing message",
                    preview = composerReplyPreview(editingTarget),
                    onClose = onCancelEdit,
                )
            } else if (replyTarget != null) {
                ComposerContextCard(
                    title = "Replying to @${replyTarget.sender.username}",
                    preview = composerReplyPreview(replyTarget),
                    onClose = onCancelReply,
                )
            }

            if (state.isRecording) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = NovaAccentSoft,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("●", color = NovaAccent, fontSize = 16.sp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Recording ${formatConversationVoiceDuration(state.recordingElapsedMs)}",
                                color = NovaInk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                            Text("Up to 5 minutes", color = NovaMuted, fontSize = 10.sp)
                        }
                        ComposerSmallChip("Cancel", state::cancelRecording)
                        Surface(onClick = state::finishVoiceRecording, shape = CircleShape, color = NovaAccent) {
                            Text(
                                "■",
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                                color = NovaBackground,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            state.selectedImage?.let { selectedImage ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NovaMediaImage(
                        source = selectedImage.toString(),
                        modifier = Modifier.size(70.dp).clip(RoundedCornerShape(15.dp)),
                        contentDescription = "Selected message photo",
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Photo ready", color = NovaInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("It will appear instantly while uploading.", color = NovaMuted, fontSize = 11.sp)
                    }
                    ComposerSmallChip("Remove", state::removeImage)
                }
            }

            state.voiceDraft?.let { voiceDraft ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(shape = CircleShape, color = NovaAccentSoft) {
                        Text("🎤", modifier = Modifier.padding(12.dp), fontSize = 18.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Voice message ready", color = NovaInk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatConversationVoiceDuration(voiceDraft.durationMs), color = NovaMuted, fontSize = 11.sp)
                    }
                    ComposerSmallChip("Remove", state::removeVoiceDraft)
                }
            }

            if (errorMessage != null) {
                Text(
                    errorMessage,
                    color = NovaMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (editingTarget == null && !state.isRecording) {
                    Surface(
                        onClick = {
                            if (state.voiceDraft == null) imagePicker.launch("image/*")
                        },
                        shape = CircleShape,
                        color = if (state.voiceDraft == null) NovaAccentSoft else NovaBackground,
                    ) {
                        Text(
                            "+",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                            color = NovaAccent,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isMutating && !state.isRecording,
                    placeholder = {
                        Text(
                            when {
                                editingTarget != null -> "Edit message"
                                username == "group" -> "Message group"
                                else -> "Message @$username"
                            },
                            color = NovaMuted,
                        )
                    },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccent,
                        unfocusedBorderColor = NovaBorder,
                        cursorColor = NovaAccent,
                        focusedContainerColor = NovaBackground,
                        unfocusedContainerColor = NovaBackground,
                    ),
                )

                if (
                    editingTarget == null && !state.isRecording &&
                    state.selectedImage == null && state.voiceDraft == null && draft.isBlank()
                ) {
                    Surface(onClick = ::requestVoiceRecording, shape = CircleShape, color = NovaAccentSoft) {
                        Text("🎤", modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp), fontSize = 16.sp)
                    }
                }

                val enabled = isConversationComposerSendEnabled(
                    draft = draft,
                    editingTarget = editingTarget,
                    isMutating = isMutating,
                    isRecording = state.isRecording,
                    hasSelectedImage = state.selectedImage != null,
                    hasVoiceDraft = state.voiceDraft != null,
                )
                Surface(
                    onClick = { if (enabled) onSend() },
                    shape = CircleShape,
                    color = if (enabled) NovaAccent else NovaAccentSoft,
                ) {
                    Text(
                        when {
                            isMutating -> "…"
                            editingTarget != null -> "✓"
                            else -> "↑"
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = if (enabled) NovaBackground else NovaMuted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}


@Composable
private fun ComposerContextCard(title: String, preview: String, onClose: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NovaAccentSoft,
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = NovaAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(preview, color = NovaMuted, fontSize = 12.sp, maxLines = 1)
            }
            Surface(onClick = onClose, shape = CircleShape, color = NovaSurface) {
                Text(
                    "×",
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    color = NovaMuted,
                    fontSize = 18.sp,
                )
            }
        }
    }
}


@Composable
private fun ComposerSmallChip(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = NovaBackground) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = NovaMuted,
            fontSize = 11.sp,
        )
    }
}


internal fun isConversationComposerSendEnabled(
    draft: String,
    editingTarget: NovaMessage?,
    isMutating: Boolean,
    isRecording: Boolean,
    hasSelectedImage: Boolean,
    hasVoiceDraft: Boolean,
): Boolean {
    return if (editingTarget != null) {
        !isMutating && !editingTarget.isDeleted &&
            draft.trim() != editingTarget.body &&
            (draft.isNotBlank() || editingTarget.imageUrl.isNotBlank() || editingTarget.audioUrl.isNotBlank())
    } else {
        !isRecording && (draft.isNotBlank() || hasSelectedImage || hasVoiceDraft)
    }
}


private fun composerReplyPreview(message: NovaMessage): String = when {
    message.isDeleted -> "Message deleted"
    message.share?.kind == "post" -> "↗ Shared post"
    message.share?.kind == "profile" -> "↗ Shared profile"
    message.share?.kind == "reel" -> "↗ Shared Reel"
    message.body.isNotBlank() -> message.body
    message.audioUrl.isNotBlank() -> "🎤 Voice message"
    message.imageUrl.isNotBlank() -> "📷 Photo"
    else -> "Message"
}


internal fun formatConversationVoiceDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L).coerceAtMost(5 * 60L)
    return "%d:%02d".format(Locale.US, totalSeconds / 60L, totalSeconds % 60L)
}
