package com.nova.app.feature.plants

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontFamily
import com.nova.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nova.app.core.auth.NovaSessionStore
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
    showEmptyStartCard: Boolean = true,
    onRequestConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val loginGeneration = remember(context, repository) {
        NovaSessionStore(context).identity().generation
    }
    key(loginGeneration) {
        PlantupiaAccountSelfServiceForms(
            spaces, repository, onCreated, onRefreshSpaces, onPlantCreated,
            preferredSpaceId, enabled, creationRequest, showEmptyStartCard,
            onRequestConsumed,
        )
    }
}

@Composable
private fun PlantupiaAccountSelfServiceForms(
    spaces: List<PlantupiaSpace>,
    repository: PlantupiaRepository,
    onCreated: () -> Unit,
    onRefreshSpaces: () -> Unit,
    onPlantCreated: (plantId: String, spaceId: String) -> Unit,
    preferredSpaceId: String?,
    enabled: Boolean,
    creationRequest: PlantupiaCreateMode?,
    showEmptyStartCard: Boolean,
    onRequestConsumed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Create → Home opens a real editor below the photo-led space hero.
    // Bring only its heading into view; avoid trying to scroll a long form in full.
    val creationEditorHeading = remember { BringIntoViewRequester() }
    val writableSpaces = spaces.filter { it.canAddPlant }
    // Only unfinished form text/selection survives Activity recreation. This
    // never restores a save, a URI permission or authority to write a space.
    var spaceChapter by rememberSaveable { mutableStateOf(0) }
    var plantChapter by rememberSaveable { mutableStateOf(0) }
    var spaceOpen by rememberSaveable { mutableStateOf(false) }
    var plantOpen by rememberSaveable { mutableStateOf(false) }
    var accessNotice by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var spaceName by rememberSaveable { mutableStateOf("") }
    var spaceLocation by rememberSaveable { mutableStateOf("") }
    var spaceDescription by rememberSaveable { mutableStateOf("") }
    var plantName by rememberSaveable { mutableStateOf("") }
    var species by rememberSaveable { mutableStateOf("") }
    var selectedSpeciesKey by rememberSaveable { mutableStateOf<String?>(null) }
    var unknownPlantType by rememberSaveable { mutableStateOf(false) }
    var placement by rememberSaveable { mutableStateOf("") }
    var selectedSpaceId by rememberSaveable { mutableStateOf<String?>(null) }
    // The newly returned UUID is authorized, but may precede the refreshing list.
    var justCreatedSpace by remember { mutableStateOf<Pair<String, String>?>(null) }
    var spaceMenuOpen by remember { mutableStateOf(false) }
    var plantPhoto by remember { mutableStateOf<Uri?>(null) }
    var photoPickerOrigin by remember { mutableStateOf<PlantupiaRepository?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (photoPickerOrigin === repository && repository.isCurrentSession()) plantPhoto = it
        photoPickerOrigin = null
    }

    LaunchedEffect(spaces, enabled) {
        // Restored selections must wait for the current authorized list. An
        // initial loading/failed response is not proof that a space was revoked.
        if (!enabled) return@LaunchedEffect
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

    LaunchedEffect(spaceOpen, plantOpen, accessNotice, enabled) {
        if (enabled && accessNotice && !spaceOpen && !plantOpen) {
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
            if (showEmptyStartCard) Surface(color = paper, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
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
            CreationChapter(
                eyebrow = "A PLACE FOR LIFE / 0${spaceChapter + 1}",
                title = if (spaceChapter == 0) "New space" else "A little context",
                description = if (spaceChapter == 0)
                    "Every plant lives somewhere. Begin with a place that belongs to you."
                else "Add only the details you want to remember. This space stays private.",
                artwork = R.drawable.plantupia_artlab_window,
                busy = saving,
                onClose = { spaceOpen = false; error = null; spaceChapter = 0 },
                onBack = if (spaceChapter > 0) ({ spaceChapter--; error = null }) else null,
            ) {
                if (spaceChapter == 0) {
                    OutlinedTextField(
                        value = spaceName, onValueChange = { spaceName = it.take(140) },
                        label = { Text("Space name *") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    Text("A desk, balcony, room or an entire installation.", style = PlantupiaText.supporting)
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        enabled = spaceName.isNotBlank() && !saving,
                        onClick = { spaceChapter = 1; error = null },
                        colors = ButtonDefaults.buttonColors(containerColor = ink),
                    ) { Text("Continue") }
                } else {
                    Text(spaceName, style = PlantupiaText.itemTitle)
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
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        enabled = !saving && spaceName.isNotBlank(),
                        onClick = {
                            val submittedName = spaceName
                            val submittedDescription = spaceDescription
                            val submittedLocation = spaceLocation
                            saving = true
                            error = null
                            scope.launch {
                                val result = repository.createSpace(submittedName, submittedDescription, submittedLocation)
                                if (!repository.isCurrentSession()) return@launch
                                when (result) {
                                    is ApiResult.Failure -> error = result.message
                                    is ApiResult.Success -> {
                                        selectedSpaceId = result.value
                                        justCreatedSpace = result.value to submittedName.trim()
                                        spaceOpen = false
                                        plantOpen = true
                                        spaceChapter = 0
                                        plantChapter = 0
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
                    ) { Text(if (saving) "Saving…" else "Create space & continue") }
                }
                error?.let { Text(it, color = Color(0xFF9A4A31), fontSize = 13.sp) }
            }
        }

        if (plantOpen) {
            val typeChosen = signatureTypeChosen(selectedSpeciesKey, unknownPlantType)
            val currentChapter = if (!typeChosen) 0 else plantChapter
            val spaceLabel = writableSpaces.firstOrNull { it.id == selectedSpaceId }?.name
                ?: justCreatedSpace?.takeIf { it.first == selectedSpaceId }?.second
            CreationChapter(
                eyebrow = "A LIVING SPECIMEN / 0${currentChapter + 1}",
                title = when (currentChapter) {
                    0 -> "New plant"
                    1 -> "A name and a place"
                    else -> "Its first record"
                },
                description = when (currentChapter) {
                    0 -> "Start with what you know. A confirmed species is not required."
                    1 -> "Make this particular plant recognizable in your own space."
                    else -> "Keep a real photograph, or begin without one. This is the beginning of its private history."
                },
                artwork = if (currentChapter == 0) R.drawable.plantupia_signature_plate_inspection
                    else R.drawable.plantupia_artlab_courtyard,
                busy = saving,
                onClose = { plantOpen = false; error = null; plantChapter = 0 },
                onBack = if (currentChapter > 0) ({ plantChapter = currentChapter - 1; error = null }) else null,
            ) {
                if (currentChapter == 0) {
                    PlantupiaSpeciesPicker(
                        repository = repository,
                        selectedKey = selectedSpeciesKey,
                        selectedName = species,
                        unknownMode = unknownPlantType,
                        enabled = !saving,
                        onSelected = { key, scientificName ->
                            selectedSpeciesKey = key
                            species = scientificName
                            unknownPlantType = false
                            plantChapter = 1
                        },
                        onUnknown = {
                            selectedSpeciesKey = null
                            species = ""
                            unknownPlantType = true
                            plantChapter = 1
                        },
                        onBrowse = {
                            selectedSpeciesKey = null
                            unknownPlantType = false
                            species = ""
                            plantChapter = 0
                        },
                    )
                    if (typeChosen) Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        enabled = !saving, onClick = { plantChapter = 1 },
                        colors = ButtonDefaults.buttonColors(containerColor = ink),
                    ) { Text("Continue") }
                } else if (currentChapter == 1) {
                    Text(if (unknownPlantType) "Type not confirmed" else species, style = PlantupiaText.supporting)
                    OutlinedTextField(
                        value = plantName, onValueChange = { plantName = it.take(120) },
                        label = { Text("Plant name *") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    OutlinedButton(
                        onClick = { spaceMenuOpen = true },
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    ) { Text(spaceLabel ?: "Choose a space", color = ink) }
                    DropdownMenu(expanded = spaceMenuOpen, onDismissRequest = { spaceMenuOpen = false }) {
                        writableSpaces.forEach { space ->
                            DropdownMenuItem(text = { Text(space.name) }, onClick = {
                                selectedSpaceId = space.id
                                justCreatedSpace = null
                                spaceMenuOpen = false
                            })
                        }
                    }
                    if (unknownPlantType) {
                        OutlinedTextField(
                            value = species, onValueChange = { species = it.take(180) },
                            label = { Text("Type you suspect (optional, not verified)") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(), enabled = !saving,
                        )
                    }
                    OutlinedTextField(
                        value = placement, onValueChange = { placement = it.take(180) },
                        label = { Text("Placement within the space") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), enabled = !saving,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        enabled = !saving && plantName.isNotBlank() && spaceLabel != null,
                        onClick = { plantChapter = 2; error = null },
                        colors = ButtonDefaults.buttonColors(containerColor = ink),
                    ) { Text("Continue") }
                } else {
                    Text(plantName, style = PlantupiaText.pageHeading)
                    Text(spaceLabel ?: "Space access needs to be refreshed", style = PlantupiaText.supporting)
                    PlantupiaActionRow(
                        icon = com.nova.app.ui.icons.NovaIconAsset.Photo,
                        title = if (plantPhoto == null) "Add photo" else "Change photo",
                        detail = if (plantPhoto == null) "Optional · actual plant" else null,
                        enabled = !saving,
                        onClick = { photoPickerOrigin = repository; photoPicker.launch("image/*") },
                    )
                    plantPhoto?.let { uri ->
                        AsyncImage(
                            model = uri, contentDescription = "Selected plant photo",
                            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit,
                        )
                        TextButton(onClick = { plantPhoto = null }, enabled = !saving) {
                            Text("Remove photo", color = textMuted)
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        enabled = !saving && plantName.isNotBlank() && spaceLabel != null && typeChosen,
                        onClick = {
                            val targetSpace = selectedSpaceId ?: return@Button
                            val submittedName = plantName
                            val submittedSpecies = species
                            val submittedSpeciesKey = selectedSpeciesKey
                            val submittedPlacement = placement
                            val submittedPhoto = plantPhoto
                            saving = true
                            error = null
                            scope.launch {
                                val result = repository.createPlant(
                                    targetSpace, submittedName, submittedSpecies, submittedPlacement, submittedPhoto,
                                    selectedSpeciesKey = submittedSpeciesKey,
                                )
                                if (!repository.isCurrentSession()) return@launch
                                when (result) {
                                    is ApiResult.Failure -> error = result.message
                                    is ApiResult.Success -> {
                                        plantName = ""
                                        species = ""
                                        selectedSpeciesKey = null
                                        unknownPlantType = false
                                        placement = ""
                                        plantPhoto = null
                                        plantOpen = false
                                        plantChapter = 0
                                        onPlantCreated(result.value, targetSpace)
                                    }
                                }
                                saving = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ink),
                    ) { Text(if (saving) "Saving…" else "Add plant") }
                }
                error?.let { Text(it, color = Color(0xFF9A4A31), fontSize = 13.sp) }
            }
        }

        if (!spaceOpen && !plantOpen) error?.let { Text(it, color = Color(0xFF9A4A31), fontSize = 12.sp) }
    }
}

/** An authored, full-screen creation chapter, not a new route or separate data owner. */
@Composable
private fun CreationChapter(
    eyebrow: String,
    title: String,
    description: String,
    artwork: Int,
    busy: Boolean,
    onClose: () -> Unit,
    onBack: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!busy) (onBack ?: onClose)() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = !busy,
        ),
    ) {
        Surface(color = paper, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (onBack != null) TextButton(enabled = !busy, onClick = onBack) { Text("Back", color = ink) }
                    TextButton(enabled = !busy, onClick = onClose) { Text("Cancel", color = ink) }
                }
                androidx.compose.runtime.key(eyebrow) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(eyebrow, color = textMuted, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Text(title, color = ink, fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 40.sp)
                        Text(description, style = PlantupiaText.body)
                        Image(
                            painterResource(artwork), contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(118.dp),
                            contentScale = ContentScale.Fit,
                        )
                        androidx.compose.material3.HorizontalDivider(color = PlantupiaColors.border)
                        content()
                    }
                }
            }
        }
    }
}
