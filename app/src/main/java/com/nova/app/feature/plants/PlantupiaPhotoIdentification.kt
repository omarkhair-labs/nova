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
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
internal fun PlantupiaPhotoIdentification(repository: PlantupiaRepository, whole: Uri,
                                         onChoose: (String, String) -> Unit) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<Uri?>(null) }
    var open by remember { mutableStateOf(false) }
    DisposableEffect(detail) {
        val owned = detail?.takeIf { it != whole }?.let { ownedCaptureFile(context.cacheDir, it) }
        onDispose { owned?.delete() }
    }
    val second = detail
    TextButton(onClick = { open = true }) {
        Text(if (second == null) "Identify with a second view" else "Change detail photo")
    }
    if (second != null) {
        AsyncImage(model = second, contentDescription = "Second view for identification",
            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(120.dp))
        key(second) {
            PlantupiaSpeciesReview(analyse = { repository.identifyPlantPhotos(whole, second) },
                current = repository::isCurrentSession, onChoose = onChoose)
        }
    }
    if (open) PlantupiaCaptureStudio(plantName = "Leaf or flower on the same plant",
        onClose = { open = false }, onCapture = { uri ->
            if (repository.isCurrentSession() && uri != whole) {
                detail = uri
                open = false
                true
            } else false
        })
}

@Composable
internal fun PlantupiaSpeciesReview(analyse: suspend () -> ApiResult<JSONObject>, current: () -> Boolean,
                                   onChoose: ((String, String) -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var match by remember { mutableStateOf<Pair<String, String>?>(null) }
    var related by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    TextButton(enabled = !running, onClick = {
        running = true
        match = null
        related = emptyList()
        message = ""
        scope.launch {
            try {
                val result = analyse()
                if (current()) when (result) {
                    is ApiResult.Failure -> message = result.message
                    is ApiResult.Success -> {
                        val row = result.value.optJSONObject("candidate")
                        val name = row?.optString("scientific_name").orEmpty()
                        val key = if (row != null && !row.isNull("key")) row.getString("key") else ""
                        val genus = if (result.value.isNull("genus")) "" else result.value.optString("genus")
                        when {
                            name.isNotBlank() -> match = key to name
                            genus.isNotBlank() -> {
                                message = "Possible genus · $genus. The species is uncertain."
                                val guides = result.value.optJSONArray("genus_guides")
                                related = (0 until (guides?.length() ?: 0)).mapNotNull { i ->
                                    guides?.optJSONObject(i)?.let { it.getString("key") to it.getString("scientific_name") }
                                }
                            }
                            else -> message = "No confident match across these views. You can leave the type unknown."
                        }
                    }
                }
            } finally { running = false }
        }
    }) { Text(if (running) "Comparing both views…" else "Compare two private photos") }
    if (message.isNotBlank()) Text(message, style = PlantupiaText.supporting)
    match?.let { (key, name) ->
        Text("Suggested plant type · $name", style = PlantupiaText.itemTitle)
        if (onChoose != null && key.isNotBlank()) TextButton(onClick = { onChoose(key, name) }) { Text("Use as my selection") }
        else if (onChoose != null) Text("No published care guide for this match.", style = PlantupiaText.supporting)
    }
    if (related.isNotEmpty() && onChoose != null) {
        Text("Compare published types in this genus", style = PlantupiaText.supporting)
        related.forEach { (key, name) ->
            TextButton(onClick = { onChoose(key, name) }) { Text("Select $name") }
        }
    }
}
