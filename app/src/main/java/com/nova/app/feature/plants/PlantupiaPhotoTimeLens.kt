package com.nova.app.feature.plants

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Slider
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.remember
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

/** A human-controlled flip between actual uploaded records, NOT inferred growth. */
@Composable
internal fun PlantupiaPhotoTimeLens(
    pair: PlantupiaLoadedPhotoPair,
    hasOlderPages: Boolean,
    onOpenPhoto: (PlantupiaCareEvent) -> Unit,
) {
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
            Text("LIVING TIME", style = PlantupiaText.sectionLabel)
            Text("See it at another point in time", style = PlantupiaText.editorialHeading)
            Text("Two recorded moments. Changes in viewpoint or light may alter the image; this is not a growth or health measurement.", style = PlantupiaText.supporting)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(false, true).forEach { later ->
                    val entry = if (later) pair.later else pair.earlier
                    val active = !sideBySide && showLater == later
                    Surface(
                        onClick = { showLater = later; sideBySide = false; wipe = false },
                        color = if (active) PlantupiaColors.forest else PlantupiaColors.softSage,
                        border = BorderStroke(1.dp, if (active) PlantupiaColors.forest
                            else PlantupiaColors.border),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp)
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
            }
            TextButton(onClick = { sideBySide = !sideBySide; wipe = false },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (sideBySide) "Single photo" else "Compare photos", color = PlantupiaColors.forest)
            }
            if (sideBySide) {
                TextButton(
                    onClick = { wipe = !wipe },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(if (wipe) "Side by side" else "Slide across photos", color = PlantupiaColors.forest)
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
                Text("Recorded on " + visible.observedAt.take(10),
                    style = PlantupiaText.sectionLabel)
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
        if (divider > 0f && divider < 1f) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width * split
                drawLine(mat, Offset(x, 0f), Offset(x, size.height), 4.dp.toPx())
                drawLine(ink, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
            }
        }
    }
    Slider(
        value = divider,
        onValueChange = onSlide,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Reveal earlier photo" },
    )
    Text("Slide to reveal either date. These photos have their original framing.", style = PlantupiaText.supporting)
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
