package com.nova.app.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Composable
internal fun PlantupiaFieldInspection(repository: PlantupiaRepository, plant: PlantupiaPlant, event: PlantupiaCareEvent) =
    key(repository, plant.id, event.id) {
        var expanded by remember { mutableStateOf(false) }
        PlantupiaActionRow(icon = com.nova.app.ui.icons.NovaIconAsset.Photo, title = "Inspect this record", detail = "Private evidence · visible observations", onClick = { expanded = true })
        if (expanded) {
            val scope = rememberCoroutineScope()
            var record by remember { mutableStateOf<JSONObject?>(null) }
            var feedback by remember { mutableStateOf("") }
            var busy by remember { mutableStateOf(false) }
            var generation by remember { mutableIntStateOf(0) }
            LaunchedEffect(generation) {
                feedback = ""
                when (val result = repository.fieldEvidence(plant.id, event.id)) {
                    is ApiResult.Failure -> {
                        record = null
                        feedback = result.message
                    }
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
            PlantupiaLivingWorkspace("FIELD NOTES", plant.name, "Recorded " + event.observedAt.take(10), busy, { expanded = false }) {
                if (feedback.isNotBlank()) Text(feedback, color = MaterialTheme.colorScheme.error)
                if (record == null) {
                    if (feedback.isBlank()) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    else TextButton(enabled = !busy, onClick = { generation++ }) { Text("Refresh inspection") }
                }
                record?.let { data ->
                    val quality = data.optJSONObject("quality")
                    val width = quality?.optInt("width") ?: 0
                    val height = quality?.optInt("height") ?: 0
                    if (event.photoUrl.isNotBlank()) {
                        if (width > 0 && height > 0) {
                            PlantupiaRegionStudy(
                                url = event.photoUrl,
                                width = width,
                                height = height,
                                regions = fieldRegions(data.optJSONArray("regions"), width, height),
                                totalRegions = data.optInt("region_count"),
                                canTrace = plant.canRecord && plant.active,
                                canSuggest = data.optBoolean("can_suggest_region"),
                                busy = busy,
                                revision = generation,
                                suggest = { body, result -> submit("suggest-region/", body, result) },
                            ) { body -> submit("regions/", body) { generation++ } }
                            CaptureNotes(quality)
                        } else {
                            PlantupiaEvidencePhoto(event.photoUrl, "Original recorded plant photograph",
                                Modifier.fillMaxWidth().height(260.dp), framing = PlantupiaPhotoFraming.FULL_RECORD)
                            Text("Image dimensions are unavailable for tracing.", style = PlantupiaText.supporting)
                        }
                    }
                    Text("A photograph alone does not establish a diagnosis.", style = PlantupiaText.supporting)
                    val reviews = data.optJSONArray("reviews") ?: JSONArray()
                    if (reviews.length() > 0) Text("Specialist findings", style = PlantupiaText.editorialHeading)
                    for (index in 0 until reviews.length()) {
                        val review = reviews.getJSONObject(index)
                        Text(review.getString("finding").replace('_', ' '), style = PlantupiaText.itemTitle)
                        Text(review.getString("rationale"), style = PlantupiaText.supporting)
                    }
                    HorizontalDivider(color = PlantupiaColors.border)
                    Text("What do you notice?", style = PlantupiaText.editorialHeading)
                    var symptom by remember { mutableStateOf("uncertain") }
                    var substrate by remember { mutableStateOf("unknown") }
                    var advice by remember(symptom, substrate) { mutableStateOf<JSONObject?>(null) }
                    InspectionChoice("Visible observation", symptom, listOf("uncertain", "wilting", "spots", "discoloration", "damage"), !busy) { symptom = it }
                    if (symptom == "wilting")
                        InspectionChoice("Reported substrate condition", substrate, listOf("unknown", "wet", "dry"), !busy) { substrate = it }
                    TextButton(enabled = !busy && symptom != "uncertain", onClick = {
                        submit("", JSONObject().put("symptom", symptom).put("substrate_report", substrate)) { answer ->
                            advice = answer
                        }
                    }) { Text("Show inspection steps") }
                    advice?.let { InspectionEvidence(it, symptom) }
                    if (data.optBoolean("can_review")) {
                        var finding by remember { mutableStateOf("insufficient_evidence") }
                        var rationale by remember { mutableStateOf("") }
                        val submission = remember(finding, rationale, generation) { UUID.randomUUID().toString() }
                        InspectionChoice("Specialist review", finding, listOf("insufficient_evidence", "corrected_observation", "supported_observation"), !busy) { finding = it }
                        OutlinedTextField(rationale, { rationale = it.take(1000) }, enabled = !busy,
                            modifier = Modifier.fillMaxWidth(), label = { Text("Reason for this finding") })
                        TextButton(enabled = !busy && rationale.isNotBlank(), onClick = {
                            submit("reviews/", JSONObject().put("submission_id", submission).put("finding", finding).put("rationale", rationale)) {
                                rationale = ""; generation++
                            }
                        }) { Text("Save review to this record") }
                    }
                    TextButton(enabled = !busy, onClick = { generation++ }) { Text("Refresh inspection") }
                }
            }
        }
    }

@Composable
internal fun InspectionChoice(label: String, selected: String, values: List<String>, enabled: Boolean, change: (String) -> Unit) {
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
private fun CaptureNotes(quality: JSONObject?) {
    val values = quality?.optJSONArray("capture_flags") ?: return
    val notes = (0 until values.length()).mapNotNull { index ->
        when (values.optString(index)) {
            "low_resolution" -> "Use the full-size original for finer detail."
            "predominantly_dark" -> "Try brighter, even light for a repeat capture."
            "predominantly_bright" -> "Change angle to reduce glare."
            "low_detail_proxy" -> "Hold the phone still and focus on the part you want to compare."
            else -> null
        }
    }
    if (notes.isNotEmpty()) {
        Text("For your next photograph", style = PlantupiaText.itemTitle)
        notes.forEach { Text(it, style = PlantupiaText.supporting) }
    }
}

@Composable
private fun InspectionEvidence(result: JSONObject, symptom: String) {
    Text("Reported · $symptom", style = PlantupiaText.itemTitle)
    val options = result.optJSONArray("inspection_options") ?: JSONArray()
    if (options.length() == 0) Text("No inspection steps for this report.", style = PlantupiaText.supporting)
    for (index in 0 until options.length()) {
        val option = options.optJSONObject(index) ?: continue
        fun known(key: String) = option.optString(key).takeUnless { it.isBlank() || it == "null" }
        known("hypothesis")?.let { Text("Possible ${it.replace('_', ' ')}", style = PlantupiaText.editorialHeading) }
        known("supporting_report")?.let { Text("Supports this possibility · reported $it substrate", style = PlantupiaText.supporting) }
        known("contradictory_report")?.let { Text("Weighs against it · reported $it substrate", style = PlantupiaText.supporting) }
        Text(option.optString("inspection"), style = PlantupiaText.body)
    }
    val care = result.optJSONArray("recent_care") ?: JSONArray()
    Text("Recorded context", style = PlantupiaText.itemTitle)
    result.optJSONObject("placement_at_observation")?.let { place ->
        Text("Room position at this visit · " + place.optString("x_m") + ", " +
            place.optString("y_m") + " m · layout " + place.optInt("revision"),
            style = PlantupiaText.supporting)
    }
    if (result.optString("species_basis") == "unknown") {
        Text("Plant type is unknown.", style = PlantupiaText.supporting)
    }
    result.optInt("guide_version").takeIf { it > 0 }?.let {
        Text("Reviewed guide available · revision $it", style = PlantupiaText.supporting)
    }
    if (care.length() == 0) {
        Text("No earlier care entry is recorded here.", style = PlantupiaText.supporting)
    }
    for (index in 0 until care.length()) {
        val record = care.optJSONObject(index) ?: continue
        val date = record.optString("observed_at").take(10)
        val note = record.optString("note")
        Text(listOf(date, note).filter(String::isNotBlank).joinToString(" · "), style = PlantupiaText.supporting)
    }
}
