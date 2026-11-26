package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.plants.data.PlantupiaCareEvent

/**
 * Field Notes: an evidence-based index of photos in already-loaded private events.
 * This is not a two-photo growth prediction and never contains stock/client images.
 */
@Composable
internal fun PlantupiaPhotoIndex(
    events: List<PlantupiaCareEvent>,
    hasOlderPages: Boolean,
    onOpenPhoto: (PlantupiaCareEvent) -> Unit,
    alignmentEnabled: Boolean = false,
) {
    val photos = recordedPlantPhotos(events)
    val defaultPair = plantupiaLoadedPhotoPair(events)
    if (photos.isEmpty()) return
    var choosing by remember { mutableStateOf(false) }
    var firstId by remember { mutableStateOf<String?>(null) }
    var secondId by remember { mutableStateOf<String?>(null) }
    val comparison = if (choosing) signatureComparisonPair(events, firstId, secondId) else defaultPair

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(if (photos.size == 1) "1 saved photograph" else "${photos.size} saved photographs",
            style = PlantupiaText.itemTitle)
        // Two verified snapshots of the same person's private plant history.
        // The oldest *loaded* photo is not necessarily the first ever recorded.
        if (photos.size > 1) {
            if (!choosing) {
                TextButton(
                    onClick = {
                        firstId = defaultPair?.earlier?.id
                        secondId = defaultPair?.later?.id
                        choosing = true
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Choose two recorded photos") }
            } else {
                Text("COMPARE TWO MOMENTS", style = PlantupiaText.sectionLabel)
                SignaturePhotoChoice("First photo", photos, firstId) { firstId = it }
                SignaturePhotoChoice("Second photo", photos, secondId) { secondId = it }
                if (comparison == null) Text(
                    "Choose two different photos from different recorded dates.",
                    style = PlantupiaText.supporting,
                )
                TextButton(onClick = { choosing = false; firstId = null; secondId = null }) {
                    Text("Reset comparison")
                }
            }
        }
        if (comparison != null) {
            PlantupiaPhotoTimeLens(
                pair = comparison,
                hasOlderPages = hasOlderPages,
                onOpenPhoto = onOpenPhoto,
                alignmentEnabled = alignmentEnabled,
            )
        }
        if (photos.size != 2 || comparison == null) LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(photos, key = { it.id }) { photo ->
                Surface(
                    color = PlantupiaColors.paper,
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.width(162.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        PlantupiaEvidencePhoto(
                            photoUrl = photo.photoUrl,
                            photoDescription = "Photo recorded on " + photo.observedAt.take(10),
                            modifier = Modifier.fillMaxWidth().height(137.dp),
                            cornerRadius = 15.dp,
                            framing = PlantupiaPhotoFraming.FULL_RECORD,
                    onOpenPhoto = { onOpenPhoto(photo) },
                        )
                        Text(
                            photo.observedAt.take(10),
                            modifier = Modifier.padding(start = 11.dp, end = 11.dp, bottom = 9.dp),
                            color = PlantupiaColors.forest,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
        if (hasOlderPages && comparison == null) {
            Text(
                "Older photos may appear when you load earlier history.",
                style = PlantupiaText.supporting,
            )
        }
    }
}

@Composable
private fun SignaturePhotoChoice(
    label: String,
    photos: List<PlantupiaCareEvent>,
    chosenId: String?,
    onChoose: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val chosen = photos.indexOfFirst { it.id == chosenId }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(if (chosen < 0) label else "$label · ${photos[chosen].observedAt.take(10)} / ${chosen + 1}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            photos.forEachIndexed { index, event ->
                DropdownMenuItem(
                    text = { Text("${event.observedAt.take(10)} · Photo ${index + 1}") },
                    onClick = { expanded = false; onChoose(event.id) },
                )
            }
        }
    }
}
