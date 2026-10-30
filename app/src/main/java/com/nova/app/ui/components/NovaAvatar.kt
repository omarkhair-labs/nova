package com.nova.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft


@Composable
fun NovaAvatar(
    source: String,
    fallbackText: String,
    size: Dp = 86.dp,
    modifier: Modifier = Modifier,
    plantupiaUserId: Long? = null,
    plantupiaAvatarVariant: Int = -1,
) {
    // Explicit Plantupia opt-in preserves Nova's existing shared identity seam.
    // Default/legacy Nova rendering is unchanged, including real uploaded media.
    if (plantupiaUserId != null) {
        PlantupiaAvatar(
            userId = plantupiaUserId,
            displayName = fallbackText,
            photoUrl = source,
            size = size,
            modifier = modifier,
            chosenVariant = plantupiaAvatarVariant,
        )
        return
    }
    val fallbackInitial = fallbackText.trim().firstOrNull()?.uppercase()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(NovaAccentSoft),
        contentAlignment = Alignment.Center,
    ) {
        if (fallbackInitial != null) {
            Text(
                text = fallbackInitial,
                color = NovaAccent,
                fontSize = (size.value * 0.34f).sp,
                fontWeight = FontWeight.Bold,
            )
        } else {
            NovaBrandMark(
                modifier = Modifier.fillMaxSize(0.54f),
                monochrome = true,
                monochromeColor = NovaAccent,
            )
        }

        if (source.isNotBlank()) {
            AsyncImage(
                model = source,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
