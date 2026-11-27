package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Slider
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.core.network.ApiResult
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A human-controlled flip between actual uploaded records, NOT inferred growth. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaPhotoTimeLens(
    pair: PlantupiaLoadedPhotoPair,
    hasOlderPages: Boolean,
    onOpenPhoto: (PlantupiaCareEvent) -> Unit,
    alignmentEnabled: Boolean = false,
    automaticAlignment: (suspend (JSONObject) -> ApiResult<JSONObject>)? = null,
    isCurrentSession: () -> Boolean = { true },
) {
    var align by remember(pair.earlier.id, pair.later.id) { mutableStateOf(false) }
    if (align && alignmentEnabled) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlantupiaAlignedVisits(pair, automaticAlignment, isCurrentSession) { align = false }
        }
        return
    }
    var showLater by rememberSaveable(pair.earlier.id, pair.later.id) {
        mutableStateOf(true)
    }
    var sideBySide by rememberSaveable(pair.earlier.id, pair.later.id) { mutableStateOf(false) }
    var wipe by rememberSaveable(pair.earlier.id, pair.later.id) { mutableStateOf(false) }
    var divider by rememberSaveable(pair.earlier.id, pair.later.id) { mutableStateOf(0.5f) }
    val selected = if (showLater) pair.later else pair.earlier
    Surface(
        color = PlantupiaColors.paper,
        border = BorderStroke(1.dp, PlantupiaColors.border),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Across time", style = PlantupiaText.editorialHeading)
            val elapsedDays = remember(pair.earlier.observedAt, pair.later.observedAt) {
                runCatching {
                    ChronoUnit.DAYS.between(LocalDate.parse(pair.earlier.observedAt.take(10)),
                        LocalDate.parse(pair.later.observedAt.take(10)))
                }.getOrNull()?.takeIf { it > 0 }
            }
            elapsedDays?.let { days ->
                Text(if (days == 1L) "1 day apart" else "$days days apart", style = PlantupiaText.supporting)
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val dateControl: @Composable (Boolean, Modifier) -> Unit = { later, modifier ->
                    val entry = if (later) pair.later else pair.earlier
                    val active = !sideBySide && showLater == later
                    Surface(
                        onClick = { showLater = later; sideBySide = false; wipe = false },
                        color = if (active) PlantupiaColors.forest else PlantupiaColors.softSage,
                        border = BorderStroke(1.dp, if (active) PlantupiaColors.forest
                            else PlantupiaColors.border),
                        shape = RoundedCornerShape(9.dp),
                        modifier = modifier.heightIn(min = 56.dp)
                            .semantics {
                                role = Role.Tab
                                this.selected = active
                                contentDescription = (if (later) "Later recorded photo, "
                                    else "Earlier recorded photo, ") + entry.observedAt.take(10)
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(if (later) "LATER" else "EARLIER",
                                color = if (active) PlantupiaColors.paper
                                    else PlantupiaColors.forest,
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text(entry.observedAt.take(10),
                                color = if (active) PlantupiaColors.paper
                                    else PlantupiaColors.forest,
                                fontSize = 12.sp)
                        }
                    }
                }
                if (maxWidth / LocalDensity.current.fontScale >= 250.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dateControl(false, Modifier.weight(1f))
                        dateControl(true, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dateControl(false, Modifier.fillMaxWidth())
                        dateControl(true, Modifier.fillMaxWidth())
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { sideBySide = !sideBySide; wipe = false },
                    modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (sideBySide) "Single photo" else "Compare photos", color = PlantupiaColors.forest)
                }
                if (sideBySide) {
                    TextButton(
                        onClick = { wipe = !wipe },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(if (wipe) "Side by side" else "Slide across photos", color = PlantupiaColors.forest)
                    }
                }
            }
            if (sideBySide && wipe) {
                PhotoWipe(pair, divider, { divider = it }, onOpenPhoto)
            } else if (sideBySide) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val portrait: @Composable (PlantupiaCareEvent, Modifier) -> Unit = { event, m ->
                        Column(m, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlantupiaEvidencePhoto(event.photoUrl, "Recorded photo " + event.observedAt.take(10),
                                Modifier.fillMaxWidth().height(178.dp), framing = PlantupiaPhotoFraming.FULL_RECORD,
                                onOpenPhoto = { onOpenPhoto(event) })
                            Text(event.observedAt.take(10), style = PlantupiaText.supporting)
                        }
                    }
                    if (maxWidth / LocalDensity.current.fontScale >= 260.dp) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            portrait(pair.earlier, Modifier.weight(1f))
                            portrait(pair.later, Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            portrait(pair.earlier, Modifier.fillMaxWidth())
                            portrait(pair.later, Modifier.fillMaxWidth())
                        }
                    }
                }
            } else Crossfade(targetState = selected.id, animationSpec = tween(180),
                label = "actual-saved-photo-time-lens") { id ->
                val visible = if (id == pair.later.id) pair.later else pair.earlier
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                PlantupiaEvidencePhoto(
                    photoUrl = visible.photoUrl,
                    photoDescription = "Actual recorded plant photograph from " +
                        visible.observedAt.take(10) + ". Tap to inspect.",
                    modifier = Modifier.fillMaxWidth().height(248.dp),
                    cornerRadius = 11.dp,
                    framing = PlantupiaPhotoFraming.FULL_RECORD,
                    onOpenPhoto = { onOpenPhoto(visible) },
                )
                }
            }
            Text("Visual comparison of saved photographs, not measured growth or health.", style = PlantupiaText.supporting)
            if (alignmentEnabled) {
                TextButton(onClick = { align = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (automaticAlignment != null) "Align recorded photographs" else "Align using reference marks")
                }
            }
            if (hasOlderPages) {
                Text("Only photos loaded so far. Earlier pages may add older moments.",
                    style = PlantupiaText.supporting)
            }
        }
    }
}


