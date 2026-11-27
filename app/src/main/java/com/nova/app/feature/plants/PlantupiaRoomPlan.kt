package com.nova.app.feature.plants

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.*
import kotlinx.coroutines.launch

private enum class PlaceTask { EXPLORE, EDIT, LIGHT, SOURCE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaRoomPlan(
    space: PlantupiaSpace, plants: List<PlantupiaPlant>, repository: PlantupiaRepository,
    openPlant: (PlantupiaPlant) -> Unit,
) = key(space.id, repository) {
    var expanded by remember { mutableStateOf(false) }
    PlantupiaActionRow(icon = com.nova.app.ui.icons.NovaIconAsset.Home, title = "Explore this place", detail = "Recorded room and living collection", onClick = { expanded = true })
    if (expanded) {
        var room by remember { mutableStateOf<PlantupiaRoom?>(null) }
        var generation by remember { mutableIntStateOf(0) }
        var revision by remember { mutableStateOf<Int?>(null) }
        var message by remember { mutableStateOf("") }
        var saving by remember { mutableStateOf(false) }
        var task by remember { mutableStateOf(PlaceTask.EXPLORE) }
        var spatialPlan by remember { mutableStateOf(false) }
        var focusedId by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        fun returnToPlan() {
            task = PlaceTask.EXPLORE
        }
        LaunchedEffect(generation, revision) {
            room = null
            message = ""
            when (val response = repository.room(space.id, revision)) {
                is ApiResult.Failure -> message = response.message
                is ApiResult.Success -> if (repository.isCurrentSession()) {
                    room = response.value
                    if (response.value.plants.none { it.id == focusedId }) focusedId = null
                }
            }
        }
        PlantupiaLivingWorkspace(
            "LIVING SPACE", space.name, "", saving,
            close = {
                if (task != PlaceTask.EXPLORE) returnToPlan() else expanded = false
            },
            navigationKey = listOf(revision, generation, task),
        ) {
            if (task == PlaceTask.SOURCE && repository.isCurrentSession()) {
                val layout = room
                if (layout != null && layout.revision > 0) {
                    key(layout.revision) {
                        PlantupiaRoomPhotoScene(space, layout, plants, focusedId, repository)
                    }
                }
                PlantupiaSpatialViewer(space.name) { returnToPlan() }
                return@PlantupiaLivingWorkspace
            }
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
            val current = room
            if (current == null) {
                if (message.isEmpty()) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                else TextButton(enabled = !saving, onClick = { generation++ }) { Text("Refresh floor plan") }
                return@PlantupiaLivingWorkspace
            }
            if (task == PlaceTask.EDIT && space.canAddPlant && !current.historical) {
                Text("Record this room", style = PlantupiaText.editorialHeading)
                TextButton(enabled = !saving, onClick = { returnToPlan() }) { Text("Cancel edit") }
                RoomEditor(current, plants, focusedId, saving, repository::isCurrentSession) { w, l, h, positions, openings, scan, source ->
                    saving = true
                    scope.launch {
                        when (val result = repository.saveRoom(space.id, current.submission(w, l, positions, openings, h, scan, source))) {
                            is ApiResult.Failure -> message = result.message
                            is ApiResult.Success -> { returnToPlan(); generation++ }
                        }
                        saving = false
                    }
                }
                return@PlantupiaLivingWorkspace
            }
            val width = current.width
            val length = current.length
            if (width != null && length != null) {
                if (task == PlaceTask.LIGHT) {
                    TextButton(enabled = !saving, onClick = { returnToPlan() }) { Text("Return to placements") }
                    PlantupiaDaylightStudy(space.id, current, width, length, repository,
                        initialPlacement = current.plants.firstOrNull { it.id == focusedId },
                        plantName = plants.firstOrNull { it.id == focusedId }?.name)
                    return@PlantupiaLivingWorkspace
                }
                // The saved geometry is the first object, not a destination buried under a menu.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Reported room · ${width} × ${length} m · revision ${current.revision}",
                        style = PlantupiaText.itemTitle)
                    val stamp = current.revisions.firstOrNull { it.number == current.revision }
                    if (current.historical) Text(
                        "Earlier layout · care history as of that revision" +
                            (stamp?.let { " · " + it.recordedAt.take(10) } ?: ""),
                        style = PlantupiaText.supporting)
                    else Text("Current layout" + (stamp?.let { " · " + it.recordedAt.take(10) } ?: ""),
                        style = PlantupiaText.supporting)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !spatialPlan, onClick = { spatialPlan = false }, label = { Text("Floor plan") })
                    FilterChip(selected = spatialPlan, onClick = { spatialPlan = true }, label = { Text("Perspective") })
                    if (current.revisions.size > 1) LayoutRevisionPicker(current, saving) { revision = it }
                }
                if (spatialPlan && current.survey.isNotEmpty()) {
                    var displayScan by remember(current.revision) { mutableStateOf(false) }
                    TextButton(onClick = { displayScan = !displayScan }) {
                        Text(if (displayScan) "Show plant placements" else "View recorded survey")
                    }
                    if (displayScan) {
                        val points = remember(current.survey) { surveyPreviewPoints(current.survey.toFloatArray(), current.surveySource == "arcore_depth_faces") }
                        if (points.isNotEmpty()) PointCloudStage(points, "Recorded room survey", current.surveySource == "arcore_depth_faces")
                        val surveyLabel = when (current.surveySource) {
                            "arcore_depth_faces" -> "Measured surfaces"
                            "arcore_raw_depth" -> "Depth survey"
                            else -> "Tracked features"
                        }
                        Text("$surveyLabel · layout ${current.revision}", style = PlantupiaText.supporting)
                    } else PlantupiaRecordedSpatialPlan(current, width, length, focusedId) { focusedId = it }
                } else if (spatialPlan) {
                    PlantupiaRecordedSpatialPlan(current, width, length, focusedId) { focusedId = it }
                } else {
                    PlantupiaRoomDrawing(width, length, current.plants, current.windows,
                        selectedPlant = focusedId, onSelectPlant = { focusedId = it })
                    Text("● ${current.plants.size} plant placements   — ${current.windows.size} window centres",
                        style = PlantupiaText.supporting)
                }
                val selectedPoint = current.plants.firstOrNull { it.id == focusedId }
                val selectedPlant = selectedPoint?.let { point -> plants.find { it.id == point.id } }
                if (current.plants.isNotEmpty()) {
                    var pickerOpen by remember(current.revision) { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { pickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedPlant?.let { "Selected plant · " + it.name } ?: "Choose a plant in this room")
                        }
                        DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                            current.plants.forEach { point ->
                                plants.firstOrNull { it.id == point.id }?.let { plant ->
                                    DropdownMenuItem(text = { Text("${plant.name} · ${point.x}, ${point.y} m") }, onClick = {
                                        focusedId = point.id
                                        pickerOpen = false
                                    })
                                }
                            }
                        }
                    }
                } else Text("No recorded plant placements yet.", style = PlantupiaText.supporting)
                if (selectedPoint != null && selectedPlant != null) {
                    PlantupiaActionRow(
                        icon = com.nova.app.ui.icons.NovaIconAsset.Home,
                        title = "Open " + selectedPlant.name,
                        detail = "Recorded position · ${selectedPoint.x}, ${selectedPoint.y} m" +
                            selectedPoint.latest.takeIf { it.isNotBlank() }?.let { " · Last record " + it.take(10) }.orEmpty(),
                        accent = true, enabled = !saving,
                        onClick = { expanded = false; openPlant(selectedPlant) },
                    )
                }
                if (current.windows.isNotEmpty()) {
                    PlantupiaActionRow(
                        icon = com.nova.app.ui.icons.NovaIconAsset.Home,
                        title = selectedPlant?.let { "Study sunlight at " + it.name } ?: "Explore direct sunlight",
                        detail = "Study a date and a point on this plan",
                        enabled = !saving, onClick = { task = PlaceTask.LIGHT },
                    )
                } else if (!current.historical) {
                    Text("Record a window centre to study direct sunlight.", style = PlantupiaText.supporting)
                }
            } else {
                Text("This space has no recorded room dimensions.", style = PlantupiaText.editorialHeading)
            }
            HorizontalDivider(color = PlantupiaColors.border)
            Text("Layout", style = PlantupiaText.itemTitle)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !saving, onClick = { returnToPlan(); revision = null; generation++ }) {
                    Text(if (revision == null) "Refresh floor plan" else "Return to current layout")
                }
            }
            if (space.canAddPlant && !current.historical) {
                OutlinedButton(enabled = !saving, onClick = { task = PlaceTask.EDIT }, modifier = Modifier.fillMaxWidth()) {
                    Text("Record dimensions or placement")
                }
            }
            if (!current.historical && (spatialPlan || width == null || length == null)) {
                TextButton(enabled = !saving, onClick = { task = PlaceTask.SOURCE }) { Text("Explore photographic reconstruction") }
            }
        }
    }
}

