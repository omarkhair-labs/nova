package com.nova.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.nova.app.feature.plants.PlantupiaColors

/**
 * Original, abstract person-first Plantupia identity family, not a fictional
 * plant portrait or a name-initial. NovaAvatar remains unchanged.
 */
internal data class PlantupiaAvatarStyle(val figure: Int, val palette: Int, val accent: Int)

internal fun plantupiaAvatarStyle(userId: Long): PlantupiaAvatarStyle {
    // Immutable numeric API user ID gives a shared result across devices/screens.
    val id = userId.coerceAtLeast(0L)
    return PlantupiaAvatarStyle(
        figure = Math.floorMod(id, 8L).toInt(),
        palette = Math.floorMod(id / 8L + id, 5L).toInt(),
        accent = Math.floorMod(id / 40L + id, 3L).toInt(),
    )
}

@Composable
fun PlantupiaAvatar(
    userId: Long,
    displayName: String,
    photoUrl: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val style = remember(userId) { plantupiaAvatarStyle(userId) }
    var photoLoaded by remember(photoUrl) { mutableStateOf(false) }
    val name = displayName.ifBlank { "this person" }
    val backgrounds = listOf(
        Color(0xFFF4E8D5), Color(0xFFDDE8DA), Color(0xFFE6E4D4),
        Color(0xFFE1E8E2), Color(0xFFECDFC9),
    )
    val inks = listOf(
        PlantupiaColors.forest, Color(0xFF526859), Color(0xFF536A66),
        Color(0xFF725D4F), Color(0xFF4B6755),
    )
    Box(
        modifier = modifier.size(size).clip(CircleShape).semantics(mergeDescendants = true) {
            contentDescription = if (photoLoaded) "Profile photo for $name" else "Profile illustration for $name"
        },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val ink = inks[style.palette]
            val bg = backgrounds[style.palette]
            val shift = ((style.figure % 4) - 1.5f) * 0.027f
            val cx = w * (0.50f + shift)
            val cy = h * (0.41f + (style.figure / 4) * 0.018f)
            val headRadius = w * (0.166f + (style.figure % 3) * 0.008f)
            drawRect(bg)
            // Architectural arch anchors every member of this illustration family.
            drawRoundRect(
                color = ink.copy(alpha = 0.18f),
                topLeft = Offset(w * 0.13f, h * 0.11f),
                size = Size(w * 0.74f, h * 0.82f),
                cornerRadius = CornerRadius(w * 0.32f),
                style = Stroke(width = w * 0.018f),
            )
            drawRoundRect(
                color = ink,
                topLeft = Offset(w * (0.18f + shift * 0.4f), h * 0.69f),
                size = Size(w * 0.64f, h * 0.40f),
                cornerRadius = CornerRadius(w * (0.21f + (style.figure % 3) * 0.016f)),
            )
            drawRoundRect(
                color = ink,
                topLeft = Offset(cx - headRadius * 0.27f, h * 0.55f),
                size = Size(headRadius * 0.54f, h * 0.19f),
                cornerRadius = CornerRadius(w * 0.025f),
            )
            // Features are drawn, never inferred from a name or demographic data.
            drawOval(
                color = ink,
                topLeft = Offset(cx - headRadius * 1.06f, cy - headRadius * 1.19f),
                size = Size(headRadius * 2.12f, headRadius * 2.38f),
            )
            drawOval(
                color = bg,
                topLeft = Offset(cx - headRadius * 0.64f, cy - headRadius * 0.80f),
                size = Size(headRadius * 1.28f, headRadius * 1.72f),
            )
            drawCircle(ink, radius = w * 0.011f, center = Offset(cx - w * 0.04f, cy))
            drawCircle(ink, radius = w * 0.011f, center = Offset(cx + w * 0.04f, cy))
            drawLine(
                color = ink.copy(alpha = 0.65f),
                start = Offset(cx - w * 0.026f, cy + h * 0.067f),
                end = Offset(cx + w * 0.026f, cy + h * 0.067f),
                strokeWidth = w * 0.011f,
            )
            // Botanical signature stays around the human, never substitutes for them.
            val stemX = w * if (style.accent == 1) 0.17f else 0.83f
            val leafX = w * if (style.accent == 1) 0.20f else 0.72f
            val leafY = h * (0.25f + 0.045f * style.accent)
            drawLine(
                color = ink.copy(alpha = 0.8f),
                start = Offset(stemX, h * 0.55f),
                end = Offset(stemX, leafY),
                strokeWidth = w * 0.019f,
            )
            drawOval(
                color = ink.copy(alpha = 0.77f),
                topLeft = Offset(leafX, leafY),
                size = Size(w * 0.13f, h * 0.08f),
            )
            drawCircle(
                color = PlantupiaColors.paper.copy(alpha = 0.75f),
                radius = w * 0.018f,
                center = Offset(w * 0.22f, h * 0.19f),
            )
        }
        // Authorized user photo wins. Missing/broken media reveals the stable illustration.
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                onSuccess = { photoLoaded = true },
                onError = { photoLoaded = false },
            )
        }
    }
}
