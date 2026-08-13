package com.nova.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val NovaBaseBackground = Color(0xFFFAFAF8)
val NovaBaseSurface = Color(0xFFFFFFFF)
val NovaBaseInk = Color(0xFF18171C)
val NovaBaseMuted = Color(0xFF77727F)
val NovaBaseBorder = Color(0xFFE9E6EE)
val NovaBaseAccent = Color(0xFF6554E8)
val NovaBaseAccentSoft = Color(0xFFF0EDFF)
val NovaDanger = Color(0xFFBF2A3D)


data class NovaColorOverride(
    val background: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    val accent: Color,
    val accentSoft: Color,
)


val LocalNovaColorOverride = staticCompositionLocalOf<NovaColorOverride?> { null }

val NovaBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.background ?: NovaBaseBackground

val NovaSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.surface ?: NovaBaseSurface

val NovaInk: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.ink ?: NovaBaseInk

val NovaMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.muted ?: NovaBaseMuted

val NovaBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.border ?: NovaBaseBorder

val NovaAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.accent ?: NovaBaseAccent

val NovaAccentSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.accentSoft ?: NovaBaseAccentSoft
