package com.nova.app.feature.tonight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nova.app.feature.tonight.domain.model.TonightPersonRow
import com.nova.app.feature.tonight.domain.model.TonightSnapshot
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaOrbitRing
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun TonightIdentityHero(
    snapshot: TonightSnapshot,
    error: String?,
    onRetry: () -> Unit,
    onPersonClick: (String) -> Unit,
) {
    val palette = TonightTheme.live
    val people = snapshot.people.take(5)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(292.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = palette.background,
        border = BorderStroke(1.dp, palette.cardBorder),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.background,
                            palette.heroBottom,
                        ),
                    ),
                )
                .padding(NovaSpacing.lg),
        ) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd),
                shape = MaterialTheme.shapes.medium,
                color = palette.surface.copy(alpha = 0.74f),
                border = BorderStroke(1.dp, palette.liveSignal.copy(alpha = 0.48f)),
            ) {
                Text(
                    text = if (error == null) "LIVE NOW" else "RETRY",
                    modifier = Modifier
                        .then(if (error != null) Modifier.semantics { contentDescription = "Retry Tonight" } else Modifier)
                        .padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
                    color = if (error == null) palette.liveSignal else palette.ink,
                    style = NovaType.badge,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.52f),
            ) {
                Text(
                    text = "TONIGHT",
                    color = palette.muted,
                    style = NovaType.meta.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(NovaSpacing.sm))
                Text(
                    text = currentNightTitle(),
                    color = palette.ink,
                    style = NovaType.display.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = Modifier.height(NovaSpacing.sm))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = palette.liveSignal, fontWeight = FontWeight.SemiBold)) {
                            append("Live")
                        }
                        withStyle(SpanStyle(color = palette.ink)) {
                            append(" with your orbit")
                        }
                    },
                    style = NovaType.body,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 18.dp, y = 8.dp)
                    .size(196.dp),
            ) {
                NovaOrbitRing(
                    modifier = Modifier.fillMaxSize(),
                    color = palette.orbit,
                    liveColor = palette.liveSignal,
                    rings = 4,
                    showLivePoint = true,
                )

                people.getOrNull(0)?.let { row ->
                    TonightOrbitAvatar(
                        row = row,
                        size = 62.dp,
                        borderWidth = 3.dp,
                        borderColor = palette.orbit,
                        modifier = Modifier.align(Alignment.Center),
                        onClick = { onPersonClick(row.person.username) },
                    )
                }
                people.getOrNull(1)?.let { row ->
                    TonightOrbitAvatar(
                        row = row,
                        size = 42.dp,
                        borderWidth = 2.dp,
                        borderColor = palette.ink.copy(alpha = 0.72f),
                        modifier = Modifier.align(Alignment.TopCenter),
                        onClick = { onPersonClick(row.person.username) },
                    )
                }
                people.getOrNull(2)?.let { row ->
                    TonightOrbitAvatar(
                        row = row,
                        size = 44.dp,
                        borderWidth = 2.dp,
                        borderColor = palette.orbit,
                        modifier = Modifier.align(Alignment.CenterStart),
                        onClick = { onPersonClick(row.person.username) },
                    )
                }
                people.getOrNull(3)?.let { row ->
                    TonightOrbitAvatar(
                        row = row,
                        size = 42.dp,
                        borderWidth = 2.dp,
                        borderColor = palette.ink.copy(alpha = 0.72f),
                        modifier = Modifier.align(Alignment.CenterEnd),
                        onClick = { onPersonClick(row.person.username) },
                    )
                }
                people.getOrNull(4)?.let { row ->
                    TonightOrbitAvatar(
                        row = row,
                        size = 40.dp,
                        borderWidth = 2.dp,
                        borderColor = palette.liveSignal,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onClick = { onPersonClick(row.person.username) },
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(NovaSpacing.xxs)) {
                    people.take(3).forEach { row ->
                        NovaAvatar(
                            source = row.person.avatarUrl,
                            fallbackText = row.person.name.ifBlank { row.person.username },
                            size = 25.dp,
                        )
                    }
                }
                Surface(
                    modifier = Modifier.size(7.dp),
                    shape = CircleShape,
                    color = palette.liveSignal,
                ) {}
                Text(
                    text = tonightPresenceText(snapshot),
                    color = palette.ink,
                    style = NovaType.micro,
                )
            }

            if (error != null) {
                Surface(
                    onClick = onRetry,
                    modifier = Modifier.align(Alignment.BottomEnd),
                    shape = MaterialTheme.shapes.medium,
                    color = palette.surface.copy(alpha = 0.86f),
                    border = BorderStroke(1.dp, palette.divider),
                ) {
                    Text(
                        text = "Retry",
                        modifier = Modifier.padding(horizontal = NovaSpacing.md, vertical = NovaSpacing.sm),
                        color = palette.ink,
                        style = NovaType.meta.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            } else {
                Text(
                    text = "until 6 AM",
                    modifier = Modifier.align(Alignment.BottomEnd),
                    color = palette.muted,
                    style = NovaType.micro,
                )
            }
        }
    }
}

@Composable
private fun TonightOrbitAvatar(
    row: TonightPersonRow,
    size: Dp,
    borderWidth: Dp,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val name = row.person.name.ifBlank { row.person.username }

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Open $name" },
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        NovaAvatar(
            source = row.person.avatarUrl,
            fallbackText = name,
            size = size,
            modifier = Modifier
                .padding(borderWidth)
                .clip(CircleShape),
        )
    }
}

private fun currentNightTitle(): String {
    val day = ZonedDateTime.now().dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$day night"
}

private fun tonightPresenceText(snapshot: TonightSnapshot): String = when (snapshot.peopleCount) {
    0 -> if (snapshot.myMomentsCount > 0) "Your night has started" else "The night is still quiet"
    1 -> "1 person active tonight"
    else -> "${snapshot.peopleCount} people active tonight"
}
