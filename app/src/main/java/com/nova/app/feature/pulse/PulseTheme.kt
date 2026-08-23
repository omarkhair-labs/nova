package com.nova.app.feature.pulse

import androidx.compose.ui.graphics.Color

/** Shared dark-media palette for Pulse cards and the immersive viewer. */
data class PulseMediaPalette(
    val background: Color,
    val ink: Color,
    val muted: Color,
    val overlay: Color,
    val panelBorder: Color,
)

object PulseTheme {
    val media = PulseMediaPalette(
        background = Color(0xFF07090D),
        ink = Color(0xFFF8F9FB),
        muted = Color(0xFFB7BDC8),
        overlay = Color(0xFF07090D).copy(alpha = 0.76f),
        panelBorder = Color(0xFFB7BDC8).copy(alpha = 0.20f),
    )
}
