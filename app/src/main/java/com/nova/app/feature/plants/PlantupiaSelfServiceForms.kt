package com.nova.app.feature.plants

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.data.PlantupiaSpace
import com.nova.app.feature.plants.data.addPlantAccessExplanation
import com.nova.app.feature.plants.data.chooseWritableSpaceId
import kotlinx.coroutines.launch

private val ink = Color(0xFF123B2E)
private val textMuted = Color(0xFF4A5A50)
private val paper = Color(0xFFFFFDF7)

/** A signed-in person can start a living space without admin provisioning. */
@Composable
fun PlantupiaSelfServiceForms(
    spaces: List<PlantupiaSpace>,
    repository: PlantupiaRepository,
    onCreated: () -> Unit,
    onRefreshSpaces: () -> Unit,
    onPlantCreated: (plantId: String, spaceId: String) -> Unit,
    preferredSpaceId: String? = null,
    enabled: Boolean = true,
    creationRequest: PlantupiaCreateMode? = null,
    onRequestConsumed: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    // Create → Home opens a real editor below the photo-led space hero.
    // Bring only its heading into view; avoid trying to scroll a long form in full.
    val creationEditorHeading = remember { BringIntoViewRequester() }
    val writableSpaces = spaces.filter { it.canAddPlant }
    var spaceOpen by remember { mutableStateOf(false) }
    var plantOpen by remember { mutableStateOf(false) }
    var accessNotice by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var spaceName by remember { mutableStateOf("") }
    var spaceLocation by remember { mutableStateOf("") }
    var spaceDescription by remember { mutableStateOf("") }
    var plantName by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var placement by remember { mutableStateOf("") }
    var selectedSpaceId by remember { mutableStateOf<String?>(null) }
    // The newly returned UUID is authorized, but may precede the refreshing list.
    var justCreatedSpace by remember { mutableStateOf<Pair<String, String>?>(null) }
    var spaceMenuOpen by remember { mutableStateOf(false) }
    var plantPhoto by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        plantPhoto = it
    }

    LaunchedEffect(spaces) {
        if (writableSpaces.isNotEmpty()) accessNotice = false
        if (justCreatedSpace != null &&
            writableSpaces.any { it.id == justCreatedSpace?.first }
        ) {
            justCreatedSpace = null
        }
        // Do not keep a revoked/stale space selection. The only temporary
        // exception is the UUID from a just-successful createSpace response.
        if (selectedSpaceId != justCreatedSpace?.first &&
            writableSpaces.none { it.id == selectedSpaceId }
        ) {
            selectedSpaceId = writableSpaces.firstOrNull()?.id
        }
    }

    // On a Create → Home handoff open the real server-backed form, not a
    // separate mock composer. A first plant requires creating a space first.
    LaunchedEffect(creationRequest, enabled) {
        if (enabled) {
            when (creationRequest.creationForm(
                hasSpaces = writableSpaces.isNotEmpty(),
                hasVisibleSpaces = spaces.isNotEmpty(),
            )) {
                PlantupiaCreationForm.SPACE -> {
                    spaceOpen = true
                    plantOpen = false
                    accessNotice = false
                    error = null
                    onRequestConsumed()
                }
                PlantupiaCreationForm.PLANT -> {
                    selectedSpaceId = chooseWritableSpaceId(
                        writableSpaces, preferredSpaceId, selectedSpaceId,
                        justCreatedSpace?.first,
                    )
                    plantOpen = true
                    spaceOpen = false
                    accessNotice = false
                    error = null
                    onRequestConsumed()
                }
                PlantupiaCreationForm.ACCESS -> {
                    // A space the person can see is not necessarily editable.
                    // Don't open a new-space form or bypass the API role gate.
                    accessNotice = true
                    spaceOpen = false
                    plantOpen = false
                    error = null
                    onRequestConsumed()
                }
                PlantupiaCreationForm.NONE -> Unit
            }
        }
    }

    LaunchedEffect(spaceOpen, plantOpen, accessNotice) {
        if (spaceOpen || plantOpen || accessNotice) {
            // Wait for the newly visible heading to enter composition/layout.
            withFrameNanos { }
            creationEditorHeading.bringIntoView()
        }
    }

    // A failed or loading API is not an empty account. Keep composable state
    // across reloads so a newly-created space can lead into its plant editor.
    if (!enabled) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (spaces.isEmpty() && !spaceOpen && !plantOpen) {
            Surface(color = paper, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Start with your space", color = ink, fontSize = 22.sp)
                    Text(
                        "Home, balcony, office, or anywhere your plants live. Create a space, then add your plants.",
                        color = textMuted, fontSize = 13.sp,
                    )
                    Button(
                        onClick = { error = null; spaceOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ink),
                    ) { Text("+ Create my space") }
                }
            }
        } else if (!spaceOpen && !plantOpen) {
            // Stack creation actions for 320dp screens and 2x system fonts.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        error = null
                        accessNotice = false
                        selectedSpaceId = chooseWritableSpaceId(
                            writableSpaces, preferredSpaceId, selectedSpaceId, justCreatedSpace?.first,
                        )
                        plantOpen = true
                    },
                    enabled = writableSpaces.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ink),
                ) { Text("+ Add plant") }
                OutlinedButton(
                    onClick = { error = null; accessNotice = false; spaceOpen = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("+ New private space", color = ink) }
            }
            if (writableSpaces.isEmpty()) {
                Surface(
                    color = paper,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            if (spaces.any { !it.canAddPlantPermissionReported }) "Access not confirmed"
                            else "These spaces are read-only",
                            modifier = Modifier.bringIntoViewRequester(creationEditorHeading),
                            color = ink,
                            fontSize = 18.sp,
                        )
                        Text(addPlantAccessExplanation(spaces), color = textMuted, fontSize = 13.sp)
                        OutlinedButton(
                            onClick = {
                                error = null
                                onRefreshSpaces()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text("Refresh access", color = ink) }
                    }
                }
            }
        }

        if (spaceOpen) {
            Surface(color = paper, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("New space", modifier = Modifier.bringIntoViewRequester(creationEditorHeading), color = ink, fontSize = 22.sp)
                    Text("Your space is private and won't appear on your public profile.", color = textMuted, fontSize = 12.sp)
                    OutlinedTextField(
                        value = spaceName, onValueChange = { spaceName = it.take(140) },
                        label = { Text("Space name *") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedTextField(
                        value = spaceLocation, onValueChange = { spaceLocation = it.take(200) },
                        label = { Text("Location (optional)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedTextField(
                        value = spaceDescription, onValueChange = { spaceDescription = it.take(1000) },
                        label = { Text("About this space (optional)") },
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            enabled = !saving && spaceName.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    saving = true
                                    error = null
                                    when (val result = repository.createSpace(spaceName, spaceDescription, spaceLocation)) {
                                        is ApiResult.Failure -> error = result.message
                                        is ApiResult.Success -> {
                                            selectedSpaceId = result.value
                                            justCreatedSpace = result.value to spaceName.trim()
                                            spaceOpen = false
                                            plantOpen = true
                                            spaceName = ""
                                            spaceLocation = ""
                                            spaceDescription = ""
                                            onCreated()
                                        }
                                    }
                                    saving = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ink),
                        ) { Text(if (saving) "Saving…" else "Create and add a plant") }
                        TextButton(onClick = { spaceOpen = false; error = null }, enabled = !saving) {
                            Text("Cancel", color = ink)
                        }
                    }
                }
            }
        }

        if (plantOpen) {
            Surface(color = paper, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("New plant", modifier = Modifier.bringIntoViewRequester(creationEditorHeading), color = ink, fontSize = 22.sp)
                    Text("Every plant has a name, a place, and a history that grows over time.", color = textMuted, fontSize = 12.sp)
                    OutlinedButton(
                        onClick = { spaceMenuOpen = true },
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    ) {
                        Text(
                            writableSpaces.firstOrNull { it.id == selectedSpaceId }?.name
                                ?: justCreatedSpace?.takeIf { it.first == selectedSpaceId }?.second
                                ?: "Choose a space",
                            color = ink,
                        )
                    }
                    DropdownMenu(expanded = spaceMenuOpen, onDismissRequest = { spaceMenuOpen = false }) {
                        writableSpaces.forEach { space ->
                            DropdownMenuItem(text = { Text(space.name) }, onClick = {
                                selectedSpaceId = space.id
                                justCreatedSpace = null
                                spaceMenuOpen = false
                            })
                        }
                    }
                    OutlinedTextField(
                        value = plantName, onValueChange = { plantName = it.take(120) },
                        label = { Text("Plant name *") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedTextField(
                        value = species, onValueChange = { species = it.take(180) },
                        label = { Text("Species (if known)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedTextField(
                        value = placement, onValueChange = { placement = it.take(180) },
                        label = { Text("Placement within the space") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedButton(
                        onClick = { photoPicker.launch("image/*") }, enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (plantPhoto == null) "+ Real plant photo (optional)" else "Change photo", color = ink) }
                    plantPhoto?.let { uri ->
                        AsyncImage(
                            model = uri, contentDescription = "Selected plant photo",
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        TextButton(onClick = { plantPhoto = null }, enabled = !saving) {
                            Text("Remove photo", color = textMuted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            enabled = !saving && plantName.isNotBlank() && selectedSpaceId != null,
                            onClick = {
                                val targetSpace = selectedSpaceId ?: return@Button
                                scope.launch {
                                    saving = true
                                    error = null
                                    when (val result = repository.createPlant(
                                        targetSpace, plantName, species, placement, plantPhoto,
                                    )) {
                                        is ApiResult.Failure -> error = result.message
                                        is ApiResult.Success -> {
                                            plantName = ""
                                            species = ""
                                            placement = ""
                                            plantPhoto = null
                                            plantOpen = false
                                            onPlantCreated(result.value, targetSpace)
                                        }
                                    }
                                    saving = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ink),
                        ) { Text(if (saving) "Saving…" else "Add plant") }
                        TextButton(onClick = { plantOpen = false; error = null }, enabled = !saving) {
                            Text("Cancel", color = ink)
                        }
                    }
                }
            }
        }

        error?.let { Text(it, color = Color(0xFF9A4A31), fontSize = 12.sp) }
    }
}
