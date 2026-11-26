package com.nova.app.feature.plants

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.nova.app.R
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** A private specimen passport. Decorative marks never stand in for evidence photos. */
@Composable
internal fun PlantupiaSignatureIdentity(
    name: String,
    context: String,
    identity: String,
    qa: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(name, style = PlantupiaText.display.copy(fontSize = 28.sp, lineHeight = 34.sp))
        if (context.isNotBlank()) Text(context, style = PlantupiaText.body)
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PlantupiaEditorialMark("plant", Modifier.size(18.dp))
            Text(signatureIdentityLabel(identity, qa), style = PlantupiaText.supporting,
                modifier = Modifier.weight(1f))
        }
    }
}

/** Topic-specific original pictogram on a ruled field plate; never a scientific measurement. */
@Composable
internal fun PlantupiaSignatureFieldPlate(topic: String) {
    val palette = careChapterPalette(topic)
    val resource = when (topic) {
        "water" -> R.drawable.plantupia_signature_plate_water
        "light" -> R.drawable.plantupia_signature_plate_light
        "soil" -> R.drawable.plantupia_signature_plate_soil
        "humidity" -> R.drawable.plantupia_signature_plate_humidity
        "fertilizing" -> R.drawable.plantupia_signature_plate_fertilizing
        "pruning" -> R.drawable.plantupia_signature_plate_pruning
        "repotting" -> R.drawable.plantupia_signature_plate_repotting
        "inspection" -> R.drawable.plantupia_signature_plate_inspection
        "warnings" -> R.drawable.plantupia_signature_plate_warnings
        else -> null
    }
    var open by remember(topic) { mutableStateOf(false) }
    val plate: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (resource != null) {
                Image(painterResource(resource), null, Modifier.fillMaxWidth().height(172.dp))
                Text("Examine illustration", style = PlantupiaText.supporting.copy(color = palette.ink))
            } else {
                Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                    CareChapterIllustration(topic, palette.ink, Modifier.size(80.dp).clearAndSetSemantics { })
                }
            }
        }
    }
    if (resource != null) {
        Surface(onClick = { open = true }, color = palette.background, shape = RoundedCornerShape(4.dp), content = plate)
        if (open) PlantupiaPlateViewer(resource, palette) { open = false }
    } else Surface(color = palette.background, content = plate)
}

@Composable
private fun PlantupiaPlateViewer(resource: Int, palette: CareChapterPalette, close: () -> Unit) {
    val painter = painterResource(resource)
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val gestures = rememberTransformableState { ratio, move, _ ->
        val scale = (zoom * ratio).coerceIn(1f, 4f)
        val limit = plantupiaPhotoPanLimits(
            viewport.width.toFloat(), viewport.height.toFloat(),
            painter.intrinsicSize.width, painter.intrinsicSize.height, scale,
        )
        pan = Offset(
            plantupiaClampPhotoPan(pan.x + move.x, limit.maxX),
            plantupiaClampPhotoPan(pan.y + move.y, limit.maxY),
        )
        zoom = scale
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = palette.background) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        zoom = if (zoom > 1f) 1f else 2f
                        pan = Offset.Zero
                    }) { Text(if (zoom > 1f) "Reset zoom" else "Zoom 2×", color = palette.ink) }
                    TextButton(onClick = close) { Text("Close illustration", color = palette.ink) }
                }
                Box(
                    Modifier.weight(1f).fillMaxWidth().onSizeChanged {
                        viewport = it
                        pan = Offset.Zero
                    }.clipToBounds().transformable(gestures),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painter,
                        contentDescription = palette.label + " topic illustration",
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = pan.x
                            translationY = pan.y
                        },
                    )
                }
                Text(palette.label, color = palette.ink, style = PlantupiaText.editorialHeading,
                    modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}
