package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.auth.domain.model.NovaUser
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaTab

/** Data and conversation lifetime stay here; the presentation has no repository.
 * Selecting a plant does not grant permission to share its context.
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
    var retry by remember(repository) { mutableStateOf(0) }
    var loadingPlants by remember(repository) { mutableStateOf(true) }
    var showPicker by rememberSaveable(user?.id) { mutableStateOf(false) }
    var query by remember(repository) { mutableStateOf("") }

    LaunchedEffect(repository, retry) {
        loadingPlants = true
        plantsUnavailable = false
        when (val response = repository.plants()) {
            is ApiResult.Success -> if (repository.isCurrentSession()) {
                plants = response.value.filter { it.active }
                if (selectedPlantId != null && plants.none { it.id == selectedPlantId }) selectedPlantId = null
            }
            is ApiResult.Failure -> if (repository.isCurrentSession()) {
                plantsUnavailable = true
            }
        }
        if (repository.isCurrentSession()) loadingPlants = false
    }

    val selected = plants.firstOrNull { it.id == selectedPlantId }
    val visiblePlants = plants.filter { plant ->
        query.isBlank() || listOf(plant.name, plant.species, plant.placement)
            .any { it.contains(query.trim(), ignoreCase = true) }
    }
    fun closePicker() {
        showPicker = false
        query = ""
    }

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
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .statusBarsPadding().imePadding()) {
            PlantupiaAskPresentation(
                user = user,
                selected = selected,
                onProfile = onProfile,
                onChoosePlant = { showPicker = true },
            ) {
                // A different account or plant cancels the prior chat and consent.
                key(repository, selected?.id) {
                    PlantupiaAssistantSheet(
                        plant = selected,
                        repository = repository,
                        onDismiss = onToday,
                        fullPage = true,
                        onOpenHistory = if (selected == null || user == null) null else ({
                            if (repository.isCurrentSession()) {
                                PlantupiaPlantOpenSignal.open(user.id, selected.id, PlantupiaDetailChapter.HISTORY)
                            }
                        }),
                        onCircle = onCircle,
                    )
                }
            }
        }
    }
    if (showPicker) {
        PlantupiaAskPlantPicker(
            plants = visiblePlants,
            selectedId = selected?.id,
            showSearch = plants.size >= 8 || query.isNotBlank(),
            query = query,
            onQuery = { query = it.take(80) },
            loading = loadingPlants,
            unavailable = plantsUnavailable,
            onRetry = { retry++ },
            onChoose = { selectedPlantId = it; closePicker() },
            onDismiss = { closePicker() },
        )
    }
}
