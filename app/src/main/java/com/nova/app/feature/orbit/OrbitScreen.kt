package com.nova.app.feature.orbit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.app.appContainer
import com.nova.app.feature.orbit.domain.model.OrbitEvent
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaInlineLoading
import com.nova.app.ui.components.NovaInlineRetry
import com.nova.app.ui.components.NovaLoadingState
import com.nova.app.ui.components.NovaMediaImage
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
import com.nova.app.feature.plants.PlantupiaCircleEditorialIntro
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaCirclePanel
import com.nova.app.feature.plants.PlantupiaCreateIntent
import com.nova.app.feature.plants.PlantupiaCreateMode
import com.nova.app.feature.stories.StoriesRail
import com.nova.app.core.messaging.NovaMessagesSignal
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.ui.components.NovaUnreadDot
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime


private enum class CircleSection(val label: String) {
    PlantQuestions("Plant questions"),
    PeopleActivity("People & moments"),
}

private enum class CircleFilter(val label: String) {
    All("All"),
    Moments("Shared moments"),
    People("People"),
}


@Composable
fun OrbitScreen(
    displayName: String,
    username: String,
    avatarUrl: String,
    userId: Long = 0L,
    avatarVariant: Int = -1,
    onSharedMoments: () -> Unit = {},
    onPersonClick: (String) -> Unit,
    onPostClick: (Long) -> Unit,
    onDiscoveryClick: () -> Unit,
    onHomeClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.orbitRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { OrbitStateOwner(repository, scope) }
    val state = owner.state
    var selectedSection by rememberSaveable { mutableStateOf(CircleSection.PlantQuestions) }
    var selectedFilter by rememberSaveable { mutableStateOf(CircleFilter.All) }

    LaunchedEffect(owner, selectedSection) {
        if (selectedSection == CircleSection.PeopleActivity) owner.load(showSpinner = true)
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    val filteredEvents = remember(state.events, selectedFilter) {
        state.events.filter { event -> selectedFilter.accepts(event) }
    }

    Scaffold(
        containerColor = PlantupiaColors.cream,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Orbit,
                onHomeClick = onHomeClick,
                onOrbitClick = {},
                onCreateClick = onCreateClick,
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        LazyColumn(
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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    com.nova.app.feature.plants.PlantupiaAccountHeader(displayName, username, avatarUrl, userId, avatarVariant, onProfileClick)
                    PlantupiaCircleEditorialIntro()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircleDestinationLink("Shared moments", "What people chose to publish", onSharedMoments, Modifier.weight(1f))
                        CircleDestinationLink("People", "Find and follow intentionally", onDiscoveryClick, Modifier.weight(1f))
                    }
                }
            }

            item(key = "circle-content-navigation") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
                ) {
                    CircleSection.entries.forEach { section ->
                        val isSelected = selectedSection == section
                        Surface(
                            onClick = { selectedSection = section },
                            modifier = Modifier.weight(1f),
                            shape = com.nova.app.feature.plants.PlantupiaShapes.badge,
                            color = if (isSelected) PlantupiaColors.forest else PlantupiaColors.paper,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) PlantupiaColors.forest else PlantupiaColors.border,
                            ),
                        ) {
                            Text(
                                text = section.label,
                                modifier = Modifier.padding(horizontal = NovaSpacing.sm, vertical = 12.dp),
                                color = if (isSelected) PlantupiaColors.cream else PlantupiaColors.forest,
                                style = com.nova.app.feature.plants.PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }
                }
            }

            if (selectedSection == CircleSection.PlantQuestions) {
                // Human help starts at a real plant. No source care entry is
                // shared until the owner previews exact public text and photo.
                item(key = "circle-start-your-own-question") {
                    Surface(
                        onClick = {
                            PlantupiaCreateIntent.request(PlantupiaCreateMode.QUESTION)
                            onHomeClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = PlantupiaColors.softSage,
                        shape = com.nova.app.feature.plants.PlantupiaShapes.card,
                    ) {
                        Row(
                            modifier = Modifier.padding(NovaSpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("ASK FROM A PLANT", style = com.nova.app.feature.plants.PlantupiaText.sectionLabel)
                                Text("Ask the Circle", style = com.nova.app.feature.plants.PlantupiaText.editorialHeading)
                                Text("Choose a plant, then review exactly what becomes public.", style = com.nova.app.feature.plants.PlantupiaText.supporting)
                            }
                            NovaIcon(
                                asset = NovaIconAsset.Create,
                                contentDescription = null,
                                tint = PlantupiaColors.forest,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
                item(key = "circle-real-plant-questions") {
                    PlantupiaCirclePanel(onOpenPost = onPostClick)
                }
            } else {
            // Real Nova Stories are rendered through their existing authenticated
            // StoriesRepository, upload/viewer/audience/media engine, not demo content.
            // A Story is public and temporary; it never becomes a private care event.
            item(key = "plantupia-real-stories") {
                StoriesRail(
                    displayName = displayName,
                    username = username,
                    avatarUrl = avatarUrl,
                    onSessionExpired = onSessionExpired,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
                    items(CircleFilter.entries) { filter ->
                        val selected = filter == selectedFilter
                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = CircleShape,
                            color = if (selected) PlantupiaColors.forest else PlantupiaColors.paper,
                            border = BorderStroke(
                                1.dp,
                                if (selected) PlantupiaColors.forest else PlantupiaColors.border,
                            ),
                        ) {
                            Text(
                                text = filter.label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (selected) PlantupiaColors.cream else PlantupiaColors.forestText,
                                style = com.nova.app.feature.plants.PlantupiaText.supporting.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }
                }
            }

            when {
                state.loading && state.events.isEmpty() -> item {
                    NovaLoadingState(message = "Loading your Circle...")
                }

                state.events.isEmpty() -> item {
                    NovaEmptyState(
                        title = "Your circle is quiet",
                        message = "People you follow and activity they share will appear here.",
                        actionLabel = "Discover people",
                        onAction = onDiscoveryClick,
                    )
                }

                filteredEvents.isEmpty() && state.nextCursor != null -> item {
                    LaunchedEffect(selectedFilter, state.nextCursor) { owner.loadMore() }
                    NovaInlineLoading(message = "Loading more Circle activity...")
                }

                filteredEvents.isEmpty() -> item {
                    NovaEmptyState(
                        title = "Nothing in ${selectedFilter.label.lowercase()} yet",
                        message = "This view will fill as real Circle activity happens.",
                    )
                }

                else -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Circle activity", style = com.nova.app.feature.plants.PlantupiaText.editorialHeading)
                            Text(
                                if (state.loading) "Refreshing..." else "${filteredEvents.size} updates",
                                color = NovaMuted,
                                style = NovaType.micro,
                            )
                        }
                    }
                    items(filteredEvents, key = { it.id }) { event ->
                        CircleActivityRow(
                            event = event,
                            onActorClick = { onPersonClick(event.actor.username) },
                            onClick = {
                                when {
                                    event.post != null -> onPostClick(event.post.id)
                                    event.kind == "follow" -> onPersonClick(
                                        event.person?.username ?: event.actor.username,
                                    )
                                    else -> onPersonClick(event.actor.username)
                                }
                            },
                        )
                    }
                    if (state.nextCursor != null) {
                        item(key = "circle-next-${state.nextCursor}") {
                            LaunchedEffect(state.nextCursor) { owner.loadMore() }
                            NovaInlineLoading(message = "Loading more Circle activity...")
                        }
                    }
                }
            }

            state.error?.let { error ->
                item {
                    NovaInlineRetry(
                        message = error,
                        onRetry = {
                            owner.clearError()
                            owner.load(showSpinner = state.events.isEmpty())
                        },
                    )
                }
            }
            }
        }
    }

}


