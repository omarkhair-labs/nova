package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * Original botanical/architectural empty-place drawing, not photo evidence.
 * Decorative: the adjacent screen text is the accessible description.
 */
@Composable
internal fun PlantupiaEmptyPlaceArt(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clearAndSetSemantics { },
    ) {
        val w = size.width
        val h = size.height
        val ink = PlantupiaColors.forest
        val sage = PlantupiaColors.mutedSage
        val stroke = (w.coerceAtMost(h) * 0.015f).coerceAtLeast(1f)

        // Quiet spatial niche: a place awaiting a real living record.
        drawRoundRect(
            color = ink.copy(alpha = 0.22f),
            topLeft = Offset(w * 0.29f, h * 0.11f),
            size = Size(w * 0.42f, h * 0.80f),
            cornerRadius = CornerRadius(h * 0.30f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = ink.copy(alpha = 0.30f),
            start = Offset(w * 0.20f, h * 0.83f),
            end = Offset(w * 0.80f, h * 0.83f),
            strokeWidth = stroke,
        )
        // A graphic botanical motif—not an observed plant, not a pot photograph.
        drawLine(sage, Offset(w * 0.50f, h * 0.70f), Offset(w * 0.50f, h * 0.32f), strokeWidth = stroke * 1.7f)
        drawLine(sage, Offset(w * 0.50f, h * 0.52f), Offset(w * 0.43f, h * 0.40f), strokeWidth = stroke * 1.35f)
        drawLine(sage, Offset(w * 0.50f, h * 0.60f), Offset(w * 0.57f, h * 0.47f), strokeWidth = stroke * 1.35f)
        drawOval(sage, topLeft = Offset(w * 0.405f, h * 0.31f), size = Size(w * 0.063f, h * 0.12f))
        drawOval(sage, topLeft = Offset(w * 0.54f, h * 0.39f), size = Size(w * 0.060f, h * 0.12f))
        drawOval(sage, topLeft = Offset(w * 0.472f, h * 0.21f), size = Size(w * 0.054f, h * 0.13f))
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * 0.443f, h * 0.65f),
            size = Size(w * 0.114f, h * 0.175f),
            cornerRadius = CornerRadius(h * 0.018f),
        )
        drawLine(
            color = PlantupiaColors.paper.copy(alpha = 0.72f),
            start = Offset(w * 0.465f, h * 0.72f),
            end = Offset(w * 0.535f, h * 0.72f),
            strokeWidth = stroke,
        )
    }
}

/**
 * Empty field-book silhouette only. No text, dates, photos, fake observations,
 * verified conditions, or synthetic plant records are depicted.
 */
@Composable
internal fun PlantupiaEmptyFieldNotesArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val ink = PlantupiaColors.forest
        val sage = PlantupiaColors.mutedSage
        val stroke = (size.minDimension * 0.018f).coerceAtLeast(1f)
        // Open blank notebook leaf with a physical page spine.
        drawRoundRect(
            color = PlantupiaColors.paper,
            topLeft = Offset(w * 0.15f, h * 0.07f),
            size = Size(w * 0.71f, h * 0.84f),
            cornerRadius = CornerRadius(w * 0.055f),
        )
        drawRoundRect(
            color = ink.copy(alpha = 0.65f),
            topLeft = Offset(w * 0.15f, h * 0.07f),
            size = Size(w * 0.71f, h * 0.84f),
            cornerRadius = CornerRadius(w * 0.055f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = ink.copy(alpha = 0.32f),
            start = Offset(w * 0.30f, h * 0.14f),
            end = Offset(w * 0.30f, h * 0.82f),
            strokeWidth = stroke,
        )
        // Unwritten paper: only two corner registration marks and a leaf motif,
        // not decorative pretend handwriting or a fabricated observation.
        drawLine(
            color = ink.copy(alpha = 0.30f),
            start = Offset(w * 0.38f, h * 0.22f),
            end = Offset(w * 0.46f, h * 0.22f),
            strokeWidth = stroke,
        )
        drawLine(
            color = ink.copy(alpha = 0.30f),
            start = Offset(w * 0.38f, h * 0.75f),
            end = Offset(w * 0.46f, h * 0.75f),
            strokeWidth = stroke,
        )
        drawLine(
            color = sage,
            start = Offset(w * 0.62f, h * 0.67f),
            end = Offset(w * 0.62f, h * 0.38f),
            strokeWidth = stroke * 1.7f,
        )
        drawOval(
            color = sage,
            topLeft = Offset(w * 0.49f, h * 0.32f),
            size = Size(w * 0.12f, h * 0.105f),
        )
        drawOval(
            color = sage,
            topLeft = Offset(w * 0.63f, h * 0.40f),
            size = Size(w * 0.11f, h * 0.10f),
        )
    }
}
