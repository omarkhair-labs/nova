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
    var plants by remember(accountId) { mutableStateOf(emptyList<PlantupiaPlant>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(true) }
    var revision by remember { mutableIntStateOf(0) }
    var selected by remember(accountId) { mutableStateOf(LocalDate.now()) }
    var month by remember(accountId) { mutableStateOf(YearMonth.from(LocalDate.now())) }
    var weekMode by remember { mutableStateOf(false) }

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

    val entries = remember(plants, plans) {
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
        careCalendarEntries(personal, approved)
    }
    val visible = if (weekMode) careCalendarWeekDays(selected) else careCalendarMonthDays(month)
    val selectedEntries = entries.filter { it.date == selected }
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    val dayFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE d MMM yyyy", locale) }

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Care calendar", style = PlantupiaText.editorialHeading)
        Text("Your real check-ins and opted-in reviewed inspections, in one calendar. Dates are not watering instructions.", style = PlantupiaText.supporting)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

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
                                    items.toString() + " •",
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
        if (!busy && selectedEntries.isEmpty()) {
            Text("Nothing scheduled for this date.", style = PlantupiaText.supporting)
        }
        selectedEntries.forEach { entry ->
            Card(onClick = { PlantupiaPlantOpenSignal.open(accountId, entry.plantId) }) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(entry.plantName, style = MaterialTheme.typography.titleSmall)
                    Text(entry.title, style = MaterialTheme.typography.bodyMedium)
                    Text(entry.origin + " · Open plant to record or change the check",
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (!busy && entries.isEmpty() && error == null) {
            Text("No dates yet. Create a personal follow-up or opt in to a reviewed inspection from your plant.", style = PlantupiaText.body)
        }
        error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { revision++ }) { Text("Retry care calendar") }
        }
        Text(
            "Schedule changes and completed observations are recorded in each plant's Living History. " +
                "Android notification delivery needs separate permission and may not happen at an exact minute.",
            style = PlantupiaText.supporting,
        )
    }
}