@Composable
private fun CircleDestinationLink(
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = PlantupiaColors.paper,
        border = BorderStroke(1.dp, PlantupiaColors.border),
        shape = com.nova.app.feature.plants.PlantupiaShapes.inset,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = com.nova.app.feature.plants.PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
            Text(body, style = com.nova.app.feature.plants.PlantupiaText.supporting)
        }
    }
}


@Composable
private fun CircleActivityRow(
    event: OrbitEvent,
    onActorClick: () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(vertical = NovaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
        ) {
            Box(modifier = Modifier.size(48.dp).clickable(onClick = onActorClick)) {
                NovaAvatar(
                    source = event.actor.avatarUrl,
                    fallbackText = event.actor.name.ifBlank { event.actor.username },
                    size = 48.dp,
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(20.dp),
                    shape = CircleShape,
                    color = PlantupiaColors.softSage,
                    border = BorderStroke(1.dp, PlantupiaColors.cream),
                ) {
                    NovaIcon(
                        asset = orbitIcon(event.kind),
                        contentDescription = null,
                        modifier = Modifier.padding(4.dp),
                        tint = PlantupiaColors.forest,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        event.actor.name.ifBlank { event.actor.username },
                        color = PlantupiaColors.forestText,
                        style = com.nova.app.feature.plants.PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        orbitRelativeTime(event.createdAt),
                        color = PlantupiaColors.secondaryText,
                        style = com.nova.app.feature.plants.PlantupiaText.sectionLabel,
                    )
                }
                Text(
                    orbitActionText(event),
                    color = PlantupiaColors.secondaryText,
                    style = com.nova.app.feature.plants.PlantupiaText.supporting,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                orbitContextText(event)?.let { context ->
                    Spacer(modifier = Modifier.height(NovaSpacing.xs))
                    Text(context, color = PlantupiaColors.secondaryText, style = com.nova.app.feature.plants.PlantupiaText.supporting, maxLines = 1)
                }
            }
            val preview = event.post?.previewUrl?.takeIf(String::isNotBlank)
                ?: event.pulse?.previewUrl?.takeIf(String::isNotBlank)
            if (preview != null) {
                NovaMediaImage(
                    source = preview,
                    modifier = Modifier.size(54.dp),
                    contentDescription = "Open Circle activity media",
                )
            } else {
                NovaIcon(
                    asset = NovaIconAsset.Back,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).graphicsLayer { scaleX = -1f },
                    tint = PlantupiaColors.secondaryText,
                )
            }
        }
        HorizontalDivider(color = PlantupiaColors.border.copy(alpha = 0.82f))
    }
}


private fun CircleFilter.accepts(event: OrbitEvent): Boolean = when (this) {
    CircleFilter.All -> true
    CircleFilter.Moments -> event.post != null || event.pulse != null || event.kind in setOf("like", "comment", "repost", "pulse_reply")
    CircleFilter.People -> event.kind == "follow"
}


internal fun orbitRelativeTime(
    value: String,
    now: Instant = Instant.now(),
): String {
    val instant = runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull() ?: return ""
    val elapsed = Duration.between(instant, now).coerceAtLeast(Duration.ZERO)
    return when {
        elapsed.toMinutes() < 1 -> "now"
        elapsed.toHours() < 1 -> "${elapsed.toMinutes()}m"
        elapsed.toDays() < 1 -> "${elapsed.toHours()}h"
        elapsed.toDays() < 7 -> "${elapsed.toDays()}d"
        else -> "${elapsed.toDays() / 7}w"
    }
}
