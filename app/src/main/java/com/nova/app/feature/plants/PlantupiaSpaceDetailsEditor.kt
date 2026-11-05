package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.data.PlantupiaSpace
import kotlinx.coroutines.launch

/** Edit a real private space without discarding its plants or their care history. */
@Composable
internal fun PlantupiaSpaceDetailsEditor(
    space: PlantupiaSpace?,
    repository: PlantupiaRepository,
    onSaved: () -> Unit,
) {
    if (space == null || !space.canAddPlant) return
    var editing by remember(space.id) { mutableStateOf(false) }
    var saving by remember(space.id) { mutableStateOf(false) }
    var name by remember(space.id, editing) { mutableStateOf(space.name) }
    var location by remember(space.id, editing) { mutableStateOf(space.location) }
    var description by remember(space.id, editing) { mutableStateOf(space.description) }
    var error by remember(space.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val lifetime = remember(space.id, repository) { PlantupiaCallbackLifetime(repository::isCurrentSession) }
    DisposableEffect(lifetime) { onDispose { lifetime.dispose() } }

    if (!editing) {
        TextButton(onClick = { editing = true; error = null }) {
            Text("Edit this space", color = PlantupiaColors.forest)
        }
    } else {
        Surface(
            color = PlantupiaColors.paper,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "SPACE DETAILS",
                    style = PlantupiaText.sectionLabel.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    "This space stays private. Changing its name or place does not erase any plant history.",
                    style = PlantupiaText.supporting,
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(140) },
                    label = { Text("Space name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), enabled = !saving,
                )
                OutlinedTextField(
                    value = location, onValueChange = { location = it.take(200) },
                    label = { Text("Placement or location (private)") },
                    modifier = Modifier.fillMaxWidth(), enabled = !saving,
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it.take(1000) },
                    label = { Text("About this space") }, minLines = 2,
                    modifier = Modifier.fillMaxWidth(), enabled = !saving,
                )
                error?.let { Text(it, color = PlantupiaColors.secondaryText) }
                Button(
                    enabled = !saving && name.isNotBlank() &&
                        (name.trim() != space.name || location.trim() != space.location ||
                            description.trim() != space.description),
                    onClick = {
                        val submittedName = name
                        val submittedDescription = description
                        val submittedLocation = location
                        scope.launch {
                            runPlantupiaMutation(
                                isAccountCurrent = lifetime::isCurrent,
                                isDetailCurrent = lifetime::isCurrent,
                                request = { repository.updateSpaceDetails(space.id, submittedName, submittedDescription, submittedLocation) },
                                onStarted = { saving = true },
                                onSuccess = { editing = false; error = null; onSaved() },
                                onFailure = { error = it.message },
                                onFinished = { saving = false },
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                ) { Text(if (saving) "Saving…" else "Save space details") }
                TextButton(onClick = { editing = false; error = null }, enabled = !saving) {
                    Text("Cancel", color = PlantupiaColors.forest)
                }
            }
        }
    }
}
