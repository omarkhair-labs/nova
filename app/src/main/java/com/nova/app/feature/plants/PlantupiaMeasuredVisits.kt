package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

private fun measuredRegions(raw: JSONObject): List<FieldRegion> {
    val quality = raw.optJSONObject("quality") ?: return emptyList()
    val w = quality.optInt("width")
    val h = quality.optInt("height")
    if (w <= 0 || h <= 0) return emptyList()
    return fieldRegions(raw.optJSONArray("regions"), w, h).filter { it.planarAreaCm2 != null }
}

@Composable
internal fun PlantupiaMeasuredVisits(repository: PlantupiaRepository, plantId: String, pair: PlantupiaLoadedPhotoPair) {
    var opened by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { opened = true },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Compare measured areas")
    }
    if (!opened) return
    var earlier by remember { mutableStateOf<List<FieldRegion>>(emptyList()) }
    var later by remember { mutableStateOf<List<FieldRegion>>(emptyList()) }
    var first by remember { mutableStateOf<Long?>(null) }
    var second by remember { mutableStateOf<Long?>(null) }
    var ready by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var samePart by remember { mutableStateOf(false) }
    var comparable by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    val scope = rememberCoroutineScope()
    fun resetSelection() {
        samePart = false
        comparable = false
        result = null
        error = ""
    }
    LaunchedEffect(retry) {
        ready = false
        resetSelection()
        earlier = emptyList()
        later = emptyList()
        first = null
        second = null
        val a = repository.fieldEvidence(plantId, pair.earlier.id)
        val b = repository.fieldEvidence(plantId, pair.later.id)
        if (!repository.isCurrentSession()) return@LaunchedEffect
        if (a is ApiResult.Success && b is ApiResult.Success) {
            earlier = measuredRegions(a.value)
            later = measuredRegions(b.value)
            ready = true
        } else error = "Both private measurement records could not be loaded."
    }
    PlantupiaLivingWorkspace("LIVING TIME", "Measured visits", "Projected areas in the recorded image plane", busy,
        close = { opened = false }) {
        if (!ready && error.isBlank()) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) {
            Text(error, style = PlantupiaText.supporting)
            if (!ready) TextButton(onClick = { retry++ }) { Text("Retry measurements") }
        }
        if (ready && (earlier.isEmpty() || later.isEmpty())) {
            Text("Two calibrated records are needed", style = PlantupiaText.editorialHeading)
            Text("Open each saved photograph in History, outline the same flat part, and calibrate both with independent reference lengths.",
                style = PlantupiaText.body)
        } else if (ready) {
            Text(pair.earlier.observedAt.take(10), style = PlantupiaText.editorialHeading)
            PlantupiaEvidencePhoto(pair.earlier.photoUrl, "Earlier saved plant photograph",
                Modifier.fillMaxWidth().height(180.dp), framing = PlantupiaPhotoFraming.FULL_RECORD)
            earlier.forEach { region ->
                MeasurementChoice(region, first == region.id, !busy) {
                    first = region.id
                    second = null
                    resetSelection()
                }
            }
            val firstRegion = earlier.firstOrNull { it.id == first }
            Text(pair.later.observedAt.take(10), style = PlantupiaText.editorialHeading)
            PlantupiaEvidencePhoto(pair.later.photoUrl, "Later saved plant photograph",
                Modifier.fillMaxWidth().height(180.dp), framing = PlantupiaPhotoFraming.FULL_RECORD)
            if (firstRegion == null) Text("Select the earlier measured part first.", style = PlantupiaText.supporting)
            else {
                val matches = later.filter { it.label == firstRegion.label }
                if (matches.isEmpty()) Text("No calibrated region with the same label in this photograph.",
                    style = PlantupiaText.supporting)
                matches.forEach { region ->
                    MeasurementChoice(region, second == region.id, !busy) {
                        second = region.id
                        resetSelection()
                    }
                }
            }
            if (first != null && second != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(samePart, { samePart = it; result = null }, enabled = !busy)
                    Text("I marked the same physical part at both visits.", style = PlantupiaText.body,
                        modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(comparable, { comparable = it; result = null }, enabled = !busy)
                    Text("Both photos use a comparable flat, face-on plane and reference method.", style = PlantupiaText.body,
                        modifier = Modifier.weight(1f))
                }
                Button(enabled = !busy && samePart && comparable, onClick = {
                    val a = first ?: return@Button
                    val b = second ?: return@Button
                    busy = true
                    result = null
                    error = ""
                    scope.launch {
                        when (val reply = repository.compareProjectedAreas(plantId, a, b)) {
                            is ApiResult.Failure -> error = reply.message
                            is ApiResult.Success -> if (repository.isCurrentSession()) result = reply.value
                        }
                        busy = false
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (busy) "Comparing…" else "Compare projected areas")
                }
            }
            result?.let { answer ->
                if (answer.optString("status") != "comparable_by_operator_report") {
                    Text("The saved measurements are not comparable.", style = PlantupiaText.itemTitle)
                    Text(comparisonReason(answer.optString("reason")), style = PlantupiaText.supporting)
                } else {
                    val a = answer.optJSONObject("earlier")?.optDouble("projected_area_cm2") ?: Double.NaN
                    val b = answer.optJSONObject("later")?.optDouble("projected_area_cm2") ?: Double.NaN
                    val delta = answer.optDouble("difference_cm2")
                    if (a.isFinite() && b.isFinite() && a > 0 && b > 0 && delta.isFinite()) {
                        Surface(color = PlantupiaColors.softSage, shape = PlantupiaShapes.card,
                            modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Projected area difference", style = PlantupiaText.itemTitle)
                                Text(String.format(Locale.ROOT, "%+.2f cm²", delta), style = PlantupiaText.display)
                                val maximum = maxOf(a, b)
                                for ((label, value) in listOf(pair.earlier.observedAt.take(10) to a, pair.later.observedAt.take(10) to b)) {
                                    Text(label + " · " + String.format(Locale.ROOT, "%.2f cm²", value),
                                        style = PlantupiaText.supporting)
                                    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                                        drawRect(PlantupiaColors.forest.copy(alpha = .12f))
                                        drawRect(PlantupiaColors.forest,
                                            size = androidx.compose.ui.geometry.Size(size.width * (value / maximum).toFloat(), size.height))
                                    }
                                }
                                Text("Based on your reported matched capture. This difference is not a verified growth rate; measurement uncertainty has not been established.",
                                    style = PlantupiaText.supporting)
                            }
                        }
                    } else Text("Measurement response unavailable.", style = PlantupiaText.supporting)
                }
            }
        }
    }
}

@Composable
private fun MeasurementChoice(region: FieldRegion, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = PlantupiaShapes.inset,
        color = if (selected) PlantupiaColors.softSage else PlantupiaColors.paper,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) PlantupiaColors.forest else PlantupiaColors.border),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (selected) "Selected · " + region.label else region.label, style = PlantupiaText.body,
                modifier = Modifier.weight(1f))
            Text(String.format(Locale.ROOT, "%.2f cm²", region.planarAreaCm2 ?: 0.0),
                style = PlantupiaText.itemTitle)
        }
    }
}

private fun comparisonReason(reason: String) = when (reason) {
    "capture_or_part_not_confirmed" -> "Confirm the part and comparable capture."
    "different_source_photographs_required" -> "Use distinct original photographs."
    "distinct_ordered_visits_required" -> "Use two visits in chronological order."
    "different_part_labels" -> "Select matching part labels."
    "independent_scale_required" -> "Both records require an accepted independent scale check."
    else -> "The records do not support a matched measurement comparison."
}
