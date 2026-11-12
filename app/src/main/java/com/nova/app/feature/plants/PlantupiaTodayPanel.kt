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

/**
 * Personal check-ins are one quiet list below the company-approved inspections.
 * Schedule navigation lives in Today's first fold; no duplicate icon or empty
 * explainer panel when there are no personal dates.
 */
@Composable
internal fun PlantupiaTodayPanel(
    plants: List<PlantupiaPlant>,
    onOpenPlant: (PlantupiaPlant) -> Unit,
) {
    val planned = scheduledPlantCheckIns(plants)
    if (planned.isEmpty()) return

    val today = LocalDate.now()
    val due = planned.filter { !LocalDate.parse(it.nextFollowUpOn).isAfter(today) }
    val upcoming = planned.firstOrNull { LocalDate.parse(it.nextFollowUpOn).isAfter(today) }
    var visible by rememberSaveable { mutableIntStateOf(3) }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("YOUR CHECK-INS", style = PlantupiaText.sectionLabel)
            if (due.isNotEmpty()) Text(
                if (due.size == 1) "1 due" else "${due.size} due",
                style = PlantupiaText.supporting,
            )
        }
        if (due.isNotEmpty()) {
            val first = due.first()
            Surface(
                onClick = { onOpenPlant(first) },
                modifier = Modifier.fillMaxWidth(),
                color = PlantupiaColors.softSage,
                shape = PlantupiaShapes.inset,
                border = BorderStroke(1.dp, PlantupiaColors.border),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlantupiaEvidencePhoto(
                        photoUrl = first.coverPhoto,
                        photoDescription = "Uploaded photo of " + first.name,
                        modifier = Modifier.size(62.dp),
                        compact = true,
                    )
                    Column(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(first.name, style = PlantupiaText.body.copy(
                            fontWeight = FontWeight.SemiBold))
                        Text("Personal check-in · " + first.nextFollowUpOn,
                            style = PlantupiaText.supporting)
                    }
                    Text("Open →", style = PlantupiaText.sectionLabel)
                }
            }
            due.drop(1).take(
                (visibleDueCheckInCount(due.size, visible) - 1).coerceAtLeast(0)
            ).forEach { plant ->
                Surface(
                    onClick = { onOpenPlant(plant) },
                    modifier = Modifier.fillMaxWidth(),
                    color = PlantupiaColors.paper,
                    shape = PlantupiaShapes.inset,
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(plant.name, style = PlantupiaText.body.copy(
                                fontWeight = FontWeight.SemiBold))
                            Text("Check-in · " + plant.nextFollowUpOn,
                                style = PlantupiaText.supporting)
                        }
                        Text("Open →", style = PlantupiaText.sectionLabel)
                    }
                }
            }
            if (due.size > visible) {
                TextButton(onClick = { visible += 8 }, modifier = Modifier.fillMaxWidth()) {
                    Text("Show remaining ${due.size - visible}", color = PlantupiaColors.forest)
                }
            }
        } else if (upcoming != null) {
            Surface(
                onClick = { onOpenPlant(upcoming) },
                modifier = Modifier.fillMaxWidth(),
                color = PlantupiaColors.paper,
                shape = PlantupiaShapes.inset,
                border = BorderStroke(1.dp, PlantupiaColors.border),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlantupiaEvidencePhoto(
                        photoUrl = upcoming.coverPhoto,
                        photoDescription = "Uploaded photo of " + upcoming.name,
                        modifier = Modifier.size(56.dp),
                        compact = true,
                    )
                    Column(modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(upcoming.name, style = PlantupiaText.body.copy(
                            fontWeight = FontWeight.SemiBold))
                        Text("Next personal check-in · " + upcoming.nextFollowUpOn,
                            style = PlantupiaText.supporting)
                    }
                    Text("Open →", style = PlantupiaText.sectionLabel)
                }
            }
        }
    }
}
