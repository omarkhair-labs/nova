package com.nova.app.feature.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nova.app.ui.components.NovaBackButton
import com.nova.app.ui.components.NovaIconButton
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaTab
import com.nova.app.feature.auth.domain.model.NovaUser

@Composable
fun PlantupiaPlantsRoute(
    user: NovaUser?,
    accountId: Long,
    onCreatePost: () -> Unit,
    onOpenLinkedPost: (Long, () -> Boolean) -> Unit,
    onToday: () -> Unit,
    onCircle: () -> Unit,
    onCreate: () -> Unit,
    onProfile: () -> Unit,
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var inventoryOffset by rememberSaveable { mutableIntStateOf(0) }
    var showingCareCalendar by rememberSaveable(accountId) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    BackHandler(enabled = showingCareCalendar) { showingCareCalendar = false }

    // Opening an entry from the calendar must reveal the real Plant Detail,
    // not leave the user trapped behind an independent calendar overlay.
    val pendingPlant = PlantupiaPlantOpenSignal.target
    LaunchedEffect(pendingPlant) {
        if (showingCareCalendar && pendingPlant?.first == accountId) {
            showingCareCalendar = false
        }
    }

    Scaffold(
        containerColor = PlantupiaColors.cream,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Plants,
                onHomeClick = onToday,
                onOrbitClick = onCircle,
                onCreateClick = onCreate,
                onProfileClick = onProfile,
            )
        },
    ) { padding ->
        if (showingCareCalendar) {
            // A dedicated full-screen destination with its own title and Back,
            // not a collapsible section underneath the My Plants feed.
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding()
                    .background(PlantupiaColors.cream),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NovaBackButton(onClick = { showingCareCalendar = false })
                    Text("My care schedule", style = PlantupiaText.editorialHeading)
                }
                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 17.dp, vertical = 4.dp),
                ) {
                    PlantupiaCareActivity(accountId)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding()
                    .verticalScroll(scroll)
                    .background(PlantupiaColors.cream)
                    .padding(horizontal = 17.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlantupiaAccountHeader(
                    user?.name.orEmpty(), user?.username.orEmpty(), user?.avatarUrl.orEmpty(),
                    accountId, user?.plantupiaAvatarVariant ?: -1, onProfile,
                )
                // The calendar is a top-level contextual icon beside My Plants,
                // not another bottom navigation tab or a large inline CTA.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("MY PLANTS", style = PlantupiaText.sectionLabel)
                    NovaIconButton(
                        asset = NovaIconAsset.CareCalendar,
                        contentDescription = "Open care calendar",
                        onClick = {
                            showingCareCalendar = true
                            scope.launch { scroll.scrollTo(0) }
                        },
                        containerColor = PlantupiaColors.paper,
                        contentColor = PlantupiaColors.forest,
                        borderColor = PlantupiaColors.border,
                    )
                }
                PlantupiaClientSection(
                    accountId = accountId,
                    onCommunityCreateClick = onCreatePost,
                    onOpenLinkedPost = onOpenLinkedPost,
                    section = PlantupiaPrimarySection.PLANTS,
                    onOpenPlant = {
                        inventoryOffset = scroll.value
                        scope.launch { scroll.animateScrollTo(0) }
                    },
                    onClosePlant = {
                        scope.launch {
                            androidx.compose.runtime.withFrameNanos { }
                            scroll.scrollTo(inventoryOffset)
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun PlantupiaShopRoute(
    user: NovaUser?,
    onToday: () -> Unit,
    onCircle: () -> Unit,
    onCreate: () -> Unit,
    onProfile: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { PlantupiaRepository(context.applicationContext) }
    Scaffold(
        containerColor = PlantupiaColors.paper,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Shop,
                onHomeClick = onToday,
                onOrbitClick = onCircle,
                onCreateClick = onCreate,
                onProfileClick = onProfile,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 12.dp)) {
            PlantupiaAccountHeader(user?.name.orEmpty(), user?.username.orEmpty(), user?.avatarUrl.orEmpty(),
                user?.id ?: 0L, user?.plantupiaAvatarVariant ?: -1, onProfile)
        }
        PlantupiaStoreShop(
            repository = repository,
            accountId = user?.id ?: 0L,
            onDismiss = onToday,
            modifier = Modifier.weight(1f),
        )
        }
    }
}
