package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.min
import java.util.Locale

internal data class FieldRegion(
    val id: Long,
    val label: String,
    val pixels: Int,
    val vertices: List<IntOffset>,
    val outlineLength: Double?,
    val colors: List<Double>,
)

private val regionColorKeys = listOf("green_like", "yellow_like", "brown_like", "other")

internal fun fieldRegions(rows: JSONArray?, width: Int, height: Int): List<FieldRegion> =
    (0 until (rows?.length() ?: 0)).mapNotNull { index ->
        val row = rows?.optJSONObject(index) ?: return@mapNotNull null
        val annotation = row.optJSONObject("annotation") ?: return@mapNotNull null
        val vertices = annotation.optJSONArray("vertices") ?: return@mapNotNull null
        val part = annotation.optString("part").replace('_', ' ')
        val measurements = row.optJSONObject("measurements") ?: return@mapNotNull null
        val geometry = measurements.optJSONObject("geometry") ?: return@mapNotNull null
        val area = geometry.optInt("projected_area_px2")
        if (part.isBlank() || area <= 0 || vertices.length() !in 3..64) return@mapNotNull null
        val outline = (0 until vertices.length()).map { index ->
            val point = vertices.optJSONArray(index) ?: return@mapNotNull null
            val x = point.optInt(0, -1)
            val y = point.optInt(1, -1)
            if (x !in 0 until width || y !in 0 until height) return@mapNotNull null
            IntOffset(x, y)
        }
        val id = row.optLong("id").takeIf { it > 0 } ?: return@mapNotNull null
        val length = geometry.optDouble("traced_outline_length_px").takeIf { it.isFinite() && it > 0 }
        val fractions = measurements.optJSONObject("color_fractions")
        val colors = regionColorKeys.map { fractions?.optDouble(it) ?: Double.NaN }
            .takeIf { it.all { fraction -> fraction in 0.0..1.0 } }.orEmpty()
        FieldRegion(id, part, area, outline, length, colors)
    }

private class RegionPhotoFrame(width: Int, height: Int, canvas: Size) {
    val scale = min(canvas.width / width, canvas.height / height)
    val origin = Offset((canvas.width - width * scale) / 2f, (canvas.height - height * scale) / 2f)

    fun project(point: IntOffset) = origin + Offset(point.x * scale, point.y * scale)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaRegionStudy(
    url: String,
    width: Int,
    height: Int,
    regions: List<FieldRegion>,
    totalRegions: Int,
    canTrace: Boolean,
    busy: Boolean,
    revision: Int,
    save: (JSONObject) -> Unit,
) {
    var tracing by remember(revision) { mutableStateOf(false) }
    var selected by remember(revision) { mutableStateOf<Long?>(null) }
    var points by remember(revision, tracing) { mutableStateOf(emptyList<IntOffset>()) }
    var part by remember(revision, tracing) { mutableStateOf("leaf") }
    var loaded by remember(url) { mutableStateOf(false) }
    var viewPhoto by remember(url) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val ink = PlantupiaColors.forest
    val paper = PlantupiaColors.paper
    val chosen = regions.firstOrNull { it.id == selected }
    val submission = remember(points, part) { UUID.randomUUID().toString() }
    val tracingEnabled = tracing && loaded && !busy
    val addPoint = rememberUpdatedState<(IntOffset) -> Unit> { point ->
        if (points.size < 64 && point !in points) {
            points = points + point
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val pickRegion = rememberUpdatedState<(IntOffset) -> Unit> { point ->
        selected = fieldRegionAtPoint(regions, point)?.id
    }
    val photoAnchor = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxWidth().height(300.dp).bringIntoViewRequester(photoAnchor)) {
        PlantupiaEvidencePhoto(
            url, "Original recorded plant photograph", Modifier.fillMaxSize(), cornerRadius = 4.dp,
            framing = PlantupiaPhotoFraming.FULL_RECORD,
            onOpenPhoto = if (tracing || regions.isNotEmpty()) null else { { viewPhoto = true } },
            onPhotoLoadChanged = { loaded = it },
        )
        if (loaded) {
            val traceGesture = if (tracingEnabled || (!busy && !tracing && regions.isNotEmpty()))
                Modifier.pointerInput(width, height, tracingEnabled, regions) {
                detectTapGestures { tap ->
                    val frame = RegionPhotoFrame(width, height, Size(size.width.toFloat(), size.height.toFloat()))
                    if (frame.scale <= 0f) return@detectTapGestures
                    val position = (tap - frame.origin) / frame.scale
                    if (position.x >= 0 && position.x < width && position.y >= 0 && position.y < height) {
                        val point = IntOffset(position.x.toInt(), position.y.toInt())
                        if (tracingEnabled) addPoint.value(point) else pickRegion.value(point)
                    }
                }
            } else Modifier
            Canvas(Modifier.fillMaxSize().then(traceGesture)) {
                val frame = RegionPhotoFrame(width, height, size)
                val outlines = if (tracing) listOf(points to true)
                    else regions.sortedBy { it.id == selected }.map { it.vertices to (it.id == selected) }
                for ((outline, active) in outlines) {
                    if (outline.isEmpty()) continue
                    val projected = outline.map(frame::project)
                    val path = Path().apply {
                        moveTo(projected.first().x, projected.first().y)
                        for (point in projected.drop(1)) lineTo(point.x, point.y)
                        if (projected.size >= 3) close()
                    }
                    if (projected.size >= 3 && active) drawPath(path, ink.copy(alpha = .28f))
                    drawPath(path, ink.copy(alpha = if (active) 1f else .65f),
                        style = Stroke(if (active) 4.dp.toPx() else 2.5.dp.toPx()))
                    drawPath(path, paper.copy(alpha = if (active) 1f else .8f),
                        style = Stroke(1.5.dp.toPx()))
                    if (tracing) projected.forEach { point ->
                        drawCircle(ink, 4.dp.toPx(), point)
                        drawCircle(paper, 2.dp.toPx(), point)
                    }
                }
            }
        }
    }
    if (viewPhoto) PlantupiaPhotoViewer(url, "this record") { viewPhoto = false }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!tracing) TextButton(enabled = loaded && !busy, onClick = { viewPhoto = true }) {
            Text("Open original photo")
        }
        if (canTrace) TextButton(enabled = !busy && (loaded || tracing), onClick = { tracing = !tracing }) {
            Text(if (tracing) "Cancel trace" else "Trace a visible region")
        }
    }
    if (tracing) {
        Text("Tap around one outline in order. Each point is placed on the original photograph.",
            style = PlantupiaText.supporting)
        InspectionChoice("Region", part,
            listOf("leaf", "plant", "stem", "flower", "pot", "soil", "exposed_root", "visible_damage"),
            !busy) { part = it }
        TextButton(enabled = !busy && points.isNotEmpty(), onClick = { points = points.dropLast(1) }) {
            Text("Undo last point")
        }
        Button(enabled = tracingEnabled && points.size >= 3, onClick = {
            val vertices = JSONArray()
            points.forEach { vertices.put(JSONArray().put(it.x).put(it.y)) }
            save(JSONObject().put("submission_id", submission).put("vertices", vertices).put("part", part))
        }) { Text("Save outline · ${points.size} points") }
    } else {
        Text("Visible regions", style = PlantupiaText.editorialHeading)
        val regionCount = if (totalRegions > regions.size) "Showing ${regions.size} of $totalRegions saved outlines."
            else if (regions.isEmpty()) "No traced regions in this record." else "Tap an outline on the photo or choose one below."
        Text(regionCount, style = PlantupiaText.supporting)
        if (chosen != null) RegionMeasurements(chosen)
        regions.forEach { region ->
            val active = selected == region.id
            Surface(
                enabled = loaded && !busy,
                onClick = {
                    selected = if (active) null else region.id
                    if (!active) scope.launch { photoAnchor.bringIntoView() }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { this.selected = active },
                color = if (active) PlantupiaColors.softSage else PlantupiaColors.paper,
                shape = PlantupiaShapes.inset,
                border = BorderStroke(1.dp, if (active) PlantupiaColors.forest else PlantupiaColors.border),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(region.label.replaceFirstChar { it.uppercase() },
                        style = PlantupiaText.itemTitle, modifier = Modifier.weight(1f))
                    Text("${region.pixels} px²", style = PlantupiaText.supporting)
                }
            }
        }
    }
}

