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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * An index/card cover may crop for composition; an observation is documentary
 * evidence and MUST keep the full source frame inside its container.
 * Neither policy alters uploaded bytes or invents missing photo content.
 */
internal enum class PlantupiaPhotoFraming(val contentScale: ContentScale) {
    COVER(ContentScale.Crop),
    FULL_RECORD(ContentScale.Fit),
}

/**
 * A fullscreen viewer is only actionable when an actual uploaded photo has
 * loaded. An unavailable URL or decorative placeholder cannot be enlarged.
 */
internal fun canOpenPlantupiaPhoto(photoUrl: String, loaded: Boolean): Boolean =
    photoUrl.isNotBlank() && loaded

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
    compact: Boolean = false,
    framing: PlantupiaPhotoFraming = PlantupiaPhotoFraming.COVER,
    onPhotoLoadChanged: ((Boolean) -> Unit)? = null,
) {
    var photoLoaded by remember(photoUrl) { mutableStateOf(false) }
    var photoFailed by remember(photoUrl) { mutableStateOf(false) }
    var retryIndex by remember(photoUrl) { mutableIntStateOf(0) }
    // A reused card must report an unloaded new URL before the prior image
    // can influence an overlaid title's contrast or claim a photo is visible.
    LaunchedEffect(photoUrl, photoLoaded) {
        onPhotoLoadChanged?.invoke(canOpenPlantupiaPhoto(photoUrl, photoLoaded))
    }
    val photoCanOpen = canOpenPlantupiaPhoto(photoUrl, photoLoaded) && onOpenPhoto != null

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(PlantupiaColors.softSage)
            .then(
                if (compact && !photoLoaded) {
                    Modifier.semantics {
                        contentDescription = when {
                            photoUrl.isBlank() -> "No uploaded photo for $photoDescription"
                            photoFailed -> "Photo unavailable for $photoDescription"
                            else -> "Loading photo for $photoDescription"
                        }
                    }
                } else Modifier
            )
            .then(if (photoCanOpen) Modifier.clickable(onClick = { onOpenPhoto?.invoke() }) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoLoaded) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PlantupiaMissingPhotoMark(Modifier.size(if (compact) 32.dp else 45.dp))
                if (!compact) Text(
                    text = when {
                        photoUrl.isBlank() -> "No photo added"
                        photoFailed -> "Photo unavailable"
                        else -> "Loading photo"
                    },
                    style = PlantupiaText.supporting,
                )
                // Keep the tiny Recent Moments card compact. The full plant
                // detail can retry a failed media GET without mutating history
                // or creating a new user upload.
                if (photoFailed && onOpenPhoto != null) {
                    TextButton(
                        onClick = {
                            photoFailed = false
                            retryIndex += 1
                        },
                    ) {
                        Text("Retry photo", color = PlantupiaColors.forest)
                    }
                }
            }
        }

        if (photoUrl.isNotBlank()) {
            // Force only the image request to restart on explicit Retry;
            // the URL, account and private event are left untouched.
            key(photoUrl, retryIndex) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = if (photoLoaded) photoDescription else null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = framing.contentScale,
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
}
