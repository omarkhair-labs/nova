package com.nova.app.feature.orbit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.app.appContainer
import com.nova.app.feature.orbit.domain.model.OrbitEvent
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.components.NovaEmptyState
import com.nova.app.ui.components.NovaInlineLoading
import com.nova.app.ui.components.NovaInlineRetry
import com.nova.app.ui.components.NovaLoadingState
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


private enum class OrbitFilter(val label: String) {
    All("All"),
    Posts("Posts"),
    People("People"),
    Pulse("Pulse"),
}


@Composable
fun OrbitScreen(
    displayName: String,
    username: String,
    avatarUrl: String,
    onPersonClick: (String) -> Unit,
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
    var selectedFilter by remember { mutableStateOf(OrbitFilter.All) }
    var selectedUsername by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(owner) { owner.load(showSpinner = true) }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    val filteredEvents = remember(state.events, selectedFilter) {
        state.events.filter { event -> selectedFilter.accepts(event) }
    }

    Scaffold(
        containerColor = NovaBackground,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Your Orbit", color = NovaInk, style = NovaType.pageTitle)
                        Text(
                            "Close connections, in real time.",
                            color = NovaMuted,
                            style = NovaType.bodyCompact,
                        )
                    }
                    Surface(
                        onClick = onDiscoveryClick,
                        shape = CircleShape,
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        NovaIcon(
                            asset = NovaIconAsset.Search,
                            contentDescription = "Discover people",
                            tint = NovaInk,
                            modifier = Modifier.padding(NovaSpacing.md).size(21.dp),
                        )
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
                    items(OrbitFilter.entries) { filter ->
                        val selected = filter == selectedFilter
                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = CircleShape,
                            color = if (selected) NovaAccent else NovaSurface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) NovaAccent else NovaBorder,
                            ),
                        ) {
                            Text(
                                text = filter.label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (selected) NovaSurface else NovaMuted,
                                style = NovaType.meta.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }
                }
            }

            if (state.events.isNotEmpty()) {
                item {
                    OrbitConstellation(
                        displayName = displayName,
                        username = username,
                        avatarUrl = avatarUrl,
                        events = state.events,
                        onPersonClick = { selectedUsername = it },
                    )
                }
            }

            when {
                state.loading && state.events.isEmpty() -> item {
                    NovaLoadingState(message = "Waking your orbit…")
                }

                state.events.isEmpty() -> item {
                    NovaEmptyState(
                        title = "Your orbit is quiet",
                        message = "Discover people and their real activity will gather here.",
                        actionLabel = "Discover people",
                        onAction = onDiscoveryClick,
                    )
                }

                filteredEvents.isEmpty() -> item {
                    NovaEmptyState(
                        title = "Nothing in ${selectedFilter.label.lowercase()} yet",
                        message = "Your live relationships will appear here as they move.",
                    )
                }

                else -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Around you", color = NovaInk, style = NovaType.sectionTitle)
                            Text(
                                if (state.loading) "Refreshing…" else "${filteredEvents.size} moments",
                                color = NovaMuted,
                                style = NovaType.micro,
                            )
                        }
                    }
                    items(filteredEvents, key = { it.id }) { event ->
                        OrbitActivityCard(event = event, onClick = { selectedUsername = event.actor.username })
                    }
                    if (state.nextCursor != null) {
                        item {
                            if (state.loadingMore) {
                                NovaInlineLoading(message = "Loading more orbit activity…")
                            } else {
                                Surface(
                                    onClick = owner::loadMore,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = androidx.compose.material3.MaterialTheme.shapes.medium,
                                    color = NovaAccentSoft,
                                ) {
                                    Text(
                                        "Show more",
                                        modifier = Modifier.padding(NovaSpacing.md),
                                        color = NovaAccent,
                                        style = NovaType.label,
                                    )
                                }
                            }
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

    selectedUsername?.let { selected ->
        val personEvents = state.events.filter { it.actor.username == selected }
        personEvents.firstOrNull()?.actor?.let { actor ->
            OrbitPersonDetailDialog(
                name = actor.name.ifBlank { actor.username },
                username = actor.username,
                avatarUrl = actor.avatarUrl,
                events = personEvents,
                onDismiss = { selectedUsername = null },
                onOpenProfile = {
                    selectedUsername = null
                    onPersonClick(actor.username)
                },
            )
        }
    }
}


@Composable
private fun OrbitPersonDetailDialog(
    name: String,
    username: String,
    avatarUrl: String,
    events: List<OrbitEvent>,
    onDismiss: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val night = Color(0xFF080B17)
    val panel = Color(0xFF12172A)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(night).statusBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onDismiss, shape = CircleShape, color = panel) {
                    Text("‹", modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("Orbit Activity", color = Color.White, style = NovaType.sectionTitle)
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(42.dp))
            }
            NovaOrbitRing(modifier = Modifier.size(210.dp), rings = 4, showLivePoint = events.isNotEmpty()) {
                NovaAvatar(source = avatarUrl, fallbackText = name, size = 92.dp)
            }
            Text(name, color = Color.White, style = NovaType.pageTitle)
            Text("@$username · ${events.size} live moments", color = Color(0xFFB8C0D8), style = NovaType.bodyCompact)
            Surface(
                onClick = onOpenProfile,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.material3.MaterialTheme.shapes.medium,
                color = NovaAccent,
            ) {
                Text(
                    "Open profile · message or call",
                    modifier = Modifier.padding(13.dp),
                    color = Color.White,
                    style = NovaType.label,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(events, key = { it.id }) { event ->
                    Surface(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.material3.MaterialTheme.shapes.medium, color = panel) {
                        Column(Modifier.padding(14.dp)) {
                            Text(orbitActionText(event), color = Color.White, style = NovaType.bodyCompact)
                            orbitContextText(event)?.let { Text(it, color = Color(0xFF9AA5C4), style = NovaType.micro) }
                            if (event.pulse != null) {
                                Text("LIVE IN YOUR ORBIT", color = Color(0xFF22D3EE), style = NovaType.badge)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun OrbitConstellation(
    displayName: String,
    username: String,
    avatarUrl: String,
    events: List<OrbitEvent>,
    onPersonClick: (String) -> Unit,
) {
    val people = events.map { it.actor }.distinctBy { it.id }.take(4)
    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = NovaAccentSoft.copy(alpha = 0.42f),
        borderColor = NovaAccent.copy(alpha = 0.18f),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(238.dp),
            contentAlignment = Alignment.Center,
        ) {
            NovaOrbitRing(
                modifier = Modifier.size(218.dp),
                rings = 4,
                showLivePoint = people.isNotEmpty(),
            ) {
                NovaAvatar(
                    source = avatarUrl,
                    fallbackText = displayName.ifBlank { username },
                    size = 74.dp,
                )
            }
            people.getOrNull(0)?.let { person ->
                OrbitPersonNode(person.name, person.username, person.avatarUrl, onPersonClick, Modifier.align(Alignment.TopCenter).offset(y = 17.dp))
            }
            people.getOrNull(1)?.let { person ->
                OrbitPersonNode(person.name, person.username, person.avatarUrl, onPersonClick, Modifier.align(Alignment.CenterEnd).offset(x = (-17).dp))
            }
            people.getOrNull(2)?.let { person ->
                OrbitPersonNode(person.name, person.username, person.avatarUrl, onPersonClick, Modifier.align(Alignment.BottomCenter).offset(y = (-17).dp))
            }
            people.getOrNull(3)?.let { person ->
                OrbitPersonNode(person.name, person.username, person.avatarUrl, onPersonClick, Modifier.align(Alignment.CenterStart).offset(x = 17.dp))
            }
        }
    }
}


@Composable
private fun OrbitPersonNode(
    name: String,
    username: String,
    avatarUrl: String,
    onPersonClick: (String) -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = { onPersonClick(username) },
        modifier = modifier.size(52.dp),
        shape = CircleShape,
        color = NovaSurface,
        border = BorderStroke(2.dp, NovaAccent.copy(alpha = 0.75f)),
    ) {
        NovaAvatar(source = avatarUrl, fallbackText = name.ifBlank { username }, size = 48.dp)
    }
}


@Composable
private fun OrbitActivityCard(event: OrbitEvent, onClick: () -> Unit) {
    NovaCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(NovaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
        ) {
            NovaAvatar(
                source = event.actor.avatarUrl,
                fallbackText = event.actor.name.ifBlank { event.actor.username },
                size = 48.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.actor.name.ifBlank { event.actor.username },
                    color = NovaInk,
                    style = NovaType.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    orbitActionText(event),
                    color = NovaMuted,
                    style = NovaType.bodyCompact,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                orbitContextText(event)?.let { context ->
                    Spacer(modifier = Modifier.height(NovaSpacing.xs))
                    Text(context, color = NovaMuted, style = NovaType.micro, maxLines = 1)
                }
            }
            Text(orbitSymbol(event.kind), color = NovaAccent, style = NovaType.sectionTitle)
        }
    }
}


private fun OrbitFilter.accepts(event: OrbitEvent): Boolean = when (this) {
    OrbitFilter.All -> true
    OrbitFilter.Posts -> event.kind in setOf("like", "comment", "repost")
    OrbitFilter.People -> event.kind == "follow"
    OrbitFilter.Pulse -> event.kind == "pulse_reply" || event.pulse != null
}
