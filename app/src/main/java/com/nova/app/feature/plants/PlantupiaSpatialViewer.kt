package com.nova.app.feature.plants

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaSpatialViewer(spaceName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var points by remember { mutableStateOf(emptyList<SpatialPoint>()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            error = ""
            scope.launch {
                try {
                    points = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use(::readPlantupiaPointCloud)
                            ?: throw IllegalArgumentException("The selected file couldn't be read.")
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (invalid: IllegalArgumentException) {
                    error = invalid.message ?: "This scene format is unavailable."
                } catch (_: java.io.IOException) {
                    error = "The selected scene couldn't be read."
                } catch (_: SecurityException) {
                    error = "Access to the selected scene ended. Choose it again."
                } finally {
                    busy = false
                }
            }
        }
    }
    Text("Point cloud study", style = PlantupiaText.editorialHeading)
    Text("Open a Plantupia reconstruction export to examine it alongside $spaceName.", style = PlantupiaText.body)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(enabled = !busy, onClick = onBack) { Text("Return to room") }
        TextButton(enabled = !busy, onClick = { picker.launch(arrayOf("*/*")) }) { Text("Open PLY scene") }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (error.isNotBlank()) Text(error, style = PlantupiaText.supporting)
    if (points.isNotEmpty()) {
        PointCloudStage(points)
        Text("Relative coordinates · ${points.size} visible points", style = PlantupiaText.supporting)
    }
    Text("The scene stays on this device for this session. The floor plan uses its own recorded measurements.",
        style = PlantupiaText.supporting)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PointCloudStage(points: List<SpatialPoint>) {
    var yaw by remember(points) { mutableFloatStateOf(0f) }
    var pitch by remember(points) { mutableFloatStateOf(0f) }
    var zoom by remember(points) { mutableFloatStateOf(1f) }
    val gestures = rememberTransformableState { ratio, pan, _ ->
        yaw = (yaw + pan.x * .012f) % 6.283185f
        pitch = (pitch - pan.y * .012f).coerceIn(-1.5f, 1.5f)
        zoom = (zoom * ratio).coerceIn(.55f, 2.8f)
    }
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(1.15f).clipToBounds().background(Color(0xFF12261F))
            .semantics { contentDescription = "Interactive reconstructed point cloud" }.transformable(gestures),
    ) {
        val cy = cos(yaw)
        val sy = sin(yaw)
        val cp = cos(pitch)
        val sp = sin(pitch)
        val projected = points.map { point ->
            val rotatedX = point.x * cy - point.z * sy
            val depth = point.x * sy + point.z * cy
            val rotatedY = point.y * cp - depth * sp
            ProjectedSpatialPoint(point, rotatedX, rotatedY, point.y * sp + depth * cp)
        }.sortedByDescending { it.depth }
        val scale = size.minDimension * .33f * zoom
        for ((point, x, y, depth) in projected) {
            val perspective = 4.5 / (4.5 + depth)
            val visualX = center.x + (x * perspective * scale).toFloat()
            val visualY = center.y + (y * perspective * scale).toFloat()
            val alpha = (.93 - .07 * depth).coerceIn(.55, 1.0).toFloat()
            drawCircle(
                Color(point.red, point.green, point.blue).copy(alpha = alpha),
                radius = (1.3 * perspective).toFloat().dp.toPx(),
                center = Offset(visualX, visualY),
            )
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { yaw -= .35f }) { Text("Turn left") }
        TextButton(onClick = { yaw += .35f }) { Text("Turn right") }
        TextButton(onClick = { pitch = (pitch + .30f).coerceAtMost(1.5f) }) { Text("Tilt view") }
        TextButton(onClick = { zoom = if (zoom > 1f) 1f else 1.65f }) { Text("Change zoom") }
        TextButton(onClick = { yaw = 0f; pitch = 0f; zoom = 1f }) { Text("Reset view") }
    }
}

private data class ProjectedSpatialPoint(val colorPoint: SpatialPoint, val x: Double, val y: Double, val depth: Double)
