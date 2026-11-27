package com.nova.app.feature.plants

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    onScheduleDay: (LocalDate) -> Unit,
) {
    val planned = scheduledPlantCheckIns(plants)
    if (planned.isEmpty()) return

    val today = LocalDate.now()
    val due = planned.filter { !LocalDate.parse(it.nextFollowUpOn).isAfter(today) }
    val upcoming = planned.firstOrNull { LocalDate.parse(it.nextFollowUpOn).isAfter(today) }
    var visible by rememberSaveable { mutableIntStateOf(3) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (due.isNotEmpty()) "Check-ins" else "Upcoming",
                style = PlantupiaText.editorialHeading)
            if (due.isNotEmpty()) {
                Text(if (due.size == 1) "1 due" else "${due.size} due", style = PlantupiaText.supporting)
            }
        }

        val week = (0..6).map { today.plusDays(it.toLong()) }
        val counts = planned.groupingBy { it.nextFollowUpOn }.eachCount()
        if (week.any { counts.containsKey(it.toString()) }) {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(week.size) { index ->
                    val day = week[index]
                    val count = counts[day.toString()] ?: 0
                    Surface(onClick = { onScheduleDay(day) }, shape = PlantupiaShapes.inset,
                        color = if (count > 0) PlantupiaColors.softSage else PlantupiaColors.paper) {
                        Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)), style = PlantupiaText.supporting)
                            Text(day.dayOfMonth.toString(), style = PlantupiaText.itemTitle)
                            if (count > 0) Text(if (count == 1) "1 check" else "$count checks", style = PlantupiaText.supporting)
                        }
                    }
                }
            }
        }
        if (due.isNotEmpty()) {
            val first = due.first()
            PersonalCheckInHero(
                plant = first,
                eyebrow = personalCheckInDateLabel(first.nextFollowUpOn, today).uppercase(Locale.ENGLISH),
                supporting = first.placement.ifBlank { "Care and observations" },
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
                supporting = upcoming.placement.ifBlank { "Care and observations" },
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
    Surface(onClick = onOpen, modifier = Modifier.fillMaxWidth(), color = PlantupiaColors.forest) {
        Column {
            if (plant.coverPhoto.isNotBlank()) {
                PlantupiaEvidencePhoto(
                    photoUrl = plant.coverPhoto,
                    photoDescription = "Uploaded photo of ${plant.name}",
                    modifier = Modifier.fillMaxWidth().height(136.dp),
                    cornerRadius = 0.dp,
                    framing = PlantupiaPhotoFraming.FULL_RECORD,
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (plant.coverPhoto.isNotBlank()) {
                    PlantupiaEvidencePhoto(plant.coverPhoto, "Portrait of " + plant.name,
                        modifier = Modifier.size(84.dp, 108.dp), compact = true)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(eyebrow, style = PlantupiaText.sectionLabel.copy(color = PlantupiaColors.sage))
                    Text(plant.name, style = PlantupiaText.editorialHeading.copy(color = PlantupiaColors.paper))
                    Text(supporting, style = PlantupiaText.supporting.copy(color = PlantupiaColors.cream))
                    Text("Review →", style = PlantupiaText.sectionLabel.copy(color = PlantupiaColors.paper))
                }
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
    Column {
        HorizontalDivider(color = PlantupiaColors.border)
        Surface(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            color = PlantupiaColors.cream,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(dateLabel, style = PlantupiaText.sectionLabel)
                    Text(plant.name, style = PlantupiaText.itemTitle)
                }
                Text("→", style = PlantupiaText.itemTitle)
            }
        }
    }
}
