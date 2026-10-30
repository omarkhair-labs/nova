package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.feature.plants.data.PlantupiaPlant
import java.time.LocalDate

/** Human-chosen check-in dates, never a watering prediction or sensor value. */
internal fun scheduledPlantCheckIns(plants: List<PlantupiaPlant>): List<PlantupiaPlant> =
    plants.filter { plant ->
        plant.active && runCatching { LocalDate.parse(plant.nextFollowUpOn) }.isSuccess
    }.sortedBy { it.nextFollowUpOn }

/** Incrementally reveal every real due record even in large client spaces. */
internal fun visibleDueCheckInCount(total: Int, requested: Int): Int =
    requested.coerceAtLeast(3).coerceAtMost(total.coerceAtLeast(0))

@Composable
internal fun PlantupiaTodayPanel(
    plants: List<PlantupiaPlant>,
    onOpenPlant: (PlantupiaPlant) -> Unit,
) {
    val active = plants.filter { it.active }
    if (active.isEmpty()) return

    val today = LocalDate.now()
    val planned = scheduledPlantCheckIns(plants)
    val due = planned.filter { !LocalDate.parse(it.nextFollowUpOn).isAfter(today) }
    val upcoming = planned.firstOrNull { LocalDate.parse(it.nextFollowUpOn).isAfter(today) }
    var visible by rememberSaveable { mutableIntStateOf(3) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your check-ins", style = PlantupiaText.editorialHeading)
            Text(
                if (due.isEmpty()) "NO DUE CHECK-INS" else "${due.size} TO CHECK",
                style = PlantupiaText.sectionLabel,
            )
        }
        if (due.isNotEmpty()) {
            val first = due.first()
            Surface(
                onClick = { onOpenPlant(first) },
                modifier = Modifier.fillMaxWidth(),
                color = PlantupiaColors.softSage,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, PlantupiaColors.border),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("READY FOR A CHECK-IN", style = PlantupiaText.sectionLabel)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlantupiaEvidencePhoto(
                            photoUrl = first.coverPhoto,
                            photoDescription = "Uploaded photo of " + first.name,
                            modifier = Modifier.size(106.dp),
                            cornerRadius = 14.dp,
                            compact = true,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                first.name,
                                style = PlantupiaText.editorialHeading,
                            )
                            Text("Review your plant", style = PlantupiaText.body)
                            Text(
                                "Check-in · " + first.nextFollowUpOn,
                                style = PlantupiaText.supporting,
                            )
                            Text("Open care →", style = PlantupiaText.sectionLabel)
                        }
                    }
                }
            }
            due.drop(1).take((visibleDueCheckInCount(due.size, visible) - 1).coerceAtLeast(0)).forEach { plant ->
                Surface(
                    onClick = { onOpenPlant(plant) },
                    modifier = Modifier.fillMaxWidth(),
                    color = PlantupiaColors.paper,
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                    shape = PlantupiaShapes.inset,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plant.name, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                            Text("Check-in · " + plant.nextFollowUpOn, style = PlantupiaText.supporting)
                        }
                        Text("Open →", style = PlantupiaText.sectionLabel)
                    }
                }
            }
            if (due.size > visible) {
                TextButton(onClick = { visible += 8 }, modifier = Modifier.fillMaxWidth()) {
                    Text("Show more · ${due.size - visible} remaining", color = PlantupiaColors.forest)
                }
            }
        } else if (upcoming != null) {
            Surface(
                onClick = { onOpenPlant(upcoming) },
                modifier = Modifier.fillMaxWidth(),
                color = PlantupiaColors.paper,
                border = BorderStroke(1.dp, PlantupiaColors.border),
                shape = PlantupiaShapes.inset,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlantupiaEvidencePhoto(
                        photoUrl = upcoming.coverPhoto,
                        photoDescription = "Uploaded photo of " + upcoming.name,
                        modifier = Modifier.size(64.dp),
                        cornerRadius = 11.dp,
                        compact = true,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Next check", style = PlantupiaText.sectionLabel)
                        Text(upcoming.name, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                        Text(upcoming.nextFollowUpOn, style = PlantupiaText.supporting)
                    }
                    Text("Open →", style = PlantupiaText.sectionLabel)
                }
            }
        } else {
            Text("No check-ins planned yet. Open a plant to choose a day.", style = PlantupiaText.supporting)
        }
    }
}
