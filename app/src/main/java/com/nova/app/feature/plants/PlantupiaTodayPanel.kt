package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
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
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Human-chosen check-in dates, never a watering prediction or sensor value. */
internal fun scheduledPlantCheckIns(plants: List<PlantupiaPlant>): List<PlantupiaPlant> =
    plants.filter { plant ->
        plant.active && runCatching { LocalDate.parse(plant.nextFollowUpOn) }.isSuccess
    }.sortedBy { it.nextFollowUpOn }

/** Incrementally reveal every real due record even in large client spaces. */
internal fun visibleDueCheckInCount(total: Int, requested: Int): Int =
    requested.coerceAtLeast(3).coerceAtMost(total.coerceAtLeast(0))

internal fun personalCheckInDateLabel(value: String, today: LocalDate = LocalDate.now()): String {
    val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return value
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.isBefore(today) -> "Due ${date.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))}"
        date == today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH))
    }
}

/**
 * Personal check-ins are a small editorial rhythm inside Today.
 *
 * A check-in is a date the person chose. It is never presented as a watering
 * prediction, diagnosis, sensor reading or proof that care was completed.
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("PERSONAL RHYTHM", style = PlantupiaText.sectionLabel)
                Text(
                    if (due.isNotEmpty()) "What needs your attention"
                    else "What you chose to revisit",
                    style = PlantupiaText.editorialHeading,
                )
            }
            Surface(
                color = if (due.isNotEmpty()) PlantupiaColors.forest else PlantupiaColors.softSage,
                shape = CircleShape,
            ) {
                Text(
                    if (due.isNotEmpty()) {
                        if (due.size == 1) "1 due" else "${due.size} due"
                    } else {
                        upcoming?.let { personalCheckInDateLabel(it.nextFollowUpOn, today) } ?: "Planned"
                    },
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    style = PlantupiaText.sectionLabel.copy(
                        color = if (due.isNotEmpty()) PlantupiaColors.cream else PlantupiaColors.forest,
                    ),
                )
            }
        }

        if (due.isNotEmpty()) {
            val first = due.first()
            PersonalCheckInHero(
                plant = first,
                eyebrow = personalCheckInDateLabel(first.nextFollowUpOn, today).uppercase(Locale.ENGLISH),
                supporting = "A check-in you chose. Open the plant before recording anything.",
                onOpen = { onOpenPlant(first) },
            )

            due.drop(1).take(
                (visibleDueCheckInCount(due.size, visible) - 1).coerceAtLeast(0),
            ).forEach { plant ->
                PersonalCheckInRow(
                    plant = plant,
                    dateLabel = personalCheckInDateLabel(plant.nextFollowUpOn, today),
                    onOpen = { onOpenPlant(plant) },
                )
            }

            if (due.size > visible) {
                TextButton(onClick = { visible += 8 }, modifier = Modifier.fillMaxWidth()) {
                    Text("Show remaining ${due.size - visible}", color = PlantupiaColors.forest)
                }
            }

            if (upcoming != null) {
                Spacer(Modifier.height(2.dp))
                HorizontalDivider(color = PlantupiaColors.border)
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("UP NEXT", style = PlantupiaText.sectionLabel)
                        Text(upcoming.name, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            personalCheckInDateLabel(upcoming.nextFollowUpOn, today),
                            style = PlantupiaText.supporting,
                        )
                    }
                    TextButton(onClick = { onOpenPlant(upcoming) }) {
                        Text("Open \u2192", color = PlantupiaColors.forest)
                    }
                }
            }
        } else if (upcoming != null) {
            PersonalCheckInHero(
                plant = upcoming,
                eyebrow = "NEXT CHECK-IN \u00B7 ${personalCheckInDateLabel(upcoming.nextFollowUpOn, today).uppercase(Locale.ENGLISH)}",
                supporting = "A date you chose for yourself. Nothing is marked done until you record what actually happened.",
                onOpen = { onOpenPlant(upcoming) },
            )
        }
    }
}

@Composable
private fun PersonalCheckInHero(
    plant: PlantupiaPlant,
    eyebrow: String,
    supporting: String,
    onOpen: () -> Unit,
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        color = PlantupiaColors.softSage,
        shape = PlantupiaShapes.card,
        border = BorderStroke(1.dp, PlantupiaColors.border),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantupiaEvidencePhoto(
                photoUrl = plant.coverPhoto,
                photoDescription = "Uploaded photo of ${plant.name}",
                modifier = Modifier.size(74.dp),
                compact = true,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(eyebrow, style = PlantupiaText.sectionLabel)
                Text(plant.name, style = PlantupiaText.editorialHeading)
                Text(supporting, style = PlantupiaText.supporting)
                Text("Review plant \u2192", style = PlantupiaText.sectionLabel)
            }
        }
    }
}

@Composable
private fun PersonalCheckInRow(
    plant: PlantupiaPlant,
    dateLabel: String,
    onOpen: () -> Unit,
) {
    Surface(
        onClick = onOpen,
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(plant.name, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                Text(dateLabel, style = PlantupiaText.supporting)
            }
            Text("Review \u2192", style = PlantupiaText.sectionLabel)
        }
    }
}
