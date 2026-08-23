package com.nova.app.feature.memories

import androidx.compose.ui.graphics.Color
import com.nova.app.ui.theme.NovaBaseAccent

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
        background = Color(0xFF12151D),
        ink = Color.White,
        muted = Color(0xFFB6BCC9),
        border = NovaBaseAccent.copy(alpha = 0.26f),
        videoBackground = Color(0xFF0E1118),
    )
}
