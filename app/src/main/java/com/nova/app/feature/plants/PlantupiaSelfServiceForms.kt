package com.nova.app.feature.plants

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
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
    val context = LocalContext.current
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
    var cameraOpen by remember { mutableStateOf(false) }
    val latestPhoto = rememberUpdatedState(plantPhoto)
    fun replacePhoto(photo: Uri?) {
        if (plantPhoto != photo) plantPhoto?.let { ownedCaptureFile(context.cacheDir, it)?.delete() }
        plantPhoto = photo
    }
    DisposableEffect(repository) {
        onDispose { latestPhoto.value?.let { ownedCaptureFile(context.cacheDir, it)?.delete() } }
    }
    var photoPickerOrigin by remember { mutableStateOf<PlantupiaRepository?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null && photoPickerOrigin === repository && repository.isCurrentSession()) replacePhoto(it)
        photoPickerOrigin = null
    }
    fun browsePhoto() {
        photoPickerOrigin = repository
        photoPicker.launch("image/*")
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
                section = "A place for life",
                step = spaceChapter,
                totalSteps = 2,
                title = if (spaceChapter == 0) "New space" else "A little context",
                description = if (spaceChapter == 0)
                    "Every plant lives somewhere. Begin with a place that belongs to you."
                else "Add only the details you want to remember. This space stays private.",
                artwork = R.drawable.plantupia_artlab_window.takeIf { spaceChapter == 0 },
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
                section = "A living specimen",
                step = currentChapter,
                totalSteps = 3,
                title = when (currentChapter) {
                    0 -> "New plant"
                    1 -> "A name and a place"
                    else -> "Its first record"
                },
                description = when (currentChapter) {
                    0 -> "Begin with your plant. A photo and confirmed species are optional."
                    1 -> "Give it a name and choose its place."
                    else -> "Review the private plant you are about to add."
                },
                artwork = null,
                busy = saving,
                onClose = { cameraOpen = false; plantOpen = false; error = null; plantChapter = 0 },
                onBack = if (currentChapter > 0) ({ plantChapter = currentChapter - 1; error = null }) else null,
            ) {
                if (currentChapter == 0) {
                    PlantupiaCreationPhotograph(plantPhoto, saving, { cameraOpen = true }, ::browsePhoto) {
                        replacePhoto(null)
                    }
                    HorizontalDivider(color = PlantupiaColors.border)
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
                    Text("Place", style = PlantupiaText.sectionLabel)
                    PlantupiaActionRow(
                        icon = com.nova.app.ui.icons.NovaIconAsset.Home,
                        title = spaceLabel ?: "Choose a space",
                        enabled = !saving,
                        onClick = { spaceMenuOpen = !spaceMenuOpen },
                    )
                    if (spaceMenuOpen) {
                        Column(
                            Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                        ) {
                            writableSpaces.forEach { space ->
                                HorizontalDivider(color = PlantupiaColors.border)
                                TextButton(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                    enabled = !saving,
                                    onClick = {
                                        selectedSpaceId = space.id
                                        justCreatedSpace = null
                                        spaceMenuOpen = false
                                    },
                                ) { Text(space.name, style = PlantupiaText.itemTitle) }
                            }
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
                    Text(if (unknownPlantType) "Type not confirmed" else species, style = PlantupiaText.supporting)
                    PlantupiaCreationPhotograph(plantPhoto, saving, { cameraOpen = true }, ::browsePhoto) {
                        replacePhoto(null)
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
                                        replacePhoto(null)
                                        cameraOpen = false
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

        if (plantOpen && cameraOpen) {
            PlantupiaCaptureStudio(
                plantName = plantName.ifBlank { "New plant" },
                onClose = { cameraOpen = false },
                onCapture = { photo ->
                    val accepted = repository.isCurrentSession() && plantOpen && cameraOpen && !saving
                    if (accepted) {
                        replacePhoto(photo)
                        cameraOpen = false
                    }
                    accepted
                },
            )
        }
        if (!spaceOpen && !plantOpen) error?.let { Text(it, color = Color(0xFF9A4A31), fontSize = 12.sp) }
    }
}


/** A real photo starts the specimen's identity; it never pretends to identify a species. */
@Composable
private fun PlantupiaCreationPhotograph(
    photo: Uri?,
    busy: Boolean,
    openCamera: () -> Unit,
    browse: () -> Unit,
    remove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (photo != null) {
            AsyncImage(model = photo, contentDescription = "Selected plant photo",
                modifier = Modifier.fillMaxWidth().height(218.dp).clip(PlantupiaShapes.card)
                    .background(PlantupiaColors.softSage),
                contentScale = ContentScale.Fit)
            Text("Private photograph", style = PlantupiaText.supporting)
        } else {
            Surface(color = PlantupiaColors.softSage, shape = PlantupiaShapes.card,
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Canvas(Modifier.size(54.dp)) {
                        val stroke = 1.8.dp.toPx()
                        val inset = size.width * .18f
                        val leg = size.width * .2f
                        val right = size.width - inset
                        val bottom = size.height - inset
                        val corners = listOf(
                            Offset(inset, inset) to Offset(1f, 1f),
                            Offset(right, inset) to Offset(-1f, 1f),
                            Offset(inset, bottom) to Offset(1f, -1f),
                            Offset(right, bottom) to Offset(-1f, -1f),
                        )
                        for ((point, direction) in corners) {
                            drawLine(ink, point, point + Offset(direction.x * leg, 0f), stroke)
                            drawLine(ink, point, point + Offset(0f, direction.y * leg), stroke)
                        }
                        drawCircle(ink, size.width * .14f, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                    }
                    Text("Meet your plant", style = PlantupiaText.editorialHeading)
                    Text("Capture its actual appearance, or continue without a photo.", style = PlantupiaText.supporting)
                }
            }
        }
        Button(enabled = !busy, onClick = openCamera,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ink)) {
            Text(if (photo == null) "Take a plant photo" else "Retake plant photo")
        }
        OutlinedButton(enabled = !busy, onClick = browse,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (photo == null) "Choose existing photo" else "Change photo")
        }
        if (photo != null) TextButton(onClick = remove, enabled = !busy) {
            Text("Remove photo", color = textMuted)
        }
    }
}

