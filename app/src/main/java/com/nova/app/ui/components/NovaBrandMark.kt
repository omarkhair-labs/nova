package com.nova.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nova.app.ui.theme.NovaBaseInk

/**
 * Provisional Plantupia Nature x Architecture mark for internal QA.
 * Keeps NovaBrandMark's stable call signature while replacing Nova's
 * former gradient emblem. Final official vector needs brand approval.
 */
@Composable
fun NovaBrandMark(
    modifier: Modifier = Modifier,
    monochrome: Boolean = false,
    monochromeColor: Color = NovaBaseInk,
    interactionProgress: Float = 1f,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val scale = interactionProgress.coerceIn(0f, 1f)
        val stroke = size.minDimension * 0.052f
        val ink = if (monochrome) monochromeColor else Color(0xFF123B2E)

        drawLine(
            color = ink,
            start = Offset(w * 0.5f, h * 0.13f),
            end = Offset(w * 0.5f, h * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        val left = Path().apply {
            moveTo(w * (0.13f - (1f - scale) * 0.09f), h * 0.76f)
            cubicTo(w * 0.28f, h * 0.75f, w * 0.24f, h * 0.41f, w * 0.5f, h * 0.41f)
        }
        val right = Path().apply {
            moveTo(w * (0.87f + (1f - scale) * 0.09f), h * 0.64f)
            cubicTo(w * 0.73f, h * 0.65f, w * 0.74f, h * 0.28f, w * 0.5f, h * 0.28f)
        }
        drawPath(left, ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawPath(right, ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}
