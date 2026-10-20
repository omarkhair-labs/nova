package com.nova.app.feature.plants

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val cream = Color(0xFFF3EEDE)
    val paper = Color(0xFFFFFDF7)
    val sage = Color(0xFF9FB79A)
    val mutedSage = Color(0xFF7E9C87)
    val softSage = Color(0xFFE9EDE3)
    val border = Color(0xFFD5DED1)
    val secondaryText = Color(0xFF4A5A50)
    val errorText = Color(0xFF9A4A31)
}

internal object PlantupiaShapes {
    val card = RoundedCornerShape(20.dp)
    val inset = RoundedCornerShape(14.dp)
    val badge = RoundedCornerShape(10.dp)
}

internal object PlantupiaText {
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
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = PlantupiaColors.secondaryText,
    )
}
