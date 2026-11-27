package com.nova.app.feature.plants

/**
 * Native care choices must remain readable at 320dp and larger system text.
 * Width is measured *inside* the editor, after actual horizontal padding.
 * A third column is allowed only when every icon + short label has room.
 */
internal fun plantupiaCareActionColumns(availableWidthDp: Float, fontScale: Float): Int =
    if (availableWidthDp < 380f || fontScale >= 1.3f) 2 else 3
