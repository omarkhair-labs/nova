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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.ui.graphics.Color
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

internal data class FieldPatch(val color: String, val first: IntOffset, val last: IntOffset, val fraction: Double)

internal data class FieldRegion(
    val id: Long,
    val label: String,
    val pixels: Int,
    val vertices: List<IntOffset>,
    val outlineLength: Double?,
    val colors: List<Double>,
    val reviewedProposal: Boolean = false,
    val planarAreaCm2: Double? = null,
    val calibrationError: Double? = null,
    val calibrationReason: String? = null,
    val patches: List<FieldPatch> = emptyList(),
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
        val calibration = geometry.optJSONObject("calibration")
        val calibrated = calibration?.optString("method") == "face-on-two-segment-v1"
        val accepted = calibrated && calibration?.optString("status") == "accepted"
        val cm2 = geometry.optDouble("projected_area_cm2").takeIf { accepted && it.isFinite() && it > 0 }
        val scaleError = calibration?.optDouble("scale_check_relative_error")?.takeIf { it.isFinite() && it >= 0 }
        val patches = measurements.optJSONArray("visible_patches")
        val parsed = (0 until minOf(patches?.length() ?: 0, 8)).mapNotNull { i ->
            val patch = patches?.optJSONObject(i) ?: return@mapNotNull null
            val b = patch.optJSONArray("bounds") ?: return@mapNotNull null
            val appearance = patch.optString("appearance")
            val fraction = patch.optDouble("region_fraction")
            if (b.length() != 4 || appearance !in setOf("yellow_like", "brown_like") || fraction !in 0.0..1.0)
                return@mapNotNull null
            val first = IntOffset(b.optInt(0), b.optInt(1))
            val last = IntOffset(b.optInt(2), b.optInt(3))
            if (first.x !in 0 until width || first.y !in 0 until height ||
                last.x !in first.x until width || last.y !in first.y until height) return@mapNotNull null
            FieldPatch(appearance, first, last, fraction)
        }
        FieldRegion(id, part, area, outline, length, colors, patches = parsed,
            reviewedProposal = measurements.optString("method") in setOf("reviewed-grabcut-points-v1", "reviewed-grabcut-frame-v1") &&
                measurements.optJSONObject("assistance")?.optBoolean("operator_accepted") == true,
            planarAreaCm2 = cm2, calibrationError = scaleError,
            calibrationReason = calibration?.takeIf { calibrated && !accepted }?.optString("reason"))
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
    canSuggest: Boolean,
    busy: Boolean,
    revision: Int,
    suggest: (JSONObject, (JSONObject) -> Unit) -> Unit,
    save: (JSONObject) -> Unit,
) {
    var tracing by remember(revision, url) { mutableStateOf(false) }
    var assisting by remember(revision, url) { mutableStateOf(false) }
    var selected by remember(revision, url) { mutableStateOf<Long?>(null) }
    var points by remember(revision, tracing, url) { mutableStateOf(emptyList<IntOffset>()) }
    var part by remember(revision, tracing, assisting, url) { mutableStateOf("leaf") }
    var foreground by remember(revision, assisting, url) { mutableStateOf(emptyList<IntOffset>()) }
    var background by remember(revision, assisting, url) { mutableStateOf(emptyList<IntOffset>()) }
    var includePoint by remember(revision, assisting, url) { mutableStateOf(true) }
    var proposal by remember(revision, assisting, url) { mutableStateOf<OutlineProposal?>(null) }
    var feedback by remember(revision, assisting, url) { mutableStateOf("") }
    var calibrating by remember(revision, tracing, assisting, url) { mutableStateOf(false) }
    var scaleDraft by remember(revision, tracing, assisting, url) { mutableStateOf(PlanarScaleDraft()) }
    fun resetProposal() { proposal = null; feedback = "" }
    var loaded by remember(url) { mutableStateOf(false) }
    var viewPhoto by remember(url) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val ink = PlantupiaColors.forest
    val paper = PlantupiaColors.paper
    val chosen = regions.firstOrNull { it.id == selected }
    var highlighted by remember(selected) { mutableStateOf<Int?>(null) }
    val scalePayload = if (calibrating) scaleDraft.payload() else null
    val scaleReady = !calibrating || scalePayload != null
    val submission = remember(points, part, proposal?.token, calibrating, scaleDraft) { UUID.randomUUID().toString() }
    val tracingEnabled = tracing && loaded && !busy && canTrace
    val assistingEnabled = assisting && loaded && !busy && canTrace && canSuggest
    val marking = tracing || assisting
    val includeInk = Color(0xFF1E7953)
    val excludeInk = Color(0xFFB85B38)
    val addPrompt = rememberUpdatedState<(IntOffset) -> Unit> { point ->
        if (includePoint && foreground.size < 6 && point !in foreground && point !in background) {
            foreground = foreground + point
            resetProposal()
            if (background.isEmpty()) includePoint = false
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else if (!includePoint && background.size < 6 && point !in background && point !in foreground) {
            background = background + point
            resetProposal()
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val addScalePoint = rememberUpdatedState<(IntOffset) -> Unit> { point ->
        if (scaleDraft.points.size < 4 && point !in scaleDraft.points) {
            scaleDraft = scaleDraft.copy(points = scaleDraft.points + point)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
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
            onOpenPhoto = if (marking || regions.isNotEmpty()) null else { { viewPhoto = true } },
            onPhotoLoadChanged = { loaded = it },
        )
        if (loaded) {
            val traceGesture = if (tracingEnabled || assistingEnabled || (!busy && !marking && regions.isNotEmpty()))
                Modifier.pointerInput(width, height, tracingEnabled, assistingEnabled, calibrating, regions) {
                detectTapGestures { tap ->
                    val frame = RegionPhotoFrame(width, height, Size(size.width.toFloat(), size.height.toFloat()))
                    if (frame.scale <= 0f) return@detectTapGestures
                    val position = (tap - frame.origin) / frame.scale
                    if (position.x >= 0 && position.x < width && position.y >= 0 && position.y < height) {
                        val point = IntOffset(position.x.toInt(), position.y.toInt())
                        when {
                            calibrating && (tracingEnabled || assistingEnabled) -> addScalePoint.value(point)
                            tracingEnabled -> addPoint.value(point)
                            assistingEnabled -> addPrompt.value(point)
                            else -> pickRegion.value(point)
                        }
                    }
                }
            } else Modifier
            Canvas(Modifier.fillMaxSize().then(traceGesture)) {
                val frame = RegionPhotoFrame(width, height, size)
                val outlines = when {
                    tracing -> listOf(points to true)
                    assisting -> proposal?.let { listOf(it.vertices to true) }.orEmpty()
                    else -> regions.sortedBy { it.id == selected }.map { it.vertices to (it.id == selected) }
                }
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
                if (calibrating) {
                    val marks = scaleDraft.points.map(frame::project)
                    for (i in marks.indices step 2) {
                        val color = if (i == 0) Color(0xFFFFCE68) else Color(0xFF93DAE8)
                        if (i+1 < marks.size) drawLine(color, marks[i], marks[i+1], 3.dp.toPx())
                        for (j in i..minOf(i+1, marks.lastIndex)) {
                            drawCircle(ink, 7.dp.toPx(), marks[j])
                            drawCircle(color, 5.dp.toPx(), marks[j])
                        }
                    }
                }
                if (!marking) chosen?.patches?.forEachIndexed { i, patch ->
                    val first = frame.project(patch.first)
                    val last = frame.project(patch.last)
                    val bounds = Size(last.x - first.x, last.y - first.y)
                    if (highlighted == i) drawRect(paper.copy(alpha = .25f), first, bounds)
                    drawRect(if (patch.color == "yellow_like") Color(0xFFFFDA52) else Color(0xFFEB9B78),
                        first, bounds, style = Stroke(if (highlighted == i) 3.dp.toPx() else 1.dp.toPx()))
                }
                if (assisting) {
                    for ((seeds, isIncluded) in listOf(foreground to true, background to false)) {
                        for (seed in seeds) {
                            val center = frame.project(seed)
                            drawCircle(paper, 10.dp.toPx(), center)
                            drawCircle(if (isIncluded) includeInk else excludeInk, 8.dp.toPx(), center)
                            val radius = 4.dp.toPx()
                            drawLine(paper, center - Offset(radius, 0f), center + Offset(radius, 0f), 2.dp.toPx())
                            if (isIncluded) drawLine(paper, center - Offset(0f, radius), center + Offset(0f, radius), 2.dp.toPx())
                        }
                    }
                }
            }
        }
    }
    if (viewPhoto) PlantupiaPhotoViewer(url, "this record") { viewPhoto = false }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!marking) TextButton(enabled = loaded && !busy, onClick = { viewPhoto = true }) {
            Text("Open original photo")
        }
        if (canTrace && !assisting) TextButton(enabled = !busy && (loaded || tracing), onClick = { tracing = !tracing }) {
            Text(if (tracing) "Cancel trace" else "Trace a visible region")
        }
        if (canSuggest && canTrace && !tracing) TextButton(enabled = !busy && (loaded || assisting),
            onClick = { assisting = !assisting }) {
            Text(if (assisting) "Cancel suggestion" else "Suggest an outline")
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
        if (points.size >= 3) PlanarScaleControls(calibrating, scaleDraft, !busy,
            onToggle = {
                calibrating = it
                if (it) scope.launch { photoAnchor.bringIntoView() }
            }, onChange = { scaleDraft = it }, onLocate = { scope.launch { photoAnchor.bringIntoView() } })
        Button(enabled = tracingEnabled && points.size >= 3 && scaleReady, onClick = {
            val body = JSONObject().put("submission_id", submission).put("vertices", pointArray(points)).put("part", part)
            scalePayload?.let { body.put("point_reference", it) }
            save(body)
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Save outline · ${points.size} points")
        }
    } else if (assisting) {
        Text("Find the edge", style = PlantupiaText.editorialHeading)
        Text(if (foreground.isEmpty()) "Try the foreground outline, or tap inside the part to guide it."
            else if (background.isEmpty()) "Tap outside that part to exclude the background."
            else "Add points to correct the selection, then find its outline.",
            style = PlantupiaText.body)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(selected = includePoint, enabled = assistingEnabled, onClick = { includePoint = true },
                label = { Text("+ Include · ${foreground.size}") })
            FilterChip(selected = !includePoint, enabled = assistingEnabled, onClick = { includePoint = false },
                label = { Text("− Exclude · ${background.size}") })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(enabled = assistingEnabled && (if (includePoint) foreground.isNotEmpty() else background.isNotEmpty()),
                onClick = {
                    if (includePoint) foreground = foreground.dropLast(1) else background = background.dropLast(1)
                    calibrating = false
                    resetProposal()
                }) { Text("Undo point") }
            TextButton(enabled = assistingEnabled && (foreground.isNotEmpty() || background.isNotEmpty()),
                onClick = {
                    foreground = emptyList()
                    background = emptyList()
                    includePoint = true
                    calibrating = false
                    resetProposal()
                }) { Text("Clear points") }
        }
        Button(enabled = assistingEnabled && !calibrating && (foreground.isEmpty() == background.isEmpty()),
            onClick = {
                resetProposal()
                val request = if (foreground.isEmpty()) JSONObject().put("mode", "automatic")
                    else JSONObject().put("foreground", pointArray(foreground))
                    .put("background", pointArray(background))
                suggest(request) { response ->
                    proposal = parseOutlineProposal(response, width, height)
                    feedback = if (proposal != null) "Inspect the proposed boundary on the photo."
                        else response.optString("reason").take(240).ifBlank {
                            "No usable outline was returned. Change the points or trace manually."
                        }
                    scope.launch { photoAnchor.bringIntoView() }
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (busy) "Looking at the photograph…" else if (foreground.isEmpty()) "Find foreground outline" else "Find outline")
        }
        if (feedback.isNotBlank()) Text(feedback, style = PlantupiaText.supporting)
        proposal?.let { candidate ->
            InspectionChoice("Label this part", part,
                listOf("leaf", "plant", "stem", "flower", "pot", "soil", "exposed_root", "visible_damage"),
                assistingEnabled) { part = it }
            Text("A computer-assisted outline, not an identified plant part. Save only if the boundary matches what you see.",
                style = PlantupiaText.supporting)
            PlanarScaleControls(calibrating, scaleDraft, !busy,
                onToggle = {
                    calibrating = it
                    if (it) scope.launch { photoAnchor.bringIntoView() }
                }, onChange = { scaleDraft = it }, onLocate = { scope.launch { photoAnchor.bringIntoView() } })
            Button(enabled = assistingEnabled && scaleReady, onClick = {
                val body = JSONObject().put("submission_id", submission)
                    .put("vertices", pointArray(candidate.vertices)).put("part", part)
                    .put("suggestion_token", candidate.token).put("outline_method", candidate.method)
                scalePayload?.let { body.put("point_reference", it) }
                save(body)
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Accept and save outline")
            }
        }
        OutlinedButton(enabled = !busy, onClick = { assisting = false; tracing = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Draw the outline myself")
        }
        Text("Uses the saved private photograph. No species or health conclusion is made.",
            style = PlantupiaText.supporting)
    } else {
        Text("Visible regions", style = PlantupiaText.editorialHeading)
        val regionCount = if (totalRegions > regions.size) "Showing ${regions.size} of $totalRegions saved outlines."
            else if (regions.isEmpty()) "No outlined regions in this record." else "Tap an outline on the photo or choose one below."
        Text(regionCount, style = PlantupiaText.supporting)
        if (chosen != null) {
            RegionMeasurements(chosen)
            if (chosen.patches.isNotEmpty()) {
                Text("Visible colour patches", style = PlantupiaText.itemTitle)
                Text("Select a patch to locate it on the photograph. Lighting and natural markings affect colour.",
                    style = PlantupiaText.supporting)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chosen.patches.forEachIndexed { i, patch ->
                        val color = if (patch.color == "yellow_like") "Yellow" else "Brown"
                        androidx.compose.material3.FilterChip(selected = highlighted == i, onClick = {
                            highlighted = if (highlighted == i) null else i
                            scope.launch { photoAnchor.bringIntoView() }
                        }, label = { Text("$color ${i+1} · ${(100 * patch.fraction).roundToInt()}%") })
                    }
                }
            }
        }
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
        if (region.planarAreaCm2 != null) {
            Text(String.format(Locale.ROOT, "%.2f cm²", region.planarAreaCm2), style = PlantupiaText.display)
            Text("Estimated projected area · reported flat, face-on plane", style = PlantupiaText.supporting)
            Text("${region.pixels} px² in the original image", style = PlantupiaText.body)
        } else {
            Text("${region.pixels} px²", style = PlantupiaText.display)
            Text("Visible image area · not physical size", style = PlantupiaText.supporting)
        }
        region.calibrationError?.let {
            Text("Independent scale discrepancy: " + String.format(Locale.ROOT, "%.1f%%", it*100),
                style = PlantupiaText.supporting)
        }
        region.calibrationReason?.let {
            Text("Scale not accepted · " + scaleRejectionText(it), style = PlantupiaText.supporting)
        }
        Text(if (region.reviewedProposal) "Computer-assisted boundary · accepted by the recorder"
            else "Boundary traced by the recorder", style = PlantupiaText.supporting)
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

internal data class OutlineProposal(val vertices: List<IntOffset>, val token: String, val method: String)

private fun pointArray(points: List<IntOffset>): JSONArray = JSONArray().apply {
    points.forEach { point -> put(JSONArray().put(point.x).put(point.y)) }
}

/** Only the server-signed original-grid proposal can be accepted. */
internal fun parseOutlineProposal(raw: JSONObject, width: Int, height: Int): OutlineProposal? {
    if (width <= 0 || height <= 0 || raw.optString("status") != "candidate" ||
        raw.optString("method") !in setOf("reviewed-grabcut-points-v1", "reviewed-grabcut-frame-v1")) return null
    val token = raw.optString("suggestion_token")
    if (token.isBlank() || token.length > 8192 || token == "null") return null
    val array = raw.optJSONArray("vertices") ?: return null
    if (array.length() !in 3..64) return null
    val points = (0 until array.length()).map { i ->
        val pair = array.optJSONArray(i) ?: return null
        if (pair.length() != 2) return null
        val x = pair.optInt(0, -1)
        val y = pair.optInt(1, -1)
        if (x !in 0 until width || y !in 0 until height) return null
        IntOffset(x, y)
    }
    if (points.distinct().size != points.size) return null
    return OutlineProposal(points, token, raw.getString("method"))
}
