package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nova.app.feature.auth.domain.model.NovaUser
import com.nova.app.feature.plants.data.PlantupiaPlant

/** Blank-canvas presentation: a title, one context control, then the actual conversation.
 * Read-only inputs and explicit callbacks; no server or session logic in this view.
 */
@Composable
internal fun PlantupiaAskPresentation(
    user: NovaUser?,
    selected: PlantupiaPlant?,
    onProfile: () -> Unit,
    onChoosePlant: () -> Unit,
    conversation: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PlantupiaAccountHeader(
                user?.name.orEmpty(), user?.username.orEmpty(), user?.avatarUrl.orEmpty(),
                user?.id ?: 0L, user?.plantupiaAvatarVariant ?: -1,
                onProfile = onProfile, compactBrand = true, screenTitle = "Ask Plantupia",
            )
            Surface(onClick = onChoosePlant, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp), color = PlantupiaColors.paper,
                border = BorderStroke(1.dp, PlantupiaColors.border)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selected != null) PlantupiaEvidencePhoto(
                        photoUrl = selected.coverPhoto, photoDescription = selected.name,
                        modifier = Modifier.size(32.dp), compact = true,
                    )
                    Text(selected?.name ?: "General care", style = PlantupiaText.body, modifier = Modifier.weight(1f))
                    Text("⌄", color = PlantupiaColors.forest, modifier = Modifier.clearAndSetSemantics {})
                }
            }
        }
        HorizontalDivider(color = PlantupiaColors.border)
        Box(Modifier.fillMaxWidth().weight(1f)) { conversation() }
    }
}

/** The collection is revealed only on demand. Selection does not change consent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlantupiaAskPlantPicker(
    plants: List<PlantupiaPlant>,
    selectedId: String?,
    showSearch: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    loading: Boolean,
    unavailable: Boolean,
    onRetry: () -> Unit,
    onChoose: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PlantupiaColors.paper) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.82f).imePadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Choose a plant", style = PlantupiaText.editorialHeading, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Done") }
            }
            if (showSearch) OutlinedTextField(value = query, onValueChange = onQuery,
                label = { Text("Find a plant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (unavailable) {
                Text("Couldn't load plants.", style = PlantupiaText.supporting)
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    PlantupiaAskChoiceRow("General care", selectedId == null, null) { onChoose(null) }
                }
                items(plants, key = { it.id }) { plant ->
                    PlantupiaAskChoiceRow(plant.name, selectedId == plant.id, plant) { onChoose(plant.id) }
                }
                if (!loading && !unavailable && plants.isEmpty() && query.isNotBlank()) item {
                    Text("No matching plants", style = PlantupiaText.supporting,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun PlantupiaAskChoiceRow(label: String, active: Boolean, plant: PlantupiaPlant?, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        .semantics { selected = active }, color = if (active) PlantupiaColors.softSage else PlantupiaColors.paper,
        shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (plant != null) PlantupiaEvidencePhoto(
                photoUrl = plant.coverPhoto, photoDescription = plant.name,
                modifier = Modifier.size(38.dp), compact = true,
            )
            Text(label, style = PlantupiaText.body, modifier = Modifier.weight(1f))
            if (active) Text("✓", color = PlantupiaColors.forest, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}
