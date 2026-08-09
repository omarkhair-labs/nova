package com.nova.app.core.messaging

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File
import java.util.UUID


data class NovaVoiceDraft(
    val file: File,
    val durationMs: Long,
)


class NovaVoiceRecorder(context: Context) {
    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    val isRecording: Boolean
        get() = recorder != null

    fun start(): Result<Unit> {
        if (recorder != null) return Result.failure(IllegalStateException("Already recording."))

        val file = File(
            appContext.cacheDir,
            "nova-voice-${UUID.randomUUID()}.m4a",
        )
        val next = createRecorder()
        return runCatching {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioEncodingBitRate(128_000)
            next.setAudioSamplingRate(44_100)
            next.setOutputFile(file.absolutePath)
            next.prepare()
            next.start()
            recorder = next
            outputFile = file
            startedAtMs = SystemClock.elapsedRealtime()
        }.onFailure {
            runCatching { next.reset() }
            next.release()
            file.delete()
            recorder = null
            outputFile = null
            startedAtMs = 0L
        }
    }

    fun stop(): Result<NovaVoiceDraft> {
        val active = recorder ?: return Result.failure(IllegalStateException("Not recording."))
        val file = outputFile ?: return Result.failure(IllegalStateException("Recording file is unavailable."))
        val duration = (SystemClock.elapsedRealtime() - startedAtMs).coerceAtLeast(0L)

        recorder = null
        outputFile = null
        startedAtMs = 0L

        return runCatching {
            active.stop()
            active.release()
            NovaVoiceDraft(file = file, durationMs = duration)
        }.onFailure {
            runCatching { active.release() }
            file.delete()
        }
    }

    fun cancel() {
        val active = recorder
        val file = outputFile
        recorder = null
        outputFile = null
        startedAtMs = 0L
        if (active != null) {
            runCatching { active.stop() }
            runCatching { active.release() }
        }
        file?.delete()
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            MediaRecorder()
        }
    }
}
