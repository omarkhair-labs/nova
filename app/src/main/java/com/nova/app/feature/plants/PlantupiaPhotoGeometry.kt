package com.nova.app.feature.plants

import kotlin.math.max
import kotlin.math.min

/**
 * Limits for translating a centered, ContentScale.Fit image inside a clipped
 * viewport *after* it has been scaled about the center by graphicsLayer.
 *
 * Values are draw-time pixels, never magic constants in dp. For a source whose
 * scaled axis still fits the viewport, translation is zero along that axis.
 * No user data, image bytes or intrinsic EXIF attributes are changed.
 */
internal data class PlantupiaPhotoPanLimits(val maxX: Float, val maxY: Float)

internal fun plantupiaPhotoPanLimits(
    viewportWidth: Float,
    viewportHeight: Float,
    imageWidth: Float,
    imageHeight: Float,
    zoom: Float,
): PlantupiaPhotoPanLimits {
    if (!viewportWidth.isFinite() || !viewportHeight.isFinite() ||
        !imageWidth.isFinite() || !imageHeight.isFinite() || !zoom.isFinite() ||
        viewportWidth <= 0f || viewportHeight <= 0f ||
        imageWidth <= 0f || imageHeight <= 0f || zoom <= 1f
    ) return PlantupiaPhotoPanLimits(0f, 0f)

    val fit = min(viewportWidth / imageWidth, viewportHeight / imageHeight)
    val renderedWidth = imageWidth * fit * zoom
    val renderedHeight = imageHeight * fit * zoom
    return PlantupiaPhotoPanLimits(
        maxX = max(0f, (renderedWidth - viewportWidth) / 2f),
        maxY = max(0f, (renderedHeight - viewportHeight) / 2f),
    )
}

internal fun plantupiaClampPhotoPan(value: Float, limit: Float): Float =
    if (value.isFinite() && limit.isFinite() && limit > 0f) value.coerceIn(-limit, limit)
    else 0f
