package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.min

@Composable
internal fun PlantupiaFieldInspection(repository: PlantupiaRepository, plant: PlantupiaPlant, event: PlantupiaCareEvent) =
    key(repository, plant.id, event.id) {
        var expanded by remember { mutableStateOf(false) }
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Close inspection" else "Inspect this record") }
        if (expanded) {
            val scope = rememberCoroutineScope()
            var record by remember { mutableStateOf<JSONObject?>(null) }
            var feedback by remember { mutableStateOf("") }
            var busy by remember { mutableStateOf(false) }
            var generation by remember { mutableIntStateOf(0) }
            LaunchedEffect(generation) {
                when (val result = repository.fieldEvidence(plant.id, event.id)) {
                    is ApiResult.Failure -> feedback = result.message
                    is ApiResult.Success -> if (repository.isCurrentSession()) record = result.value
                }
            }
            fun submit(action: String, body: JSONObject, success: (JSONObject) -> Unit) {
                if (busy) return
                busy = true
                scope.launch {
                    when (val result = repository.fieldEvidence(plant.id, event.id, action, body)) {
                        is ApiResult.Failure -> feedback = result.message
                        is ApiResult.Success -> if (repository.isCurrentSession()) { feedback = ""; success(result.value) }
                    }
                    busy = false
                }
            }
            if (feedback.isNotBlank()) Text(feedback)
            TextButton(enabled = !busy, onClick = { generation++ }) { Text("Refresh inspection") }
            record?.let { data ->
                val regions = data.optJSONArray("regions") ?: JSONArray()
                for (i in 0 until regions.length()) {
                    val item = regions.getJSONObject(i)
                    val geometry = item.getJSONObject("measurements").getJSONObject("geometry")
                    Text("Traced ${item.getJSONObject("annotation").getString("part").replace('_', ' ')} · ${geometry.getInt("projected_area_px2")} image pixels")
                }
                val reviews = data.optJSONArray("reviews") ?: JSONArray()
                for (i in 0 until reviews.length()) {
                    val review = reviews.getJSONObject(i)
                    Text("${review.getString("finding").replace('_', ' ')} · ${review.getString("rationale")}")
                }
                val quality = data.optJSONObject("quality")
                val w = quality?.optInt("width") ?: 0
                val h = quality?.optInt("height") ?: 0
                if (plant.canRecord && plant.active && w > 0 && h > 0 && event.photoUrl.isNotBlank()) {
                    RegionTrace(event.photoUrl, w, h, busy, generation) { body ->
                        submit("regions/", body) { generation++ }
                    }
                }
                var symptom by remember { mutableStateOf("uncertain") }
                var substrate by remember { mutableStateOf("unknown") }
                var advice by remember(symptom, substrate) { mutableStateOf("") }
                Choice("Visible observation", symptom, listOf("uncertain", "wilting", "spots", "discoloration", "damage"), !busy) { symptom = it }
                if (symptom == "wilting")
                    Choice("Reported substrate condition", substrate, listOf("unknown", "wet", "dry"), !busy) { substrate = it }
                TextButton(enabled = !busy && symptom != "uncertain", onClick = {
                    submit("", JSONObject().put("symptom", symptom).put("substrate_report", substrate)) { answer ->
                        val options = answer.optJSONArray("inspection_options") ?: JSONArray()
                        advice = (0 until options.length()).joinToString("\n") {
                            val option = options.getJSONObject(it)
                            val cause = option.optString("hypothesis").takeUnless { v -> v.isEmpty() || v == "null" }
                            val contradiction = option.optString("contradictory_report").takeUnless { v -> v.isEmpty() || v == "null" }
                            (cause?.let { c -> "Possible ${c.replace('_', ' ')}. " }.orEmpty()) +
                                (contradiction?.let { c -> "Your $c substrate report weighs against this. " }.orEmpty()) +
                                option.getString("inspection")
                        }
                    }
                }) { Text("Show inspection steps") }
                if (advice.isNotBlank()) Text(advice)
                if (data.optBoolean("can_review")) {
                    var finding by remember { mutableStateOf("insufficient_evidence") }
                    var rationale by remember { mutableStateOf("") }
                    val submission = remember(finding, rationale, generation) { UUID.randomUUID().toString() }
                    Choice("Specialist review", finding, listOf("insufficient_evidence", "corrected_observation", "supported_observation"), !busy) { finding = it }
                    OutlinedTextField(rationale, { rationale = it.take(1000) }, enabled = !busy, label = { Text("Reason for this finding") })
                    TextButton(enabled = !busy && rationale.isNotBlank(), onClick = {
                        submit("reviews/", JSONObject().put("submission_id", submission).put("finding", finding).put("rationale", rationale)) {
                            rationale = ""; generation++
                        }
                    }) { Text("Save review to this record") }
                }
            }
        }
    }

