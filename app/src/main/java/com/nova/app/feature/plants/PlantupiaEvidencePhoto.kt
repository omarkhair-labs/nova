package com.nova.app.feature.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * A real user's uploaded plant photo, NEVER a synthetic plant or stock-room
 * picture. A missing, loading, or broken photo has an honest illustrated state.
 * It may open fullscreen only AFTER the real photo actually loads.
 *
 * The parent must supply a real size, e.g. fillMaxWidth + height.
 */
@Composable
internal fun PlantupiaEvidencePhoto(
    photoUrl: String,
    photoDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    onOpenPhoto: (() -> Unit)? = null,
) {
    var photoLoaded by remember(photoUrl) { mutableStateOf(false) }
    var photoFailed by remember(photoUrl) { mutableStateOf(false) }
    val photoCanOpen = photoLoaded && onOpenPhoto != null

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(PlantupiaColors.softSage)
            .then(if (photoCanOpen) Modifier.clickable(onClick = { onOpenPhoto?.invoke() }) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoLoaded) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PlantupiaMissingPhotoMark(Modifier.size(45.dp))
                Text(
                    text = when {
                        photoUrl.isBlank() -> "No photo added"
                        photoFailed -> "Photo unavailable"
                        else -> "Loading photo"
                    },
                    style = PlantupiaText.supporting,
                )
            }
        }

        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = if (photoLoaded) photoDescription else null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onSuccess = {
                    photoLoaded = true
                    photoFailed = false
                },
                onError = {
                    photoLoaded = false
                    photoFailed = true
                },
            )
        }
    }
}
