package com.nova.app.feature.plants

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Week/month calendar over the SAME server-owned approved plans and user-chosen
 * follow-ups used by Today and Living History. Tapping an entry opens its plant.
 * Scheduling, completion, opt-in and evidence stay in the existing plant flow.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaCareActivity(accountId: Long, onViewChanged: () -> Unit = {}) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(accountId) { PlantupiaRepository(context.applicationContext) }
    var plans by remember(accountId) { mutableStateOf(emptyList<JSONObject>()) }
    var recordedRows by remember(accountId) { mutableStateOf(emptyList<JSONObject>()) }
    var plants by remember(accountId) { mutableStateOf(emptyList<PlantupiaPlant>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var historyError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(true) }
    var historyBusy by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var selected by androidx.compose.runtime.saveable.rememberSaveable(accountId) { mutableStateOf(LocalDate.now()) }
    var month by androidx.compose.runtime.saveable.rememberSaveable(accountId) { mutableStateOf(YearMonth.from(LocalDate.now())) }
    var weekMode by androidx.compose.runtime.saveable.rememberSaveable(accountId) { mutableStateOf(true) }
    val visible = if (weekMode) careCalendarWeekDays(selected) else careCalendarMonthDays(month)
    val visibleStart = visible.first()
    val visibleEnd = visible.last()

    LaunchedEffect(accountId, revision) {
        busy = true
        error = null
        when (val response = repository.plants()) {
            is ApiResult.Success -> if (repository.isCurrentSession()) plants = response.value
            is ApiResult.Failure -> if (repository.isCurrentSession()) error = response.message
        }
        when (val response = repository.demoGet("care-plans/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) {
                plans = response.value.demoRows().filter { it.optBoolean("enabled") && it.optBoolean("valid") }
            }
            is ApiResult.Failure -> if (repository.isCurrentSession() && response.statusCode != 404) {
                error = response.message
            }
        }
        busy = false
    }

    // One authenticated, bounded read-only query for the displayed week/month.
    LaunchedEffect(accountId, revision, visibleStart, visibleEnd) {
        historyBusy = true
        historyError = null
        recordedRows = emptyList()
        when (val response = repository.calendarHistory(
            visibleStart.toString(), visibleEnd.toString(),
        )) {
            is ApiResult.Success -> if (repository.isCurrentSession()) recordedRows = response.value
            is ApiResult.Failure -> if (repository.isCurrentSession()) historyError = response.message
        }
        historyBusy = false
    }

    val entries = remember(plants, plans, recordedRows) {
        val personal = scheduledPlantCheckIns(plants).mapNotNull { plant ->
            parseCareCalendarDate(plant.nextFollowUpOn)?.let { day ->
                CareCalendarEntry(day, plant.id, plant.name, "Personal check-in", "Chosen by you")
            }
        }
        val approved = plans.mapNotNull { plan ->
            parseCareCalendarDate(plan.optString("next_due_on"))?.let { day ->
                CareCalendarEntry(
                    day,
                    plan.optString("plant_id"),
                    plan.optString("plant_name").ifBlank { "Plant" },
                    plan.optString("action").ifBlank { "Reviewed inspection" },
                    "Reviewed plan",
                )
            }
        }
        val recorded = recordedRows.mapNotNull { item ->
            parseCareCalendarDate(item.optString("date"))?.let { day ->
                val label = when (item.optString("kind")) {
                    "care" -> "Care recorded"
                    "observation" -> "Observation recorded"
                    "issue" -> "Issue recorded"
                    "outcome" -> "Outcome recorded"
                    else -> "Recorded moment"
                }
                val count = item.optInt("count", 1).coerceAtLeast(1)
                CareCalendarEntry(
                    day,
                    item.optString("plant_id"),
                    item.optString("plant_name"),
                    if (count == 1) label else "$count × $label",
                    "Living History",
                    recorded = true,
                )
            }
        }
        careCalendarEntries(personal + recorded, approved)
    }
    val selectedEntries = entries.filter { it.date == selected }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE d MMM yyyy", locale) }

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Care schedule", style = PlantupiaText.pageHeading)
        if (busy || historyBusy) LinearProgressIndicator(Modifier.fillMaxWidth())

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PlantupiaColors.paper,
            shape = PlantupiaShapes.card,
            border = BorderStroke(1.dp, PlantupiaColors.border),
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(
                        onClick = {
                            if (weekMode) {
                                selected = selected.minusWeeks(1)
                                month = YearMonth.from(selected)
                            } else {
                                month = month.minusMonths(1)
                                selected = month.atDay(1)
                            }
                            onViewChanged()
                        },
                        modifier = Modifier.semantics { contentDescription = if (weekMode) "Previous week" else "Previous month" },
                    ) { Text("\u2039", style = PlantupiaText.editorialHeading) }
                    Text(
                        if (weekMode) {
                            val first = visible.first().format(DateTimeFormatter.ofPattern("d MMM", locale))
                            val last = visible.last().format(DateTimeFormatter.ofPattern("d MMM", locale))
                            "$first \u2014 $last"
                        } else month.format(dateFormatter),
                        style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    TextButton(
                        onClick = {
                            if (weekMode) {
                                selected = selected.plusWeeks(1)
                                month = YearMonth.from(selected)
                            } else {
                                month = month.plusMonths(1)
                                selected = month.atDay(1)
                            }
                            onViewChanged()
                        },
                        modifier = Modifier.semantics { contentDescription = if (weekMode) "Next week" else "Next month" },
                    ) { Text("\u203A", style = PlantupiaText.editorialHeading) }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(onClick = {
                        selected = LocalDate.now()
                        month = YearMonth.from(selected)
                        onViewChanged()
                    }) { Text("Today", color = PlantupiaColors.forest) }
                    FilterChip(selected = weekMode, onClick = {
                        if (!weekMode) {
                            weekMode = true
                            onViewChanged()
                        }
                    }, label = { Text("Week") })
                    FilterChip(selected = !weekMode, onClick = {
                        if (weekMode) {
                            weekMode = false
                            month = YearMonth.from(selected)
                            onViewChanged()
                        }
                    }, label = { Text("Month") })
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("• Planned", style = PlantupiaText.supporting)
            Text("✓ Recorded", style = PlantupiaText.supporting.copy(color = PlantupiaColors.forest))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 354.dp && LocalDensity.current.fontScale <= 1.3f) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    // One weekday ruler, then a compact navigable grid. Month cells stay
                    // 48dp tappable without a wall of outlined rectangular cards.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        visible.take(7).forEach { day ->
                            Text(
                                day.format(DateTimeFormatter.ofPattern("EEEEE", locale)),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = PlantupiaColors.secondaryText,
                            )
                        }
                    }
                    Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        visible.chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                week.forEach { day ->
                                    val items = entries.count { it.date == day }
                                    val recordedCount = entries.count { it.date == day && it.recorded }
                                    val plannedCount = items - recordedCount
                                    val highlighted = day == selected
                                    val inMonth = YearMonth.from(day) == month
                                    val today = day == LocalDate.now()
                                    val dayTint by animateColorAsState(
                                        targetValue = when {
                                            highlighted -> PlantupiaColors.softSage
                                            weekMode -> PlantupiaColors.paper
                                            else -> Color.Transparent
                                        },
                                        label = "care-day-selection",
                                    )
                                    Surface(
                                        modifier = Modifier.weight(1f)
                                            .heightIn(min = if (weekMode) 56.dp else 48.dp)
                                            .clickable(role = Role.Button) {
                                                selected = day
                                                month = YearMonth.from(day)
                                            }
                                            .semantics {
                                                this.selected = highlighted
                                                contentDescription =
                                                    day.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale)) +
                                                    ", $items care entries, $recordedCount recorded"
                                            },
                                        shape = RoundedCornerShape(if (weekMode) 12.dp else 10.dp),
                                        color = dayTint,
                                        border = BorderStroke(
                                            if (highlighted) 1.5.dp else if (today) 1.dp else 0.dp,
                                            if (highlighted) PlantupiaColors.forest
                                            else if (today) PlantupiaColors.mutedSage
                                            else Color.Transparent,
                                        ),
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 3.dp, horizontal = 1.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(1.dp),
                                        ) {
                                            Text(
                                                day.dayOfMonth.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (highlighted || today) FontWeight.Bold else FontWeight.Normal,
                                                color = if (inMonth || weekMode) PlantupiaColors.forest
                                                    else PlantupiaColors.secondaryText.copy(alpha = .5f),
                                            )
                                            if (items > 0) {
                                                Text(
                                                    when {
                                                        plannedCount > 0 && recordedCount > 0 -> "•✓"
                                                        recordedCount > 0 -> "✓"
                                                        else -> "•"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (recordedCount > 0) PlantupiaColors.forest
                                                    else PlantupiaColors.mutedSage,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            } else {
                // Keep every day selectable when seven 48 dp targets cannot fit.
                // This is the same displayed period and the same server-owned records.
                var dateMenu by remember(visibleStart, visibleEnd) { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { dateMenu = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Choose date · " + selected.format(dayFormatter))
                    }
                    Box(Modifier.fillMaxWidth()) {
                        DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                            visible.forEach { day ->
                                val count = entries.count { it.date == day }
                                val recorded = entries.count { it.date == day && it.recorded }
                                DropdownMenuItem(
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(day.format(dayFormatter))
                                            if (count > 0) Text("${count - recorded} planned · $recorded recorded",
                                                style = PlantupiaText.supporting)
                                        }
                                    },
                                    onClick = {
                                        dateMenu = false
                                        selected = day
                                        month = YearMonth.from(day)
                                    },
                                    modifier = Modifier.semantics { this.selected = day == selected },
                                )
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { selected = selected.minusDays(1); month = YearMonth.from(selected) }) {
                            Text("Previous day")
                        }
                        TextButton(onClick = { selected = selected.plusDays(1); month = YearMonth.from(selected) }) {
                            Text("Next day")
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = PlantupiaColors.border)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(selected.format(dayFormatter), style = PlantupiaText.editorialHeading)
            val recordedOnDay = selectedEntries.count { it.recorded }
            val plannedOnDay = selectedEntries.size - recordedOnDay
            Text(
                when {
                    busy || historyBusy -> "Checking this day…"
                    error != null || historyError != null -> "Some records could not be loaded."
                    selectedEntries.isEmpty() -> "Nothing planned or recorded."
                    recordedOnDay > 0 && plannedOnDay > 0 -> "$recordedOnDay recorded \u00B7 $plannedOnDay planned"
                    recordedOnDay > 0 -> if (recordedOnDay == 1) "1 recorded moment" else "$recordedOnDay recorded moments"
                    else -> if (plannedOnDay == 1) "1 planned entry" else "$plannedOnDay planned entries"
                },
                style = PlantupiaText.supporting,
            )
        }
        if (!busy && !historyBusy && selectedEntries.isEmpty() && historyError == null && error == null &&
            entries.isEmpty()) {
            Text("Open a plant to plan a check-in.", style = PlantupiaText.supporting)
        }
        listOf(false, true).forEach { recorded ->
            val group = selectedEntries.filter { it.recorded == recorded }.sortedBy { it.plantName }
            if (group.isNotEmpty()) Text(if (recorded) "Recorded" else "Planned",
                style = PlantupiaText.itemTitle)
            group.forEach { entry ->
            Surface(
                onClick = {
                    PlantupiaPlantOpenSignal.open(
                        accountId, entry.plantId,
                        if (entry.recorded) PlantupiaDetailChapter.HISTORY else PlantupiaDetailChapter.CARE,
                        recordedDay = entry.date.takeIf { entry.recorded },
                    )
                },
                color = PlantupiaColors.paper,
                shape = PlantupiaShapes.inset,
                border = BorderStroke(1.dp, PlantupiaColors.border),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val plant = plants.firstOrNull { it.id == entry.plantId }
                    PlantupiaEvidencePhoto(
                        photoUrl = plant?.coverPhoto.orEmpty(),
                        photoDescription = "Actual photo of " + entry.plantName,
                        modifier = Modifier.size(56.dp),
                        cornerRadius = 12.dp,
                        compact = true,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(entry.plantName, style = PlantupiaText.itemTitle)
                        Text(entry.title, style = PlantupiaText.body)
                        Text(entry.origin + when {
                            entry.recorded -> " · Recorded ✓"
                            entry.date < LocalDate.now() -> " · Due"
                            else -> " · Planned"
                        }, style = PlantupiaText.supporting)
                    }
                }
            }
        }
        }
        historyError?.let { message ->
            Text("Recorded care history is unavailable: $message", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { revision++ }) { Text("Retry history") }
        }
        error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { revision++ }) { Text("Retry care calendar") }
        }
    }
}
