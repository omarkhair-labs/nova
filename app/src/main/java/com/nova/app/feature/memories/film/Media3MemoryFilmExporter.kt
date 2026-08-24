package com.nova.app.feature.memories.film

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.nova.app.feature.memories.domain.model.MemoryFilmPlan
import com.nova.app.feature.memories.domain.model.MemoryFilmScene
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine


@OptIn(UnstableApi::class)
class Media3MemoryFilmExporter(context: Context) : MemoryFilmExporter {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeTransformer: Transformer? = null

    override suspend fun export(
        plan: MemoryFilmPlan,
        onProgress: (Int) -> Unit,
    ): Result<MemoryFilmExport> {
        if (!plan.filmReady || plan.scenes.isEmpty()) {
            return Result.failure(IllegalStateException("This week does not have enough media for a film."))
        }

        val editedItems = plan.scenes
            .sortedBy { it.index }
            .mapNotNull(::editedItem)
        if (editedItems.isEmpty()) {
            return Result.failure(IllegalStateException("Nova couldn't prepare any film scenes."))
        }

        val outputDirectory = File(appContext.cacheDir, "memory-films").apply { mkdirs() }
        pruneOldExports(outputDirectory)
        val safeWeek = plan.startsAt.take(10).replace(Regex("[^0-9-]"), "")
        val outputFile = File(
            outputDirectory,
            "nova-memory-${safeWeek.ifBlank { "week" }}-${System.currentTimeMillis()}.mp4",
        )
        if (outputFile.exists()) outputFile.delete()

        return suspendCancellableCoroutine { continuation ->
            val sequence = EditedMediaItemSequence.withVideoFrom(editedItems)
            val composition = Composition.Builder(sequence).build()
            val progressHolder = ProgressHolder()
            var progressRunnable: Runnable? = null

            fun stopProgress() {
                progressRunnable?.let { mainHandler.removeCallbacks(it) }
            }

            continuation.invokeOnCancellation {
                mainHandler.post {
                    activeTransformer?.cancel()
                    activeTransformer = null
                    stopProgress()
                    outputFile.delete()
                }
            }

            mainHandler.post {
                if (!continuation.isActive) {
                    outputFile.delete()
                    return@post
                }

                val listener = object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        exportResult: ExportResult,
                    ) {
                        stopProgress()
                        activeTransformer = null
                        onProgress(100)
                        if (continuation.isActive) {
                            continuation.resume(
                                Result.success(
                                    MemoryFilmExport(
                                        filePath = outputFile.absolutePath,
                                        durationMs = plan.totalDurationMs,
                                    )
                                )
                            )
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException,
                    ) {
                        stopProgress()
                        activeTransformer = null
                        outputFile.delete()
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(exportException))
                        }
                    }
                }

                try {
                    val transformer = Transformer.Builder(appContext)
                        .addListener(listener)
                        .build()
                    activeTransformer = transformer

                    progressRunnable = object : Runnable {
                        override fun run() {
                            if (activeTransformer !== transformer || !continuation.isActive) return
                            if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                                onProgress(progressHolder.progress.coerceIn(0, 99))
                            }
                            mainHandler.postDelayed(this, 300L)
                        }
                    }

                    onProgress(0)
                    transformer.start(composition, outputFile.absolutePath)
                    progressRunnable?.let(mainHandler::post)
                } catch (error: Throwable) {
                    stopProgress()
                    activeTransformer = null
                    outputFile.delete()
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(error))
                    }
                }
            }
        }
    }

    override fun cancel() {
        mainHandler.post {
            activeTransformer?.cancel()
            activeTransformer = null
        }
    }

    private fun editedItem(scene: MemoryFilmScene): EditedMediaItem? {
        if (scene.mediaUrl.isBlank() || scene.durationMs <= 0L) return null

        val mediaItem = when (scene.mediaType) {
            "image" -> MediaItem.Builder()
                .setUri(Uri.parse(scene.mediaUrl))
                .setImageDurationMs(scene.durationMs)
                .build()

            "video" -> {
                val start = scene.trimStartMs.coerceAtLeast(0L)
                val end = start + scene.durationMs.coerceAtLeast(1L)
                MediaItem.Builder()
                    .setUri(Uri.parse(scene.mediaUrl))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(start)
                            .setEndPositionMs(end)
                            .build()
                    )
                    .build()
            }

            else -> return null
        }

        val videoEffects = mutableListOf<Effect>(
            Presentation.createForWidthAndHeight(
                720,
                1280,
                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
            )
        )
        captionEffect(scene.caption)?.let(videoEffects::add)

        val builder = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), videoEffects))
            .setRemoveAudio(true)
        if (scene.mediaType == "image") builder.setFrameRate(30)
        return builder.build()
    }

    private fun captionEffect(rawCaption: String): Effect? {
        val caption = rawCaption.trim().take(56)
        if (caption.isBlank()) return null

        val text = SpannableString(caption).apply {
            setSpan(
                ForegroundColorSpan(Color.WHITE),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                AbsoluteSizeSpan(48),
                0,
                length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        val settings = StaticOverlaySettings.Builder()
            .setOverlayFrameAnchor(0f, -1f)
            .setBackgroundFrameAnchor(0f, -0.76f)
            .setScale(0.68f, 0.68f)
            .build()
        val overlay = TextOverlay.createStaticTextOverlay(text, settings)
        return OverlayEffect(listOf(overlay))
    }

    private fun pruneOldExports(directory: File) {
        directory.listFiles()
            ?.filter { it.isFile && it.extension.equals("mp4", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(3)
            ?.forEach { it.delete() }
    }
}
