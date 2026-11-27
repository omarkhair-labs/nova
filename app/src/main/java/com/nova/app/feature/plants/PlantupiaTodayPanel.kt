package com.nova.app.feature.plants

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.BorderStroke
import java.time.OffsetDateTime
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

/** Prefer the latest actual record, then an uploaded portrait, then a stable active plant.
 * The fallback is simply a collection entry; it is never described as recent activity.
 */
internal fun todayCollectionFocus(plants: List<PlantupiaPlant>): PlantupiaPlant? {
    val active = plants.filter { it.active }
    return active.sortedWith(compareByDescending<PlantupiaPlant> {
        runCatching { OffsetDateTime.parse(it.lastEventAt).toInstant() }.getOrNull()
    }.thenByDescending { it.coverPhoto.isNotBlank() }.thenBy { it.id }).firstOrNull()
}

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
                style = PlantupiaText.editorialHeading, modifier = Modifier.semantics { heading() })
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
                        modifier = Modifier.semantics {
                            contentDescription = day.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)) +
                                ", " + count + if (count == 1) " check-in" else " check-ins"
                        },
                        color = if (count > 0) PlantupiaColors.softSage else PlantupiaColors.paper) {
                        Column(Modifier.heightIn(min = 54.dp).padding(horizontal = 11.dp, vertical = 8.dp),
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
internal fun TodayCollectionPortrait(plant: PlantupiaPlant, onOpen: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("From your collection", style = PlantupiaText.editorialHeading,
            modifier = Modifier.semantics { heading() })
        val recordedDay = runCatching { OffsetDateTime.parse(plant.lastEventAt).toLocalDate() }.getOrNull()
        PersonalCheckInHero(plant,
            eyebrow = recordedDay?.let { "LAST RECORDED · " + it.format(DateTimeFormatter.ofPattern("d MMM yyyy",Locale.ENGLISH)).uppercase(Locale.ENGLISH) }
                ?: "YOUR PLANT",
            supporting = listOf(plant.species,plant.placement).filter { it.isNotBlank() }.joinToString(" · "),
            onOpen = onOpen, action = "Open plant →", collection = true)
    }
}

@Composable
private fun PersonalCheckInHero(
    plant: PlantupiaPlant,
    eyebrow: String,
    supporting: String,
    onOpen: () -> Unit,
    action: String = "Review →",
    collection: Boolean = false,
) {
    val ink = if (collection) PlantupiaColors.forest else PlantupiaColors.paper
    val secondary = if (collection) PlantupiaColors.secondaryText else PlantupiaColors.cream
    Surface(onClick = onOpen, modifier = Modifier.fillMaxWidth(),
        shape = PlantupiaShapes.inset,
        border = if (collection) BorderStroke(1.dp,PlantupiaColors.border) else null,
        color = if (collection) PlantupiaColors.paper else PlantupiaColors.forest) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
            val sideBySide = maxWidth / LocalDensity.current.fontScale >= 255.dp
            val identity: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(eyebrow, style = PlantupiaText.sectionLabel.copy(
                        color = if (collection) PlantupiaColors.forest else PlantupiaColors.sage))
                    Text(plant.name, style = PlantupiaText.editorialHeading.copy(color = ink))
                    if (supporting.isNotBlank()) Text(supporting, style = PlantupiaText.supporting.copy(color = secondary))
                    Text(action, style = PlantupiaText.itemTitle.copy(color = ink),modifier = Modifier.padding(top=6.dp))
                }
            }
            val photo: @Composable (Modifier) -> Unit = { modifier ->
                PlantupiaEvidencePhoto(plant.coverPhoto, "Uploaded portrait of " + plant.name,
                    modifier = modifier, compact = sideBySide, cornerRadius = 6.dp)
            }
            if (sideBySide) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp),verticalAlignment = Alignment.CenterVertically) {
                    if (plant.coverPhoto.isNotBlank() || collection) photo(Modifier.size(98.dp,136.dp))
                    Column(Modifier.weight(1f)) { identity() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    identity()
                    if (plant.coverPhoto.isNotBlank()) photo(Modifier.fillMaxWidth().height(150.dp))
                    else if (collection) Text("No uploaded portrait yet",style = PlantupiaText.supporting.copy(color=secondary))
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
