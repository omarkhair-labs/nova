package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.data.PlantupiaRoom
import com.nova.app.feature.plants.data.RoomPlant
import com.nova.app.feature.plants.data.RoomDaylightSample
import com.nova.app.feature.plants.data.RoomDaylightScenario
import com.nova.app.feature.plants.data.daylightSampleTimes
import com.nova.app.feature.plants.data.sampledDirectRay
import com.nova.app.feature.plants.data.compareDaylightPoints
import androidx.compose.runtime.key
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaDaylightStudy(
    spaceId: String,
    room: PlantupiaRoom,
    width: Double,
    length: Double,
    repository: PlantupiaRepository,
    initialPlacement: RoomPlant?,
    plantName: String?,
) {
    if (room.historical) Text("Simulating saved layout · revision ${room.revision}", style = PlantupiaText.itemTitle)
    val windows = remember(room.windows, width, length) {
        room.windows.filter { (it.x == 0.0 || it.x == width) != (it.y == 0.0 || it.y == length) }
    }
    if (windows.isEmpty()) {
        Text("Record a window centre on one wall to study sunlight.", style = PlantupiaText.supporting)
        if (!room.historical) PlantupiaAmbientLightCheck()
        return
    }

    var windowId by remember(windows) { mutableStateOf(windows.first().id) }
    var xText by remember(width, initialPlacement) { mutableStateOf((initialPlacement?.x ?: (width / 2)).toString()) }
    var yText by remember(length, initialPlacement) { mutableStateOf((initialPlacement?.y ?: (length / 2)).toString()) }
    var height by remember { mutableStateOf("0") }
    var bearing by remember { mutableStateOf("") }
    var screenOn by remember { mutableStateOf(false) }
    var screenDepth by remember { mutableStateOf("") }
    var screenWidth by remember { mutableStateOf("") }
    var screenHeight by remember { mutableStateOf("") }
    val screen = if (screenOn) listOfNotNull(screenDepth.toDoubleOrNull(), screenWidth.toDoubleOrNull(), screenHeight.toDoubleOrNull()) else null
    var aperture by remember { mutableStateOf("") }
    var sill by remember { mutableStateOf("") }
    var head by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var date by remember(room.revision) {
        val recorded = room.revisions.firstOrNull { it.number == room.revision }?.recordedAt?.take(10)
        mutableStateOf(if (room.historical && recorded != null) recorded else LocalDate.now().toString())
    }
    var zone by remember { mutableStateOf(ZoneId.systemDefault().id) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val x = xText.toDoubleOrNull()?.takeIf { it > 0 && it < width }
    val y = yText.toDoubleOrNull()?.takeIf { it > 0 && it < length }
    val position = if (x != null && y != null) x to y else null
    val recordedPoint = initialPlacement?.let { it.x to it.y }?.takeIf {
        it.first > 0 && it.first < width && it.second > 0 && it.second < length
    }
    var referencePoint by remember(room.revision, width, length, initialPlacement) { mutableStateOf(recordedPoint) }
    val times = remember(date, zone) {
        runCatching { daylightSampleTimes(LocalDate.parse(date.trim()), ZoneId.of(zone.trim())) }
            .getOrDefault(emptyList())
    }
    val planeHeight = height.toDoubleOrNull()
    val orientation = bearing.toDoubleOrNull()
    val openingWidth = aperture.toDoubleOrNull()
    val bottom = sill.toDoubleOrNull()
    val top = head.toDoubleOrNull()
    val lat = latitude.toDoubleOrNull()
    val lon = longitude.toDoubleOrNull()
    val ceiling = room.height ?: 1000.0
    val scenario = if ((!screenOn || (screen?.size == 3 && screen.all { it in .001..1000.0 } && screen[2] <= ceiling)) && position != null && times.size >= 2 && planeHeight != null && orientation != null &&
        openingWidth != null && bottom != null && top != null && lat != null && lon != null
    ) {
        val window = windows.first { it.id == windowId }
        val edgeDistance = if (window.y == 0.0 || window.y == length) {
            minOf(window.x, width - window.x)
        } else {
            minOf(window.y, length - window.y)
        }
        if (planeHeight in 0.0..ceiling && orientation in 0.0..360.0 &&
            openingWidth in 0.001..(2 * edgeDistance) && bottom >= 0 && bottom < top && top in 0.001..ceiling &&
            lat in -90.0..90.0 && lon in -180.0..180.0
        ) RoomDaylightScenario(room.revision, windowId, position.first, position.second, planeHeight,
            orientation, openingWidth, bottom, top, lat, lon, times, historical = room.historical, screen = screen) else null
    } else null
    // The footprint depends on the aperture and plane, not the chosen point.
    val simulation = scenario?.copy(x = 0.0, y = 0.0)
    var samples by remember(simulation) { mutableStateOf(emptyList<RoomDaylightSample>()) }
    var submittedPoint by remember(simulation) { mutableStateOf<Pair<Double, Double>?>(null) }
    var message by remember(simulation) { mutableStateOf("") }
    var index by remember(simulation) { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(true) }
    var playing by remember(simulation, samples) { mutableStateOf(false) }
    LaunchedEffect(playing, samples, editing) {
        if (!playing || editing) return@LaunchedEffect
        while (playing && index < samples.lastIndex) {
            delay(260)
            index++
        }
        playing = false
    }
    val active = samples.getOrNull(index)
    fun reaches(sample: RoomDaylightSample): Boolean =
        position?.let { sampledDirectRay(sample, it, submittedPoint) } == true
    val clock = active?.at?.format(DateTimeFormatter.ofPattern("HH:mm xxx"))
    val status = active?.let {
        when {
            it.elevation <= 0 -> "Sun below the horizon"
            reaches(it) -> "Direct sun reaches this point"
            else -> "No direct ray at this point"
        }
    }
    val atRecordedPlant = initialPlacement != null && x == initialPlacement.x && y == initialPlacement.y
    Text(if (atRecordedPlant && plantName != null) "Light at $plantName" else "Light in this room", style = PlantupiaText.editorialHeading)
    if (active != null && clock != null && status != null) {
        Text(clock, style = PlantupiaText.display)
        Text(status, style = PlantupiaText.itemTitle)
        Text("Simulated direct rays · " + active.at.toLocalDate(), style = PlantupiaText.supporting)
    }
    PlantupiaRoomDrawing(
        width, length, room.plants, room.windows,
        selectedWindow = windowId, beam = active?.footprint.orEmpty(), shadow = active?.shadow.orEmpty(), sample = position,
        referencePoint = referencePoint.takeIf { samples.isNotEmpty() },
        onChoosePoint = if (busy) null else { px, py ->
            xText = String.format(Locale.ROOT, "%.3f", px).takeIf { it.toDouble() in 0.000001..<width } ?: px.toString()
            yText = String.format(Locale.ROOT, "%.3f", py).takeIf { it.toDouble() in 0.000001..<length } ?: py.toString()
        },
    )
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (message.isNotBlank()) Text(message, style = PlantupiaText.supporting)
    if (active != null && clock != null && status != null) {
        Text("Amber shows where direct rays reach the ${scenario?.height} m plane.", style = PlantupiaText.supporting)
        val ink = Color(0xFFAD6732)
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val tick = size.width / samples.size
            samples.forEachIndexed { i, sample ->
                val color = if (reaches(sample)) ink else PlantupiaColors.softSage
                drawRect(color, Offset(i * tick, 0f), Size(tick, size.height))
            }
        }
        if (samples.size > 1) {
            Button(enabled = !busy && !editing, onClick = {
                if (playing) playing = false else { index = 0; playing = true }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (playing) "Pause daylight" else "Play the day") }
            Slider(
                value = index.toFloat(),
                onValueChange = { playing = false; index = it.roundToInt().coerceIn(0, samples.lastIndex) },
                valueRange = 0f..samples.lastIndex.toFloat(),
                steps = samples.size - 2,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Simulation time"
                    stateDescription = "$clock, $status"
                },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("From " + samples.first().at.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = PlantupiaText.supporting)
                Text("To " + samples.last().at.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = PlantupiaText.supporting)
            }
        }
        Text(if (screenOn) "The dark patch shows the shadow of your opaque screen. Glass, diffuse light and lamps are unmodelled." else "One clear opening. Glass, diffuse light and lamps are unmodelled.",
            style = PlantupiaText.supporting)
        Text("Compare two positions", style = PlantupiaText.editorialHeading)
        val ref = referencePoint
        val candidate = position
        val comparison = if (ref != null && candidate != null && ref != candidate)
            compareDaylightPoints(samples, ref, candidate, submittedPoint) else null
        if (comparison != null) {
            Text(if (ref == recordedPoint && plantName != null) "Recorded position of $plantName" else "Reference point · square marker",
                style = PlantupiaText.itemTitle)
            Text("${comparison.referenceHits} of ${comparison.sampleCount} sampled times with a direct ray", style = PlantupiaText.body)
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                val tick = size.width / samples.size
                samples.forEachIndexed { i,sample ->
                    drawRect(if (sampledDirectRay(sample,ref!!,submittedPoint)) ink else PlantupiaColors.softSage,
                        Offset(i*tick,0f),Size(tick,size.height))
                }
            }
            Text("Alternative point · cross marker", style = PlantupiaText.itemTitle)
            Text("${comparison.candidateHits} of ${comparison.sampleCount} sampled times with a direct ray", style = PlantupiaText.body)
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                val tick = size.width / samples.size
                samples.forEachIndexed { i,sample ->
                    drawRect(if (reaches(sample)) ink else PlantupiaColors.softSage,
                        Offset(i*tick,0f),Size(tick,size.height))
                }
            }
            Text("Same day and height, using the reported opening and screen. Counts are sampled moments, not measured light, exposure hours or a plant placement recommendation.",
                style = PlantupiaText.supporting)
        } else Text(if (ref == null) "Keep a reference point, then tap elsewhere in the plan."
            else "Tap another point in the plan to compare it with the reference.", style = PlantupiaText.supporting)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(enabled = !busy && candidate != null && candidate != ref,
                onClick = { referencePoint = candidate }) { Text("Keep point as reference") }
            if (recordedPoint != null && ref != recordedPoint)
                TextButton(enabled = !busy, onClick = { referencePoint = recordedPoint }) { Text("Use recorded position") }
        }
        TextButton(onClick = { playing = false; editing = !editing }) { Text(if (editing) "Hide scenario inputs" else "Change date or placement") }
    } else if (!editing) {
        Text("Run the updated scenario to see sunlight here.", style = PlantupiaText.supporting)
        Button(enabled = !busy, onClick = { editing = true }) { Text("Show scenario inputs") }
    }
    if (!room.historical) key(room.revision, position, planeHeight) { PlantupiaAmbientLightCheck() }
    if (!editing) return

    HorizontalDivider(color = PlantupiaColors.border)
    Text("Window opening", style = PlantupiaText.itemTitle)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        windows.forEach { window ->
            TextButton(enabled = !busy, onClick = { windowId = window.id }) {
                Text(if (window.id == windowId) "✓ ${window.id}" else window.id)
            }
        }
    }
    DaylightField("Opening width (m)", aperture, !busy) { aperture = it }
    DaylightField("Sill above floor (m)", sill, !busy) { sill = it }
    DaylightField("Head above floor (m)", head, !busy) { head = it }
    DaylightField("Top wall outward bearing from true north (°)", bearing, !busy) { bearing = it }
    if (initialPlacement != null && plantName != null) {
        Text("Recorded position of $plantName · ${initialPlacement.x}, ${initialPlacement.y} m", style = PlantupiaText.supporting)
        if (initialPlacement.x !in 0.0..width || initialPlacement.y !in 0.0..length || initialPlacement.x == 0.0 || initialPlacement.y == 0.0 || initialPlacement.x == width || initialPlacement.y == length)
            Text("Choose a point inside the room to run this study.", style = PlantupiaText.supporting)
    }
    Text("Sample point", style = PlantupiaText.itemTitle)
    Text("Tap inside the plan, or specify offsets from its top-left corner.", style = PlantupiaText.supporting)
    DaylightField("Along width (m)", xText, !busy) { xText = it }
    DaylightField("Along length (m)", yText, !busy) { yText = it }
    DaylightField("Sample height (m)", height, !busy) { height = it }
    androidx.compose.material3.FilterChip(selected = screenOn, enabled = !busy, onClick = { screenOn = !screenOn },
        label = { Text("Model an opaque screen") })
    if (screenOn) {
        Text("Place a vertical screen parallel to the window, centred on its opening.", style = PlantupiaText.supporting)
        DaylightField("Distance inside the window (m)", screenDepth, !busy) { screenDepth = it }
        DaylightField("Screen width (m)", screenWidth, !busy) { screenWidth = it }
        DaylightField("Screen height from floor (m)", screenHeight, !busy) { screenHeight = it }
    }
    Text("Place and day", style = PlantupiaText.itemTitle)
    DaylightField("Latitude (°)", latitude, !busy) { latitude = it }
    DaylightField("Longitude (°)", longitude, !busy) { longitude = it }
    DaylightField("Date (YYYY-MM-DD)", date, !busy) { date = it }
    DaylightField("Time zone (e.g. Africa/Cairo)", zone, !busy) { zone = it }
    if (scenario == null) {
        Text("Use an interior point, an opening that fits the wall and a valid date and time zone.",
            style = PlantupiaText.supporting)
    }
    Button(
        enabled = !busy && scenario != null,
        onClick = {
            val requested = scenario ?: return@Button
            busy = true
            message = ""
            scope.launch {
                when (val result = repository.roomDaylight(spaceId, requested)) {
                    is ApiResult.Failure -> message = result.message
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        samples = result.value
                        submittedPoint = requested.x to requested.y
                        index = samples.indexOfFirst { it.direct }.takeIf { it >= 0 }
                            ?: samples.indices.maxByOrNull { samples[it].elevation } ?: 0
                        editing = false
                    }
                }
                busy = false
            }
        },
    ) { Text("Simulate this day") }
}

@Composable
private fun DaylightField(
    label: String,
    value: String,
    enabled: Boolean,
    change: (String) -> Unit,
) {
    OutlinedTextField(value, { change(it.take(64)) }, modifier = Modifier.fillMaxWidth(),
        enabled = enabled, label = { Text(label) })
}
