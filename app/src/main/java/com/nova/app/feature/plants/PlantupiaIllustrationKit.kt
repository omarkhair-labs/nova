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

/**
 * A graphic *possibility*, not a real plant photograph or inventory record.
 * Use only in an authenticated, genuine empty-plant state.
 */
@Composable
internal fun PlantupiaEmptyPlantArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val ink = PlantupiaColors.forest
        val sage = PlantupiaColors.mutedSage
        val s = size.minDimension
        val stroke = (s * .021f).coerceAtLeast(1f)
        // Architecture gives this imagined planting space somewhere to exist.
        drawRoundRect(
            color = ink.copy(alpha = .18f),
            topLeft = Offset(w * .32f, h * .075f),
            size = Size(w * .36f, h * .79f),
            cornerRadius = CornerRadius(s * .15f),
            style = Stroke(stroke),
        )
        drawLine(
            color = ink.copy(alpha = .27f),
            start = Offset(w * .18f, h * .83f),
            end = Offset(w * .82f, h * .83f),
            strokeWidth = stroke,
        )
        // Open vessel, soil line and a small drawn sprout: never user evidence.
        drawRoundRect(
            color = ink.copy(alpha = .86f),
            topLeft = Offset(w * .42f, h * .62f),
            size = Size(w * .16f, h * .20f),
            cornerRadius = CornerRadius(s * .03f),
            style = Stroke(stroke * 1.25f),
        )
        drawLine(sage, Offset(w * .50f, h * .65f), Offset(w * .50f, h * .34f), stroke * 1.65f)
        drawOval(
            sage,
            topLeft = Offset(w * .38f, h * .335f),
            size = Size(w * .115f, h * .10f),
        )
        drawOval(
            sage,
            topLeft = Offset(w * .51f, h * .285f),
            size = Size(w * .103f, h * .105f),
        )
    }
}

/**
 * An original conversation mark for a public Q&A zero state.
 * No avatars, fake messages, reply counts, or implied real activity.
 */
@Composable
internal fun PlantupiaEmptyCircleArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val ink = PlantupiaColors.forest
        val sage = PlantupiaColors.mutedSage
        val s = size.minDimension
        val stroke = (s * .027f).coerceAtLeast(1f)
        drawRoundRect(
            color = ink.copy(alpha = .17f),
            topLeft = Offset(w * .12f, h * .09f),
            size = Size(w * .74f, h * .78f),
            cornerRadius = CornerRadius(s * .34f),
            style = Stroke(stroke * .8f),
        )
        drawRoundRect(
            color = ink.copy(alpha = .86f),
            topLeft = Offset(w * .18f, h * .25f),
            size = Size(w * .50f, h * .27f),
            cornerRadius = CornerRadius(s * .06f),
            style = Stroke(stroke),
        )
        drawLine(ink, Offset(w * .29f, h * .35f), Offset(w * .56f, h * .35f), stroke * .75f)
        drawLine(ink, Offset(w * .29f, h * .42f), Offset(w * .47f, h * .42f), stroke * .75f)
        drawRoundRect(
            color = sage.copy(alpha = .89f),
            topLeft = Offset(w * .39f, h * .59f),
            size = Size(w * .37f, h * .17f),
            cornerRadius = CornerRadius(s * .065f),
            style = Stroke(stroke),
        )
        drawLine(sage, Offset(w * .47f, h * .67f), Offset(w * .66f, h * .67f), stroke * .75f)
    }
}

/**
 * Existing plant, **missing user photo**. Different from the zero-plant art:
 * an empty field-note photo mount expresses absence without fabricating the
 * plant's appearance, growth, lighting or a user's room.
 * Decorative only; neighboring text names the missing-photo state.
 */
@Composable
internal fun PlantupiaMissingPhotoMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val s = size.minDimension
        val ink = PlantupiaColors.forest
        val sage = PlantupiaColors.mutedSage
        val stroke = (s * 0.030f).coerceAtLeast(1f)
        drawRoundRect(
            color = ink.copy(alpha = 0.82f),
            topLeft = Offset(w * 0.14f, h * 0.18f),
            size = Size(w * 0.72f, h * 0.62f),
            cornerRadius = CornerRadius(s * 0.08f),
            style = Stroke(width = stroke),
        )
        // Empty picture mount, not a landscape, plant likeness or camera feed.
        drawLine(
            ink.copy(alpha = 0.46f),
            Offset(w * 0.23f, h * 0.66f),
            Offset(w * 0.77f, h * 0.66f),
            strokeWidth = stroke * 0.7f,
        )
        drawLine(
            sage,
            Offset(w * 0.50f, h * 0.36f),
            Offset(w * 0.50f, h * 0.53f),
            strokeWidth = stroke,
        )
        drawOval(
            color = sage,
            topLeft = Offset(w * 0.385f, h * 0.34f),
            size = Size(w * 0.12f, h * 0.075f),
        )
        drawOval(
            color = sage,
            topLeft = Offset(w * 0.51f, h * 0.37f),
            size = Size(w * 0.12f, h * 0.075f),
        )
    }
}

/**
 * A place for people to *arrive*, not a claim that any person has joined.
 * Two open architectural thresholds share one grounded botanical element.
 * Distinct from Q&A, missing photograph, and empty plant inventory art.
 */
@Composable
internal fun PlantupiaEmptyPeopleArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val ink = PlantupiaColors.forest
        val leaf = PlantupiaColors.mutedSage
        val s = size.minDimension
        val stroke = (s * .025f).coerceAtLeast(1f)
        drawRoundRect(
            color = ink.copy(alpha = .48f),
            topLeft = Offset(w * .19f, h * .09f),
            size = Size(w * .27f, h * .74f),
            cornerRadius = CornerRadius(s * .28f, s * .28f),
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = ink.copy(alpha = .25f),
            topLeft = Offset(w * .54f, h * .20f),
            size = Size(w * .27f, h * .63f),
            cornerRadius = CornerRadius(s * .26f, s * .26f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = ink.copy(alpha = .30f),
            start = Offset(w * .12f, h * .85f),
            end = Offset(w * .88f, h * .85f),
            strokeWidth = stroke,
        )
        drawLine(
            color = leaf,
            start = Offset(w * .50f, h * .84f),
            end = Offset(w * .50f, h * .38f),
            strokeWidth = stroke * 1.2f,
        )
        drawOval(
            color = leaf,
            topLeft = Offset(w * .38f, h * .41f),
            size = Size(w * .115f, h * .13f),
        )
        drawOval(
            color = leaf,
            topLeft = Offset(w * .51f, h * .32f),
            size = Size(w * .13f, h * .14f),
        )
    }
}