@Composable
private fun Choice(label: String, selected: String, values: List<String>, enabled: Boolean, change: (String) -> Unit) {
    var opened by remember { mutableStateOf(false) }
    Box {
        TextButton(enabled = enabled, onClick = { opened = true }) { Text("$label · ${selected.replace('_', ' ')}") }
        DropdownMenu(expanded = opened, onDismissRequest = { opened = false }) {
            values.forEach { value ->
                DropdownMenuItem(text = { Text(value.replace('_', ' ')) }, onClick = { opened = false; change(value) })
            }
        }
    }
}

@Composable
private fun RegionTrace(url: String, width: Int, height: Int, busy: Boolean, generation: Int, save: (JSONObject) -> Unit) {
    var tracing by remember(generation) { mutableStateOf(false) }
    TextButton(enabled = !busy, onClick = { tracing = !tracing }) { Text(if (tracing) "Cancel trace" else "Trace a visible region") }
    if (tracing) {
        var part by remember { mutableStateOf("leaf") }
        var points by remember { mutableStateOf(emptyList<IntOffset>()) }
        var loaded by remember { mutableStateOf(false) }
        val ink = MaterialTheme.colorScheme.tertiary
        Choice("Region", part, listOf("leaf", "plant", "stem", "flower", "pot", "soil", "exposed_root", "visible_damage"), !busy) { part = it }
        Text("Tap around one visible outline in order, then save.")
        Box(Modifier.fillMaxWidth().height(280.dp)) {
            AsyncImage(model = url, contentDescription = "Private photo for tracing", contentScale = ContentScale.Fit,
                onSuccess = { loaded = true }, onError = { loaded = false }, modifier = Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize().pointerInput(loaded, busy, width, height) {
                if (loaded && !busy) detectTapGestures { tap ->
                    val scale = min(size.width.toFloat()/width, size.height.toFloat()/height)
                    val x = (tap.x - (size.width-width*scale)/2f)/scale
                    val y = (tap.y - (size.height-height*scale)/2f)/scale
                    if (x >= 0 && x < width && y >= 0 && y < height && points.size < 64)
                        points = points + IntOffset(x.toInt(), y.toInt())
                }
            }) {
                val scale = min(size.width/width, size.height/height)
                val origin = Offset((size.width-width*scale)/2f, (size.height-height*scale)/2f)
                val path = points.map { origin + Offset(it.x*scale, it.y*scale) }
                path.forEach { drawCircle(ink, 3.dp.toPx(), it) }
                path.zipWithNext().forEach { (a,b) -> drawLine(ink, a, b, 2.dp.toPx()) }
                if (path.size >= 3) drawLine(ink, path.last(), path.first(), 1.dp.toPx())
            }
        }
        val submission = remember(points, part) { UUID.randomUUID().toString() }
        TextButton(enabled = !busy && points.isNotEmpty(), onClick = { points = points.dropLast(1) }) { Text("Undo last point") }
        Button(enabled = loaded && !busy && points.size >= 3, onClick = {
            val vertices = JSONArray()
            points.forEach { vertices.put(JSONArray().put(it.x).put(it.y)) }
            save(JSONObject().put("submission_id", submission).put("vertices", vertices).put("part", part))
        }) { Text("Save trace · ${points.size} points") }
    }
}
