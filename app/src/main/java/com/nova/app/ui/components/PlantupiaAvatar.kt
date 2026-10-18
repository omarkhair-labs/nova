package com.nova.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.nova.app.R

/**
 * Authored, human-first Plantupia identity.
 *
 * The eight choices are real native vector assets (not a name initial, a demo
 * URL, or fake photographs). Every choice has its own designed full portrait
 * and reduced-detail silhouette for 24-40dp. Photo always takes precedence.
 *
 * IMPORTANT: public API identity/saved -1..7 choices are not migrated.
 */
internal data class PlantupiaAvatarStyle(val figure: Int, val palette: Int, val accent: Int)

internal enum class PlantupiaPortraitDetail { Micro, Standard, Hero }

internal fun plantupiaPortraitDetail(sizeDp: Float): PlantupiaPortraitDetail = when {
    sizeDp <= 40f -> PlantupiaPortraitDetail.Micro
    sizeDp < 80f -> PlantupiaPortraitDetail.Standard
    else -> PlantupiaPortraitDetail.Hero
}

internal fun plantupiaAvatarStyle(userId: Long, chosenVariant: Int = -1): PlantupiaAvatarStyle {
    val id = userId.coerceAtLeast(0L)
    val figure = chosenVariant.takeIf { it in 0..7 } ?: Math.floorMod(id, 8L).toInt()
    // Keep original selection/fallback mapping stable, even though the new
    // authored asset kit owns each portrait's calibrated background palette.
    return PlantupiaAvatarStyle(
        figure = figure,
        palette = if (chosenVariant in 0..7) (figure * 3) % 5 else Math.floorMod(id / 8L + id, 5L).toInt(),
        accent = if (chosenVariant in 0..7) figure % 3 else Math.floorMod(id / 40L + id, 3L).toInt(),
    )
}

/** One resource mapping used by Me, Create, People, Posts, Circle and DM. */
internal fun plantupiaPortraitResource(figure: Int, detail: PlantupiaPortraitDetail): Int {
    val micro = detail == PlantupiaPortraitDetail.Micro
    return when (figure) {
        0 -> if (micro) R.drawable.plantupia_artlab_window_micro else R.drawable.plantupia_artlab_window
        1 -> if (micro) R.drawable.plantupia_artlab_courtyard_micro else R.drawable.plantupia_artlab_courtyard
        2 -> if (micro) R.drawable.plantupia_portrait_2_micro else R.drawable.plantupia_portrait_2_hero
        3 -> if (micro) R.drawable.plantupia_portrait_3_micro else R.drawable.plantupia_portrait_3_hero
        4 -> if (micro) R.drawable.plantupia_portrait_4_micro else R.drawable.plantupia_portrait_4_hero
        5 -> if (micro) R.drawable.plantupia_portrait_5_micro else R.drawable.plantupia_portrait_5_hero
        6 -> if (micro) R.drawable.plantupia_portrait_6_micro else R.drawable.plantupia_portrait_6_hero
        7 -> if (micro) R.drawable.plantupia_portrait_7_micro else R.drawable.plantupia_portrait_7_hero
        else -> if (micro) R.drawable.plantupia_artlab_window_micro else R.drawable.plantupia_artlab_window
    }
}

@Composable
fun PlantupiaAvatar(
    userId: Long,
    displayName: String,
    photoUrl: String,
    size: Dp,
    modifier: Modifier = Modifier,
    chosenVariant: Int = -1,
) {
    val style = remember(userId, chosenVariant) { plantupiaAvatarStyle(userId, chosenVariant) }
    val portrait = plantupiaPortraitResource(style.figure, plantupiaPortraitDetail(size.value))
    var photoLoaded by remember(photoUrl) { mutableStateOf(false) }
    val name = displayName.ifBlank { "this person" }

    Box(
        modifier = modifier.size(size).clip(CircleShape).semantics(mergeDescendants = true) {
            contentDescription = if (photoLoaded) "Profile photo for $name" else "Profile illustration for $name"
        },
    ) {
        Image(
            painter = painterResource(portrait),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        // Never replace a real uploaded photo with an illustration when the
        // image can be loaded. Offline/missing photos fall back to the same ID.
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
