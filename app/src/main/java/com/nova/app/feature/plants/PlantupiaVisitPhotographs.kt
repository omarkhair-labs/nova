package com.nova.app.feature.plants

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaVisitPhotographs(
    repository: PlantupiaRepository, plant: PlantupiaPlant, event: PlantupiaCareEvent, views: JSONArray,
    busy: Boolean, onBusy: (Boolean) -> Unit, onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var studio by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf<Uri?>(null) }
    var part by remember { mutableStateOf("whole") }
    var selection by remember { mutableStateOf<Long?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var bytes by remember(selection) { mutableStateOf<ByteArray?>(null) }
    var readError by remember(selection) { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val submission = remember(draft, part) { UUID.randomUUID().toString() }
    DisposableEffect(draft) {
        val owned = draft?.let { ownedCaptureFile(context.cacheDir, it) }
        onDispose { owned?.delete() }
    }
    LaunchedEffect(selection, retry) {
        val id = selection ?: return@LaunchedEffect
        readError = null
        val response = repository.visitPhoto(plant.id, event.id, id)
        if (!repository.isCurrentSession()) return@LaunchedEffect
        when (response) {
            is ApiResult.Success -> bytes = response.value
            is ApiResult.Failure -> readError = response.message
        }
    }
    if (views.length() > 0) {
        Text("Views from this visit", style = PlantupiaText.itemTitle)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (index in 0 until views.length()) {
                val row = views.getJSONObject(index)
                val id = row.getLong("id")
                FilterChip(selected = selection == id, onClick = { selection = if (selection == id) null else id },
                    label = { Text("${index + 1} · " + row.getString("part")) })
            }
        }
        if (selection != null) {
            val image = bytes
            when {
                image != null -> AsyncImage(model = image, contentDescription = "Selected photograph from this visit",
                    modifier = Modifier.fillMaxWidth().height(260.dp), contentScale = ContentScale.Fit)
                readError != null -> {
                    Text(readError.orEmpty(), style = PlantupiaText.supporting)
                    TextButton(onClick = { retry++ }) { Text("Retry photograph") }
                }
                else -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (plant.canRecord && plant.active && views.length() < 6) {
        val photo = draft
        if (photo == null) {
            TextButton(enabled = !busy, onClick = { studio = true }) { Text("Add a view to this visit") }
        } else {
            AsyncImage(model = photo, contentDescription = "Photograph to attach", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(180.dp))
            Button(enabled = !busy, onClick = {
                onBusy(true)
                saveError = null
                scope.launch {
                    try {
                        val result = repository.addVisitPhoto(plant.id, event.id, submission, part, photo)
                        if (repository.isCurrentSession()) when (result) {
                            is ApiResult.Success -> {
                                draft = null
                                selection = result.value.getLong("id")
                                onSaved()
                            }
                            is ApiResult.Failure -> saveError = result.message
                        }
                    } finally { onBusy(false) }
                }
            }) { Text(if (busy) "Saving photograph…" else "Save $part view to this visit") }
            TextButton(enabled = !busy, onClick = { draft = null; saveError = null }) { Text("Discard photograph") }
            saveError?.let { Text(it, style = PlantupiaText.supporting) }
        }
    }
    if (studio) PlantupiaCaptureStudio(plantName = plant.name, onClose = { studio = false },
        reference = event.takeIf { it.photoUrl.isNotBlank() }, onVisitPart = { part = it },
        onCapture = { uri ->
            if (repository.isCurrentSession()) {
                draft = uri
                studio = false
                true
            } else false
        })
}
