package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRoom
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Orthographic projection of the actual reported room revision; no inferred walls or plant volumes. */
internal class RecordedRoomProjection(
    private val width: Double, private val length: Double, private val height: Double?,
    private val canvas: Size, private val yaw: Float, private val elevation: Float, zoom: Float,
) {
    private val h = height ?: 0.0
    private val scale = (canvas.minDimension * .72 / hypot(hypot(width, length), h)).toFloat() * zoom
    fun point(x: Double, y: Double, z: Double = 0.0): Offset {
        val px = x - width / 2
        val py = y - length / 2
        val pz = z - h / 2
        val horizontal = px * cos(yaw) - py * sin(yaw)
        val depth = px * sin(yaw) + py * cos(yaw)
        return Offset(canvas.width / 2 + (horizontal * scale).toFloat(),
            canvas.height / 2 + ((depth * sin(elevation) - pz * cos(elevation)) * scale).toFloat())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaRecordedSpatialPlan(
    room: PlantupiaRoom, width: Double, length: Double, plants: List<PlantupiaPlant>,
    onOpenPlant: (PlantupiaPlant) -> Unit, onReturn: () -> Unit,
) {
    var yaw by remember(room) { mutableFloatStateOf(-.65f) }
    var elevation by remember(room) { mutableFloatStateOf(.67f) }
    var zoom by remember(room) { mutableFloatStateOf(1f) }
    var selected by remember(room) { mutableStateOf<String?>(null) }
    val pose = rememberUpdatedState(Triple(yaw, elevation, zoom))
    val selectable = rememberUpdatedState(room.plants)
    val gestures = rememberTransformableState { ratio, pan, _ ->
        yaw = (yaw + pan.x * .012f) % 6.283185f
        elevation = (elevation - pan.y * .008f).coerceIn(.22f, 1.56f)
        zoom = (zoom * ratio).coerceIn(.75f, 2.5f)
    }
    val height = room.height
    val ink = PlantupiaColors.forest
    val paper = PlantupiaColors.paper
    val windowInk = Color(0xFF477F83)
    Text("Recorded spatial plan", style = PlantupiaText.editorialHeading)
    Text("Revision ${room.revision} · " + (if (room.historical) "Earlier layout" else "Current layout"),
        style = PlantupiaText.supporting)
    Text(String.format(Locale.ROOT, "%.2f × %.2f m", width, length) +
        (height?.let { String.format(Locale.ROOT, " × %.2f m", it) } ?: ""),
        style = PlantupiaText.itemTitle)
    Canvas(Modifier.fillMaxWidth().height(340.dp).clip(PlantupiaShapes.inset)
        .background(PlantupiaColors.cream).padding(12.dp)
        .semantics { contentDescription = "3D view of recorded room geometry" }
        .transformable(gestures)
        .pointerInput(width, length, height, room.revision) {
            detectTapGestures { tap ->
                val (a,b,c) = pose.value
                val projection = RecordedRoomProjection(width,length,height,
                    Size(size.width.toFloat(),size.height.toFloat()),a,b,c)
                selected = selectable.value.map { it.id to (projection.point(it.x,it.y) - tap).getDistance() }
                    .filter { it.second <= 27.dp.toPx() }.minByOrNull { it.second }?.first
            }
        }) {
        val projection = RecordedRoomProjection(width,length,height,size,yaw,elevation,zoom)
        val corners = listOf(0.0 to 0.0,width to 0.0,width to length,0.0 to length)
        val floor = corners.map { (x,y) -> projection.point(x,y) }
        fun path(points: List<Offset>) = Path().apply {
            moveTo(points[0].x,points[0].y)
            points.drop(1).forEach { lineTo(it.x,it.y) }
            close()
        }
        drawPath(path(floor), PlantupiaColors.softSage.copy(alpha=.68f))
        // A consistent quarter grid communicates scale without asserting plant geometry.
        for (i in 1..3) {
            val x = width*i/4
            val y = length*i/4
            drawLine(ink.copy(alpha=.15f), projection.point(x,0.0), projection.point(x,length),1.dp.toPx())
            drawLine(ink.copy(alpha=.15f), projection.point(0.0,y), projection.point(width,y),1.dp.toPx())
        }
        drawPath(path(floor), ink, style=Stroke(1.8.dp.toPx()))
        if (height != null) {
            val top = corners.map { (x,y) -> projection.point(x,y,height) }
            drawPath(path(top), ink.copy(alpha=.54f),style=Stroke(1.2.dp.toPx()))
            floor.zip(top).forEach { (bottom,upper) ->
                drawLine(ink.copy(alpha=.58f),bottom,upper,1.2.dp.toPx())
            }
        }
        for (window in room.windows) {
            val mark = projection.point(window.x,window.y)
            // Centre recorded in plan; no assumed opening width or elevation.
            val s = 6.dp.toPx()
            drawRect(paper, mark-Offset(s,s), Size(2*s,2*s))
            drawRect(windowInk, mark-Offset(s,s), Size(2*s,2*s),style=Stroke(2.dp.toPx()))
        }
        room.plants.sortedBy { projection.point(it.x,it.y).y }.forEach { plant ->
            val p = projection.point(plant.x,plant.y)
            if (selected == plant.id) drawCircle(ink.copy(alpha=.2f),16.dp.toPx(),p)
            drawCircle(paper,9.dp.toPx(),p)
            drawCircle(ink,8.dp.toPx(),p,style=Stroke(1.5.dp.toPx()))
            drawCircle(if(selected==plant.id) ink else PlantupiaColors.mutedSage,4.dp.toPx(),p)
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement=Arrangement.spacedBy(2.dp)) {
        TextButton(onClick={yaw-=.4f}) { Text("Turn left") }
        TextButton(onClick={yaw+=.4f}) { Text("Turn right") }
        TextButton(onClick={elevation=if(elevation<1f)1.48f else .67f}) { Text("Change tilt") }
        TextButton(onClick={zoom=if(zoom>1.1f)1f else 1.65f}) { Text("Change zoom") }
        TextButton(onClick={yaw=-.65f;elevation=.67f;zoom=1f}) { Text("Reset view") }
    }
    Text(if(height==null) "Height has not been recorded. Only the reported floor plane is displayed."
        else "Wireframe uses the reported width, length and height.", style=PlantupiaText.supporting)
    Text("Circles are recorded plant positions; squares mark window centres projected on the floor. Drag to orbit or pinch to zoom. No plant shape or camera scan is inferred.",
        style=PlantupiaText.supporting)
    if(room.plants.isEmpty()) Text("No recorded plant positions.",style=PlantupiaText.supporting)
    room.plants.forEach { point ->
        plants.find { it.id==point.id }?.let { plant ->
            PlantupiaActionRow(icon=com.nova.app.ui.icons.NovaIconAsset.Home,
                title=plant.name,
                detail=(if(selected==plant.id) "Selected · " else "")+
                    String.format(Locale.ROOT,"%.2f, %.2f m",point.x,point.y)+
                    (point.latest.takeIf{it.isNotBlank()}?.let{" · "+it.take(10)}?:""),
                accent=selected==plant.id,onClick={selected=point.id})
        }
    }
    val focused=plants.firstOrNull{it.id==selected}
    if(focused!=null) OutlinedButton(onClick={onOpenPlant(focused)},
        modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Open "+focused.name) }
    TextButton(onClick=onReturn) { Text("Return to floor plan") }
}
