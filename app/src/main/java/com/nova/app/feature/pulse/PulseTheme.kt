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
    private val deepSpace = Color(0xFF0A0B14)

    val media = PulseMediaPalette(
        background = deepSpace,
        ink = NovaBrandSoftLight,
        muted = Color(0xFFB9B4CE),
        overlay = deepSpace.copy(alpha = 0.78f),
        panelBorder = NovaBrandPurple.copy(alpha = 0.24f),
    )
}
