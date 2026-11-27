package com.nova.app.feature.plants

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaSpace
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import java.util.UUID

/** Navigation hint only: resolve against the authorized server inventory. */
internal object PlantupiaPlaceOpenSignal {
    var target by mutableStateOf<Pair<Long, String>?>(null)
        private set
    fun open(accountId: Long, spaceId: String) {
        if (accountId <= 0 || runCatching { UUID.fromString(spaceId) }.isFailure) return
        target = accountId to spaceId
        NovaRootNavigationSignal.request(NovaRootTab.Plants)
    }
    fun consume() { target = null }
}

internal fun featuredPlantupiaSpace(spaces: List<PlantupiaSpace>, plants: List<PlantupiaPlant>): PlantupiaSpace? {
    val candidate = todayCollectionFocus(plants.filter { p -> spaces.any { it.id == p.spaceId } })
    return spaces.firstOrNull { it.id == candidate?.spaceId } ?: spaces.firstOrNull()
}

private fun unitCount(count: Int) = "$count active " + if (count == 1) "plant" else "plants"

/** Space-first editorial index. Photos are explicitly portraits, not invented room imagery. */
@Composable
internal fun PlantupiaPlaceIndex(
    spaces: List<PlantupiaSpace>, plants: List<PlantupiaPlant>, featured: Boolean,
    openSpace: (PlantupiaSpace) -> Unit, openPlant: (PlantupiaPlant) -> Unit,
) {
    val focus = featuredPlantupiaSpace(spaces, plants)
    val primary = focus?.let { s -> todayCollectionFocus(plants.filter { it.spaceId == s.id }) }
    var count by rememberSaveable { mutableIntStateOf(8) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Your spaces", style = PlantupiaText.editorialHeading, modifier = Modifier.semantics { heading() })
        if (featured && focus != null) {
            val units = plants.filter { it.spaceId == focus.id && it.active }
            Surface(color = PlantupiaColors.forest, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    val portrait = units.firstOrNull { it.id == primary?.id && it.coverPhoto.isNotBlank() }
                        ?: units.firstOrNull { it.coverPhoto.isNotBlank() }
                    if (portrait != null) {
                        PlantupiaEvidencePhoto(portrait.coverPhoto, "Plant portrait: " + portrait.name,
                            modifier = Modifier.fillMaxWidth().height(224.dp), cornerRadius = 0.dp)
                    }
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (portrait == null) "YOUR PLACE" else "PLANT PORTRAIT / " + portrait.name,
                            color = Color(0xFFDAEBC9), style = PlantupiaText.sectionLabel)
                        Text(focus.name, color = Color.White, style = PlantupiaText.display.copy(fontSize = 36.sp, lineHeight = 40.sp))
                        Text(listOf(focus.location, unitCount(units.size)).filter { it.isNotBlank() }.joinToString(" · "),
                            color = Color(0xFFDAEBC9), style = PlantupiaText.supporting)
                        Button(onClick = { openSpace(focus) }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.cream, contentColor = PlantupiaColors.forest)) {
                            Text("Enter this place")
                        }
                    }
                }
            }
            if (primary != null) {
                Text("From your collection", style = PlantupiaText.sectionLabel)
                PlantupiaPlacePlantRow(primary, action = "Open plant →") { openPlant(primary) }
            }
        }
        val others = if (featured) spaces.filter { it.id != focus?.id } else spaces
        others.take(count).forEach { space ->
            val units = plants.filter { it.active && it.spaceId == space.id }
            HorizontalDivider(color = PlantupiaColors.border)
            Surface(onClick = { openSpace(space) }, color = Color.Transparent, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
                Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val portrait = units.firstOrNull { it.coverPhoto.isNotBlank() }
                    if (portrait != null) PlantupiaEvidencePhoto(portrait.coverPhoto, "Plant portrait: " + portrait.name,
                        Modifier.size(76.dp), cornerRadius = 3.dp, compact = true)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(space.name, style = PlantupiaText.editorialHeading)
                        Text(unitCount(units.size), style = PlantupiaText.supporting)
                        if (space.location.isNotBlank()) Text(space.location, style = PlantupiaText.supporting)
                    }
                    Text("→", style = PlantupiaText.itemTitle)
                }
            }
        }
        if (others.size > count) TextButton(onClick = { count += 8 }) { Text("More spaces") }
    }
}

