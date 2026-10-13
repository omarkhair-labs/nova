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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val transform = rememberTransformableState { zoomDelta, panDelta, _ ->
        val nextZoom = (zoom * zoomDelta).coerceIn(1f, 4f)
        zoom = nextZoom
        pan = if (nextZoom <= 1f) Offset.Zero else {
            Offset(
                x = (pan.x + panDelta.x).coerceIn(-550f, 550f),
                y = (pan.y + panDelta.y).coerceIn(-850f, 850f),
            )
        }
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
                    )
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
                    modifier = Modifier.fillMaxSize().clipToBounds()
                        .transformable(state = transform),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Actual photo of plant " + plantName,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = pan.x
                            translationY = pan.y
                        },
                        contentScale = ContentScale.Fit,
                    )
                    if (zoom <= 1f) {
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
