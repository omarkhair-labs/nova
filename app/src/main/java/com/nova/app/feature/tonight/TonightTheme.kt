package com.nova.app.feature.tonight

import androidx.compose.ui.graphics.Color
import com.nova.app.ui.theme.NovaBaseAccent
import com.nova.app.ui.theme.NovaBaseLive

/**
 * Deliberate Tonight presentation palette.
 *
 * Tonight is allowed to feel darker and more atmospheric than ordinary Nova
 * surfaces; the Plantupia identity still applies, with feature mood owned here instead of
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
        background = Color(0xFF0D2018),
        surface = Color(0xFF183B2D),
        ink = Color(0xFFF3EEDE),
        muted = Color(0xFFD7D2C1),
        divider = Color.White.copy(alpha = 0.08f),
        cardBorder = NovaBaseAccent.copy(alpha = 0.34f),
        mediaVideoBackground = Color(0xFF091912),
        mediaTextBackground = Color(0xFF1D4232),
        liveSignal = NovaBaseLive,
        orbit = NovaBaseAccent,
        heroBottom = Color(0xFF153726),
        heroGlow = Color(0xFF325A40),
    )
}
