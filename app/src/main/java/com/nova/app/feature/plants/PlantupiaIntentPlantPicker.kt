package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nova.app.feature.plants.data.PlantupiaPlant

/**
 * Choose an actual writable living plant before private care / a public question.
 * Never turns read-only or archived plants into enabled write targets.
 */
internal fun filterIntentPlants(plants: List<PlantupiaPlant>, search: String): List<PlantupiaPlant> {
    val needle = search.trim()
    return actionablePlantChoices(plants).filter {
        needle.isEmpty() ||
            it.name.contains(needle, ignoreCase = true) ||
            it.label.contains(needle, ignoreCase = true) ||
            it.species.contains(needle, ignoreCase = true) ||
            it.placement.contains(needle, ignoreCase = true)
    }
}

@Composable
internal fun PlantupiaIntentPlantPicker(
    plants: List<PlantupiaPlant>,
    onChoosePlant: (PlantupiaPlant) -> Unit,
) {
    val authorized = actionablePlantChoices(plants)
    if (authorized.size < 2) return
    var search by rememberSaveable { mutableStateOf("") }
    val matches = filterIntentPlants(plants, search)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text("CHOOSE YOUR PLANT", style = PlantupiaText.sectionLabel)
        if (authorized.size > 7) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it.take(80) },
                label = { Text("Find a plant") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (matches.isEmpty()) {
            Text("No writable plants match your search.", style = PlantupiaText.supporting)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(matches, key = { it.id }) { plant ->
                    Surface(
                        onClick = { onChoosePlant(plant) },
                        modifier = Modifier
                            .widthIn(min = 165.dp, max = 205.dp)
                            .heightIn(min = 66.dp),
                        color = PlantupiaColors.paper,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PlantupiaColors.border),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (plant.coverPhoto.isNotBlank()) {
                                AsyncImage(
                                    model = plant.coverPhoto,
                                    contentDescription = "Photo chosen for ${plant.name}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(45.dp),
                                )
                            } else {
                                PlantupiaBrandGlyph(modifier = Modifier.size(45.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    plant.name,
                                    style = PlantupiaText.body,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (plant.species.isNotBlank()) {
                                    Text(
                                        plant.species,
                                        style = PlantupiaText.supporting,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