@Composable
private fun RegionMeasurements(region: FieldRegion) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("${region.label.replaceFirstChar { it.uppercase() }} · image measurement",
            style = PlantupiaText.itemTitle)
        Text("${region.pixels} px²", style = PlantupiaText.display)
        Text("Visible image area · not physical size", style = PlantupiaText.supporting)
        region.outlineLength?.let { Text("Traced outline " + String.format(Locale.ROOT, "%.1f", it) + " px",
            style = PlantupiaText.body) }
        if (region.colors.isNotEmpty()) {
            val swatches = listOf(
                androidx.compose.ui.graphics.Color(0xFF58794F), androidx.compose.ui.graphics.Color(0xFFDAC76B),
                androidx.compose.ui.graphics.Color(0xFF8D634C), PlantupiaColors.border,
            )
            Row(Modifier.fillMaxWidth().height(8.dp)) {
                region.colors.forEachIndexed { index, fraction ->
                    if (fraction > 0) {
                        Canvas(Modifier.weight(fraction.toFloat()).height(8.dp)) { drawRect(swatches[index]) }
                    }
                }
            }
            val labels = listOf("Green-like", "Yellow-like", "Brown-like", "Other")
            val percentages = labels.zip(region.colors).joinToString(" · ") { (label, fraction) ->
                "$label ${(fraction * 100).roundToInt()}%"
            }
            Text(percentages, style = PlantupiaText.supporting)
            Text("Colours depend on capture conditions.", style = PlantupiaText.supporting)
        }
    }
}

/** A tap selects a saved human trace, never a newly inferred region. Overlap prefers the smallest traced area. */
internal fun fieldRegionAtPoint(regions: List<FieldRegion>, point: IntOffset): FieldRegion? =
    regions.filter { region ->
        val vertices = region.vertices
        var inside = false
        for (i in vertices.indices) {
            val a = vertices[i]
            val b = vertices[(i + 1) % vertices.size]
            if ((a.y > point.y) != (b.y > point.y)) {
                val crossing = a.x + (b.x - a.x).toDouble() * (point.y - a.y) / (b.y - a.y)
                if (point.x < crossing) inside = !inside
            }
        }
        inside
    }.minWithOrNull(compareBy<FieldRegion> { it.pixels }.thenBy { it.id })
