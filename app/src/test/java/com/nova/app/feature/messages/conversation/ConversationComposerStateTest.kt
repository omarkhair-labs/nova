package com.nova.app.feature.messages.conversation

import com.nova.app.core.messaging.NovaVoiceDraft
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.feature.messages.domain.model.NovaMessage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder


class ConversationComposerStateTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private var nowMs = 10_000L
    private val recorder = FakeVoiceRecorder()
    private val recordingChanges = mutableListOf<Boolean>()
    private val errors = mutableListOf<String?>()
    private val state by lazy {
        ConversationComposerState(
            recorder = recorder,
            elapsedRealtime = { nowMs },
            onRecordingChanged = recordingChanges::add,
            onErrorMessage = errors::add,
        )
    }

    @Test
    fun recordingCannotStartWhileEditingAndSuccessfulStartPublishesState() {
        state.beginVoiceRecording(isEditing = true)

        assertEquals(0, recorder.startCount)
        assertFalse(state.isRecording)

        state.beginVoiceRecording(isEditing = false)

        assertEquals(1, recorder.startCount)
        assertTrue(state.isRecording)
        assertEquals(listOf(null), errors)
        assertEquals(listOf(true), recordingChanges)
    }

    @Test
    fun shortRecordingIsRejectedAndItsTemporaryFileIsDeleted() {
        val file = temporaryFolder.newFile("short.m4a")
        recorder.stopResult = Result.success(NovaVoiceDraft(file, 999L))

        state.beginVoiceRecording(isEditing = false)
        state.finishVoiceRecording()

        assertFalse(state.isRecording)
        assertNull(state.voiceDraft)
        assertFalse(file.exists())
        assertEquals(listOf(true, false), recordingChanges)
        assertEquals(
            "Voice message is too short. Record for at least 1 second.",
            errors.last(),
        )
    }

    @Test
    fun fiveMinuteLimitStopsRecordingAndKeepsTheValidDraft() {
        val file = temporaryFolder.newFile("five-minutes.m4a")
        recorder.stopResult = Result.success(
            NovaVoiceDraft(file, ConversationMaxVoiceDurationMs)
        )

        state.beginVoiceRecording(isEditing = false)
        nowMs += ConversationMaxVoiceDurationMs
        state.updateRecordingElapsed()

        assertFalse(state.isRecording)
        assertEquals(1, recorder.stopCount)
        assertEquals(file, state.voiceDraft?.file)
        assertTrue(file.exists())
        assertEquals(listOf(true, false), recordingChanges)
    }

    @Test
    fun editAndDisposeCancelActiveWorkAndDeleteUnsentVoiceDrafts() {
        state.beginVoiceRecording(isEditing = false)
        state.prepareForEdit()

        assertEquals(1, recorder.cancelCount)
        assertFalse(state.isRecording)

        val file = temporaryFolder.newFile("unsent.m4a")
        recorder.stopResult = Result.success(NovaVoiceDraft(file, 1_500L))
        state.beginVoiceRecording(isEditing = false)
        state.finishVoiceRecording()
        state.dispose()

        assertNull(state.voiceDraft)
        assertFalse(file.exists())
        assertEquals(2, recorder.cancelCount)
    }

    @Test
    fun sendEligibilityPreservesTextAttachmentVoiceAndEditRules() {
        assertFalse(sendEnabled())
        assertTrue(sendEnabled(draft = "hello"))
        assertTrue(sendEnabled(hasSelectedImage = true))
        assertTrue(sendEnabled(hasVoiceDraft = true))
        assertFalse(sendEnabled(draft = "hello", isRecording = true))

        val original = message(body = "hello")
        assertFalse(sendEnabled(draft = "hello", editingTarget = original))
        assertTrue(sendEnabled(draft = "updated", editingTarget = original))
        assertFalse(sendEnabled(draft = "updated", editingTarget = original, isMutating = true))
        assertFalse(sendEnabled(draft = "updated", editingTarget = original.copy(deletedAt = "now")))
        assertTrue(sendEnabled(draft = "", editingTarget = message(body = "hello", imageUrl = "photo.jpg")))
    }

    @Test
    fun durationFormattingRemainsClampedToTheFiveMinuteContract() {
        assertEquals("0:00", formatConversationVoiceDuration(-1L))
        assertEquals("0:01", formatConversationVoiceDuration(1_999L))
        assertEquals("5:00", formatConversationVoiceDuration(ConversationMaxVoiceDurationMs + 20_000L))
    }

    private fun sendEnabled(
        draft: String = "",
        editingTarget: NovaMessage? = null,
        isMutating: Boolean = false,
        isRecording: Boolean = false,
        hasSelectedImage: Boolean = false,
        hasVoiceDraft: Boolean = false,
    ) = isConversationComposerSendEnabled(
        draft = draft,
        editingTarget = editingTarget,
        isMutating = isMutating,
        isRecording = isRecording,
        hasSelectedImage = hasSelectedImage,
        hasVoiceDraft = hasVoiceDraft,
    )

    private fun message(
        body: String,
        imageUrl: String = "",
    ) = NovaMessage(
        id = 1L,
        clientId = "client-1",
        sender = NovaPostAuthor(1L, "alice", "Alice", ""),
        body = body,
        imageUrl = imageUrl,
        replyTo = null,
        reactions = emptyList(),
        createdAt = "2026-08-14T00:00:00Z",
        deliveredAt = null,
        readAt = null,
        isMine = true,
    )

    private class FakeVoiceRecorder : ConversationVoiceRecorder {
        var startCount = 0
        var stopCount = 0
        var cancelCount = 0
        var startResult: Result<Unit> = Result.success(Unit)
        var stopResult: Result<NovaVoiceDraft> = Result.failure(IllegalStateException("No draft"))

        override fun start(): Result<Unit> {
            startCount += 1
            return startResult
        }

        override fun stop(): Result<NovaVoiceDraft> {
            stopCount += 1
            return stopResult
        }

        override fun cancel() {
            cancelCount += 1
        }
    }
}
