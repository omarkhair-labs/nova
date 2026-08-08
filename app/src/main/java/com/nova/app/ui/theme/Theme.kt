package com.nova.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NovaLightColorScheme = lightColorScheme(
    primary = NovaAccent,
    onPrimary = NovaSurface,
    primaryContainer = NovaAccentSoft,
    onPrimaryContainer = NovaInk,
    background = NovaBackground,
    onBackground = NovaInk,
    surface = NovaSurface,
    onSurface = NovaInk,
    surfaceVariant = NovaAccentSoft,
    onSurfaceVariant = NovaMuted,
    outline = NovaBorder,
    error = NovaDanger,
)

@Composable
fun NovaTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NovaLightColorScheme,
        typography = Typography,
        content = content,
    )
}
