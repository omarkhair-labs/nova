package com.nova.app.feature.plants

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
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
        var expandedTopic by remember { mutableStateOf<String?>(null) }
        var narrativeOpen by remember { mutableStateOf(false) }
        var legacyOpen by remember { mutableStateOf(false) }
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
                modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp),
                color = Color(0xFFE6EDE3),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, PlantupiaColors.border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = RoundedCornerShape(17.dp), color = Color(0xFFFFF1CC)) {
                        CareChapterIllustration(
                            "light", Color(0xFF966019),
                            Modifier.padding(10.dp).size(51.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("THE FIELD GUIDE", style = PlantupiaText.sectionLabel)
                        Text(
                            "Care guide",
                            style = PlantupiaText.editorialHeading,
                        )
                        Text(
                            "Water, light and the details that matter.",
                            style = PlantupiaText.supporting,
                        )
                    }
                    Text(if (open) "−" else "+", style = PlantupiaText.editorialHeading)
                }
            }
            AnimatedVisibility(
                visible = open,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Loading care guidance…", style = PlantupiaText.supporting)
                } else if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }) { Text("Retry care guide") }
                } else guide?.let { value ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (value.version == null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFFFF0D3),
                                shape = RoundedCornerShape(22.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(17.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CareChapterIllustration(
                                        "inspection", Color(0xFF966019), Modifier.size(64.dp),
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text("THE GUIDE IS GROWING", style = PlantupiaText.sectionLabel)
                                        Text("No reviewed guide yet", style = PlantupiaText.editorialHeading)
                                        Text(
                                            if (value.verifiedIdentity) "This type has no published, reviewed guide yet."
                                            else if (value.identityBasis == "user_selected")
                                                "No reviewed guide is published for the type you selected yet."
                                            else "The plant type is not confirmed yet.",
                                            style = PlantupiaText.body,
                                        )
                                    }
                                }
                            }
                            Text(
                                "You can record what you notice and choose a check-in date.",
                                style = PlantupiaText.supporting,
                            )
                        } else {
                            Text(value.species, style = MaterialTheme.typography.titleMedium)
                            if (value.identityBasis == "user_selected") {
                                Text(
                                    "General care for the type you selected. Plantupia has not verified the identity of your individual plant.",
                                    style = PlantupiaText.supporting,
                                )
                            }
                            val pilotPending = value.contextNotes.contains(
                                "not yet Plantupia-approved", ignoreCase = true,
                            )
                            Text(
                                when {
                                    pilotPending -> "PILOT REFERENCE · BOTANICAL APPROVAL PENDING"
                                    value.topics.isEmpty() -> "SINGLE REFERENCE NOTE"
                                    value.verifiedIdentity -> "PUBLISHED CARE CHAPTERS"
                                    else -> "PUBLISHED GENERAL CARE CHAPTERS"
                                },
                                style = PlantupiaText.sectionLabel,
                            )
                            if (value.topics.isEmpty()) {
                                // Legacy approved versions receive the new visual reading experience,
                                // but their original content stays complete and unchanged.
                                Surface(
                                    color = Color(0xFFE8F0E4),
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(17.dp),
                                        verticalArrangement = Arrangement.spacedBy(15.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(13.dp),
                                        ) {
                                            CareChapterIllustration(
                                                "repotting", PlantupiaColors.forest,
                                                Modifier.size(56.dp),
                                            )
                                            Column {
                                                Text("CARE REFERENCE", style = PlantupiaText.sectionLabel)
                                                Text("Care notes", style = PlantupiaText.editorialHeading)
                                            }
                                        }
                                        Text(
                                            value.instructions,
                                            style = PlantupiaText.body,
                                            maxLines = if (legacyOpen) Int.MAX_VALUE else 4,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        TextButton(
                                            onClick = { legacyOpen = !legacyOpen },
                                            modifier = Modifier.heightIn(min = 48.dp),
                                        ) {
                                            Text(
                                                if (legacyOpen) "Show less" else "Read complete care notes",
                                                color = PlantupiaColors.forest,
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "This reference is one source note. Individual illustrated chapters " +
                                        "will appear when the company publishes their reviewed content.",
                                    style = PlantupiaText.supporting,
                                )
                            } else {
                                Text(
                                    "Explore care by topic. These are reviewed instructions, not automatic reminders.",
                                    style = PlantupiaText.supporting,
                                )
                                PlantupiaCareChapterGallery(
                                    topics = value.topics,
                                    selectedTopic = expandedTopic,
                                    onSelect = { topic -> expandedTopic = if (expandedTopic == topic) null else topic },
                                    onOpenSource = { source ->
                                        try { uriHandler.openUri(source) }
                                        catch (_: Exception) { error = "The topic source couldn't open on this device." }
                                    },
                                )
                                if (value.legacyInstructions.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { narrativeOpen = !narrativeOpen },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                    ) {
                                        Text(if (narrativeOpen) "Hide full reviewed notes" else "Read full reviewed notes")
                                    }
                                    if (narrativeOpen) Text(value.legacyInstructions)
                                }
                            }
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
