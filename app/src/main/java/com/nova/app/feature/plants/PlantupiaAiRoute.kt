package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
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
import com.nova.app.feature.auth.domain.model.NovaUser
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
    user: NovaUser?,
    onToday: () -> Unit,
    onCircle: () -> Unit,
    onCreate: () -> Unit,
    onProfile: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context, user?.id) { PlantupiaRepository(context.applicationContext) }
    var plants by remember(repository) { mutableStateOf(emptyList<PlantupiaPlant>()) }
    var plantsUnavailable by remember(repository) { mutableStateOf(false) }
    var selectedPlantId by rememberSaveable(user?.id) { mutableStateOf<String?>(null) }
    var plantQuery by remember(repository) { mutableStateOf("") }
    var retry by remember(repository) { mutableStateOf(0) }
    var loadingPlants by remember(repository) { mutableStateOf(true) }

    LaunchedEffect(repository, retry) {
        loadingPlants = true
        plantsUnavailable = false
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
        if (repository.isCurrentSession()) loadingPlants = false
    }
    val chosenPlant = plants.firstOrNull { it.id == selectedPlantId }
    val visiblePlants = plants.filter { plant ->
        plantQuery.isBlank() || listOf(plant.name, plant.species, plant.placement)
            .any { it.contains(plantQuery.trim(), ignoreCase = true) }
    }
    Scaffold(
        containerColor = PlantupiaColors.cream,
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
            Column(
                modifier = Modifier.padding(horizontal = 17.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                PlantupiaAccountHeader(
                    user?.name.orEmpty(), user?.username.orEmpty(), user?.avatarUrl.orEmpty(),
                    user?.id ?: 0L, user?.plantupiaAvatarVariant ?: -1,
                    onProfile = onProfile, compactBrand = true,
                )
                Text("ASK PLANTUPIA / PRIVATE BY DEFAULT", style = PlantupiaText.sectionLabel)
                Text("Start with a plant, or don't.", style = PlantupiaText.pageHeading)
                Text(
                    "General questions stay general. Choose a plant only when its real context would help.",
                    style = PlantupiaText.supporting,
                )
            }
            if (loadingPlants) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (plants.size >= 8) {
                OutlinedTextField(
                    value = plantQuery, onValueChange = { plantQuery = it.take(80) },
                    label = { Text("Find a plant for context") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp),
                )
                if (visiblePlants.isEmpty()) Text("No matching plants. General questions are still available.",
                    modifier = Modifier.padding(horizontal = 17.dp), style = PlantupiaText.supporting)
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Surface(
                        onClick = { selectedPlantId = null },
                        modifier = Modifier.heightIn(min = 58.dp).widthIn(min = 138.dp),
                        color = if (selectedPlantId == null) PlantupiaColors.forest else PlantupiaColors.paper,
                        border = BorderStroke(1.dp, if (selectedPlantId == null) PlantupiaColors.forest else PlantupiaColors.border),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(Modifier.padding(horizontal = 15.dp, vertical = 10.dp)) {
                            Text(
                                "GENERAL",
                                style = PlantupiaText.sectionLabel.copy(
                                    color = if (selectedPlantId == null) PlantupiaColors.sage else PlantupiaColors.forest,
                                ),
                            )
                            Text(
                                "No plant context",
                                style = PlantupiaText.body.copy(
                                    color = if (selectedPlantId == null) PlantupiaColors.paper else PlantupiaColors.forestText,
                                ),
                            )
                        }
                    }
                }
                items(visiblePlants, key = { it.id }) { plant ->
                    val chosen = chosenPlant?.id == plant.id
                    Surface(
                        onClick = { selectedPlantId = plant.id },
                        modifier = Modifier.heightIn(min = 58.dp).widthIn(min = 154.dp),
                        color = if (chosen) PlantupiaColors.forest else PlantupiaColors.paper,
                        border = BorderStroke(1.dp, if (chosen) PlantupiaColors.forest else PlantupiaColors.border),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            PlantupiaEvidencePhoto(
                                photoUrl = plant.coverPhoto,
                                photoDescription = plant.name,
                                modifier = Modifier.size(40.dp),
                                compact = true,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "PLANT",
                                    style = PlantupiaText.sectionLabel.copy(
                                        color = if (chosen) PlantupiaColors.sage else PlantupiaColors.forest,
                                    ),
                                )
                                Text(
                                    plant.name,
                                    maxLines = 1,
                                    style = PlantupiaText.body.copy(
                                        color = if (chosen) PlantupiaColors.paper else PlantupiaColors.forestText,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            if (plantsUnavailable) {
                Text(
                    "Your plants couldn't load. General questions still work.",
                    modifier = Modifier.padding(horizontal = 17.dp),
                    style = PlantupiaText.supporting,
                )
                TextButton(onClick = { retry++ }, modifier = Modifier.padding(horizontal = 17.dp)) {
                    Text("Retry loading plants")
                }
            } else if (chosenPlant != null) {
                Text(
                    "Choose whether to share this plant’s context during the chat. You can switch it off anytime.",
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
