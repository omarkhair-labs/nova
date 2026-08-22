package com.nova.app.feature.tonight

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.feature.tonight.domain.model.TonightPersonRow
import com.nova.app.feature.tonight.domain.model.TonightPulse
import com.nova.app.feature.tonight.domain.model.TonightSnapshot
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import java.time.Duration
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlinx.coroutines.delay


private val TonightBackground = Color(0xFF090B12)
private val TonightSurfaceColor = Color(0xFF111521)
private val TonightInk = Color(0xFFF7F8FC)
private val TonightMuted = Color(0xFFB1B7C5)


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

    when {
        state.loading && state.snapshot == null -> TonightLoadingCard()
        state.snapshot?.isTonight == true -> TonightLiveCard(
            snapshot = state.snapshot,
            error = state.error,
            onRetry = { owner.load(currentUtcOffsetMinutes(), showSpinner = false) },
            onPersonClick = onPersonClick,
            liveRoomsContent = liveRoomsContent,
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


@Composable
private fun TonightLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(112.dp),
        shape = RoundedCornerShape(28.dp),
        color = TonightBackground,
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
        shape = RoundedCornerShape(28.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (error != null) {
                        "Couldn't check Tonight. Tap to retry."
                    } else {
                        "Wakes at 6 PM with the live moments around you."
                    },
                    color = NovaMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }
            Text(
                text = if (error != null) "retry" else "6 PM",
                color = if (error != null) NovaAccent else NovaMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
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
    liveRoomsContent: (@Composable () -> Unit)?,
) {
    val value = snapshot ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = TonightBackground,
        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.30f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tonight",
                            color = TonightInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NovaAccent.copy(alpha = 0.18f),
                        ) {
                            Text(
                                text = "LIVE",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                color = NovaAccent,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        text = tonightSummary(value),
                        color = TonightMuted,
                        fontSize = 10.sp,
                    )
                }
                Text(
                    text = if (error != null) "Retry" else "until 6 AM",
                    color = if (error != null) NovaAccent else TonightMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = if (error != null) Modifier.clickable(onClick = onRetry) else Modifier,
                )
            }

            if (value.people.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = TonightSurfaceColor,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("✦", color = NovaAccent, fontSize = 18.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (value.myMomentsCount > 0) "Your night has started" else "The night is still quiet",
                            color = TonightInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (value.myMomentsCount > 0) {
                                "Your Pulse is live. People you follow will appear here as their night starts."
                            } else {
                                "Post a Pulse or check back as people you follow start sharing."
                            },
                            color = TonightMuted,
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                        )
                    }
                }
            } else {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(value.people, key = { it.person.id }) { row ->
                        TonightPersonCard(
                            row = row,
                            onClick = { onPersonClick(row.person.username) },
                        )
                    }
                }
            }

            if (value.myMomentsCount > 0) {
                Text(
                    text = "You have ${value.myMomentsCount} live ${if (value.myMomentsCount == 1) "moment" else "moments"} tonight.",
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = TonightMuted,
                    fontSize = 9.sp,
                )
            }

            liveRoomsContent?.let { content ->
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(1.dp),
                        color = Color.White.copy(alpha = 0.07f),
                    ) {}
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}


@Composable
private fun TonightPersonCard(
    row: TonightPersonRow,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(184.dp).height(132.dp),
        shape = RoundedCornerShape(22.dp),
        color = TonightSurfaceColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TonightPulseBackdrop(row.latestPulse)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TonightBackground.copy(alpha = if (row.latestPulse.mediaType == "text") 0.08f else 0.42f)),
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
                        color = TonightInk,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${row.momentsCount} ${if (row.momentsCount == 1) "moment" else "moments"}",
                        color = TonightMuted,
                        fontSize = 8.sp,
                    )
                }
            }

            if (row.latestPulse.mediaType != "text" && row.latestPulse.note.isNotBlank()) {
                Text(
                    text = row.latestPulse.note,
                    modifier = Modifier.align(Alignment.BottomStart).padding(11.dp),
                    color = TonightInk,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


@Composable
private fun TonightPulseBackdrop(pulse: TonightPulse) {
    when (pulse.mediaType) {
        "image" -> NovaMediaImage(
            source = pulse.mediaUrl,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Tonight moment",
        )
        "video" -> Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF05070C)),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶", color = TonightInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        else -> Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF171B2A)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = pulse.note,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 30.dp),
                color = TonightInk,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
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
