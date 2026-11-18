package com.nova.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val NovaBrandDeepSpace = Color(0xFF123B2E)
val NovaBrandPurple = Color(0xFF123B2E)
val NovaBrandElectricBlue = Color(0xFF7E9C87)
val NovaBrandSunrisePeach = Color(0xFF9FB79A)
val NovaBrandSoftLight = Color(0xFFEAEADD)

val NovaBaseBackground = Color(0xFFF3EEDE)
val NovaBaseSurface = Color(0xFFFFFDF7)
val NovaBaseInk = Color(0xFF16302A)
val NovaBaseMuted = Color(0xFF4A5A50)
val NovaBaseBorder = Color(0xFFD7D2C1)
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
