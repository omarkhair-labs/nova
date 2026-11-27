package com.nova.app.feature.plants

internal enum class PlantupiaCaptureQuality { DARK, GLARE, LOW_DETAIL, READY }

/**
 * Preview advice from contrast and local edge energy in the image. Thresholds
 * are provisional and cannot measure focus distance, illuminance, or health.
 */
internal fun captureQuality(
    width: Int,
    height: Int,
    luma: (x: Int, y: Int) -> Int,
): PlantupiaCaptureQuality? {
    val left = width / 5
    val top = height / 5
    val right = width - left
    val bottom = height - top
    if (right - left < 8 || bottom - top < 8) return null
    val step = maxOf(1, minOf(width, height) / 100)
    var count = 0
    var dark = 0
    var glare = 0
    var edgeEnergy = 0
    for (y in top until bottom - step step step) {
        for (x in left until right - step step step) {
            val value = luma(x, y)
            if (value < 34) dark++
            if (value > 237) glare++
            edgeEnergy += kotlin.math.abs(value - luma(x + step, y)) +
                kotlin.math.abs(value - luma(x, y + step))
            count++
        }
    }
    if (count == 0) return null
    return when {
        dark.toDouble() / count > 0.65 -> PlantupiaCaptureQuality.DARK
        glare.toDouble() / count > 0.65 -> PlantupiaCaptureQuality.GLARE
        edgeEnergy.toDouble() / (2 * count) < 4 -> PlantupiaCaptureQuality.LOW_DETAIL
        else -> PlantupiaCaptureQuality.READY
    }
}
