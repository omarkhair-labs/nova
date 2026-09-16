package com.nova.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nova.app.ui.theme.NovaBaseInk


/**
 * Nova's signature arrival: separate -> approach -> settle.
 *
 * It is deliberately short and only affects the relationship between the two forms,
 * so it reads as identity behavior instead of decorative animation.
 */
@Composable
fun NovaBrandArrival(
    modifier: Modifier = Modifier,
    monochrome: Boolean = false,
    monochromeColor: Color = NovaBaseInk,
) {
    val progress = remember { Animatable(0.18f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 720,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    NovaBrandMark(
        modifier = modifier,
        monochrome = monochrome,
        monochromeColor = monochromeColor,
        interactionProgress = progress.value,
    )
}


/**
 * A quiet repeating presence state for real waiting moments.
 * Replaces generic spinners where Nova is actively connecting or loading people/content.
 */
@Composable
fun NovaPresenceIndicator(
    modifier: Modifier = Modifier,
    monochrome: Boolean = false,
    monochromeColor: Color = NovaBaseInk,
) {
    val transition = rememberInfiniteTransition(label = "nova-presence")
    val progress by transition.animateFloat(
        initialValue = 0.74f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 760,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nova-presence-approach",
    )

    NovaBrandMark(
        modifier = modifier,
        monochrome = monochrome,
        monochromeColor = monochromeColor,
        interactionProgress = progress,
    )
}
