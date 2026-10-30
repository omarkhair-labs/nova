package com.nova.app.feature.memories

import androidx.compose.ui.graphics.Color

/** Reflective Plantupia parchment palette; the original Nova memory engine stays intact. */
data class MemoryPalette(
    val background: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    val videoBackground: Color,
)

object MemoryTheme {
    val ready = MemoryPalette(
        background = Color(0xFFF3EEDE),
        ink = Color(0xFF16302A),
        muted = Color(0xFF4A5A50),
        border = Color(0xFFD7D2C1),
        videoBackground = Color(0xFF10281F),
    )
}