@Composable
private fun PhotoWipe(
    pair: PlantupiaLoadedPhotoPair,
    divider: Float,
    onSlide: (Float) -> Unit,
    onOpenPhoto: (PlantupiaCareEvent) -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var earlierLoaded by remember(pair.earlier.photoUrl) { mutableStateOf(false) }
    var laterLoaded by remember(pair.later.photoUrl) { mutableStateOf(false) }
    val split = if (rtl) 1f - divider else divider
    val latestSlide = rememberUpdatedState(onSlide)
    val ink = PlantupiaColors.paper
    val mat = PlantupiaColors.forest
    Box(Modifier.fillMaxWidth().height(248.dp).clipToBounds().background(mat)) {
        listOf(pair.earlier, pair.later).forEachIndexed { index, event ->
            val showLeft = (index == 0) != rtl
            PlantupiaEvidencePhoto(
                photoUrl = event.photoUrl,
                photoDescription = "Recorded photograph on " + event.observedAt.take(10),
                modifier = Modifier.fillMaxSize().drawWithContent {
                    val boundary = size.width * split
                    clipRect(
                        left = if (showLeft) 0f else boundary,
                        right = if (showLeft) boundary else size.width,
                    ) { this@drawWithContent.drawContent() }
                },
                cornerRadius = 0.dp,
                framing = PlantupiaPhotoFraming.FULL_RECORD,
                compact = true,
                onPhotoLoadChanged = { loaded ->
                    if (index == 0) earlierLoaded = loaded else laterLoaded = loaded
                },
            )
        }
        // Direct manipulation of the evidence, with the slider below as an accessible alternative.
        Canvas(Modifier.fillMaxSize().pointerInput(rtl) {
            detectHorizontalDragGestures(
                onDragStart = { start ->
                    photoWipeFraction(start.x, size.width.toFloat(), rtl)?.let(latestSlide.value)
                },
                onHorizontalDrag = { change, _ ->
                    change.consume()
                    photoWipeFraction(change.position.x, size.width.toFloat(), rtl)?.let(latestSlide.value)
                },
            )
        }) {
            val x = size.width * split
            drawLine(mat, Offset(x, 0f), Offset(x, size.height), 4.dp.toPx())
            drawLine(ink, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
            val radius = minOf(19.dp.toPx(), size.width / 2f)
            if (radius > 0f) {
                val handle = Offset(x.coerceIn(radius, size.width - radius), center.y)
                drawCircle(mat, radius, handle)
                drawCircle(ink, radius - 2.dp.toPx().coerceAtMost(radius), handle)
                val s = 5.dp.toPx()
                drawLine(mat, handle + Offset(-2 * s, 0f), handle + Offset(-s, -s), 2.dp.toPx())
                drawLine(mat, handle + Offset(-2 * s, 0f), handle + Offset(-s, s), 2.dp.toPx())
                drawLine(mat, handle + Offset(2 * s, 0f), handle + Offset(s, -s), 2.dp.toPx())
                drawLine(mat, handle + Offset(2 * s, 0f), handle + Offset(s, s), 2.dp.toPx())
            }
        }
    }
    Slider(
        value = divider,
        onValueChange = onSlide,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Reveal earlier photo" },
    )
    Text(if (earlierLoaded && laterLoaded) "Drag the image or use the slider."
        else "Both saved photographs must load for a visual comparison.",
        style = PlantupiaText.supporting)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(pair.earlier, pair.later).forEachIndexed { index, event ->
            TextButton(
                onClick = { onOpenPhoto(event) },
                enabled = if (index == 0) earlierLoaded else laterLoaded,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(event.observedAt.take(10)) }
        }
    }
}

/** Convert physical position into the same chronological fraction used by the accessible slider. */
internal fun photoWipeFraction(x: Float, width: Float, rtl: Boolean): Float? {
    if (!x.isFinite() || !width.isFinite() || width <= 0f) return null
    val physical = (x / width).coerceIn(0f, 1f)
    return if (rtl) 1f - physical else physical
}
