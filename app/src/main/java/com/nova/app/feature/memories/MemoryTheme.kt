package com.nova.app.feature.memories

import androidx.compose.ui.graphics.Color

/** Deliberate reflective palette used when Nova has a memory ready to replay. */
data class MemoryPalette(
    val background: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    val videoBackground: Color,
)

object MemoryTheme {
    val ready = MemoryPalette(
        background = Color(0xFFFFF7EC),
        ink = Color(0xFF6F4528),
        muted = Color(0xFF976D4E),
        border = Color(0xFFE8D2B7),
        videoBackground = Color(0xFF0E1118),
    )
}
