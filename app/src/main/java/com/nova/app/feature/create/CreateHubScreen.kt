package com.nova.app.feature.create

import com.nova.app.feature.plants.PlantupiaActionRow
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
import com.nova.app.feature.plants.PlantupiaEditionArt
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
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
    // Secondary community formats render after the primary living-space actions.
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
            NovaIconAsset.Comment,
        ) { enterPlant(PlantupiaCreateMode.QUESTION) },
        CreateHubAction(
            "Explore the Circle",
            "",
            NovaIconAsset.People,
            onOrbitClick,
        ),
        CreateHubAction(
            "Share an experience",
            "",
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
        // Existing social engines stay available without becoming Plantupia's
        // primary creation language.
        CreateHubAction("Short update", "A brief community moment", NovaIconAsset.Edit) { openFormat("pulse") },
        CreateHubAction("Gathering", "Meet around a shared interest", NovaIconAsset.Room) { openFormat("gathering") },
        CreateHubAction("Memory", "Return to something you chose to keep", NovaIconAsset.Memory) { openFormat("memory") },
    )

    Scaffold(
        containerColor = PlantupiaColors.cream,
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
                    Text("Create", style = PlantupiaText.pageHeading)
                }
            }
            item(key = "living-create-private") {
                CreateIntentGroup(
                    title = "Private",
                    hint = "",
                    featuredFirst = true,
                    actions = listOf(
                        CreateHubAction(
                            "Record care or observation",
                            "",
                            NovaIconAsset.Edit,
                        ) { enterPlant(PlantupiaCreateMode.OBSERVATION) },
                        CreateHubAction(
                            "Add a plant",
                            "",
                            NovaIconAsset.Create,
                        ) { enterPlant(PlantupiaCreateMode.ADD_PLANT) },
                        CreateHubAction(
                            "New space",
                            "",
                            NovaIconAsset.Home,
                        ) { enterPlant(PlantupiaCreateMode.ADD_SPACE) },
                    ),
                )
            }
            item(key = "living-create-public") {
                CreateIntentGroup(
                    title = "Community",
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
                            CreateSectionLabel("Short update", "Brief community moment")
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = PlantupiaText.itemTitle)
        if (hint.isNotBlank()) Text(hint, style = PlantupiaText.supporting)
        actions.forEachIndexed { index, action ->
            if (index > 0) HorizontalDivider(color = PlantupiaColors.border)
            CreateActionCard(action, featured = featuredFirst && index == 0)
        }
    }
}

@Composable
private fun CreateActionCard(action: CreateHubAction, featured: Boolean = false) {
    PlantupiaActionRow(icon = action.icon, title = action.title,
        detail = action.subtitle.ifBlank { null }, onClick = action.onClick, accent = featured)
}

@Composable
private fun CreateSectionLabel(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = NovaSpacing.md)) {
        Text(title.uppercase(), style = PlantupiaText.sectionLabel)
        Text(subtitle, style = PlantupiaText.supporting)
    }
}
