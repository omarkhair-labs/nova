package com.nova.app.feature.memories

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.app.appContainer
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMotion
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType
import java.util.TimeZone


@Composable
fun MemoriesRail(
    onPersonClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.memoryRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { MemoryStateOwner(repository, scope) }
    val state = owner.state
    var showMemory by remember { mutableStateOf(false) }
    var showFilm by remember { mutableStateOf(false) }
    val ready = (state.memory?.stats?.highlights ?: 0) > 0
    val memoryPalette = MemoryTheme.ready

    LaunchedEffect(owner) {
        owner.load(
            utcOffsetMinutes = railUtcOffsetMinutes(),
            weeksAgo = 0,
            showSpinner = true,
        )
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    Column(
        modifier = Modifier.animateContentSize(
            animationSpec = tween(durationMillis = NovaMotion.standard),
        ),
    ) {
        NovaCard(
            onClick = { if (state.memory != null) showMemory = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            containerColor = if (ready) memoryPalette.background else NovaSurface,
            borderColor = if (ready) memoryPalette.border else NovaBorder,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = NovaAccentSoft,
                ) {
                    Text(
                        text = "✦",
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                        color = NovaAccent,
                        style = NovaType.sectionTitle.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            state.loading && state.memory == null -> "Remembering last week…"
                            state.error != null && state.memory == null -> "Couldn't build your week"
                            ready -> "Your week is ready"
                            else -> "Last week was quiet"
                        },
                        color = if (ready) memoryPalette.ink else NovaInk,
                        style = NovaType.bodyCompact.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = when {
                            state.error != null && state.memory == null -> "Tap after Nova reconnects."
                            state.memory == null -> "Pulse · Rooms · Posts"
                            else -> railSummary(
                                state.memory!!.stats.highlights,
                                state.memory!!.stats.people,
                                state.memory!!.stats.nights,
                            )
                        },
                        color = if (ready) memoryPalette.muted else NovaMuted,
                        style = NovaType.micro,
                    )
                }
                if (state.loading && state.memory == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = NovaAccent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (state.memory != null) "Open ›" else "",
                        color = NovaAccent,
                        style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }

        val hasFilmMedia = state.memory?.highlights?.any {
            it.mediaType in setOf("image", "video") && it.mediaUrl.isNotBlank()
        } == true
        if (hasFilmMedia) {
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            NovaCard(
                onClick = { showFilm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                containerColor = NovaAccentSoft,
                borderColor = NovaAccent.copy(alpha = 0.22f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("▶", color = NovaAccent, style = NovaType.bodyCompact)
                    Spacer(modifier = Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Make your Memory Film",
                            color = NovaInk,
                            style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Nova picks the scenes · you get the MP4",
                            color = NovaMuted,
                            style = NovaType.badge,
                        )
                    }
                    Text(
                        "Film ›",
                        color = NovaAccent,
                        style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }

    if (showMemory) {
        Dialog(
            onDismissRequest = { showMemory = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            MemoryScreen(
                onBack = { showMemory = false },
                onPersonClick = { username ->
                    showMemory = false
                    onPersonClick(username)
                },
                onSessionExpired = {
                    showMemory = false
                    onSessionExpired()
                },
            )
        }
    }

    if (showFilm) {
        Dialog(
            onDismissRequest = { showFilm = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            MemoryFilmScreen(
                initialWeeksAgo = 0,
                onBack = { showFilm = false },
                onSessionExpired = {
                    showFilm = false
                    onSessionExpired()
                },
            )
        }
    }
}


private fun railSummary(highlights: Int, people: Int, nights: Int): String = when {
    highlights == 0 -> "A quiet week, still yours."
    people > 0 && nights > 0 -> "$highlights moments · $people people · $nights nights"
    people > 0 -> "$highlights moments · $people people"
    else -> "$highlights moments worth keeping"
}


private fun railUtcOffsetMinutes(): Int =
    TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
