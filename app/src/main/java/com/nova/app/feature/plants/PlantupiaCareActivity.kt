package com.nova.app.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import org.json.JSONObject

/** Current persisted schedules, not fabricated alerts or a second reminder scheduler. */
@Composable
internal fun PlantupiaCareActivity(accountId: Long) {
    val context = LocalContext.current
    val repository = remember(accountId) { PlantupiaRepository(context.applicationContext) }
    var plans by remember { mutableStateOf(emptyList<JSONObject>()) }
    var plants by remember { mutableStateOf(emptyList<PlantupiaPlant>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(true) }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(revision) {
        busy = true; error = null
        when (val response = repository.plants()) {
            is ApiResult.Success -> if (repository.isCurrentSession()) plants = response.value
            is ApiResult.Failure -> if (repository.isCurrentSession()) error = response.message
        }
        when (val response = repository.demoGet("care-plans/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) plans = response.value.demoRows().filter { it.optBoolean("enabled") && it.optBoolean("valid") }
            is ApiResult.Failure -> if (repository.isCurrentSession() && response.statusCode != 404) error = response.message
        }
        busy = false
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Your scheduled checks", style = PlantupiaText.editorialHeading)
        Text("Only your opted-in inspections and chosen check-ins. This list does not turn on notifications.", style = PlantupiaText.supporting)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        plans.sortedBy { it.optString("next_due_on") }.take(12).forEach { plan ->
            TextButton(onClick = { PlantupiaPlantOpenSignal.open(accountId, plan.getString("plant_id")) }) {
                Text("${if (plan.optBoolean("due")) "Due · " else "Next · "}${plan.getString("plant_name")}\n${plan.getString("action")} · ${plan.getString("next_due_on")} · ${plan.getString("timezone")}")
            }
        }
        scheduledPlantCheckIns(plants).take(12).forEach { plant ->
            TextButton(onClick = { PlantupiaPlantOpenSignal.open(accountId, plant.id) }) { Text("${plant.name} · your check-in · ${plant.nextFollowUpOn}") }
        }
        if (!busy && error == null && plans.isEmpty() && scheduledPlantCheckIns(plants).isEmpty()) Text("No scheduled checks. Choose a date or review an optional plan from your plant.", style = PlantupiaText.body)
        error?.let { Text(it, color = PlantupiaColors.errorText); TextButton(onClick = { revision++ }) { Text("Retry care schedules") } }
        Text("Local check-in delivery needs your explicit reminder choice and Android permission. Reviewed inspection plans currently use Today and this list; they do not schedule OS alerts.", style = PlantupiaText.supporting)
    }
}
