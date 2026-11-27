package com.nova.app.feature.plants

import android.graphics.ImageFormat
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

internal data class LiveLensTarget(val enabled: Boolean, val width: Int, val height: Int, val selection: Int = 0)
internal data class LiveLensReading(val phase: LiveLensPhase, val region: LiveLensRegion?, val timestamp: Long)

/**
 * The existing private CameraX analyzer owns both the capture hints and the
 * experimental lens. Only small in-memory YUV samples are examined. No frame,
 * candidate mask, or inference is persisted or uploaded.
 */
internal class PlantupiaLiveLensAnalyzer(
    private val previewAllowed: Boolean,
    private val active: AtomicBoolean,
    private val main: Executor,
    private val onQuality: (PlantupiaCaptureQuality?) -> Unit,
    private val onLens: (LiveLensReading?) -> Unit,
) : ImageAnalysis.Analyzer {
    private val target = AtomicReference(LiveLensTarget(false, 0, 0))
    private val sensorToView = AtomicReference<Matrix?>(null)
    // All of these are owned by the single analysis executor.
    private var lastQualityAt = 0L
    private var lastLensAt = 0L
    private var previousAt = 0L
    private val tracker = LiveLensTracker()
    private var previousTarget: LiveLensTarget? = null
    private var previousTransform: Matrix? = null

    /** Called on main; changing the viewport or turning the aid off drops the old result. */
    fun configure(enabled: Boolean, width: Int, height: Int, selection: Int = 0) {
        val next = LiveLensTarget(previewAllowed && enabled, width, height, selection)
        val old = target.get()
        if (old != next) {
            target.set(next)
            onLens(null)
        }
    }

    override fun getTargetCoordinateSystem(): Int =
        if (previewAllowed) ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED
        else ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL

    override fun updateTransform(matrix: Matrix?) {
        if (!previewAllowed) return
        val copy = matrix?.let { Matrix(it) }
        val old = sensorToView.get()
        if (old == copy) return
        sensorToView.set(copy)
        main.execute {
            if (active.get() && sensorToView.get() === copy) onLens(null)
        }
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (!active.get()) return
            val now = SystemClock.elapsedRealtime()
            if (now - lastQualityAt >= 300L) {
                lastQualityAt = now
                val reading = runCatching {
                    val luma = image.planes.first()
                    val crop = image.cropRect
                    captureQuality(crop.width(), crop.height()) { x, y ->
                        sample(luma, x + crop.left, y + crop.top)
                    }
                }.getOrNull()
                main.execute { if (active.get()) onQuality(reading) }
            }
            val viewport = target.get()
            val transform = sensorToView.get()
            if (!viewport.enabled || transform == null) {
                tracker.reset()
                previousTarget = null
                previousTransform = null
                return
            }
            // At most ~7 proposals/s; CameraX keeps only the latest frame.
            if (now - lastLensAt < 140L) return
            lastLensAt = now
            if (viewport !== previousTarget) tracker.reset()
            else if (transform !== previousTransform || now - previousAt > 450L) tracker.interrupt()
            val frame = runCatching { analyseLens(image, viewport, transform) }.getOrNull()
            val update = tracker.update(frame)
            previousAt = now
            previousTarget = viewport
            previousTransform = transform
            main.execute {
                // Never draw a result from an old viewport/rotation/disabled mode.
                if (active.get() && target.get() === viewport && sensorToView.get() === transform) {
                    if (SystemClock.elapsedRealtime() - now < 500L)
                        onLens(LiveLensReading(update.phase, update.region, now))
                    else onLens(null)
                }
            }
        } finally {
            image.close()
        }
    }

    private fun analyseLens(image: ImageProxy, viewport: LiveLensTarget, transform: Matrix): LiveLensFrame? {
        if (image.format != ImageFormat.YUV_420_888 || image.planes.size < 3 ||
            viewport.width <= 0 || viewport.height <= 0) return null
        val viewToSensor = Matrix()
        if (!transform.invert(viewToSensor)) return null
        // updateTransform supplies sensor -> view, while ImageInfo supplies
        // sensor -> buffer. Compose the inverse in the correct order.
        val viewToBuffer = Matrix().apply {
            setConcat(image.imageInfo.sensorToBufferTransformMatrix, viewToSensor)
        }
        val longest = maxOf(viewport.width, viewport.height)
        val width = (96f * viewport.width / longest).roundToInt()
        val height = (96f * viewport.height / longest).roundToInt()
        if (width !in 24..96 || height !in 24..96) return null
        val coordinates = FloatArray(width * height * 2)
        for (y in 0 until height) for (x in 0 until width) {
            val index = (y * width + x) * 2
            coordinates[index] = (x + .5f) * viewport.width / width
            coordinates[index + 1] = (y + .5f) * viewport.height / height
        }
        viewToBuffer.mapPoints(coordinates)
        val crop = image.cropRect
        val yuv = image.planes
        val samples = IntArray(width * height)
        for (i in samples.indices) {
            val xx = coordinates[i * 2]
            val yy = coordinates[i * 2 + 1]
            if (!xx.isFinite() || !yy.isFinite()) return null
            val x = xx.toInt()
            val y = yy.toInt()
            if (!crop.contains(x, y) || x !in 0 until image.width || y !in 0 until image.height) return null
            samples[i] = liveLensPixel(sample(yuv[0], x, y), sample(yuv[1], x / 2, y / 2), sample(yuv[2], x / 2, y / 2))
        }
        return LiveLensFrame(width, height, samples)
    }

    private fun sample(plane: ImageProxy.PlaneProxy, x: Int, y: Int): Int {
        val buffer = plane.buffer
        val index = buffer.position() + y * plane.rowStride + x * plane.pixelStride
        return buffer.get(index).toInt() and 255
    }
}
