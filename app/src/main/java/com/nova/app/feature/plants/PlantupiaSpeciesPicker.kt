package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaLibrarySpecies
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.delay

private val forest = Color(0xFF123B2E)
private val quiet = Color(0xFF4A5A50)
private val sage = Color(0xFFEAF0E8)

/**
 * Curated identity lookup. A selection is user-reported and never grants staff verification.
 * The unknown route remains available when the library is empty or offline.
 */
@Composable
internal fun PlantupiaSpeciesPicker(
    repository: PlantupiaRepository,
    selectedKey: String?,
    selectedName: String,
    enabled: Boolean,
    onSelected: (key: String, scientificName: String) -> Unit,
    onUnknown: () -> Unit,
) {
    var query by remember(repository) { mutableStateOf("") }
    var species by remember(repository) { mutableStateOf<List<PlantupiaLibrarySpecies>>(emptyList()) }
    var busy by remember(repository) { mutableStateOf(false) }
    var unavailable by remember(repository) { mutableStateOf<String?>(null) }
    var retry by remember(repository) { mutableStateOf(0) }

    LaunchedEffect(repository, selectedKey, query, retry) {
        if (selectedKey != null) return@LaunchedEffect
        busy = true
        unavailable = null
        if (query.isNotBlank()) delay(300)
        when (val result = repository.librarySpecies(query)) {
            is ApiResult.Success -> if (repository.isCurrentSession()) species = result.value
            is ApiResult.Failure -> if (repository.isCurrentSession()) {
                species = emptyList()
                unavailable = result.message
            }
        }
        if (repository.isCurrentSession()) busy = false
    }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Which plant do you have?", fontWeight = FontWeight.SemiBold, color = forest, fontSize = 16.sp)
        Text(
            "Choose from reviewed plant types, or add yours without knowing its species.",
            color = quiet, fontSize = 12.sp,
        )
        if (selectedKey != null) {
            Surface(
                color = sage, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(selectedName, color = forest, fontWeight = FontWeight.SemiBold)
                    Text("Based on your selection · Your individual plant is not verified.",
                        color = quiet, fontSize = 12.sp)
                    TextButton(enabled = enabled, onClick = { onUnknown() }) {
                        Text("Change plant type", color = forest)
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(80) },
                label = { Text("Search plant names") },
                singleLine = true,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (unavailable != null) {
                Text("Plant library isn't available right now. You can still add your plant.",
                    color = quiet, fontSize = 12.sp)
                TextButton(enabled = enabled && !busy, onClick = { retry++ }) {
                    Text("Retry library search", color = forest)
                }
            } else if (!busy && species.isEmpty()) {
                Text(
                    if (query.isBlank()) "No reviewed plant types are published yet."
                    else "No matching reviewed plant type found.",
                    color = quiet, fontSize = 12.sp,
                )
            }
            if (!busy && unavailable == null) {
                species.take(10).forEach { item ->
                    Surface(
                        onClick = { onSelected(item.key, item.scientificName) },
                        enabled = enabled,
                        shape = RoundedCornerShape(13.dp),
                        color = Color(0xFFFFFDF7),
                        border = BorderStroke(1.dp, Color(0xFFD4D9CC)),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                modifier = Modifier.width(38.dp).heightIn(min = 38.dp),
                                color = sage, shape = RoundedCornerShape(10.dp),
                            ) {
                                Text("✳", modifier = Modifier.padding(9.dp), color = forest)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.scientificName, color = forest, fontWeight = FontWeight.Medium)
                                item.aliases.firstOrNull()?.let {
                                    Text(it, color = quiet, fontSize = 12.sp)
                                }
                                if (item.reviewedGuideAvailable) Text("Reviewed general guide",
                                    color = quiet, fontSize = 11.sp)
                            }
                            Text("→", color = forest)
                        }
                    }
                }
                if (species.size > 10) Text("More results available — narrow your search.",
                    color = quiet, fontSize = 12.sp)
            }
            TextButton(enabled = enabled, onClick = onUnknown) {
                Text("I don't know the type — add my plant anyway", color = forest)
            }
        }
    }
}
