package com.nova.app.ui.components

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

/**
 * Nova's visual relationship signature.
 *
 * The ring is intentionally a semantic accent for relationship, live presence,
 * selection and progress. It should not be used as generic wallpaper.
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

            if (showLivePoint) {
                drawCircle(
                    color = liveColor,
                    radius = 3.dp.toPx(),
                    center = Offset(
                        x = size.width * 0.78f,
                        y = size.height * 0.20f,
                    ),
                )
            }
        }

        content()
    }
}
