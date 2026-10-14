package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
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
) {
    val photos = recordedPlantPhotos(events)
    if (photos.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("RECORDED PHOTOS", style = PlantupiaText.sectionLabel)
        Text(
            if (photos.size == 1)
                "One real photo so far. New photos will join your plant's living history."
            else
                "${photos.size} actual photos in the loaded history. Tap one to examine it.",
            style = PlantupiaText.supporting,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(photos, key = { it.id }) { photo ->
                Surface(
                    onClick = { onOpenPhoto(photo) },
                    color = PlantupiaColors.paper,
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.width(162.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        AsyncImage(
                            model = photo.photoUrl,
                            contentDescription = "Open actual plant photo recorded on " + photo.observedAt.take(10),
                            modifier = Modifier.fillMaxWidth().height(137.dp),
                            contentScale = ContentScale.Crop,
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
        if (hasOlderPages) {
            Text(
                "Older photos may appear when you load earlier history.",
                style = PlantupiaText.supporting,
            )
        }
    }
}
