package com.nova.app.feature.orbit

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.app.appContainer
import com.nova.app.feature.orbit.domain.model.OrbitEvent
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMotion
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType


@Composable
fun OrbitRail(
    onPersonClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.orbitRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { OrbitStateOwner(repository, scope) }
    val state = owner.state

    LaunchedEffect(Unit) { owner.load(showSpinner = true) }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    Column(
        modifier = Modifier.animateContentSize(
            animationSpec = tween(durationMillis = NovaMotion.standard),
        ),
        verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Orbit",
                    color = NovaInk,
                    style = NovaType.title.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "What’s moving around your people",
                    color = NovaMuted,
                    style = NovaType.micro,
                )
            }
            Text(
                text = if (state.error != null) "Retry" else "social motion",
                color = if (state.error != null) NovaAccent else NovaMuted,
                style = NovaType.micro.copy(
                    fontWeight = if (state.error != null) FontWeight.SemiBold else FontWeight.Normal,
                ),
                modifier = if (state.error != null) {
                    Modifier.clickable {
                        owner.clearError()
                        owner.load(showSpinner = state.events.isEmpty())
                    }
                } else {
                    Modifier
                },
            )
        }

        when {
            state.loading && state.events.isEmpty() -> {
                Row(
                    modifier = Modifier.fillMaxWidth().height(118.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = NovaAccent,
                        strokeWidth = 2.dp,
                    )
                }
            }

            state.events.isEmpty() -> OrbitEmptyCard(onRetry = { owner.load(showSpinner = true) })

            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.events, key = { it.id }) { event ->
                        OrbitEventCard(
                            event = event,
                            onClick = { onPersonClick(event.actor.username) },
                        )
                    }
                    if (state.nextCursor != null) {
                        item(key = "orbit-more") {
                            OrbitMoreCard(
                                loading = state.loadingMore,
                                onClick = owner::loadMore,
                            )
                        }
                    }
                }
            }
        }

        state.error?.takeIf { state.events.isNotEmpty() }?.let { error ->
            Text(
                text = error,
                color = NovaMuted,
                style = NovaType.micro,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


@Composable
private fun OrbitEventCard(
    event: OrbitEvent,
    onClick: () -> Unit,
) {
    val mediaUrl = event.post?.imageUrl
        ?.takeIf { it.isNotBlank() }
        ?: event.pulse?.mediaUrl?.takeIf { it.isNotBlank() }
    val textPulse = event.pulse?.takeIf { it.mediaType == "text" }

    NovaCard(
        onClick = onClick,
        modifier = Modifier.width(224.dp).height(118.dp),
    ) {
        Row(
            modifier = Modifier.padding(NovaSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NovaAvatar(
                        source = event.actor.avatarUrl,
                        fallbackText = event.actor.name.ifBlank { event.actor.username },
                        size = 30.dp,
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.actor.name.ifBlank { event.actor.username },
                            color = NovaInk,
                            style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "@${event.actor.username}",
                            color = NovaMuted,
                            style = NovaType.badge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Column {
                    Text(
                        text = orbitActionText(event),
                        color = NovaInk,
                        style = NovaType.meta.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    orbitContextText(event)?.let { context ->
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = context,
                            color = NovaMuted,
                            style = NovaType.micro,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            when {
                mediaUrl != null -> NovaMediaImage(
                    source = mediaUrl,
                    modifier = Modifier.width(64.dp).height(94.dp),
                    contentDescription = "Orbit activity media",
                )
                textPulse != null -> Surface(
                    modifier = Modifier.width(64.dp).height(94.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = NovaAccentSoft,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(NovaSpacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = textPulse.note,
                            color = NovaInk,
                            style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                else -> Surface(
                    modifier = Modifier.width(42.dp).height(94.dp),
                    shape = MaterialTheme.shapes.small,
                    color = NovaAccentSoft,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = orbitSymbol(event.kind),
                            color = NovaAccent,
                            style = NovaType.sectionTitle.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}


private fun orbitActionText(event: OrbitEvent): String = when (event.kind) {
    "like" -> "liked a post"
    "comment" -> "joined a conversation"
    "repost" -> "moved a post forward"
    "follow" -> event.person?.let { person ->
        "followed ${person.name.ifBlank { "@${person.username}" }}"
    } ?: "followed someone"
    "pulse_reply" -> "replied with a live moment"
    else -> "did something in your orbit"
}


private fun orbitContextText(event: OrbitEvent): String? = when (event.kind) {
    "comment" -> event.commentPreview.takeIf { it.isNotBlank() }?.let { "“$it”" }
    "like", "repost" -> event.post?.author?.let {
        "on ${it.name.ifBlank { "@${it.username}" }}’s post"
    }
    "pulse_reply" -> event.person?.let {
        "to ${it.name.ifBlank { "@${it.username}" }}"
    }
    else -> null
}


private fun orbitSymbol(kind: String): String = when (kind) {
    "like" -> "♥"
    "comment" -> "↳"
    "repost" -> "↻"
    "follow" -> "+"
    "pulse_reply" -> "◉"
    else -> "✦"
}


@Composable
private fun OrbitMoreCard(
    loading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.width(96.dp).height(118.dp),
        shape = MaterialTheme.shapes.large,
        color = NovaAccentSoft,
        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.3f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = NovaAccent,
                    strokeWidth = 2.dp,
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("→", color = NovaAccent, style = NovaType.sectionTitle)
                    Text("More", color = NovaInk, style = NovaType.micro.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}


@Composable
private fun OrbitEmptyCard(onRetry: () -> Unit) {
    NovaCard(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth().height(92.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovaSpacing.md),
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
                    style = NovaType.sectionTitle,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your orbit is quiet",
                    color = NovaInk,
                    style = NovaType.meta.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Follow people and their social movement will show up here.",
                    color = NovaMuted,
                    style = NovaType.micro,
                )
            }
        }
    }
}