@Composable
internal fun PlantupiaPlacePlantRow(plant: PlantupiaPlant, action: String = "Open plant", onOpen: () -> Unit) {
    Surface(onClick = onOpen, color = Color.Transparent, modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp)) {
        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (plant.coverPhoto.isNotBlank()) PlantupiaEvidencePhoto(plant.coverPhoto, plant.name, Modifier.size(68.dp), cornerRadius = 3.dp, compact = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(plant.name, style = PlantupiaText.itemTitle)
                if (plant.placement.isNotBlank()) Text(plant.placement, style = PlantupiaText.supporting)
                if (plant.lastEventAt.isNotBlank()) Text("Last record " + plant.lastEventAt.take(10), style = PlantupiaText.supporting)
            }
            Text(action, style = PlantupiaText.supporting, color = PlantupiaColors.forest)
        }
    }
}

@Composable
internal fun PlantupiaPlaceHeading(space: PlantupiaSpace, plants: List<PlantupiaPlant>, onOpenPhoto: (String, String) -> Unit) {
    val portrait = plants.firstOrNull { it.coverPhoto.isNotBlank() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("YOUR PLACE", style = PlantupiaText.sectionLabel)
        Text(space.name, style = PlantupiaText.display.copy(fontSize = 38.sp, lineHeight = 42.sp),
            modifier = Modifier.semantics { heading() })
        if (space.location.isNotBlank()) Text(space.location, style = PlantupiaText.supporting)
        Text(unitCount(plants.size), style = PlantupiaText.body)
        if (portrait != null) {
            PlantupiaEvidencePhoto(portrait.coverPhoto, "Plant portrait: " + portrait.name, Modifier.fillMaxWidth().height(220.dp),
                cornerRadius = 3.dp, onOpenPhoto = { onOpenPhoto(portrait.coverPhoto, portrait.name) })
            Text("Photograph of " + portrait.name, style = PlantupiaText.supporting)
        }
        HorizontalDivider(color = PlantupiaColors.border)
    }
}

/** Explicit private capture, then a retained record and optional visual selection. */
@Composable
internal fun PlantupiaObservationRoute(plant: PlantupiaPlant, repository: PlantupiaRepository,
    reference: PlantupiaCareEvent?, close: (Boolean) -> Unit) {
    var candidate by remember(plant.id, repository) { mutableStateOf<Pair<Uri, String>?>(null) }
    val current = candidate
    if (current == null) PlantupiaAtlasCapture(
        title = plant.name, action = "Save private observation",
        disclosure = "Only saved to this plant's private record. You can inspect a detail afterwards.",
        reference = reference, leaf = false,
        onCaptureWithNote = { uri, note ->
            if (!repository.isCurrentSession() || !plant.active || !plant.canRecord) false
            else { candidate = uri to note; true }
        },
        onClose = { close(false) }, onCapture = { false },
    ) else AtlasObservation(plant, current.first, repository, note = current.second, autoAnalyse = false, close = { close(true) })
}

/** Real dated source photos: no interpolated biological change. */
@Composable
internal fun PlantupiaChronologyHeader(events: List<PlantupiaCareEvent>, onOpenPhoto: (PlantupiaCareEvent) -> Unit, onRepeat: ((PlantupiaCareEvent) -> Unit)?) {
    val photos = recordedPlantPhotos(events)
    if (photos.isEmpty()) return
    var selectedId by rememberSaveable { mutableStateOf(photos.first().id) }
    val selected = photos.firstOrNull { it.id == selectedId } ?: photos.first()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Through time", style = PlantupiaText.editorialHeading)
        Text(selected.observedAt.take(10), style = PlantupiaText.sectionLabel)
        Crossfade(targetState = selected, animationSpec = tween(180), label = "Recorded photograph") { event ->
            PlantupiaEvidencePhoto(event.photoUrl, "Photograph recorded " + event.observedAt.take(10),
                Modifier.fillMaxWidth().height(250.dp), cornerRadius = 3.dp,
                framing = PlantupiaPhotoFraming.FULL_RECORD, onOpenPhoto = { onOpenPhoto(event) })
        }
        if (photos.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(photos.size, key = { photos[it].id }) { i ->
                val event = photos[i]
                FilterChip(selected = selected.id == event.id, onClick = { selectedId = event.id },
                    label = { Text(event.observedAt.take(10)) })
            }
        }
        if (onRepeat != null) Button(onClick = { onRepeat(selected) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Photograph again from the same view")
        }
        Text("Only recorded photographs. Compare dates without inferring growth.", style = PlantupiaText.supporting)
    }
}
