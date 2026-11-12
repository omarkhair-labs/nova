package com.nova.app.feature.plants

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset

/** Native image detail on top of a real plant's original user-uploaded photo. */
@Composable
internal fun PlantupiaPhotoViewer(
    photoUrl: String,
    plantName: String,
    onClose: () -> Unit,
) {
    var zoom by remember(photoUrl) { mutableFloatStateOf(1f) }
    var pan by remember(photoUrl) { mutableStateOf(Offset.Zero) }
    var imageLoaded by remember(photoUrl) { mutableStateOf(false) }
    var imageFailed by remember(photoUrl) { mutableStateOf(false) }
    var retryIndex by remember(photoUrl) { mutableIntStateOf(0) }
    var viewport by remember(photoUrl) { mutableStateOf(IntSize.Zero) }
    var imageSize by remember(photoUrl) { mutableStateOf(IntSize.Zero) }

    // Transformable detects gestures, but its translation has to follow the
    // actual fitted image bounds, not fixed px that break across aspect ratios.
    val transform = rememberTransformableState { zoomDelta, panDelta, _ ->
        val nextZoom = (zoom * zoomDelta).coerceIn(1f, 4f)
        val limits = plantupiaPhotoPanLimits(
            viewport.width.toFloat(), viewport.height.toFloat(),
            imageSize.width.toFloat(), imageSize.height.toFloat(), nextZoom,
        )
        pan = Offset(
            x = plantupiaClampPhotoPan(pan.x + panDelta.x, limits.maxX),
            y = plantupiaClampPhotoPan(pan.y + panDelta.y, limits.maxY),
        )
        zoom = nextZoom
    }
    // Rotation/screen resize and an asynchronously decoded image must not
    // leave the photo permanently translated outside the visible viewport.
    LaunchedEffect(viewport, imageSize, zoom) {
        val limits = plantupiaPhotoPanLimits(
            viewport.width.toFloat(), viewport.height.toFloat(),
            imageSize.width.toFloat(), imageSize.height.toFloat(), zoom,
        )
        pan = Offset(
            plantupiaClampPhotoPan(pan.x, limits.maxX),
            plantupiaClampPhotoPan(pan.y, limits.maxY),
        )
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101C16)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Photo of " + plantName,
                        color = Color(0xFFFFFDF7),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (imageLoaded) {
                        TextButton(
                            onClick = {
                                val nextZoom = if (zoom > 1f) 1f else 2f
                                val limits = plantupiaPhotoPanLimits(
                                    viewport.width.toFloat(), viewport.height.toFloat(),
                                    imageSize.width.toFloat(), imageSize.height.toFloat(), nextZoom,
                                )
                                zoom = nextZoom
                                pan = Offset(
                                    plantupiaClampPhotoPan(pan.x, limits.maxX),
                                    plantupiaClampPhotoPan(pan.y, limits.maxY),
                                )
                            },
                        ) {
                            Text(
                                if (zoom > 1f) "Reset" else "Zoom 2×",
                                color = Color(0xFFE1E8DC),
                            )
                        }
                    }
                    IconButton(onClick = onClose) {
                        NovaIcon(
                            asset = NovaIconAsset.Close,
                            contentDescription = "Close photo",
                            modifier = Modifier.size(23.dp),
                            tint = Color.White,
                        )
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize()
                        .onSizeChanged { viewport = it }
                        .clipToBounds()
                        .transformable(state = transform, enabled = imageLoaded),
                    contentAlignment = Alignment.Center,
                ) {
                    if (photoUrl.isNotBlank()) {
                        key(photoUrl, retryIndex) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = if (imageLoaded) "Actual photo of plant " + plantName else null,
                                modifier = Modifier.fillMaxSize().graphicsLayer {
                                    scaleX = zoom
                                    scaleY = zoom
                                    translationX = pan.x
                                    translationY = pan.y
                                },
                                contentScale = ContentScale.Fit,
                                onSuccess = { state ->
                                    // Decode dimensions describe the source's aspect,
                                    // even if Coil scaled the resource for this request.
                                    imageSize = IntSize(
                                        state.result.image.width.coerceAtLeast(0),
                                        state.result.image.height.coerceAtLeast(0),
                                    )
                                    imageLoaded = true
                                    imageFailed = false
                                },
                                onError = {
                                    imageLoaded = false
                                    imageSize = IntSize.Zero
                                    imageFailed = true
                                },
                            )
                        }
                    }
                    if (!imageLoaded) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                when {
                                    photoUrl.isBlank() -> "No photo added"
                                    imageFailed -> "Photo unavailable"
                                    else -> "Loading original photo…"
                                },
                                color = Color(0xFFFFFDF7),
                                fontSize = 14.sp,
                            )
                            if (photoUrl.isNotBlank() && imageFailed) {
                                TextButton(
                                    onClick = {
                                        imageFailed = false
                                        zoom = 1f
                                        pan = Offset.Zero
                                        retryIndex += 1
                                    },
                                ) {
                                    Text("Retry original photo", color = Color(0xFFE1E8DC))
                                }
                            }
                        }
                    }
                    if (imageLoaded && zoom <= 1f) {
                        Text(
                            "Pinch to zoom",
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                            color = Color(0xFFE1E8DC),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}
