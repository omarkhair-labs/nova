package com.nova.app.feature.plants

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
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
@Composable
internal fun PlantupiaCareActivity(accountId: Long) {
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
    var selected by remember(accountId) { mutableStateOf(LocalDate.now()) }
    var month by remember(accountId) { mutableStateOf(YearMonth.from(LocalDate.now())) }
    var weekMode by remember(accountId) { mutableStateOf(true) }
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

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your plants through time", style = PlantupiaText.supporting)
        Text("Planned checks and real care you recorded, never guessed watering dates.", style = PlantupiaText.supporting)
        if (busy || historyBusy) LinearProgressIndicator(Modifier.fillMaxWidth())

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = {
                if (weekMode) {
                    selected = selected.minusWeeks(1)
                    month = YearMonth.from(selected)
                } else {
                    month = month.minusMonths(1)
                    selected = month.atDay(1)
                }
            }) { Text("‹") }
            Text(
                if (weekMode) "Week of ${visible.first()}" else month.format(dateFormatter),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            OutlinedButton(onClick = {
                if (weekMode) {
                    selected = selected.plusWeeks(1)
                    month = YearMonth.from(selected)
                } else {
                    month = month.plusMonths(1)
                    selected = month.atDay(1)
                }
            }) { Text("›") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                selected = LocalDate.now()
                month = YearMonth.from(selected)
            }) { Text("Today") }
            Spacer(Modifier.weight(1f))
            FilterChip(selected = weekMode, onClick = { weekMode = true }, label = { Text("Week") })
            FilterChip(selected = !weekMode, onClick = {
                weekMode = false
                month = YearMonth.from(selected)
            }, label = { Text("Month") })
        }
        val groups = visible.chunked(7)
        groups.forEachIndexed { index, week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    val items = entries.count { it.date == day }
                    val recordedCount = entries.count { it.date == day && it.recorded }
                    val highlighted = day == selected
                    val inMonth = YearMonth.from(day) == month
                    Surface(
                        modifier = Modifier.weight(1f).heightIn(min = 61.dp).clickable {
                            selected = day
                            month = YearMonth.from(day)
                        },
                        shape = MaterialTheme.shapes.small,
                        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (inMonth || weekMode) 0.5f else 0.2f),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            if (index == 0) Text(
                                day.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (highlighted || day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (items > 0) {
                                Text(
                                    if (recordedCount > 0) "$items · ✓" else "$items ·",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        Text(selected.format(dayFormatter), style = MaterialTheme.typography.titleMedium)
        if (!busy && !historyBusy && selectedEntries.isEmpty() && historyError == null) {
            Text("Nothing scheduled for this date.", style = PlantupiaText.supporting)
        }
        selectedEntries.sortedWith(
            compareByDescending<CareCalendarEntry> { it.recorded }.thenBy { it.plantName }
        ).forEach { entry ->
            Card(onClick = { PlantupiaPlantOpenSignal.open(accountId, entry.plantId) }) {
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
                        Text(entry.plantName, style = MaterialTheme.typography.titleSmall)
                        Text(entry.title, style = MaterialTheme.typography.bodyMedium)
                        Text(entry.origin, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        if (entry.recorded) "Done ✓"
                        else if (entry.date < LocalDate.now()) "Due"
                        else "Planned",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.recorded) PlantupiaColors.forest
                            else PlantupiaColors.secondaryText,
                    )
                }
            }
        }
        if (!busy && !historyBusy && entries.isEmpty() && error == null && historyError == null) {
            Text("No dates yet. Create a personal follow-up or opt in to a reviewed inspection from your plant.", style = PlantupiaText.body)
        }
        historyError?.let { message ->
            Text("Recorded care history is unavailable: $message", color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { revision++ }) { Text("Retry history") }
        }
        error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { revision++ }) { Text("Retry care calendar") }
        }
        Text(
            "Tap any plant to open its complete Living History, record a check or change a follow-up. " +
                "Android notification delivery needs separate permission and may not happen at an exact minute.",
            style = PlantupiaText.supporting,
        )
    }
}
