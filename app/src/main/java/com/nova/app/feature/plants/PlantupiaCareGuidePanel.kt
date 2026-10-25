package com.nova.app.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaCareGuide
import com.nova.app.feature.plants.data.PlantupiaRepository

@Composable
internal fun PlantupiaCareGuidePanel(plantId: String, repository: PlantupiaRepository) {
    key(plantId, repository) {
        var open by remember { mutableStateOf(false) }
        var loading by remember { mutableStateOf(false) }
        var guide by remember { mutableStateOf<PlantupiaCareGuide?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var retry by remember { mutableIntStateOf(0) }
        val uriHandler = LocalUriHandler.current
        LaunchedEffect(open, retry) {
            if (!open) return@LaunchedEffect
            loading = true
            error = null
            when (val result = repository.careGuide(plantId)) {
                is ApiResult.Success -> if (repository.isCurrentSession()) guide = result.value
                is ApiResult.Failure -> if (repository.isCurrentSession()) error = "Care guidance couldn't be loaded. Check your connection and retry."
            }
            loading = false
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { open = !open }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (open) "Close care guide" else "Understand this plant · Care guide")
            }
            if (open) {
                if (loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Loading care guidance…") }
                else if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) { Text("Retry care guide") }
                } else guide?.let { value ->
                    if (value.version == null) {
                        Text("Care guidance not yet verified", style = MaterialTheme.typography.titleMedium)
                        Text(if (value.verifiedIdentity) "This identity has no published, reviewed guide yet." else "Species not confirmed. We can't give species-specific instructions yet.")
                        Text("You can record what you notice and choose your own check-in day in the care form below.")
                    } else {
                        Text(value.species, style = MaterialTheme.typography.titleMedium)
                        Text("Reviewed guidance · version ${value.version}")
                        Text(value.instructions)
                        if (value.contextNotes.isNotBlank()) Text(value.contextNotes)
                        Text("Reviewed by ${value.reviewer} · ${value.reviewedAt.take(10)}", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            try { uriHandler.openUri(value.sourceUrl) }
                            catch (_: Exception) { error = "The source link couldn't open on this device." }
                        }) { Text("Source: ${value.sourceReference}") }
                        Text("Guidance is separate from your observations. Reading this guide does not record care.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (value.placement.isNotBlank()) Text("Recorded placement: ${value.placement}")
                }
            }
        }
    }
}
