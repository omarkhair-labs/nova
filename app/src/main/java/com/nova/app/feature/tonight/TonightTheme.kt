package com.nova.app.feature.tonight

import androidx.compose.ui.graphics.Color
import com.nova.app.ui.theme.NovaBaseAccent

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
)

object TonightTheme {
    val live = TonightPalette(
        background = Color(0xFF090B12),
        surface = Color(0xFF111521),
        ink = Color(0xFFF7F8FC),
        muted = Color(0xFFB1B7C5),
        divider = Color.White.copy(alpha = 0.07f),
        cardBorder = NovaBaseAccent.copy(alpha = 0.30f),
        mediaVideoBackground = Color(0xFF05070C),
        mediaTextBackground = Color(0xFF171B2A),
    )
}
