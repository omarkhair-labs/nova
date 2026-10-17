package com.nova.app.feature.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.PlantupiaCreateIntent
import com.nova.app.feature.plants.PlantupiaCreateMode
import com.nova.app.feature.memories.MemoriesRail
import com.nova.app.feature.pulse.PulseRail
import com.nova.app.feature.rooms.RoomsRail
import com.nova.app.feature.stories.StoriesRail
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType
import kotlinx.coroutines.launch


private data class CreateHubAction(
    val title: String,
    val subtitle: String,
    val icon: NovaIconAsset,
    val onClick: () -> Unit,
)


/** Truthful central Create destination backed only by existing Nova flows. */
@Composable
fun CreateHubScreen(
    displayName: String,
    username: String,
    avatarUrl: String,
    onCreatePost: () -> Unit,
    onOpenReels: () -> Unit,
    onPersonClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    // Private care and public media require different intentional entry points.
    // Nova engines remain functional; secondary formats are opened on demand.
    var activeFormat by remember { mutableStateOf<String?>(null) }
    var showOtherFormats by remember { mutableStateOf(false) }
    var formatOpenRevision by remember { mutableIntStateOf(0) }
    val createListState = rememberLazyListState()
    // Secondary Nova formats render after the existing five primary sections.
    // Repeated taps also return to the live editor if the user has scrolled away.
    LaunchedEffect(formatOpenRevision) {
        if (formatOpenRevision > 0 && activeFormat != null) {
            createListState.animateScrollToItem(5)
        }
    }
    fun openFormat(format: String) {
        activeFormat = format
        formatOpenRevision += 1
    }

    val context = LocalContext.current
    val plantRepository = remember(context) { PlantupiaRepository(context.applicationContext) }
    var livingCover by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(plantRepository) {
        when (val result = plantRepository.plants()) {
            is ApiResult.Success ->
                livingCover = result.value.firstOrNull { it.active && it.coverPhoto.isNotBlank() }?.coverPhoto
            is ApiResult.Failure -> Unit
        }
    }

    fun enterPlant(mode: PlantupiaCreateMode) {
        PlantupiaCreateIntent.request(mode)
        onHomeClick()
    }

    val publicActions = listOf(
        CreateHubAction(
            "Ask about your plant",
            "Choose a plant, a question and the photo you approve for publishing",
            NovaIconAsset.Orbit,
        ) { enterPlant(PlantupiaCreateMode.QUESTION) },
        CreateHubAction(
            "Share an experience",
            "A public photo or story, without sharing your private plant history",
            NovaIconAsset.Create,
            onCreatePost,
        ),
        CreateHubAction(
            "Story",
            "Photo, video or text for 24 hours — public only if you choose",
            NovaIconAsset.Story,
        ) { openFormat("story") },
        CreateHubAction(
            "Video",
            "Upload your own video",
            NovaIconAsset.Reels,
            onOpenReels,
        ),
    )
    val otherFormats = listOf(
        // Retain Nova's engines, but do not turn every format into a separate
        // Plantupia destination until its role in a living-space journey is clear.
        CreateHubAction("Pulse", "A short update for your community", NovaIconAsset.Orbit) { openFormat("pulse") },
        CreateHubAction("Gathering", "Meet around shared interests", NovaIconAsset.Room) { openFormat("gathering") },
        CreateHubAction("Memory", "Revisit moments over time", NovaIconAsset.Memory) { openFormat("memory") },
    )

    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Create,
                onHomeClick = onHomeClick,
                onOrbitClick = onOrbitClick,
                onCreateClick = {},
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = createListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = NovaSpacing.lg,
                end = NovaSpacing.lg,
                top = NovaSpacing.lg,
                bottom = NovaSpacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(NovaSpacing.lg),
        ) {
            item(key = "living-create-heading") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovaSpacing.xs)) {
                        Text("A LIVING RECORD", color = NovaAccent, style = NovaType.micro)
                        Text("Today's moments become tomorrow's history.", color = NovaInk, style = NovaType.screenTitle)
                    }
                    Surface(onClick = onProfileClick, shape = CircleShape, color = NovaAccentSoft) {
                        NovaAvatar(
                            source = avatarUrl,
                            fallbackText = displayName.ifBlank { username },
                            size = 44.dp,
                        )
                    }
                }
            }
            item(key = "living-create-private") {
                Surface(
                    onClick = { enterPlant(PlantupiaCreateMode.OBSERVATION) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = NovaAccent,
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("01 / PRIVATE · YOUR PLANT", color = Color(0xFFBDD4BD), style = NovaType.micro)
                        Text(
                            "Record a moment in your plant's life",
                            color = Color(0xFFF3EEDE),
                            style = NovaType.screenTitle,
                        )
                        Text(
                            "An observation, photo or care action. Everything stays in your private plant history.",
                            color = Color(0xFFD5E5D8),
                            style = NovaType.bodyCompact,
                        )
                        if (!livingCover.isNullOrBlank()) {
                            AsyncImage(
                                model = livingCover,
                                contentDescription = "Real photo of your plants",
                                modifier = Modifier.fillMaxWidth().height(174.dp).clip(RoundedCornerShape(15.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Text("Choose a plant and record →", color = Color(0xFFF3EEDE), style = NovaType.label)
                    }
                }
            }
            item(key = "living-create-new-plant") {
                CreateActionCard(
                    CreateHubAction(
                        title = "Add a plant to your space",
                        subtitle = "A real plant with its photo and placement. If you're new, we'll create a space first.",
                        icon = NovaIconAsset.Create,
                    ) { enterPlant(PlantupiaCreateMode.ADD_PLANT) },
                )
            }
            item(key = "living-create-public") {
                Column(verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
                    Text("02 / THE CIRCLE · SHARING IS OPTIONAL", color = NovaAccent, style = NovaType.micro)
                    Text(
                        "From your space to your community",
                        color = NovaInk,
                        style = NovaType.sectionTitle,
                    )
                    Text(
                        "Your public question, photo or post is separate from your private plant history.",
                        color = NovaMuted,
                        style = NovaType.bodyCompact,
                    )
                    publicActions.forEach { action -> CreateActionCard(action) }
                }
            }
            item(key = "living-create-other") {
                TextButton(onClick = {
                    showOtherFormats = !showOtherFormats
                    if (!showOtherFormats) activeFormat = null
                }) {
                    Text(
                        if (showOtherFormats) "Hide other formats ↑" else "Other social formats (optional) ↓",
                        color = NovaAccent,
                    )
                }
                if (showOtherFormats) {
                    Column(verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
                        otherFormats.forEach { action -> CreateActionCard(action) }
                    }
                }
            }
            if (activeFormat != null) {
                item(key = "living-create-format") {
                    when (activeFormat) {
                        "story" -> {
                            CreateSectionLabel("Story", "Photo, video or text — 24 hours")
                            StoriesRail(
                                displayName = displayName,
                                username = username,
                                avatarUrl = avatarUrl,
                                onSessionExpired = onSessionExpired,
                            )
                        }
                        "pulse" -> {
                            CreateSectionLabel("Pulse", "Short moments")
                            PulseRail(
                                displayName = displayName,
                                username = username,
                                avatarUrl = avatarUrl,
                                onSessionExpired = onSessionExpired,
                            )
                        }
                        "gathering" -> {
                            CreateSectionLabel("Gathering", "Community gatherings")
                            RoomsRail(onPersonClick = onPersonClick, onSessionExpired = onSessionExpired)
                        }
                        "memory" -> {
                            CreateSectionLabel("Memory", "Your memories")
                            MemoriesRail(onPersonClick = onPersonClick, onSessionExpired = onSessionExpired)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun CreateActionCard(action: CreateHubAction) {
    NovaCard(onClick = action.onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(NovaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
        ) {
            Surface(
                shape = CircleShape,
                color = NovaAccentSoft,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                NovaIcon(
                    asset = action.icon,
                    contentDescription = null,
                    tint = NovaAccent,
                    modifier = Modifier.padding(NovaSpacing.sm).size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(action.title, color = NovaInk, style = NovaType.label)
                Text(action.subtitle, color = NovaMuted, style = NovaType.meta)
            }
            NovaIcon(
                asset = NovaIconAsset.Back,
                contentDescription = null,
                tint = NovaMuted,
                modifier = Modifier.size(18.dp).graphicsLayer { scaleX = -1f },
            )
        }
    }
}


@Composable
private fun CreateSectionLabel(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = NovaSpacing.md)) {
        Text(title, color = NovaInk, style = NovaType.sectionTitle)
        Text(subtitle, color = NovaMuted, style = NovaType.meta)
    }
}
