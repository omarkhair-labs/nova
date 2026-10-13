package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaSpace
import com.nova.app.feature.plants.data.resolveFocusedSpace

/**
 * One real space at a time. All chip labels and counts come from the authorized
 * API; selecting a site must never mix another site's plants into its hero/list.
 */
@Composable
internal fun PlantupiaSpaceSwitcher(
    spaces: List<PlantupiaSpace>,
    plants: List<PlantupiaPlant>,
    focusedSpaceId: String?,
    onSelectSpace: (String) -> Unit,
) {
    if (spaces.isEmpty()) return
    val selected = resolveFocusedSpace(spaces, focusedSpaceId)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("YOUR SPACES", style = PlantupiaText.sectionLabel)
            Text(
                if (spaces.size == 1) "1 space" else "${spaces.size} spaces",
                style = PlantupiaText.supporting,
            )
        }
        if (spaces.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(spaces, key = { it.id }) { space ->
                    val activeCount = plants.count { it.spaceId == space.id && it.active }
                    val isSelected = selected?.id == space.id
                    Surface(
                        onClick = { onSelectSpace(space.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) PlantupiaColors.forest else PlantupiaColors.paper,
                        border = BorderStroke(1.dp, if (isSelected) PlantupiaColors.forest else PlantupiaColors.border),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)) {
                            Text(
                                space.name,
                                color = if (isSelected) PlantupiaColors.paper else PlantupiaColors.forest,
                                style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                            )
                            Text(
                                "${activeCount} plants",
                                color = if (isSelected) PlantupiaColors.softSage else PlantupiaColors.secondaryText,
                                style = PlantupiaText.supporting,
                            )
                        }
                    }
                }
            }
        } else {
            Text("This is the space you're tracking.", style = PlantupiaText.supporting)
        }
    }
}
