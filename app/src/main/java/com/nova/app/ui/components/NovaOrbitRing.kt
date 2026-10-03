package com.nova.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBaseLive
import kotlin.math.cos
import kotlin.math.sin

// The live point starts where it used to sit statically (upper right) and then
// travels once around the ring. Slow on purpose: presence, not a spinner.
private const val LIVE_POINT_START_ANGLE_DEG = -45f
private const val LIVE_POINT_ORBIT_MILLIS = 9000

/**
 * Nova's visual relationship signature.
 *
 * The ring is intentionally a semantic accent for relationship, live presence,
 * selection and progress. It should not be used as generic wallpaper.
 *
 * When [showLivePoint] is true the live point slowly orbits the ring. The
 * animation only runs for rings that actually show a live point.
 */
@Composable
fun NovaOrbitRing(
    modifier: Modifier = Modifier,
    color: Color = NovaAccent,
    liveColor: Color = NovaBaseLive,
    rings: Int = 3,
    showLivePoint: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val ringCount = rings.coerceAtLeast(1)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension / 2f
            val gap = outerRadius / (ringCount + 1f)
            val strokeWidth = 1.dp.toPx()

            repeat(ringCount) { index ->
                drawCircle(
                    color = color.copy(alpha = 0.13f + (index * 0.08f)),
                    radius = outerRadius - (index * gap),
                    style = Stroke(width = strokeWidth),
                )
            }
        }

        if (showLivePoint) {
            OrbitLivePoint(
                ringCount = ringCount,
                liveColor = liveColor,
            )
        }

        content()
    }
}

@Composable
private fun OrbitLivePoint(
    ringCount: Int,
    liveColor: Color,
) {
    val transition = rememberInfiniteTransition(label = "nova-orbit-live-point")
    // Keep the State object (no `by`) so the value is only read in the draw
    // phase: the dot redraws every frame without recomposing anything.
    val angle = transition.animateFloat(
        initialValue = LIVE_POINT_START_ANGLE_DEG,
        targetValue = LIVE_POINT_START_ANGLE_DEG + 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = LIVE_POINT_ORBIT_MILLIS, easing = LinearEasing),
        ),
        label = "nova-orbit-live-point-angle",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val outerRadius = size.minDimension / 2f
        val gap = outerRadius / (ringCount + 1f)
        // Ride between the outer ring and the next one.
        val orbitRadius = outerRadius - (gap * 0.5f)
        val radians = Math.toRadians(angle.value.toDouble())

        drawCircle(
            color = liveColor,
            radius = 3.dp.toPx(),
            center = Offset(
                x = center.x + (orbitRadius * cos(radians)).toFloat(),
                y = center.y + (orbitRadius * sin(radians)).toFloat(),
            ),
        )
    }
}
