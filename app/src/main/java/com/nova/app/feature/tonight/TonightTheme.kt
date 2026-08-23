package com.nova.app.feature.tonight

import androidx.compose.ui.graphics.Color
import com.nova.app.ui.theme.NovaBaseAccent
import com.nova.app.ui.theme.NovaBaseLive

/**
 * Deliberate Tonight presentation palette.
 *
 * Tonight is allowed to feel darker and more atmospheric than ordinary Nova
 * surfaces, but all of that feature mood must be owned here instead of being
 * scattered across composables.
 */
data class TonightPalette(
    val background: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val divider: Color,
    val cardBorder: Color,
    val mediaVideoBackground: Color,
    val mediaTextBackground: Color,
    val liveSignal: Color,
    val orbit: Color,
    val heroBottom: Color,
    val heroGlow: Color,
)

object TonightTheme {
    val live = TonightPalette(
        background = Color(0xFF0B1020),
        surface = Color(0xFF12172A),
        ink = Color(0xFFF8F7FD),
        muted = Color(0xFFB7B9C8),
        divider = Color.White.copy(alpha = 0.08f),
        cardBorder = NovaBaseAccent.copy(alpha = 0.34f),
        mediaVideoBackground = Color(0xFF070A13),
        mediaTextBackground = Color(0xFF171B31),
        liveSignal = NovaBaseLive,
        orbit = NovaBaseAccent,
        heroBottom = Color(0xFF17152E),
        heroGlow = Color(0xFF28204A),
    )
}
