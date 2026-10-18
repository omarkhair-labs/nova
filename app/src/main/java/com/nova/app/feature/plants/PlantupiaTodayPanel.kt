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
    val today = LocalDate.now()
    val planned = scheduledPlantCheckIns(plants)
    val due = planned.filter { !LocalDate.parse(it.nextFollowUpOn).isAfter(today) }
    val future = planned.filter { LocalDate.parse(it.nextFollowUpOn).isAfter(today) }
    var dueVisibleCount by rememberSaveable { mutableIntStateOf(3) }

    // Quiet-care composition: a giant zero-count attention card implies urgency
    // when no human-selected check-in is due. Preserve the real next review
    // without taking visual priority away from the living space.
    if (due.isEmpty()) {
        if (future.isNotEmpty()) {
            val next = future.first()
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("NEXT CHECK-IN · ALL SPACES", style = PlantupiaText.sectionLabel)
                Surface(
                    onClick = { onOpenPlant(next) },
                    color = PlantupiaColors.paper,
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                    shape = PlantupiaShapes.inset,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                next.name,
                                style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                "You chose to check again on " + next.nextFollowUpOn,
                                style = PlantupiaText.supporting,
                            )
                        }
                        Text("Open →", style = PlantupiaText.sectionLabel)
                    }
                }
            }
        } else if (plants.any { it.active }) {
            Text(
                "No check-ins planned. Record what you notice when you visit your plants.",
                style = PlantupiaText.supporting,
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("CHECK-INS ACROSS YOUR SPACES", style = PlantupiaText.sectionLabel)
        Surface(
            color = PlantupiaColors.softSage,
            border = BorderStroke(1.dp, PlantupiaColors.border),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(15.dp),
                        color = PlantupiaColors.paper,
                    ) {
                        Text(
                            text = due.size.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = PlantupiaText.editorialHeading,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            if (due.isNotEmpty()) "Check-ins need your attention"
                            else "No check-ins due today",
                            style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            "Check the soil and leaves before deciding whether to water.",
                            style = PlantupiaText.supporting,
                        )
                    }
                }
                if (due.isNotEmpty()) {
                    due.take(visibleDueCheckInCount(due.size, dueVisibleCount)).forEach { plant ->
                        Surface(
                            onClick = { onOpenPlant(plant) },
                            color = PlantupiaColors.paper,
                            shape = PlantupiaShapes.inset,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                PlantupiaEvidencePhoto(
                                    photoUrl = plant.coverPhoto,
                                    photoDescription = "Uploaded photo of " + plant.name,
                                    modifier = Modifier.size(54.dp),
                                    cornerRadius = 10.dp,
                                    compact = true,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(plant.name, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                                    Text("Check-in you chose: " + plant.nextFollowUpOn, style = PlantupiaText.supporting)
                                }
                                Text("Open →", style = PlantupiaText.sectionLabel)
                            }
                        }
                    }
                    if (due.size > dueVisibleCount) {
                        val remaining = due.size - dueVisibleCount
                        TextButton(
                            onClick = { dueVisibleCount += 8 },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Show ${minOf(8, remaining)} more check-ins · $remaining remaining",
                                color = PlantupiaColors.forest,
                            )
                        }
                    }
                } else if (future.isNotEmpty()) {
                    val next = future.first()
                    Text(
                        "Next check-in: " + next.name + " · " + next.nextFollowUpOn,
                        style = PlantupiaText.supporting,
                    )
                    TextButton(onClick = { onOpenPlant(next) }) {
                        Text("Open plant", color = PlantupiaColors.forest)
                    }
                } else {
                    Text(
                        "No check-ins planned yet. Open a plant, record care or an observation, and choose when to revisit.",
                        style = PlantupiaText.supporting,
                    )
                    plants.firstOrNull { it.active }?.let { plant ->
                        TextButton(onClick = { onOpenPlant(plant) }) {
                            Text("Start with " + plant.name + " →", color = PlantupiaColors.forest)
                        }
                    }
                }
            }
        }
    }
}