/** An authored, full-screen creation chapter, not a new route or separate data owner. */
@Composable
private fun CreationChapter(
    section: String,
    step: Int,
    totalSteps: Int,
    title: String,
    description: String,
    artwork: Int?,
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
                key(section, step) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CreationFrontispiece(section, step, totalSteps, title, description, artwork)
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun CreationFrontispiece(
    section: String,
    step: Int,
    totalSteps: Int,
    title: String,
    description: String,
    artwork: Int?,
) {
    val tint = PlantupiaColors.forest
    Text(section, style = PlantupiaText.sectionLabel)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalSteps) { position ->
            Canvas(Modifier.weight(1f).height(3.dp)) {
                val shade = when {
                    position == step -> tint
                    position < step -> PlantupiaColors.mutedSage
                    else -> PlantupiaColors.border
                }
                drawLine(shade, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height)
            }
        }
    }
    Text("Step ${step + 1} of $totalSteps", style = PlantupiaText.supporting)
    val typeset: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = PlantupiaText.display)
            Text(description, style = PlantupiaText.body)
        }
    }
    if (artwork != null) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val painter = painterResource(artwork)
            if (maxWidth / LocalDensity.current.fontScale >= 305.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                    typeset(Modifier.weight(1f))
                    Box(Modifier.size(112.dp, 132.dp).background(PlantupiaColors.softSage), contentAlignment = Alignment.Center) {
                        Image(painter, contentDescription = null, modifier = Modifier.padding(8.dp).fillMaxSize(),
                            contentScale = ContentScale.Fit)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    typeset(Modifier.fillMaxWidth())
                    Image(painter, contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(92.dp).background(PlantupiaColors.softSage),
                        contentScale = ContentScale.Fit)
                }
            }
        }
    } else typeset(Modifier.fillMaxWidth())
    HorizontalDivider(color = PlantupiaColors.border)
}
