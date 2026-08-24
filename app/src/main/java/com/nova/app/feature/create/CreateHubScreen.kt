package com.nova.app.feature.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.feature.memories.MemoriesRail
import com.nova.app.feature.pulse.PulseRail
import com.nova.app.feature.rooms.RoomsRail
import com.nova.app.feature.stories.StoriesRail
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.components.NovaOrbitRing
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
import com.nova.app.ui.theme.NovaSurface
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun reveal(index: Int) {
        scope.launch { listState.animateScrollToItem(index) }
    }

    val actions = listOf(
        CreateHubAction("Post", "Share a photo and caption", NovaIconAsset.Create, onCreatePost),
        CreateHubAction("Story", "Add a 24-hour moment", NovaIconAsset.Reels) { reveal(2) },
        CreateHubAction("Pulse", "Share what is happening now", NovaIconAsset.Orbit) { reveal(3) },
        CreateHubAction("Room", "Start a place with your people", NovaIconAsset.Inbox) { reveal(4) },
        CreateHubAction("Memory", "Relive a week or make a film", NovaIconAsset.Home) { reveal(5) },
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
            state = listState,
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
            verticalArrangement = Arrangement.spacedBy(NovaSpacing.xl),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Create", color = NovaInk, style = NovaType.pageTitle)
                        Text(
                            "What memory will we make today?",
                            color = NovaMuted,
                            style = NovaType.bodyCompact,
                        )
                    }
                    NovaOrbitRing(
                        modifier = Modifier.size(52.dp),
                        rings = 2,
                        showLivePoint = false,
                    ) {
                        NovaAvatar(
                            source = avatarUrl,
                            fallbackText = displayName.ifBlank { username },
                            size = 38.dp,
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
                    actions.forEach { action ->
                        CreateActionCard(action)
                    }
                    NovaCard(
                        onClick = onOpenReels,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = NovaAccentSoft,
                        borderColor = NovaAccent.copy(alpha = 0.22f),
                    ) {
                        Row(
                            modifier = Modifier.padding(NovaSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NovaAccent,
                            ) {
                                NovaIcon(
                                    asset = NovaIconAsset.Reels,
                                    contentDescription = null,
                                    tint = NovaSurface,
                                    modifier = Modifier.padding(NovaSpacing.sm).size(20.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reel", color = NovaInk, style = NovaType.label)
                                Text(
                                    "Open the real short-video composer",
                                    color = NovaMuted,
                                    style = NovaType.meta,
                                )
                            }
                            Text("Open", color = NovaAccent, style = NovaType.meta)
                        }
                    }
                }
            }

            item {
                CreateSectionLabel("Story", "Photo, video or text - available for 24 hours")
                StoriesRail(
                    displayName = displayName,
                    username = username,
                    avatarUrl = avatarUrl,
                    onSessionExpired = onSessionExpired,
                )
            }

            item {
                CreateSectionLabel("Pulse", "A live or short moment for your orbit")
                PulseRail(
                    displayName = displayName,
                    username = username,
                    avatarUrl = avatarUrl,
                    onSessionExpired = onSessionExpired,
                )
            }

            item {
                CreateSectionLabel("Rooms", "Start or return to a shared place")
                RoomsRail(
                    onPersonClick = onPersonClick,
                    onSessionExpired = onSessionExpired,
                )
            }

            item {
                CreateSectionLabel("Memories", "Relive your week and build a film")
                MemoriesRail(
                    onPersonClick = onPersonClick,
                    onSessionExpired = onSessionExpired,
                )
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
