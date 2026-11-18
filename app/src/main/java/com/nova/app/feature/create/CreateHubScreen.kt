package com.nova.app.feature.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaText
import com.nova.app.feature.plants.PlantupiaCreateIntent
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import com.nova.app.feature.plants.PlantupiaCreateMode
import com.nova.app.feature.memories.MemoriesRail
import com.nova.app.feature.pulse.PulseRail
import com.nova.app.feature.rooms.RoomsRail
import com.nova.app.feature.stories.StoriesRail
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
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


// Four fixed top-level sections precede the optional native social media editor.
internal const val CREATE_FORMAT_ITEM_INDEX = 4

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
    onPersonClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSessionExpired: () -> Unit,
    // Signed-in public identity, shared with Me / Circle / Inbox.
    userId: Long = 0L,
    avatarVariant: Int = -1,
) {
    // Private care and public media require different intentional entry points.
    // Nova engines remain functional; secondary formats are opened on demand.
    var activeFormat by rememberSaveable { mutableStateOf<String?>(null) }
    var showOtherFormats by rememberSaveable { mutableStateOf(false) }
    var formatOpenRevision by remember { mutableIntStateOf(0) }
    val createListState = rememberLazyListState()
    // Secondary Nova formats render after the existing five primary sections.
    // Repeated taps also return to the live editor if the user has scrolled away.
    LaunchedEffect(formatOpenRevision) {
        if (formatOpenRevision > 0 && activeFormat != null) {
            createListState.animateScrollToItem(CREATE_FORMAT_ITEM_INDEX)
        }
    }
    fun openFormat(format: String) {
        activeFormat = format
        formatOpenRevision += 1
    }

    fun enterPlant(mode: PlantupiaCreateMode) {
        PlantupiaCreateIntent.request(mode)
        NovaRootNavigationSignal.request(NovaRootTab.Plants)
    }

    // Public creation is intentional; the inherited infinite Reels feed is not
    // a Plantupia destination. Existing video posts and 24h stories still work.
    val publicActions = listOf(
        CreateHubAction(
            "Ask about your plant",
            "Review the exact question and photo before sharing",
            NovaIconAsset.Orbit,
        ) { enterPlant(PlantupiaCreateMode.QUESTION) },
        CreateHubAction(
            "Explore the Circle",
            "Browse public plant questions and people's experiences",
            NovaIconAsset.Orbit,
            onOrbitClick,
        ),
        CreateHubAction(
            "Share an experience",
            "Publish an experience to your community",
            NovaIconAsset.Create,
            onCreatePost,
        ),
        CreateHubAction(
            "Story",
            "Temporary photo, video or text",
            NovaIconAsset.Story,
        ) { openFormat("story") },
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.nova.app.feature.plants.PlantupiaAccountHeader(displayName, username, avatarUrl, userId, avatarVariant, onProfileClick)
                    Text("A moment worth keeping", style = PlantupiaText.pageHeading)
                    Text("Your plant records stay private. You choose what to share.", style = PlantupiaText.supporting)
                }
            }
            item(key = "living-create-private") {
                CreateIntentGroup(
                    title = "IN YOUR SPACE",
                    hint = "",
                    featuredFirst = true,
                    actions = listOf(
                        CreateHubAction(
                            "Record care or observation",
                            "Choose a plant and record what happened",
                            NovaIconAsset.Edit,
                        ) { enterPlant(PlantupiaCreateMode.OBSERVATION) },
                        CreateHubAction(
                            "Add a plant",
                            "Create a real plant record in your space",
                            NovaIconAsset.Create,
                        ) { enterPlant(PlantupiaCreateMode.ADD_PLANT) },
                        CreateHubAction(
                            "New space",
                            "Choose where your plants live",
                            NovaIconAsset.Home,
                        ) { enterPlant(PlantupiaCreateMode.ADD_SPACE) },
                    ),
                )
            }
            item(key = "living-create-public") {
                CreateIntentGroup(
                    title = "SHARE BY CHOICE",
                    hint = "",
                    actions = publicActions,
                )
            }
            item(key = "living-create-other") {
                Column(verticalArrangement = Arrangement.spacedBy(NovaSpacing.xs)) {
                    TextButton(onClick = {
                        showOtherFormats = !showOtherFormats
                        if (!showOtherFormats) activeFormat = null
                    }) {
                        Text(
                            if (showOtherFormats) "Hide more formats ↑" else "More community formats ↓",
                            color = NovaAccent,
                        )
                    }
                    if (showOtherFormats) {
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
private fun CreateIntentGroup(
    title: String,
    hint: String,
    actions: List<CreateHubAction>,
    featuredFirst: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
    ) {
        Text(title, style = PlantupiaText.sectionLabel)
        if (hint.isNotBlank()) Text(hint, style = PlantupiaText.supporting)
        actions.forEachIndexed { index, action ->
            CreateActionCard(action, featured = featuredFirst && index == 0)
        }
    }
}

@Composable
private fun CreateActionCard(action: CreateHubAction, featured: Boolean = false) {
    val foreground = if (featured) PlantupiaColors.paper else PlantupiaColors.forest
    Surface(
        onClick = action.onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = if (featured) 104.dp else 74.dp),
        shape = RoundedCornerShape(if (featured) 22.dp else 16.dp),
        color = if (featured) PlantupiaColors.forest else PlantupiaColors.paper,
        border = BorderStroke(1.dp, if (featured) PlantupiaColors.forest else PlantupiaColors.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (featured) 19.dp else 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (featured) Color(0xFF315947) else PlantupiaColors.softSage,
            ) {
                NovaIcon(
                    asset = action.icon,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.padding(12.dp).size(if (featured) 27.dp else 22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    action.title,
                    style = if (featured) PlantupiaText.editorialHeading.copy(color = foreground)
                        else PlantupiaText.body.copy(
                            color = foreground,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        ),
                )
                Text(
                    action.subtitle,
                    style = PlantupiaText.supporting.copy(
                        color = if (featured) PlantupiaColors.cream else PlantupiaColors.secondaryText,
                    ),
                )
            }
            NovaIcon(
                asset = NovaIconAsset.Back,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(20.dp).graphicsLayer { scaleX = -1f },
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
