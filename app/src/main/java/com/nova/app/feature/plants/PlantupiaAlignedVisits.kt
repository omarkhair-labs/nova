package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaAlignedVisits(pair: PlantupiaLoadedPhotoPair, close: () -> Unit) {
    var earlier by remember(pair) { mutableStateOf(emptyList<VisitPoint>()) }
    var later by remember(pair) { mutableStateOf(emptyList<VisitPoint>()) }
    var opacity by remember(pair) { mutableFloatStateOf(.5f) }
    val editingEarlier = earlier.size < 4
    val current = if (editingEarlier) earlier else later
    val currentPhoto = if (editingEarlier) pair.earlier else pair.later
    val transform = remember(earlier, later) { fitVisitAlignment(earlier, later) }
    Text("Align two visits", style = PlantupiaText.editorialHeading)
    if (transform != null) {
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clipToBounds().background(PlantupiaColors.forest)) {
            AsyncImage(pair.earlier.photoUrl, "Earlier record", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            AsyncImage(
                pair.later.photoUrl, "Aligned later record",
                Modifier.fillMaxSize().graphicsLayer {
                    val zoom = hypot(transform.a, transform.b).toFloat()
                    scaleX = zoom
                    scaleY = zoom
                    rotationZ = Math.toDegrees(atan2(transform.b, transform.a)).toFloat()
                    translationX = transform.x.toFloat() * size.width
                    translationY = transform.y.toFloat() * size.width
                    alpha = opacity
                },
                contentScale = ContentScale.Fit,
            )
        }
        Slider(opacity, { opacity = it }, modifier = Modifier.semantics {
            contentDescription = "Later photograph opacity"
        })
        Text("Blend the earlier and later photographs. Alignment is based on your reference marks.", style = PlantupiaText.body)
        Text("Viewpoint, light and moving foliage can still change the image.", style = PlantupiaText.supporting)
    } else {
        Text(
            if (later.size == 4) "The reference marks disagree. Try again with fixed points spread across the background."
            else "On each photo, mark four of the same stationary points in the same order. Use a wall, shelf or fixed edge.",
            style = PlantupiaText.body,
        )
        Text("${currentPhoto.observedAt.take(10)} · ${current.size} of 4 marks", style = PlantupiaText.sectionLabel)
        if (current.size < 4) {
            Text(if (current.size == 3) "Place the fourth mark away from the first three to check the alignment."
                else "Tap a fixed detail. Keep the marks spread apart.", style = PlantupiaText.supporting)
        }
        VisitReferencePhoto(currentPhoto.photoUrl, current) { mark ->
            if (editingEarlier && earlier.size < 4) earlier = earlier + mark
            else if (later.size < 4) later = later + mark
        }
    }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (transform == null && (earlier.isNotEmpty() || later.isNotEmpty())) {
            TextButton(onClick = {
                if (later.isNotEmpty()) later = later.dropLast(1) else earlier = earlier.dropLast(1)
            }) { Text("Undo last mark") }
        }
        TextButton(onClick = { earlier = emptyList(); later = emptyList(); opacity = .5f },
            enabled = earlier.isNotEmpty()) { Text("Start over") }
        TextButton(onClick = close) { Text("Close alignment") }
    }
}

@Composable
private fun VisitReferencePhoto(url: String, marks: List<VisitPoint>, onMark: (VisitPoint) -> Unit) {
    var aspect by remember(url) { mutableFloatStateOf(0f) }
    var failed by remember(url) { mutableStateOf(false) }
    val add = rememberUpdatedState(onMark)
    Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f).clipToBounds().background(PlantupiaColors.forest)) {
        AsyncImage(
            model = url,
            contentDescription = "Photograph for stationary reference marks",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            onSuccess = { result ->
                val image = result.result.image
                failed = false
                if (image.width > 0 && image.height > 0) aspect = image.width.toFloat() / image.height
            },
            onError = { failed = true; aspect = 0f },
        )
        val gesture = if (aspect > 0) Modifier.pointerInput(url, aspect) {
            detectTapGestures { tap ->
                if (size.width <= 0) return@detectTapGestures
                val imageWidth = min(size.width.toFloat(), size.height * aspect)
                val imageHeight = imageWidth / aspect
                val dx = tap.x - size.width / 2f
                val dy = tap.y - size.height / 2f
                if (kotlin.math.abs(dx) <= imageWidth / 2f && kotlin.math.abs(dy) <= imageHeight / 2f) {
                    add.value(VisitPoint((dx / size.width).toDouble(), (dy / size.width).toDouble()))
                }
            }
        } else Modifier
        Canvas(Modifier.fillMaxSize().then(gesture)) {
            marks.forEachIndexed { index, mark ->
                val point = center + Offset(mark.x.toFloat() * size.width, mark.y.toFloat() * size.width)
                drawCircle(PlantupiaColors.forest, (8 + index).dp.toPx(), point)
                drawCircle(PlantupiaColors.paper, (4 + index).dp.toPx(), point)
            }
        }
    }
    if (failed) Text("This record photograph could not load.", style = PlantupiaText.supporting)
}
