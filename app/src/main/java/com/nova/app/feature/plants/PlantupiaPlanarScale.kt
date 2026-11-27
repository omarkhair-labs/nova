package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

internal data class PlanarScaleDraft(
    val points: List<IntOffset> = emptyList(),
    val referenceCm: String = "",
    val checkCm: String = "",
    val faceOn: Boolean = false,
) {
    fun payload(): JSONObject? {
        val first = referenceCm.replace(',', '.').toDoubleOrNull()
        val second = checkCm.replace(',', '.').toDoubleOrNull()
        if (!faceOn || points.size != 4 || points.distinct().size != 4 ||
            first == null || second == null || !first.isFinite() || !second.isFinite() ||
            first !in .001..10000.0 || second !in .001..10000.0) return null
        return JSONObject().put("face_on_coplanar", true)
            .put("points", JSONArray().apply {
                points.forEach { put(JSONArray().put(it.x).put(it.y)) }
            }).put("reference_cm", first).put("check_cm", second)
    }
}

@Composable
internal fun PlanarScaleControls(
    active: Boolean, draft: PlanarScaleDraft, enabled: Boolean,
    onToggle: (Boolean) -> Unit, onChange: (PlanarScaleDraft) -> Unit, onLocate: () -> Unit,
) {
    if (!active) {
        OutlinedButton(enabled = enabled, onClick = { onToggle(true) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Calibrate projected area")
        }
        return
    }
    Surface(color = PlantupiaColors.softSage, shape = PlantupiaShapes.inset, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reference scale", style = PlantupiaText.editorialHeading)
            Text("Use two independently measured, nearly perpendicular lengths beside the same flat part. Photograph them face-on in its plane. Do not use a curved or tilted leaf.",
                style = PlantupiaText.body)
            Text(when (draft.points.size) {
                0 -> "Mark the first end of the gold reference."
                1 -> "Mark its second end."
                2 -> "Mark the first end of the blue check length."
                3 -> "Mark its second end."
                else -> "Four endpoints marked. Enter their measured lengths."
            }, style = PlantupiaText.itemTitle)
            TextButton(enabled = enabled, onClick = onLocate) { Text("Return to the photograph") }
            TextButton(enabled = enabled && draft.points.isNotEmpty(), onClick = {
                onChange(draft.copy(points = draft.points.dropLast(1)))
                onLocate()
            }) { Text("Undo last reference mark") }
            OutlinedTextField(draft.referenceCm, { onChange(draft.copy(referenceCm = it.take(16))) },
                label = { Text("Gold length (cm)") }, singleLine = true, enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.checkCm, { onChange(draft.copy(checkCm = it.take(16))) },
                label = { Text("Blue check length (cm)") }, singleLine = true, enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = draft.faceOn, enabled = enabled,
                    onCheckedChange = { onChange(draft.copy(faceOn = it)) })
                Text("Both references and the measured part are flat, in the same plane, and viewed face-on.",
                    style = PlantupiaText.body, modifier = Modifier.weight(1f))
            }
            Text("The independent length must agree within 3%. Otherwise only pixel area is retained. This check cannot prove real-world accuracy.",
                style = PlantupiaText.supporting)
            TextButton(enabled = enabled, onClick = { onToggle(false) }) { Text("Keep pixel measurements only") }
        }
    }
}

internal fun scaleRejectionText(reason: String) = when (reason) {
    "two_distinct_markers_required" -> "two different square markers were not visible"
    "marker_detector_unavailable" -> "marker detection unavailable"
    "marker_too_small_or_oblique" -> "bring the camera closer and face the markers"
    "marker_overlaps_region", "place_markers_on_opposite_sides" -> "place a marker on each side of the outline"
    "plane_not_confirmed" -> "flat viewing plane not confirmed"
    "reference_too_short" -> "reference lengths are too short in the image"
    "references_not_near_perpendicular" -> "reference directions are not sufficiently distinct"
    "references_too_far_from_region" -> "references are too far from the region"
    "independent_scale_check_failed" -> "the independent length did not agree"
    else -> "reference could not be verified"
}
