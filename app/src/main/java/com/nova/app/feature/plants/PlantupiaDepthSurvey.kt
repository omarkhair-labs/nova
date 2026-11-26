package com.nova.app.feature.plants

import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.exceptions.NotTrackingException
import com.google.ar.core.exceptions.NotYetAvailableException
import java.nio.ByteOrder
import java.util.TreeMap
import kotlin.math.floor

/** Sparse, metre-scaled raw-depth observations relative to the survey's first tracked corner. */
internal class PlantupiaDepthSurvey {
    private val cells = TreeMap<Long, FloatArray>()
    private var timestamp = 0L

    fun clear() {
        cells.clear()
        timestamp = 0L
    }

    fun coordinates(): FloatArray {
        if (cells.size < 30) return floatArrayOf()
        return FloatArray(cells.size * 3).also { output ->
            cells.values.forEachIndexed { index, xyz -> xyz.copyInto(output, index * 3) }
        }
    }

    fun observe(frame: Frame, origin: Pose) {
        try {
            frame.acquireRawDepthImage16Bits().use { depth ->
                if (depth.timestamp == timestamp) return
                frame.acquireRawDepthConfidenceImage().use { confidence ->
                    if (depth.width != confidence.width || depth.height != confidence.height ||
                        depth.timestamp != confidence.timestamp) return
                    timestamp = depth.timestamp
                    val camera = frame.camera
                    val intrinsics = camera.textureIntrinsics
                    val size = intrinsics.imageDimensions
                    val focus = intrinsics.focalLength
                    val center = intrinsics.principalPoint
                    val sx = depth.width.toFloat() / size[0]
                    val sy = depth.height.toFloat() / size[1]
                    val localFromCamera = origin.inverse().compose(camera.pose)
                    val plane = depth.planes.first()
                    val scores = confidence.planes.first()
                    val buffer = plane.buffer.order(ByteOrder.LITTLE_ENDIAN)
                    val mask = scores.buffer
                    val step = maxOf(4, depth.width / 28)
                    for (y in step / 2 until depth.height step step) {
                        for (x in step / 2 until depth.width step step) {
                            val score = mask.get(y * scores.rowStride + x * scores.pixelStride).toInt() and 255
                            if (score < 160) continue
                            val mm = buffer.getShort(y * plane.rowStride + x * plane.pixelStride).toInt() and 65535
                            if (mm !in 500..5000) continue
                            val z = mm * .001f
                            val point = localFromCamera.transformPoint(floatArrayOf(
                                (x / sx - center[0]) * z / focus[0],
                                -(y / sy - center[1]) * z / focus[1], -z))
                            if (point.any { it !in -100f..100f }) continue
                            // A stable hash of each 10 cm cell samples all visited viewpoints
                            // after reaching the cap instead of filling entirely on the first frame.
                            var key = 0xcbf29ce484222325UL.toLong()
                            for (v in point) key = (key xor floor(v * 10f).toLong()) * 1099511628211L
                            if (cells.size < 1500 || key <= cells.lastKey()) {
                                cells[key] = point
                                if (cells.size > 1500) cells.pollLastEntry()
                            }
                        }
                    }
                }
            }
        } catch (_: NotYetAvailableException) {
            // Depth can start later than plane tracking.
        } catch (_: NotTrackingException) {
            // A later tracked frame may contribute.
        }
    }
}
