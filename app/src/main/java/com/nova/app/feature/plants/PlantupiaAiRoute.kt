package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaTab

/**
 * AI is an actual native root and a personal-context chooser, not Circle.
 * No plant information is included in a prompt merely because a plant chip
 * is selected. The existing chat asks for per-question consent, resets on
 * each send and sends authorized context only to the protected backend.
 */
@Composable
internal fun PlantupiaAiRoute(
    onToday: () -> Unit,
    onCircle: () -> Unit,
    onCreate: () -> Unit,
    onProfile: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { PlantupiaRepository(context.applicationContext) }
    var plants by remember(repository) { mutableStateOf(emptyList<PlantupiaPlant>()) }
    var plantsUnavailable by remember(repository) { mutableStateOf(false) }
    var selectedPlantId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        when (val response = repository.plants()) {
            is ApiResult.Success -> if (repository.isCurrentSession()) {
                plants = response.value.filter { it.active }
                plantsUnavailable = false
                if (selectedPlantId != null && plants.none { it.id == selectedPlantId }) {
                    selectedPlantId = null
                }
            }
            is ApiResult.Failure -> if (repository.isCurrentSession()) {
                plantsUnavailable = true
            }
        }
    }
    val chosenPlant = plants.firstOrNull { it.id == selectedPlantId }
    Scaffold(
        containerColor = PlantupiaColors.paper,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.AI,
                onHomeClick = onToday,
                onOrbitClick = onCircle,
                onCreateClick = onCreate,
                onProfileClick = onProfile,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ASK · WITH CONTEXT, NOT ASSUMPTIONS", style = PlantupiaText.sectionLabel)
                TextButton(onClick = onProfile) { Text("My profile") }
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = { selectedPlantId = null },
                    modifier = Modifier.heightIn(min = 48.dp),
                    color = if (selectedPlantId == null) PlantupiaColors.forest else PlantupiaColors.softSage,
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "General care",
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                        style = PlantupiaText.body.copy(
                            color = if (selectedPlantId == null) PlantupiaColors.paper
                            else PlantupiaColors.forest,
                        ),
                    )
                }
                plants.forEach { plant ->
                    val chosen = chosenPlant?.id == plant.id
                    Surface(
                        onClick = { selectedPlantId = plant.id },
                        modifier = Modifier.heightIn(min = 48.dp),
                        color = if (chosen) PlantupiaColors.forest else PlantupiaColors.softSage,
                        border = BorderStroke(1.dp, PlantupiaColors.border),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            plant.name,
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                            style = PlantupiaText.body.copy(
                                color = if (chosen) PlantupiaColors.paper
                                else PlantupiaColors.forest,
                            ),
                        )
                    }
                }
            }
            if (plantsUnavailable) {
                Text(
                    "Your plants couldn't load. General questions still work.",
                    modifier = Modifier.padding(horizontal = 17.dp),
                    style = PlantupiaText.supporting,
                )
            } else if (chosenPlant != null) {
                Text(
                    "Choosing a plant does not share its history. Approve context for each question.",
                    modifier = Modifier.padding(horizontal = 17.dp),
                    style = PlantupiaText.supporting,
                )
            }
            // Recreate chat state and cancel an old in-flight request on scope
            // switch, never carry another plant's private conversation across.
            key(chosenPlant?.id) {
                PlantupiaAssistantSheet(
                    plant = chosenPlant,
                    repository = repository,
                    onDismiss = onToday,
                    fullPage = true,
                    onCircle = onCircle,
                )
            }
        }
    }
}
