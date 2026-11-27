package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

internal data class VisitExclusion(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    fun json() = JSONArray().put(left).put(top).put(right).put(bottom)
}

internal fun visitExclusion(points: List<VisitPoint>): VisitExclusion? {
    if (points.size != 2 || points.any { !it.x.isFinite() || !it.y.isFinite() ||
                it.x !in 0.0..1.0 || it.y !in 0.0..1.0 }) return null
    val (a, b) = points
    val w = abs(a.x - b.x)
    val h = abs(a.y - b.y)
    if (w < .10 || h < .10 || w*h !in 0.025..0.70) return null
    return VisitExclusion(minOf(a.x,b.x),minOf(a.y,b.y),maxOf(a.x,b.x),maxOf(a.y,b.y))
}

internal data class VisitRegistration(val transform: VisitTransform, val aspect: Double)

internal fun parseVisitRegistration(raw: JSONObject): VisitRegistration? {
    if (raw.optString("status") != "aligned" || raw.optString("model") != "static-orb-similarity-v1" ||
        raw.optString("basis") != "operator_marked_moving_regions") return null
    val w = raw.optInt("frame_width")
    val h = raw.optInt("frame_height")
    if (w !in 240..768 || h !in 240..768) return null
    val aspect = w.toDouble()/h
    if (aspect !in 0.45..2.25 || raw.optInt("test_matches") !in 6..2000) return null
    val error = raw.optDouble("test_p75_error_px")
    if (!error.isFinite() || error !in 0.0..2.5) return null
    val obj = raw.optJSONObject("transform") ?: return null
    val a = obj.optDouble("a")
    val b = obj.optDouble("b")
    val x = obj.optDouble("x")
    val y = obj.optDouble("y")
    if (listOf(a,b,x,y).any { !it.isFinite() } || hypot(a,b) !in 0.85..1.18 ||
        abs(Math.toDegrees(atan2(b,a))) > 15 || abs(x) > .25 || abs(y) > .25) return null
    return VisitRegistration(VisitTransform(a,b,x,y),aspect)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaAutoAlignedVisits(
    pair: PlantupiaLoadedPhotoPair,
    request: suspend (JSONObject) -> ApiResult<JSONObject>,
    isCurrentSession: () -> Boolean,
    manual: () -> Unit,
    close: () -> Unit,
) {
    var earlier by remember(pair) { mutableStateOf(emptyList<VisitPoint>()) }
    var later by remember(pair) { mutableStateOf(emptyList<VisitPoint>()) }
    var answer by remember(pair) { mutableStateOf<VisitRegistration?>(null) }
    var busy by remember(pair) { mutableStateOf(false) }
    var message by remember(pair) { mutableStateOf("") }
    var opacity by remember(pair) { mutableFloatStateOf(.5f) }
    val scope = rememberCoroutineScope()
    val firstBox = visitExclusion(earlier)
    val secondBox = visitExclusion(later)
    val selectingEarlier = firstBox == null
    val active = if (selectingEarlier) earlier else later
    val photo = if (selectingEarlier) pair.earlier else pair.later

    Text("Match the background", style = PlantupiaText.editorialHeading)
    val aligned = answer
    if (aligned != null) {
        VisitBlend(pair, aligned.transform, opacity, { opacity = it }, aligned.aspect)
        Text("Proposed alignment from your stationary background. Slide to inspect both photographs.",
            style = PlantupiaText.body)
        Text("Moving leaves, lighting and perspective may still change. No growth or health measurement is inferred.",
            style = PlantupiaText.supporting)
    } else {
        Text("On each photo, tap opposite corners around the whole plant and anything else that moves. Leave fixed wall or shelf details outside.",
            style = PlantupiaText.body)
        Text(photo.observedAt.take(10) + " · " + (if (selectingEarlier) "Earlier" else "Later") + " · " + active.size + " of 2 corners",
            style = PlantupiaText.sectionLabel)
        VisitExclusionPhoto(photo.photoUrl, active, !busy && active.size < 2) { point ->
            if (selectingEarlier && earlier.size < 2) earlier = earlier + point
            else if (!selectingEarlier && later.size < 2) later = later + point
            message = ""
        }
        if (active.size == 2 && visitExclusion(active) == null)
            Text("Choose a wider rectangle, but leave enough fixed background around it.", style = PlantupiaText.supporting)
        if (firstBox != null && secondBox != null) {
            Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), onClick = {
                busy = true
                message = ""
                scope.launch {
                    try {
                        val body = JSONObject().put("earlier_exclusion", firstBox.json()).put("later_exclusion", secondBox.json())
                        val result = request(body)
                        if (isCurrentSession()) when (result) {
                            is ApiResult.Failure -> message = result.message
                            is ApiResult.Success -> {
                                answer = parseVisitRegistration(result.value)
                                if (answer == null) message = when (result.value.optString("reason")) {
                                    "different_frame_geometry" -> "Use similar framing and orientation in both photographs."
                                    "background_photometry_changed" -> "The background lighting or appearance has changed too much."
                                    "insufficient_image_resolution" -> "Both records need enough detail."
                                    else -> "The fixed background did not support a dependable alignment. Try reference marks."
                                }
                            }
                        }
                    } finally { busy = false }
                }
            }) { Text(if (busy) "Matching fixed details…" else "Match both photographs") }
        }
        if (message.isNotBlank()) Text(message, style = PlantupiaText.supporting)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (answer == null && (earlier.isNotEmpty() || later.isNotEmpty())) {
            TextButton(enabled = !busy, onClick = {
                if (later.isNotEmpty()) later = later.dropLast(1) else earlier = earlier.dropLast(1)
                message = ""
            }) { Text("Undo corner") }
        }
        TextButton(enabled = !busy && earlier.isNotEmpty(), onClick = {
            earlier = emptyList(); later = emptyList(); answer = null; message = ""; opacity = .5f
        }) { Text("Start over") }
        TextButton(enabled = !busy, onClick = manual) { Text("Use reference marks") }
        TextButton(onClick = close) { Text("Close alignment") }
    }
}