@Composable
private fun LayoutRevisionPicker(room: PlantupiaRoom, saving: Boolean, onSelect: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(enabled = !saving, onClick = { open = true }) { Text("Browse layout history") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            room.revisions.forEachIndexed { index, revision ->
                DropdownMenuItem(
                    text = {
                        val lead = if (index == 0) "Current" else "Revision ${revision.number}"
                        Text("$lead · ${revision.recordedAt.take(10)}")
                    },
                    onClick = {
                        open = false
                        onSelect(revision.number.takeUnless { index == 0 })
                    },
                )
            }
        }
    }
}

@Composable
private fun RoomEditor(
    room: PlantupiaRoom, plants: List<PlantupiaPlant>, initialPlantId: String?, saving: Boolean,
    sessionCurrent: () -> Boolean,
    save: (Double, Double, Double?, List<RoomPlant>, List<RoomWindow>, List<Float>, String) -> Unit,
) {
    var width by remember(room) { mutableStateOf(room.width?.toString().orEmpty()) }
    var length by remember(room) { mutableStateOf(room.length?.toString().orEmpty()) }
    var height by remember(room) { mutableStateOf(room.height?.toString().orEmpty()) }
    var selected by remember(room) { mutableStateOf(plants.firstOrNull { it.id == initialPlantId }) }
    val edits = remember(room) { mutableStateMapOf<String, Pair<String, String>>() }
    var addWindow by remember(room) { mutableStateOf(false) }
    var wall by remember(room) { mutableStateOf("Top") }
    var windowOffset by remember(room) { mutableStateOf("") }
    var windowName by remember(room) { mutableStateOf("") }
    var choose by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var arCandidate by remember(room) { mutableStateOf<Pair<Double, Double>?>(null) }
    var surveyPoints by remember(room) { mutableStateOf(emptyList<SpatialPoint>()) }
    var surveySource by remember(room) { mutableStateOf("arcore_features") }
    var rawSurvey by remember(room) { mutableStateOf(emptyList<Float>()) }
    var attachSurvey by remember(room) { mutableStateOf(false) }
    var showSurveyPoints by remember(room) { mutableStateOf(false) }
    val currentSession by rememberUpdatedState(sessionCurrent)
    val surveyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && currentSession()) {
            val w = result.data?.getDoubleExtra(PlantupiaFloorSurveyActivity.WIDTH, Double.NaN) ?: Double.NaN
            val l = result.data?.getDoubleExtra(PlantupiaFloorSurveyActivity.LENGTH, Double.NaN) ?: Double.NaN
            if (w.isFinite() && l.isFinite() && w in .3..30.0 && l in .3..30.0) {
                arCandidate = w to l
                val sample = result.data?.getFloatArrayExtra(PlantupiaFloorSurveyActivity.FEATURES)
                surveySource = result.data?.getStringExtra(PlantupiaFloorSurveyActivity.SOURCE) ?: "arcore_features"
                surveyPoints = surveyPreviewPoints(sample, surveySource == "arcore_depth_faces")
                rawSurvey = sample?.takeIf { surveyPoints.isNotEmpty() }?.toList().orEmpty()
                attachSurvey = false
                showSurveyPoints = false
            }
        }
    }
    val prior = room.plants.find { it.id == selected?.id }
    val selectedId = selected?.id
    val coordinates = selectedId?.let { edits[it] } ?: (prior?.x?.toString().orEmpty() to prior?.y?.toString().orEmpty())
    val w = width.toDoubleOrNull()?.takeIf { it > 0 && it <= 1000 }
    val l = length.toDoubleOrNull()?.takeIf { it > 0 && it <= 1000 }
    val moved = edits.mapNotNull { (id, fields) ->
        val x = fields.first.toDoubleOrNull()
        val y = fields.second.toDoubleOrNull()
        if (w != null && l != null && x != null && y != null && x in 0.0..w && y in 0.0..l)
            RoomPlant(id, x, y, "") else null
    }
    val draftPlants = room.plants.filter { it.id !in edits } + moved
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Use metres from the same marked corner. Follow the same two walls on every visit.")
        OutlinedButton(enabled = !saving, onClick = {
            surveyLauncher.launch(Intent(context, PlantupiaFloorSurveyActivity::class.java))
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Measure floor with AR camera") }
        if (rawSurvey.isNotEmpty()) {
            FilterChip(selected = attachSurvey, enabled = !saving, onClick = { attachSurvey = !attachSurvey },
                label = { Text("Save this 3D survey with the layout") })
        }
        arCandidate?.let { (measuredWidth, measuredLength) ->
            Surface(color = PlantupiaColors.softSage, shape = PlantupiaShapes.inset, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(String.format(Locale.ROOT, "%.2f × %.2f m", measuredWidth, measuredLength), style = PlantupiaText.editorialHeading)
                    Text("AR estimate. Check both lengths with a physical measure before saving.", style = PlantupiaText.supporting)
                    if (surveyPoints.isNotEmpty()) {
                        TextButton(onClick = { showSurveyPoints = !showSurveyPoints }) {
                            Text(if (showSurveyPoints) "Hide survey" else "View survey in 3D")
                        }
                        if (showSurveyPoints) {
                            Text(when (surveySource) {
                                "arcore_depth_faces" -> "${surveyPoints.size / 3} measured surface patches"
                                "arcore_raw_depth" -> "${surveyPoints.size} depth points"
                                else -> "${surveyPoints.size} tracked features"
                            }, style = PlantupiaText.supporting)
                            PointCloudStage(surveyPoints, "Measured geometry from the floor survey", surveySource == "arcore_depth_faces")
                        }
                    }
                    if (room.plants.isEmpty() && room.windows.isEmpty()) {
                        TextButton(enabled = !saving, onClick = {
                            width = String.format(Locale.ROOT, "%.3f", measuredWidth)
                            length = String.format(Locale.ROOT, "%.3f", measuredLength)
                            arCandidate = null
                        }) { Text("Use in draft") }
                    } else Text("Existing positions use the saved coordinate frame. Enter checked dimensions manually to preserve that frame.", style = PlantupiaText.supporting)
                    TextButton(onClick = { arCandidate = null; rawSurvey = emptyList(); attachSurvey = false }) { Text("Dismiss measurement") }
                }
            }
        }
        OutlinedTextField(width, { width = it }, label = { Text("Width (m)") }, singleLine = true)
        OutlinedTextField(length, { length = it }, label = { Text("Length (m)") }, singleLine = true)
        OutlinedTextField(height, { height = it.take(32) }, label = { Text("Measured height (m, optional)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !saving)
        Box {
            TextButton(onClick = { choose = true }) { Text(selected?.name ?: "Choose a plant to position") }
            DropdownMenu(expanded = choose, onDismissRequest = { choose = false }) {
                plants.forEach { plant ->
                    DropdownMenuItem(text = { Text(plant.name) }, onClick = { selected = plant; choose = false })
                }
            }
        }
        if (w != null && l != null) {
            if (selectedId != null) Text("Tap the plan to position " + selected?.name.orEmpty(),
                style = PlantupiaText.itemTitle)
            PlantupiaRoomDrawing(w, l, draftPlants, room.windows, selectedPlant = selectedId,
                onSelectPlant = { id -> selected = plants.firstOrNull { it.id == id } },
                onChoosePoint = if (selectedId == null || saving) null else { x, y ->
                    edits[selectedId] = String.format(Locale.ROOT, "%.2f", x) to String.format(Locale.ROOT, "%.2f", y)
                })
        }
        if (selectedId != null) {
            OutlinedTextField(coordinates.first, { edits[selectedId] = it to coordinates.second },
                label = { Text("Along width (m)") }, singleLine = true, enabled = !saving)
            OutlinedTextField(coordinates.second, { edits[selectedId] = coordinates.first to it },
                label = { Text("Along length (m)") }, singleLine = true, enabled = !saving)
        }
        val h = height.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        val heightValid = height.isBlank() || h != null && h.isFinite() && h > 0 && h <= 1000
        TextButton(onClick = { addWindow = !addWindow }) { Text(if (addWindow) "Cancel window" else "Add window centre") }
        if (addWindow) {
            OutlinedTextField(windowName, { windowName = it.take(128) }, label = { Text("Window name") }, singleLine = true)
            var menu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { menu = true }) { Text("Wall · $wall") }
                DropdownMenu(menu, { menu = false }) {
                    listOf("Top", "Right", "Bottom", "Left").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { wall = it; menu = false })
                    }
                }
            }
            OutlinedTextField(windowOffset, { windowOffset = it }, singleLine = true,
                label = { Text(if (wall == "Top" || wall == "Bottom") "From left (m)" else "From top (m)") })
        }
        val distance = windowOffset.toDoubleOrNull()
        val span = if (wall == "Top" || wall == "Bottom") w else l
        val windowValid = !addWindow || (distance != null && span != null && distance > 0 && distance < span &&
            windowName.isNotBlank() && room.windows.none { it.id == windowName.trim() })
        val valid = heightValid && windowValid && w != null && l != null && w > 0 && l > 0 && w <= 1000 && l <= 1000 &&
            moved.size == edits.size && draftPlants.all { it.x in 0.0..w && it.y in 0.0..l }
        Button(enabled = valid && !saving, onClick = {
            if (w != null && l != null) {
                val windows = if (addWindow && distance != null) {
                    val wx = when (wall) { "Left" -> 0.0; "Right" -> w; else -> distance }
                    val wy = when (wall) { "Top" -> 0.0; "Bottom" -> l; else -> distance }
                    room.windows + RoomWindow(windowName.trim(), wx, wy)
                } else room.windows
                save(w, l, h, draftPlants, windows, if (attachSurvey) rawSurvey else emptyList(), surveySource)
            }
        }) { Text(if (saving) "Saving…" else "Save room revision") }
    }
}

/** Pixel hit testing only; saved geometry is never changed by viewing the plan. */
internal fun nearestRoomPlant(plants: List<RoomPlant>, x: Float, y: Float, ox: Float, oy: Float, scale: Float, radius: Float): String? {
    if (!scale.isFinite() || scale <= 0f || !radius.isFinite() || radius <= 0f) return null
    return plants.map { plant ->
        val dx = x - (ox + plant.x * scale).toFloat()
        val dy = y - (oy + plant.y * scale).toFloat()
        plant.id to (dx * dx + dy * dy)
    }.filter { it.second <= radius * radius }.minByOrNull { it.second }?.first
}
