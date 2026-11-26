package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nova.app.feature.plants.data.RoomPlant
import com.nova.app.feature.plants.data.RoomWindow
import kotlin.math.min

/** The same metric frame drives drawing and hit testing, including in RTL layouts. */
private class RoomDrawingFrame(width: Double, length: Double, canvas: Size) {
    val scale = min(canvas.width / width, canvas.height / length).toFloat()
    val footprint = Size((width * scale).toFloat(), (length * scale).toFloat())
    val origin = Offset((canvas.width - footprint.width) / 2f, (canvas.height - footprint.height) / 2f)

    fun project(x: Double, y: Double) =
        origin + Offset((x * scale).toFloat(), (y * scale).toFloat())
}

@Composable
internal fun PlantupiaRoomDrawing(
    width: Double,
    length: Double,
    plants: List<RoomPlant>,
    windows: List<RoomWindow>,
    selectedPlant: String? = null,
    selectedWindow: String? = null,
    beam: List<Pair<Double, Double>> = emptyList(),
    sample: Pair<Double, Double>? = null,
    onSelectPlant: ((String?) -> Unit)? = null,
    onChoosePoint: ((Double, Double) -> Unit)? = null,
) {
    val choosePoint = rememberUpdatedState(onChoosePoint)
    val selectPlant = rememberUpdatedState(onSelectPlant)
    val ink = PlantupiaColors.forest
    val paper = PlantupiaColors.paper
    val accent = Color(0xFFAD6732)
    Canvas(
        Modifier.fillMaxWidth().height(264.dp)
            .background(PlantupiaColors.softSage)
            .padding(22.dp)
            .semantics { contentDescription = "Floor plan with ${plants.size} recorded plants and ${windows.size} windows" }
            .pointerInput(width, length, plants) {
                detectTapGestures { tap ->
                    val frame = RoomDrawingFrame(width, length, Size(size.width.toFloat(), size.height.toFloat()))
                    if (frame.scale <= 0f) return@detectTapGestures
                    val choose = choosePoint.value
                    if (choose != null) {
                        val position = (tap - frame.origin) / frame.scale
                        if (position.x > 0 && position.x < width && position.y > 0 && position.y < length) {
                            choose(position.x.toDouble(), position.y.toDouble())
                        }
                    } else {
                        selectPlant.value?.invoke(
                            nearestRoomPlant(plants, tap.x, tap.y, frame.origin.x, frame.origin.y, frame.scale, 24.dp.toPx()),
                        )
                    }
                }
            },
    ) {
        val frame = RoomDrawingFrame(width, length, size)
        drawRect(paper, frame.origin, frame.footprint)
        // Divide the known room into halves and quarters to aid visual orientation.
        for (quarter in 1..3) {
            val x = width * quarter / 4
            val y = length * quarter / 4
            drawLine(ink.copy(alpha = 0.12f), frame.project(x, 0.0), frame.project(x, length), 1.dp.toPx())
            drawLine(ink.copy(alpha = 0.12f), frame.project(0.0, y), frame.project(width, y), 1.dp.toPx())
        }
        if (beam.size >= 3) {
            val outline = Path().apply {
                beam.forEachIndexed { index, (x, y) ->
                    val p = frame.project(x, y)
                    if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }
            drawPath(outline, accent.copy(alpha = 0.23f))
            drawPath(outline, accent, style = Stroke(1.2.dp.toPx()))
        }
        drawRect(ink, frame.origin, frame.footprint, style = Stroke(1.5.dp.toPx()))
        windows.forEach { window ->
            val p = frame.project(window.x, window.y)
            val span = if (window.y == 0.0 || window.y == length) Offset(8.dp.toPx(), 0f)
                else Offset(0f, 8.dp.toPx())
            drawLine(paper, p - span, p + span, 6.dp.toPx(), StrokeCap.Round)
            drawLine(if (window.id == selectedWindow) accent else ink, p - span, p + span, 2.dp.toPx(), StrokeCap.Round)
        }
        plants.forEach { plant ->
            val p = frame.project(plant.x, plant.y)
            if (selectedPlant == plant.id) {
                drawCircle(ink.copy(alpha = 0.2f), 15.dp.toPx(), p)
            }
            drawCircle(ink, 7.dp.toPx(), p)
            drawCircle(PlantupiaColors.softSage, 3.dp.toPx(), p)
        }
        sample?.let { (x, y) ->
            val p = frame.project(x, y)
            val marker = 7.dp.toPx()
            drawCircle(paper, 11.dp.toPx(), p)
            drawCircle(accent, 10.dp.toPx(), p, style = Stroke(1.5.dp.toPx()))
            drawLine(accent, p - Offset(marker, 0f), p + Offset(marker, 0f), 1.5.dp.toPx())
            drawLine(accent, p - Offset(0f, marker), p + Offset(0f, marker), 1.5.dp.toPx())
        }
    }
}