@Composable
private fun VisitExclusionPhoto(url: String, points: List<VisitPoint>, enabled: Boolean, onPoint: (VisitPoint) -> Unit) {
    var aspect by remember(url) { mutableFloatStateOf(0f) }
    var failed by remember(url) { mutableStateOf(false) }
    val callback by rememberUpdatedState(onPoint)
    Box(Modifier.fillMaxWidth().aspectRatio(4f/3f).clipToBounds().background(PlantupiaColors.forest)) {
        AsyncImage(url,"Recorded photograph for moving-area marks",Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            onSuccess = { result ->
                val im = result.result.image
                failed = false
                aspect = if (im.width > 0 && im.height > 0) im.width.toFloat()/im.height else 0f
            }, onError = { failed = true; aspect = 0f })
        val taps = if (enabled && aspect > 0) Modifier.pointerInput(url, aspect, enabled) {
            detectTapGestures { touch ->
                val w = min(size.width.toFloat(), size.height * aspect)
                val h = w / aspect
                if (w > 0 && h > 0) {
                    val x = (touch.x-(size.width-w)/2f)/w
                    val y = (touch.y-(size.height-h)/2f)/h
                    if (x in 0f..1f && y in 0f..1f) callback(VisitPoint(x.toDouble(),y.toDouble()))
                }
            }
        } else Modifier
        Canvas(Modifier.fillMaxSize().then(taps).semantics { contentDescription = "Mark two opposite moving-area corners" }) {
            if (aspect > 0) {
                val w = min(size.width,size.height*aspect)
                val h = w/aspect
                fun project(p: VisitPoint) = Offset((size.width-w)/2f+p.x.toFloat()*w,(size.height-h)/2f+p.y.toFloat()*h)
                if (points.size == 2) {
                    val a = project(points[0]); val b = project(points[1])
                    val topLeft = Offset(minOf(a.x,b.x),minOf(a.y,b.y))
                    val extent = Size(abs(a.x-b.x),abs(a.y-b.y))
                    drawRect(PlantupiaColors.forest.copy(alpha = .28f),topLeft,extent)
                    drawRect(PlantupiaColors.paper,topLeft,extent,style = Stroke(2.dp.toPx()))
                }
                points.forEach { point ->
                    drawCircle(PlantupiaColors.forest,7.dp.toPx(),project(point))
                    drawCircle(PlantupiaColors.paper,4.dp.toPx(),project(point))
                }
            }
        }
    }
    if (failed) Text("This photograph could not load.", style = PlantupiaText.supporting)
}
