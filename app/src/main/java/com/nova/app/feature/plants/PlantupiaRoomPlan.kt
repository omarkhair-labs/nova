package com.nova.app.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.*
import kotlinx.coroutines.launch

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
        var editing by remember { mutableStateOf(false) }
        var spatialStudy by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        LaunchedEffect(generation, revision) {
            room = null
            message = ""
            when (val response = repository.room(space.id, revision)) {
                is ApiResult.Failure -> message = response.message
                is ApiResult.Success -> if (repository.isCurrentSession()) room = response.value
            }
        }
        PlantupiaLivingWorkspace("LIVING SPACE", space.name, "A map of recorded placements", saving, {
            if (spatialStudy) spatialStudy = false else expanded = false
        }) {
            if (spatialStudy && repository.isCurrentSession()) {
                PlantupiaSpatialViewer(space.name) { spatialStudy = false }
                return@PlantupiaLivingWorkspace
            }
            TextButton(enabled = !saving, onClick = { editing = false; revision = null; generation++ }) {
                Text(if (revision == null) "Refresh floor plan" else "Return to current layout")
            }
            if (message.isNotBlank()) Text(message)
            val current = room
            if (current != null) {
                if (current.revisions.size > 1) LayoutRevisionPicker(current, saving) {
                    editing = false
                    revision = it
                }
                if (!current.historical) TextButton(enabled = !saving, onClick = { spatialStudy = true }) { Text("Explore a 3D reconstruction") }
                val width = current.width
                val length = current.length
                if (width != null && length != null) {
                    Text("Reported room · $width × $length m · revision ${current.revision}")
                    if (current.historical) Text("Earlier layout · care history as of that revision",
                        style = PlantupiaText.supporting)
                    var focusedId by remember(current.revision) { mutableStateOf<String?>(null) }
                    var daylight by remember(current.revision) { mutableStateOf(false) }
                    if (!current.historical) TextButton(enabled = !saving && current.windows.isNotEmpty(), onClick = { daylight = !daylight }) {
                        Text(if (daylight) "Return to placements" else "Explore direct sunlight")
                    }
                    if (daylight) {
                        PlantupiaDaylightStudy(space.id, current, width, length, repository)
                    } else {
                        PlantupiaRoomDrawing(width, length, current.plants, current.windows,
                            selectedPlant = focusedId, onSelectPlant = { focusedId = it })
                        Text("Tap a plant marker, or choose from the list below. Dimensions are reported, not camera measurements.", style = PlantupiaText.supporting)
                        current.plants.firstOrNull { it.id == focusedId }?.let { point ->
                            plants.find { it.id == point.id }?.let { plant ->
                                Text(plant.name, style = PlantupiaText.editorialHeading)
                                Button(onClick = { expanded = false; openPlant(plant) }, enabled = !saving) { Text("Open plant") }
                            }
                        }
                        HorizontalDivider(color = PlantupiaColors.border)
                        Text("In this room", style = PlantupiaText.itemTitle)
                        current.plants.forEach { point ->
                            plants.find { it.id == point.id }?.let { plant ->
                                TextButton(enabled = !saving, onClick = { expanded = false; openPlant(plant) }) {
                                    Text("${plant.name} · ${point.x}, ${point.y} m" +
                                        if (point.latest.isEmpty()) "" else " · last record ${point.latest.take(10)}")
                                }
                            }
                        }
                    }
                } else Text("This space has no recorded room dimensions.")
                if (space.canAddPlant && !current.historical) {
                    TextButton(enabled = !saving, onClick = { editing = !editing }) {
                        Text(if (editing) "Cancel edit" else "Record dimensions or placement")
                    }
                    if (editing) RoomEditor(current, plants, saving) { w, l, positions, openings ->
                        saving = true
                        scope.launch {
                            when (val result = repository.saveRoom(space.id, current.submission(w, l, positions, openings))) {
                                is ApiResult.Failure -> message = result.message
                                is ApiResult.Success -> { editing = false; generation++ }
                            }
                            saving = false
                        }
                    }
                }
            } else if (message.isEmpty()) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
    room: PlantupiaRoom, plants: List<PlantupiaPlant>, saving: Boolean,
    save: (Double, Double, List<RoomPlant>, List<RoomWindow>) -> Unit,
) {
    var width by remember(room) { mutableStateOf(room.width?.toString().orEmpty()) }
    var length by remember(room) { mutableStateOf(room.length?.toString().orEmpty()) }
    var selected by remember(room) { mutableStateOf<PlantupiaPlant?>(null) }
    var addWindow by remember(room) { mutableStateOf(false) }
    var wall by remember(room) { mutableStateOf("Top") }
    var windowOffset by remember(room) { mutableStateOf("") }
    var windowName by remember(room) { mutableStateOf("") }
    var choose by remember { mutableStateOf(false) }
    val prior = room.plants.find { it.id == selected?.id }
    var x by remember(selected) { mutableStateOf(prior?.x?.toString().orEmpty()) }
    var y by remember(selected) { mutableStateOf(prior?.y?.toString().orEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Use metres from the same marked corner. Follow the same two walls on every visit.")
        OutlinedTextField(width, { width = it }, label = { Text("Width (m)") }, singleLine = true)
        OutlinedTextField(length, { length = it }, label = { Text("Length (m)") }, singleLine = true)
        Box {
            TextButton(onClick = { choose = true }) { Text(selected?.name ?: "Choose a plant to position") }
            DropdownMenu(expanded = choose, onDismissRequest = { choose = false }) {
                plants.forEach { plant ->
                    DropdownMenuItem(text = { Text(plant.name) }, onClick = { selected = plant; choose = false })
                }
            }
        }
        val id = selected?.id
        if (id != null) {
            OutlinedTextField(x, { x = it }, label = { Text("Along width (m)") }, singleLine = true)
            OutlinedTextField(y, { y = it }, label = { Text("Along length (m)") }, singleLine = true)
        }
        val w = width.toDoubleOrNull()
        val l = length.toDoubleOrNull()
        val px = x.toDoubleOrNull()
        val py = y.toDoubleOrNull()
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
        val valid = windowValid && w != null && l != null && w > 0 && l > 0 && w <= 1000 && l <= 1000 &&
            (id == null || (px != null && py != null && px in 0.0..w && py in 0.0..l))
        Button(enabled = valid && !saving, onClick = {
            if (w != null && l != null) {
                val points = if (id != null && px != null && py != null)
                    room.plants.filter { it.id != id } + RoomPlant(id, px, py, "") else room.plants
                val windows = if (addWindow && distance != null) {
                    val wx = when (wall) { "Left" -> 0.0; "Right" -> w; else -> distance }
                    val wy = when (wall) { "Top" -> 0.0; "Bottom" -> l; else -> distance }
                    room.windows + RoomWindow(windowName.trim(), wx, wy)
                } else room.windows
                save(w, l, points, windows)
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
