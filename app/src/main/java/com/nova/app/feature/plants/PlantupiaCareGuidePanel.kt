package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaCareGuide
import com.nova.app.feature.plants.data.PlantupiaRepository

/**
 * The company-authored guide is a complete source document, not a clipped preview.
 * The detail page offers a concise entry; opening reveals every approved instruction,
 * placement note and review/source record. No client-generated plant advice.
 */
@Composable
internal fun PlantupiaCareGuidePanel(plantId: String, repository: PlantupiaRepository) {
    key(plantId, repository) {
        var open by remember { mutableStateOf(false) }
        var provenanceOpen by remember { mutableStateOf(false) }
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
                is ApiResult.Failure -> if (repository.isCurrentSession()) {
                    error = "Care guidance couldn't be loaded. Check your connection and retry."
                }
            }
            loading = false
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                onClick = { open = !open },
                modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp),
                color = PlantupiaColors.softSage,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PlantupiaColors.border),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Care guide", style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            if (open) "Full guide and source information"
                            else "Read this plant's available care instructions",
                            style = PlantupiaText.supporting,
                        )
                    }
                    Text(if (open) "↑" else "→", style = PlantupiaText.body)
                }
            }
            if (open) {
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Loading care guidance…", style = PlantupiaText.supporting)
                } else if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) { Text("Retry care guide") }
                } else guide?.let { value ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (value.version == null) {
                            Text("Care guidance not yet verified", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (value.verifiedIdentity) "This identity has no published, reviewed guide yet."
                                else "Species not confirmed. We can't give species-specific instructions yet.",
                            )
                            Text("You can record what you notice and choose your own check-in day.")
                        } else {
                            Text(value.species, style = MaterialTheme.typography.titleMedium)
                            Text("Reviewed care instructions", style = PlantupiaText.sectionLabel)
                            // Never limit lines or replace the company's published guide with a summary.
                            Text(value.instructions)
                            if (value.contextNotes.isNotBlank()) {
                                Text("Context for this plant", style = PlantupiaText.sectionLabel)
                                Text(value.contextNotes)
                            }
                            OutlinedButton(
                                onClick = { provenanceOpen = !provenanceOpen },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) { Text(if (provenanceOpen) "Hide source and review details" else "Source and review details") }
                            if (provenanceOpen) {
                                Text("Guide version ${value.version} · reviewed by ${value.reviewer} · ${value.reviewedAt.take(10)}",
                                    style = PlantupiaText.supporting)
                                if (value.sourceUrl.isNotBlank()) {
                                    TextButton(onClick = {
                                        try { uriHandler.openUri(value.sourceUrl) }
                                        catch (_: Exception) { error = "The source link couldn't open on this device." }
                                    }) { Text("Source: ${value.sourceReference}") }
                                } else if (value.sourceReference.isNotBlank()) {
                                    Text(value.sourceReference, style = PlantupiaText.supporting)
                                }
                            }
                            Text(
                                "Reading guidance does not record care. Record only what you observed or did.",
                                style = PlantupiaText.supporting,
                            )
                        }
                        if (value.placement.isNotBlank()) Text("Recorded placement: ${value.placement}",
                            style = PlantupiaText.supporting)
                    }
                }
            }
        }
    }
}
