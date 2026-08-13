package com.nova.app.feature.messages

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


data class NovaChatPalette(
    val key: String,
    val label: String,
    val background: Color,
    val surface: Color,
    val raisedSurface: Color,
    val incomingBubble: Color,
    val incomingText: Color,
    val outgoingBubble: Color,
    val outgoingText: Color,
    val accent: Color,
    val accentSoft: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    val composer: Color,
)


object NovaChatThemes {
    val Nova = NovaChatPalette(
        key = "nova",
        label = "Nova",
        background = NovaBackground,
        surface = NovaSurface,
        raisedSurface = Color(0xFFFFFFFF),
        incomingBubble = NovaSurface,
        incomingText = NovaInk,
        outgoingBubble = NovaAccent,
        outgoingText = NovaBackground,
        accent = NovaAccent,
        accentSoft = NovaAccentSoft,
        ink = NovaInk,
        muted = NovaMuted,
        border = NovaBorder,
        composer = NovaSurface,
    )

    val Midnight = NovaChatPalette(
        key = "midnight",
        label = "Midnight",
        background = Color(0xFF0E1020),
        surface = Color(0xFF171A2C),
        raisedSurface = Color(0xFF20243A),
        incomingBubble = Color(0xFF20243A),
        incomingText = Color(0xFFF5F5FB),
        outgoingBubble = Color(0xFF7A67FF),
        outgoingText = Color.White,
        accent = Color(0xFF9A8BFF),
        accentSoft = Color(0xFF2B2750),
        ink = Color(0xFFF5F5FB),
        muted = Color(0xFFA7AAC1),
        border = Color(0xFF30354E),
        composer = Color(0xFF171A2C),
    )

    val Aurora = NovaChatPalette(
        key = "aurora",
        label = "Aurora",
        background = Color(0xFFF5F7FF),
        surface = Color(0xFFFFFFFF),
        raisedSurface = Color(0xFFF0F2FF),
        incomingBubble = Color(0xFFFFFFFF),
        incomingText = Color(0xFF171829),
        outgoingBubble = Color(0xFF5A54D6),
        outgoingText = Color.White,
        accent = Color(0xFF4C72E8),
        accentSoft = Color(0xFFE7ECFF),
        ink = Color(0xFF171829),
        muted = Color(0xFF71758D),
        border = Color(0xFFE0E4F3),
        composer = Color(0xFFFFFFFF),
    )

    val Ocean = NovaChatPalette(
        key = "ocean",
        label = "Ocean",
        background = Color(0xFFF3FAFC),
        surface = Color(0xFFFFFFFF),
        raisedSurface = Color(0xFFEAF7FA),
        incomingBubble = Color(0xFFFFFFFF),
        incomingText = Color(0xFF12242A),
        outgoingBubble = Color(0xFF087E99),
        outgoingText = Color.White,
        accent = Color(0xFF087E99),
        accentSoft = Color(0xFFDDF3F7),
        ink = Color(0xFF12242A),
        muted = Color(0xFF627A81),
        border = Color(0xFFD8EBEF),
        composer = Color(0xFFFFFFFF),
    )

    val Rose = NovaChatPalette(
        key = "rose",
        label = "Rose",
        background = Color(0xFFFFF7FA),
        surface = Color(0xFFFFFFFF),
        raisedSurface = Color(0xFFFFEFF5),
        incomingBubble = Color(0xFFFFFFFF),
        incomingText = Color(0xFF2B1720),
        outgoingBubble = Color(0xFFB43E72),
        outgoingText = Color.White,
        accent = Color(0xFFB43E72),
        accentSoft = Color(0xFFFFE2EE),
        ink = Color(0xFF2B1720),
        muted = Color(0xFF886A77),
        border = Color(0xFFF1D9E3),
        composer = Color(0xFFFFFFFF),
    )

    val Ember = NovaChatPalette(
        key = "ember",
        label = "Ember",
        background = Color(0xFFFFF8F3),
        surface = Color(0xFFFFFFFF),
        raisedSurface = Color(0xFFFFEEE3),
        incomingBubble = Color(0xFFFFFFFF),
        incomingText = Color(0xFF2A1C16),
        outgoingBubble = Color(0xFFD65A3A),
        outgoingText = Color.White,
        accent = Color(0xFFD65A3A),
        accentSoft = Color(0xFFFFE3D8),
        ink = Color(0xFF2A1C16),
        muted = Color(0xFF896F64),
        border = Color(0xFFF0DDD4),
        composer = Color(0xFFFFFFFF),
    )

    val All = listOf(Nova, Midnight, Aurora, Ocean, Rose, Ember)
    private val byKey = All.associateBy { it.key }

    fun resolve(key: String?): NovaChatPalette = byKey[key?.trim()?.lowercase()] ?: Nova
    fun isSupported(key: String): Boolean = byKey.containsKey(key.trim().lowercase())
}


val LocalNovaChatPalette = staticCompositionLocalOf { NovaChatThemes.Nova }
