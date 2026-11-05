package com.nova.app.feature.pulse

import androidx.compose.ui.graphics.Color
import com.nova.app.ui.theme.NovaBrandPurple
import com.nova.app.ui.theme.NovaBrandSoftLight

/** Shared dark-media palette for Pulse cards and the immersive viewer. */
data class PulseMediaPalette(
    val background: Color,
    val ink: Color,
    val muted: Color,
    val overlay: Color,
    val panelBorder: Color,
)

object PulseTheme {
    private val deepSpace = Color(0xFF10281F)

    val media = PulseMediaPalette(
        background = deepSpace,
        ink = NovaBrandSoftLight,
        muted = Color(0xFFD7D2C1),
        overlay = deepSpace.copy(alpha = 0.78f),
        panelBorder = NovaBrandPurple.copy(alpha = 0.24f),
    )
}
