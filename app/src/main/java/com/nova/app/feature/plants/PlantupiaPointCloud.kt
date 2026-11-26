package com.nova.app.feature.plants

import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.sqrt

internal const val PLANTUPIA_PLY_MAX_BYTES = 700_000
private const val MAX_POINTS = 6_000

internal data class SpatialPoint(
    val x: Double, val y: Double, val z: Double,
    val red: Int, val green: Int, val blue: Int,
)

internal fun readPlantupiaPointCloud(input: InputStream): List<SpatialPoint> {
    val bytes = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        require(bytes.size() + count <= PLANTUPIA_PLY_MAX_BYTES) { "The scene exceeds the supported file size." }
        bytes.write(buffer, 0, count)
    }
    return normalizePointCloud(parsePlantupiaPointCloud(bytes.toByteArray()))
}

internal fun parsePlantupiaPointCloud(source: ByteArray): List<SpatialPoint> {
    require(source.size in 1..PLANTUPIA_PLY_MAX_BYTES) { "The scene exceeds the supported file size." }
    val lines = source.toString(Charsets.US_ASCII).lineSequence().iterator()
    fun next() = if (lines.hasNext()) lines.next() else throw IllegalArgumentException("The scene ends early.")
    require(next() == "ply" && next() == "format ascii 1.0") { "Use a Plantupia ASCII PLY export." }
    var row = next()
    var comments = 0
    while (row.startsWith("comment ") && comments++ < 12) row = next()
    val count = row.removePrefix("element vertex ").toIntOrNull()
    require(row.startsWith("element vertex ") && count != null && count in 30..MAX_POINTS) {
        "The scene needs 30 to 6,000 vertices."
    }
    listOf("float x", "float y", "float z", "uchar red", "uchar green", "uchar blue").forEach { property ->
        require(next() == "property $property") { "Use a Plantupia point export with RGB colours." }
    }
    require(next() == "end_header") { "This viewer supports point-only scenes." }
    val whitespace = Regex("[ \\t]+")
    val points = List(count) {
        val fields = next().trim().split(whitespace)
        require(fields.size == 6) { "A vertex is invalid." }
        val coords = fields.take(3).map { token -> token.toDoubleOrNull()?.takeIf { it in -1e9..1e9 } }
        val colors = fields.drop(3).map { token -> token.toIntOrNull()?.takeIf { it in 0..255 } }
        require(coords.all { it != null } && colors.all { it != null }) { "A vertex has invalid coordinates or colours." }
        SpatialPoint(coords[0]!!, coords[1]!!, coords[2]!!, colors[0]!!, colors[1]!!, colors[2]!!)
    }
    while (lines.hasNext()) require(lines.next().isBlank()) { "Unexpected data after the vertices." }
    return points
}

internal fun normalizePointCloud(points: List<SpatialPoint>): List<SpatialPoint> {
    require(points.size >= 30) { "There are too few points to display." }
    fun median(coordinates: List<Double>) = coordinates.sorted()[coordinates.size / 2]
    val x = median(points.map { it.x })
    val y = median(points.map { it.y })
    val z = median(points.map { it.z })
    val centered = points.map { it.copy(x = it.x - x, y = it.y - y, z = it.z - z) }
    val radii = centered.map { sqrt(it.x * it.x + it.y * it.y + it.z * it.z) }
    val radius = radii.sorted()[(radii.lastIndex * .97).toInt()]
    require(radius.isFinite() && radius > 1e-12) { "The reconstructed scene has no usable extent." }
    return centered.filterIndexed { i, _ -> radii[i] <= radius * 2.5 }
        .map { it.copy(x = it.x / radius, y = it.y / radius, z = it.z / radius) }
}

/** AR supplies geometry only. Uniform ink distinguishes tracked points from photographic RGB. */
internal fun surveyPreviewPoints(sample: FloatArray?): List<SpatialPoint> {
    if (sample == null || sample.size !in 90..4500 || sample.size % 3 != 0 || sample.any { !it.isFinite() }) return emptyList()
    val points = (sample.indices step 3).map { i ->
        SpatialPoint(sample[i].toDouble(), -sample[i+1].toDouble(), sample[i+2].toDouble(), 172, 215, 185)
    }
    return runCatching { normalizePointCloud(points) }.getOrDefault(emptyList())
}
