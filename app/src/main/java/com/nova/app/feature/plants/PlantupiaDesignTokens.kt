package com.nova.app.feature.plants

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalPlantupiaNoticeCount = staticCompositionLocalOf { 0 }
internal val LocalPlantupiaNoticeCountChanged = staticCompositionLocalOf<(Int) -> Unit> { {} }

/**
 * Shared native Plantupia visual roles, derived from the approved brand direction.
 *
 * Use Nova's existing Material3, icons, media and motion systems; these roles
 * provide a distinct living-space language without replacing those engines.
 * Playfair/Inter are intentionally NOT bundled until licensed brand fonts are supplied.
 */
internal object PlantupiaColors {
    val forest = Color(0xFF123B2E)
    val forestText = Color(0xFF16302A)
    val cream = Color(0xFFF7F7F2)
    val paper = Color(0xFFFFFEFB)
    val sage = Color(0xFF9FB79A)
    val mutedSage = Color(0xFF7E9C87)
    val softSage = Color(0xFFE9EDE3)
    val border = Color(0xFFD8DED6)
    val secondaryText = Color(0xFF4A5A50)
    val errorText = Color(0xFF9A4A31)
}

internal object PlantupiaShapes {
    val card = RoundedCornerShape(16.dp)
    val inset = RoundedCornerShape(12.dp)
    val badge = RoundedCornerShape(8.dp)
}

internal object PlantupiaText {
    // Reserve expressive serif typography for living subjects, not routine navigation.
    val display = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 43.sp,
        letterSpacing = (-.5).sp, color = PlantupiaColors.forestText)
    val pageHeading = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-.35).sp, color = PlantupiaColors.forestText)
    val itemTitle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp, color = PlantupiaColors.forestText)
    val editorialHeading = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = PlantupiaColors.forestText,
    )
    val sectionLabel = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = PlantupiaColors.forest,
    )
    val body = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        color = PlantupiaColors.forestText,
    )
    val supporting = TextStyle(
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = PlantupiaColors.secondaryText,
    )
}
