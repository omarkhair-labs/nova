package com.nova.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NovaLightColorScheme = lightColorScheme(
    primary = NovaBaseAccent,
    onPrimary = NovaBaseSurface,
    primaryContainer = NovaBaseAccentSoft,
    onPrimaryContainer = NovaBaseInk,
    background = NovaBaseBackground,
    onBackground = NovaBaseInk,
    surface = NovaBaseSurface,
    onSurface = NovaBaseInk,
    surfaceVariant = NovaBaseAccentSoft,
    onSurfaceVariant = NovaBaseMuted,
    outline = NovaBaseBorder,
    error = NovaDanger,
)

@Composable
fun NovaTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NovaLightColorScheme,
        typography = Typography,
        shapes = NovaShapes,
        content = content,
    )
}
