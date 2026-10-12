package com.nova.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val NovaBrandDeepSpace = Color(0xFF153B2E)
val NovaBrandPurple = Color(0xFF153B2E)
val NovaBrandElectricBlue = Color(0xFF799A83)
val NovaBrandSunrisePeach = Color(0xFF799A83)
val NovaBrandSoftLight = Color(0xFFE6EDE3)

val NovaBaseBackground = Color(0xFFF7F6F0)
val NovaBaseSurface = Color(0xFFFFFEFA)
val NovaBaseInk = Color(0xFF153B2E)
val NovaBaseMuted = Color(0xFF64796C)
val NovaBaseBorder = Color(0xFFD5DED3)
val NovaBaseAccent = NovaBrandPurple
val NovaBaseAccentSoft = NovaBrandSoftLight
val NovaBaseLive = NovaBrandElectricBlue
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
    @Composable @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.background ?: NovaBaseBackground
val NovaSurface: Color
    @Composable @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.surface ?: NovaBaseSurface
val NovaInk: Color
    @Composable @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.ink ?: NovaBaseInk
val NovaMuted: Color
    @Composable @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.muted ?: NovaBaseMuted
val NovaBorder: Color
    @Composable @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.border ?: NovaBaseBorder
val NovaAccent: Color
    @Composable @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.accent ?: NovaBaseAccent
val NovaAccentSoft: Color
    @Composable @ReadOnlyComposable
    get() = LocalNovaColorOverride.current?.accentSoft ?: NovaBaseAccentSoft
