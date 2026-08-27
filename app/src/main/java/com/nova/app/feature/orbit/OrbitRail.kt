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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.app.appContainer
import com.nova.app.feature.orbit.domain.model.OrbitEvent
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaOrbitRing
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
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
                    text = "Your Orbit",
                    color = NovaInk,
                    style = NovaType.title.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Close connections, in real time",
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
                    items(
                        state.events.distinctBy { it.actor.id }.take(6),
                        key = { it.actor.id },
                    ) { event ->
                        OrbitPersonRailItem(
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
private fun OrbitPersonRailItem(
    event: OrbitEvent,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(66.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NovaOrbitRing(
            modifier = Modifier.size(58.dp),
            rings = 2,
            showLivePoint = event.pulse != null,
        ) {
            NovaAvatar(
                source = event.actor.avatarUrl,
                fallbackText = event.actor.name.ifBlank { event.actor.username },
                size = 46.dp,
            )
        }
        Spacer(modifier = Modifier.height(NovaSpacing.xs))
        Text(
            text = event.actor.name.ifBlank { event.actor.username }.substringBefore(' '),
            color = NovaInk,
            style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = orbitActionText(event),
            color = NovaMuted,
            style = NovaType.badge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


internal fun orbitActionText(event: OrbitEvent): String = when (event.kind) {
    "like" -> "liked a post"
    "comment" -> "joined a conversation"
    "repost" -> "moved a post forward"
    "follow" -> event.person?.let { person ->
        "followed ${person.name.ifBlank { "@${person.username}" }}"
    } ?: "followed someone"
    "pulse_reply" -> "replied with a live moment"
    else -> "did something in your orbit"
}


internal fun orbitContextText(event: OrbitEvent): String? = when (event.kind) {
    "comment" -> event.commentPreview.takeIf { it.isNotBlank() }?.let { "“$it”" }
    "like", "repost" -> event.post?.author?.let {
        "on ${it.name.ifBlank { "@${it.username}" }}’s post"
    }
    "pulse_reply" -> event.person?.let {
        "to ${it.name.ifBlank { "@${it.username}" }}"
    }
    else -> null
}


internal fun orbitIcon(kind: String): NovaIconAsset = when (kind) {
    "like" -> NovaIconAsset.LikeFilled
    "comment" -> NovaIconAsset.Comment
    "repost" -> NovaIconAsset.Repost
    "follow" -> NovaIconAsset.Create
    "pulse_reply" -> NovaIconAsset.Orbit
    else -> NovaIconAsset.Orbit
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
                    NovaIcon(
                        asset = NovaIconAsset.Back,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).graphicsLayer { scaleX = -1f },
                        tint = NovaAccent,
                    )
                    Text("More", color = NovaInk, style = NovaType.micro.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}


@Composable
private fun OrbitEmptyCard(onRetry: () -> Unit) {
    Surface(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth().height(92.dp),
        shape = MaterialTheme.shapes.large,
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
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
                NovaIcon(
                    asset = NovaIconAsset.Orbit,
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp).size(22.dp),
                    tint = NovaAccent,
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
