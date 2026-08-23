package com.nova.app.feature.tonight

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.feature.rooms.RoomTonightSection
import com.nova.app.feature.tonight.domain.model.TonightPersonRow
import com.nova.app.feature.tonight.domain.model.TonightPulse
import com.nova.app.feature.tonight.domain.model.TonightSnapshot
import com.nova.app.ui.components.NovaAvatar
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
import java.time.Duration
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlinx.coroutines.delay


@Composable
fun TonightSurface(
    onPersonClick: (String) -> Unit,
    onSessionExpired: () -> Unit,
    liveRoomsContent: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val repository = context.appContainer.tonightRepository
    val scope = rememberCoroutineScope()
    val owner = remember(repository, scope) { TonightStateOwner(repository, scope) }
    val state = owner.state
    val roomsContent: @Composable () -> Unit = liveRoomsContent ?: {
        RoomTonightSection(
            onPersonClick = onPersonClick,
            onSessionExpired = onSessionExpired,
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            owner.load(
                utcOffsetMinutes = currentUtcOffsetMinutes(),
                showSpinner = owner.state.snapshot == null,
            )
            delay(millisUntilTonightBoundary())
        }
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = NovaMotion.standard)),
    ) {
        when {
            state.loading && state.snapshot == null -> TonightLoadingCard()
            state.snapshot?.isTonight == true -> TonightLiveCard(
                snapshot = state.snapshot,
                error = state.error,
                onRetry = { owner.load(currentUtcOffsetMinutes(), showSpinner = false) },
                onPersonClick = onPersonClick,
                liveRoomsContent = roomsContent,
            )
            else -> TonightSleepingCard(
                error = state.error,
                onRetry = {
                    owner.load(
                        currentUtcOffsetMinutes(),
                        showSpinner = state.snapshot == null,
                    )
                },
            )
        }
    }
}


@Composable
private fun TonightLoadingCard() {
    val palette = TonightTheme.live
    Surface(
        modifier = Modifier.fillMaxWidth().height(112.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = palette.background,
        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.18f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = NovaAccent,
                strokeWidth = 2.dp,
            )
        }
    }
}


@Composable
private fun TonightSleepingCard(
    error: String?,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRetry),
        shape = MaterialTheme.shapes.extraLarge,
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = NovaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = NovaAccentSoft,
            ) {
                Text(
                    text = "☾",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = NovaAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tonight",
                    color = NovaInk,
                    style = NovaType.title.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = if (error != null) {
                        "Couldn't check Tonight. Tap to retry."
                    } else {
                        "Wakes at 6 PM with the live moments around you."
                    },
                    color = NovaMuted,
                    style = NovaType.micro,
                )
            }
            Text(
                text = if (error != null) "retry" else "6 PM",
                color = if (error != null) NovaAccent else NovaMuted,
                style = NovaType.micro.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}


@Composable
private fun TonightLiveCard(
    snapshot: TonightSnapshot?,
    error: String?,
    onRetry: () -> Unit,
    onPersonClick: (String) -> Unit,
    liveRoomsContent: @Composable () -> Unit,
) {
    val value = snapshot ?: return
    TonightIdentityHero(
        snapshot = value,
        error = error,
        onRetry = onRetry,
        onPersonClick = onPersonClick,
    )
}


@Composable
private fun TonightPersonCard(
    row: TonightPersonRow,
    onClick: () -> Unit,
) {
    val palette = TonightTheme.live
    Surface(
        onClick = onClick,
        modifier = Modifier.width(184.dp).height(132.dp),
        shape = MaterialTheme.shapes.large,
        color = palette.surface,
        border = BorderStroke(1.dp, palette.divider),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TonightPulseBackdrop(row.latestPulse)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background.copy(alpha = if (row.latestPulse.mediaType == "text") 0.08f else 0.42f)),
            )

            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NovaAvatar(
                    source = row.person.avatarUrl,
                    fallbackText = row.person.name.ifBlank { row.person.username },
                    size = 28.dp,
                )
                Spacer(modifier = Modifier.width(7.dp))
                Column(modifier = Modifier.width(108.dp)) {
                    Text(
                        text = row.person.name.ifBlank { row.person.username },
                        color = palette.ink,
                        style = NovaType.micro.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${row.momentsCount} ${if (row.momentsCount == 1) "moment" else "moments"}",
                        color = palette.muted,
                        style = NovaType.badge,
                    )
                }
            }

            if (row.latestPulse.mediaType != "text" && row.latestPulse.note.isNotBlank()) {
                Text(
                    text = row.latestPulse.note,
                    modifier = Modifier.align(Alignment.BottomStart).padding(11.dp),
                    color = palette.ink,
                    style = NovaType.micro,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


@Composable
private fun TonightPulseBackdrop(pulse: TonightPulse) {
    val palette = TonightTheme.live
    when (pulse.mediaType) {
        "image" -> NovaMediaImage(
            source = pulse.mediaUrl,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Tonight moment",
        )
        "video" -> Box(
            modifier = Modifier.fillMaxSize().background(palette.mediaVideoBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶", color = palette.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        else -> Box(
            modifier = Modifier.fillMaxSize().background(palette.mediaTextBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = pulse.note,
                modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = 30.dp),
                color = palette.ink,
                style = NovaType.bodyCompact.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}


private fun currentUtcOffsetMinutes(): Int =
    TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000


private fun millisUntilTonightBoundary(): Long {
    val now = ZonedDateTime.now()
    val nextBoundary = when {
        now.hour < 6 -> now.withHour(6).withMinute(0).withSecond(0).withNano(0)
        now.hour < 18 -> now.withHour(18).withMinute(0).withSecond(0).withNano(0)
        else -> now.plusDays(1).withHour(6).withMinute(0).withSecond(0).withNano(0)
    }
    return (Duration.between(now, nextBoundary).toMillis() + 1_000L).coerceAtLeast(1_000L)
}


private fun tonightSummary(snapshot: TonightSnapshot): String {
    val people = snapshot.peopleCount
    val moments = snapshot.momentsCount
    return when {
        people == 0 -> "Your people haven't shared yet."
        people == 1 && moments == 1 -> "1 person · 1 live moment"
        people == 1 -> "1 person · $moments live moments"
        else -> "$people people · $moments live moments"
    }
}
