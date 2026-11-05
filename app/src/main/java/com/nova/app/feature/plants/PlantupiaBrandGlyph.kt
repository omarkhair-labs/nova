package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Provisional UI expression of the brand guideline's "Nature x Architecture"
 * icon: a structural vertical and two rising growth arcs, not a leaf/tree.
 * Final logo artwork still requires the brand owner's approved vector asset.
 */
@Composable
fun PlantupiaBrandGlyph(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF123B2E),
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = size.minDimension * 0.052f
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.13f),
            end = Offset(w * 0.5f, h * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        val left = Path().apply {
            moveTo(w * 0.13f, h * 0.76f)
            cubicTo(w * 0.28f, h * 0.75f, w * 0.24f, h * 0.41f, w * 0.5f, h * 0.41f)
        }
        val right = Path().apply {
            moveTo(w * 0.87f, h * 0.64f)
            cubicTo(w * 0.73f, h * 0.65f, w * 0.74f, h * 0.28f, w * 0.5f, h * 0.28f)
        }
        drawPath(left, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawPath(right, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}
