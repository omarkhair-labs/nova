package com.nova.app.core.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.nova.app.core.network.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume


data class PreparedNovaVideo(
    val videoFile: File,
    val thumbnailFile: File,
    val durationMs: Long,
) {
    fun delete() {
        videoFile.delete()
        thumbnailFile.delete()
    }
}


/** Rewrites picked video into Nova's Android-safe MP4 contract and proves full duration survived. */
class NovaVideoPreparer(context: Context) {
    private val appContext = context.applicationContext

    suspend fun prepare(
        source: Uri,
        maxSourceBytes: Long,
        sizeMessage: String,
        onProgress: (Int?) -> Unit = {},
    ): ApiResult<PreparedNovaVideo> {
        val copied = copySource(source, maxSourceBytes, sizeMessage)
        val sourceFile = when (copied) {
            is ApiResult.Success -> copied.value
            is ApiResult.Failure -> return copied
        }

        val sourceDuration = mediaDuration(sourceFile)
        if (sourceDuration <= 0L || !hasVideoTrack(sourceFile)) {
            sourceFile.delete()
            return ApiResult.Failure(
                "Nova couldn't find a complete video track in that file. Pick another video.",
            )
        }

        val outputFile = File.createTempFile("nova-video-ready-", ".mp4", appContext.cacheDir)
        outputFile.delete()
        onProgress(0)
        val transformed = transform(sourceFile, outputFile, onProgress)
        sourceFile.delete()
        transformed.exceptionOrNull()?.let { error ->
            outputFile.delete()
            return ApiResult.Failure(
                "That video isn't compatible with Nova yet. Choose another clip and try again.",
            )
        }

        if (outputFile.length() <= 0L || outputFile.length() > maxSourceBytes) {
            outputFile.delete()
            return ApiResult.Failure(sizeMessage)
        }

        val outputDuration = mediaDuration(outputFile)
        if (!isDurationPreserved(sourceDuration, outputDuration)) {
            outputFile.delete()
            return ApiResult.Failure(
                "Nova couldn't preserve the full video. Nothing was uploaded; choose another clip.",
            )
        }

        val thumbnail = extractThumbnail(outputFile)
            ?: run {
                outputFile.delete()
                return ApiResult.Failure(
                    "Nova couldn't read a visible frame from that video. Nothing was uploaded.",
                )
            }
        onProgress(100)
        return ApiResult.Success(
            PreparedNovaVideo(
                videoFile = outputFile,
                thumbnailFile = thumbnail,
                durationMs = outputDuration,
            ),
        )
    }

    private suspend fun copySource(
        source: Uri,
        maxBytes: Long,
        sizeMessage: String,
    ): ApiResult<File> = withContext(Dispatchers.IO) {
        val target = File.createTempFile("nova-video-source-", ".media", appContext.cacheDir)
        try {
            val input = appContext.contentResolver.openInputStream(source)
                ?: throw IllegalStateException("Unreadable video")
            input.use { sourceStream ->
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = sourceStream.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > maxBytes) {
                            target.delete()
                            return@withContext ApiResult.Failure(sizeMessage)
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (target.length() <= 0L) {
                target.delete()
                ApiResult.Failure("Nova couldn't read that video. Pick it again and retry.")
            } else {
                ApiResult.Success(target)
            }
        } catch (_: Exception) {
            target.delete()
            ApiResult.Failure("Nova couldn't read that video. Pick it again and retry.")
        }
    }

    private suspend fun transform(
        sourceFile: File,
        outputFile: File,
        onProgress: (Int?) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            var transformer: Transformer? = null
            val listener = object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (continuation.isActive) continuation.resume(Result.success(Unit))
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    if (continuation.isActive) continuation.resume(Result.failure(exportException))
                }
            }
            try {
                val mainHandler = Handler(Looper.getMainLooper())
                transformer = Transformer.Builder(appContext)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(listener)
                    .build()
                val activeTransformer = transformer
                val progressHolder = ProgressHolder()
                val progressPoll = object : Runnable {
                    override fun run() {
                        if (!continuation.isActive) return
                        val state = activeTransformer.getProgress(progressHolder)
                        onProgress(
                            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                                progressHolder.progress.coerceIn(0, 99)
                            } else {
                                null
                            },
                        )
                        if (continuation.isActive) mainHandler.postDelayed(this, 350L)
                    }
                }
                continuation.invokeOnCancellation {
                    mainHandler.post { activeTransformer.cancel() }
                }
                activeTransformer.start(MediaItem.fromUri(Uri.fromFile(sourceFile)), outputFile.absolutePath)
                mainHandler.post(progressPoll)
            } catch (error: Throwable) {
                transformer?.cancel()
                if (continuation.isActive) continuation.resume(Result.failure(error))
            }
        }
    }

    private suspend fun mediaDuration(file: File): Long = withContext(Dispatchers.IO) {
        withRetriever(file) { retriever ->
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
        }
    }

    private suspend fun hasVideoTrack(file: File): Boolean = withContext(Dispatchers.IO) {
        withRetriever(file) { retriever ->
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes" &&
                (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0) > 0 &&
                (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0) > 0
        }
    }

    private suspend fun extractThumbnail(file: File): File? = withContext(Dispatchers.IO) {
        runCatching {
            withRetriever(file) { retriever ->
                val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: return@withRetriever null
                val target = File.createTempFile("nova-video-thumb-", ".jpg", appContext.cacheDir)
                target.outputStream().buffered().use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                        target.delete()
                        return@withRetriever null
                    }
                }
                bitmap.recycle()
                target
            }
        }.getOrNull()
    }

    private inline fun <T> withRetriever(file: File, block: (MediaMetadataRetriever) -> T): T {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            block(retriever)
        } finally {
            retriever.release()
        }
    }
}


internal fun isDurationPreserved(sourceDurationMs: Long, outputDurationMs: Long): Boolean {
    if (sourceDurationMs <= 0L || outputDurationMs <= 0L) return false
    val allowedLoss = maxOf(500L, sourceDurationMs / 20L)
    return outputDurationMs >= sourceDurationMs - allowedLoss
}
