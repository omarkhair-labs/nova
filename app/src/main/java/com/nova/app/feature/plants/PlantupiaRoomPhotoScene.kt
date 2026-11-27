package com.nova.app.feature.plants

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaRoomPhotoScene(space: PlantupiaSpace, layout: PlantupiaRoom, plants: List<PlantupiaPlant>,
                                    initialPlant: String?, repository: PlantupiaRepository) {
    val plantOptions = plants.filter { it.spaceId == space.id && it.active && layout.plants.any { p -> p.id == it.id } }
    var plantId by remember { mutableStateOf(initialPlant?.takeIf { id -> plantOptions.any { it.id == id } }) }
    var chooser by remember { mutableStateOf(false) }
    val subject = plantOptions.find { it.id == plantId }?.name ?: space.name
    Text("Photographic reconstruction", style = PlantupiaText.editorialHeading)
    Box {
        OutlinedButton(onClick = { chooser = true }) { Text("$subject · Layout ${layout.revision}") }
        DropdownMenu(expanded = chooser, onDismissRequest = { chooser = false }) {
            DropdownMenuItem(text = { Text(space.name) }, onClick = { plantId = null; chooser = false })
            plantOptions.forEach { p ->
                DropdownMenuItem(text = { Text(p.name) }, onClick = { plantId = p.id; chooser = false })
            }
        }
    }
    val path = "spaces/${space.id}/room/${layout.revision}/scene/" + (plantId?.let { "?plant=$it" } ?: "")
    key(path) { PhotoSceneBody(subject, path, space.canAddPlant, !layout.historical, repository) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoSceneBody(subject: String, path: String, manager: Boolean, currentLayout: Boolean,
                           repository: PlantupiaRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val views = remember { mutableStateListOf<Uri>() }
    val owned = remember { mutableSetOf<java.io.File>() }
    DisposableEffect(path) { onDispose { owned.forEach { it.delete() } } }
    fun clearViews() {
        views.clear()
        owned.forEach { it.delete() }
        owned.clear()
    }
    var studio by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf("loading") }
    var refresh by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(JSONObject()) }
    var saving by remember { mutableStateOf(false) }
    var geometry by remember { mutableStateOf<PhotoSceneGeometry?>(null) }
    var showPoints by remember { mutableStateOf(false) }
    fun extended(param: String) = path + (if ('?' in path) "&" else "?") + param
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selection ->
        if (repository.isCurrentSession()) {
            val combined = (views + selection).distinct()
            if (combined.size <= 8) {
                views.clear()
                views.addAll(combined)
                message = ""
            } else message = "Choose up to eight photos."
        }
    }
    LaunchedEffect(path, refresh) {
        message = ""
        while (repository.isCurrentSession()) {
            when (val result = repository.demoGet(path)) {
                is ApiResult.Failure -> { message = result.message; state = "unavailable"; break }
                is ApiResult.Success -> {
                    data = result.value
                    state = data.optString("state")
                }
            }
            if (state !in setOf("queued", "processing")) break
            delay(4000)
        }
        if (state == "ready" && repository.isCurrentSession()) {
            when (val result = repository.demoGet(extended("geometry=1"))) {
                is ApiResult.Failure -> message = result.message
                is ApiResult.Success -> {
                    geometry = withContext(Dispatchers.Default) {
                        runCatching { decodePhotoScene(result.value) }.getOrNull()
                    }
                    if (geometry == null) message = "This scene could not be displayed."
                }
            }
        }
    }
    when (state) {
        "loading" -> LinearProgressIndicator(Modifier.fillMaxWidth())
        "unavailable" -> Unit
        "queued", "processing" -> {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(if (state == "queued") "Photos received · waiting for reconstruction"
                 else "Matching the same details across viewpoints…", style = PlantupiaText.body)
            Text("You can return to this layout while the photos are processed.", style = PlantupiaText.supporting)
        }
        "ready" -> {
            val scene = geometry
            if (scene != null) {
                Text("${data.optInt("registered_views")} of ${data.optInt("views")} views aligned · relative scale",
                    style = PlantupiaText.supporting)
                if (scene.surface != null) {
                    FilterChip(selected = showPoints, onClick = { showPoints = !showPoints },
                        label = { Text("Show measured points") })
                }
                if (scene.surface != null && !showPoints) PhotoSurfaceStage(scene.surface)
                else PointCloudStage(scene.points, "Reconstructed photo landmarks")
            } else if (message.isBlank()) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        "empty", "failed" -> {
            if (state == "failed") Text("These views could not be aligned. Move sideways between shots and keep the subject still.",
                style = PlantupiaText.body)
            if (manager && currentLayout && data.optBoolean("can_build")) {
                Text("Move around $subject. Take 5–8 overlapping photos in steady light. Keep the same details in adjoining frames.",
                    style = PlantupiaText.body)
                if (views.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(views, key = { _, uri -> uri.toString() }) { i, uri ->
                        Column {
                            AsyncImage(model = uri, contentDescription = "View ${i+1}", contentScale = ContentScale.Crop,
                                modifier = Modifier.size(88.dp))
                            TextButton(enabled = !saving, onClick = { views.remove(uri) }) { Text("Remove ${i+1}") }
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = !saving && views.size < 8, onClick = { studio = true }) { Text("Take view") }
                    TextButton(enabled = !saving && views.size < 8, onClick = { gallery.launch("image/*") }) { Text("Choose photos") }
                }
                Text("The selected photos are processed privately. Members of this place can view the resulting scene.",
                    style = PlantupiaText.supporting)
                Button(enabled = !saving && views.size in 5..8, onClick = {
                    saving = true
                    message = ""
                    scope.launch {
                        try {
                            when (val response = repository.buildPhotoScene(path, views.toList())) {
                                is ApiResult.Failure -> message = response.message
                                is ApiResult.Success -> { clearViews(); state = "loading"; refresh++ }
                            }
                        } finally { saving = false }
                    }
                }) { Text(if (saving) "Sending views…" else "Build scene · ${views.size}/8 photos") }
            } else if (state == "empty") Text("No photo reconstruction recorded for this layout.", style = PlantupiaText.supporting)
        }
    }
    if (message.isNotBlank()) {
        Text(message, style = PlantupiaText.supporting)
        TextButton(onClick = { refresh++ }) { Text("Refresh scene") }
    }
    if (manager && state in setOf("queued", "processing", "ready")) {
        TextButton(enabled = !saving, onClick = { confirmation = true }) {
            Text(if (state == "ready") "Remove saved scene" else "Cancel reconstruction")
        }
    }
    if (confirmation) AlertDialog(onDismissRequest = { confirmation = false },
        title = { Text("Remove this reconstruction?") },
        text = { Text("Its photos and reconstructed geometry will be removed from this layout.") },
        dismissButton = { TextButton(onClick = { confirmation = false }) { Text("Keep") } },
        confirmButton = { TextButton(onClick = {
            confirmation = false
            saving = true
            scope.launch {
                try {
                    when (val response = repository.removePhotoScene(extended("expected=" + data.getString("digest")))) {
                        is ApiResult.Failure -> message = response.message
                        is ApiResult.Success -> { geometry = null; state = "loading"; refresh++ }
                    }
                } finally { saving = false }
            }
        }) { Text("Remove") } })
    if (studio) PlantupiaCaptureStudio(subject, onClose = { studio = false }, onCapture = { uri ->
        if (repository.isCurrentSession() && views.size < 8) {
            if (uri !in views) views.add(uri)
            ownedCaptureFile(context.cacheDir, uri)?.let { owned.add(it) }
            studio = false
            true
        } else false
    })
    HorizontalDivider()
}
