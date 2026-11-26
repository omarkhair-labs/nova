package com.nova.app.feature.plants

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaRepository
import java.time.LocalDate

/** Same event endpoint and cards as the full timeline; the calendar selects a day. */
@Composable
internal fun PlantupiaDayHistory(
    repository: PlantupiaRepository,
    plantId: String,
    day: String,
    onShowAll: () -> Unit,
    record: @Composable (PlantupiaCareEvent) -> Unit,
) {
    val date = remember(day) { LocalDate.parse(day) }
    var events by remember { mutableStateOf(emptyList<PlantupiaCareEvent>()) }
    var offset by remember { mutableIntStateOf(0) }
    var nextOffset by remember { mutableStateOf<Int?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(repository, plantId, date, offset, retry) {
        loading = true
        error = null
        val response = repository.historyPage(plantId, offset, observedOn = date)
        if (!repository.isCurrentSession()) return@LaunchedEffect
        when (response) {
            is ApiResult.Success -> {
                events = (events + response.value.events).distinctBy { it.id }
                nextOffset = response.value.nextOffset
            }
            is ApiResult.Failure -> error = response.message
        }
        loading = false
    }
    Text("Records from $day", style = PlantupiaText.editorialHeading)
    TextButton(onClick = onShowAll) { Text("View all dates") }
    events.forEach { event -> key(event.id) { record(event) } }
    when {
        loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        error != null -> {
            Text(error.orEmpty(), style = PlantupiaText.supporting)
            TextButton(onClick = { retry++ }) { Text("Retry") }
        }
        nextOffset != null -> TextButton(onClick = { nextOffset?.let { offset = it } }) {
            Text("Load more from this day")
        }
        events.isEmpty() -> Text("No records for this date.", style = PlantupiaText.supporting)
    }
}
