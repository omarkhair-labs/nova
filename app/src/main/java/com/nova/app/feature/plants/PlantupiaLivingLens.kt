package com.nova.app.feature.plants

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import java.util.UUID

private val lensDark = Color(0xFF102E25)
private val lensLight = Color(0xFFDCEDC8)
private enum class LensTask { HOME, SCAN, IDENTIFY, TIME }
private enum class LensCapture { SCAN, WHOLE, DETAIL }

/** Optical, botanical linework is a decorative identity, never a simulated detection. */
@Composable
private fun LensPlate(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * .38f
        repeat(3) { n -> drawCircle(lensLight.copy(alpha = .16f + n * .09f),
            radius = r * (1f + n * .18f), center = c, style = Stroke(.9.dp.toPx())) }
        val leaf = Path().apply {
            moveTo(c.x-r*.62f, c.y+r*.65f)
            cubicTo(c.x-r*1.05f,c.y-r*.35f,c.x+r*.05f,c.y-r*.9f,c.x+r*.66f,c.y-r*.7f)
            cubicTo(c.x+r*.85f,c.y+r*.2f,c.x+r*.18f,c.y+r*.75f,c.x-r*.62f,c.y+r*.65f)
            close()
        }
        drawPath(leaf,lensLight.copy(alpha=.12f))
        drawPath(leaf,lensLight,style=Stroke(1.6.dp.toPx()))
        drawLine(lensLight,c+Offset(-r*.73f,r*.77f),c+Offset(r*.48f,-r*.52f),1.4.dp.toPx())
        for(i in 1..4) {
            val t=i/5f
            val p=c+Offset(-r*.62f+r*1.10f*t,r*.65f-r*1.17f*t)
            drawLine(lensLight.copy(alpha=.62f),p,p+Offset(-r*.30f,-r*.27f),.9.dp.toPx())
            drawLine(lensLight.copy(alpha=.62f),p,p+Offset(r*.32f,r*.12f),.9.dp.toPx())
        }
        drawCircle(lensLight,2.5.dp.toPx(),c+Offset(r*.78f,-r*.63f))
    }
}

@Composable
internal fun PlantupiaLensEntry(plants: List<PlantupiaPlant>, repository: PlantupiaRepository, onRecordChanged: () -> Unit) =
    key(repository, plants.map { it.id }) {
        var open by remember { mutableStateOf(false) }
        Surface(onClick = { open = true }, color = lensDark, shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LIVING LENS", style = PlantupiaText.sectionLabel, color = lensLight)
                    Text("See life closer.", style = PlantupiaText.display, color = Color.White)
                    Text("Identify · inspect · compare", style = PlantupiaText.supporting, color = lensLight)
                    Text("Open lens →", style = PlantupiaText.itemTitle, color = Color.White)
                }
                LensPlate(Modifier.size(96.dp))
            }
        }
        if (open && repository.isCurrentSession()) {
            LensStudio(plants.filter { it.active }, repository, close = { open = false }, onRecordChanged = onRecordChanged)
        }
    }

@Composable
private fun LensAction(title: String, detail: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        color = PlantupiaColors.paper, shape = PlantupiaShapes.inset,
        border = BorderStroke(1.dp, PlantupiaColors.border)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = PlantupiaText.editorialHeading)
                Text(detail, style = PlantupiaText.supporting)
            }
            Text("→", style = PlantupiaText.itemTitle)
        }
    }
}

@Composable
private fun LensStudio(plants: List<PlantupiaPlant>, repository: PlantupiaRepository,
    close: () -> Unit, onRecordChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var task by remember { mutableStateOf(LensTask.HOME) }
    var selected by remember { mutableStateOf(plants.singleOrNull()?.id) }
    val subject = plants.firstOrNull { it.id == selected }
    var menu by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<LensCapture?>(null) }
    var scan by remember { mutableStateOf<Uri?>(null) }
    var whole by remember { mutableStateOf<Uri?>(null) }
    var detail by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var savedId by remember { mutableStateOf<String?>(null) }
    var savedEvent by remember { mutableStateOf<PlantupiaCareEvent?>(null) }
    var changed by remember { mutableStateOf(false) }
    DisposableEffect(scan) {
        val file = scan?.let { ownedCaptureFile(context.cacheDir, it) }
        onDispose { file?.delete() }
    }
    DisposableEffect(whole) {
        val file = whole?.let { ownedCaptureFile(context.cacheDir, it) }
        onDispose { file?.delete() }
    }
    DisposableEffect(detail) {
        val file = detail?.let { ownedCaptureFile(context.cacheDir, it) }
        onDispose { file?.delete() }
    }
    fun leave() {
        close()
        if (changed && repository.isCurrentSession()) onRecordChanged()
    }
    fun analyse(uri: Uri) {
        val plant = subject ?: return
        if (busy || !plant.canRecord || !repository.isCurrentSession()) return
        busy = true
        error = ""
        scope.launch {
            try {
                val existing = savedId
                if (existing == null) {
                    when (val result = repository.recordWithPhoto(plant.id, "observation", "", uri)) {
                        is ApiResult.Failure -> { error = result.message; return@launch }
                        is ApiResult.Success -> {
                            if (!repository.isCurrentSession()) return@launch
                            savedId = result.value
                            changed = true
                            repository.confirmSubmission(plant.id, "event", result.value)
                        }
                    }
                }
                if (!repository.isCurrentSession()) return@launch
                when (val records = repository.historyPage(plant.id)) {
                    is ApiResult.Failure -> error = "Photo saved. " + records.message
                    is ApiResult.Success -> {
                        if (!repository.isCurrentSession()) return@launch
                        savedEvent = records.value.events.firstOrNull { it.id == savedId }
                        if (savedEvent == null) error = "Photo saved. Refresh to open its analysis."
                    }
                }
            } finally { busy = false }
        }
    }
    val event = savedEvent
    if (event != null && subject != null) {
        PlantupiaFieldInspection(repository, subject, event, directClose = { leave() }, focusOutline = true)
        return
    }
    PlantupiaLivingWorkspace("LIVING LENS", when (task) {
        LensTask.HOME -> "A closer look."
        LensTask.SCAN -> "Inspect a photo"
        LensTask.IDENTIFY -> "Find its name"
        LensTask.TIME -> "See what changed"
    }, subject?.name.orEmpty(), busy, close = {
        if (task == LensTask.HOME || changed) leave() else {
            task = LensTask.HOME
            scan = null; whole = null; detail = null; error = ""
        }
    }, navigationKey = task) {
        when (task) {
            LensTask.HOME -> {
                Surface(color = lensDark, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LensPlate(Modifier.fillMaxWidth().height(148.dp))
                        Text("One plant. More ways to see it.", style = PlantupiaText.editorialHeading, color = Color.White)
                        Text("Real photographs, recorded change and visible evidence.", style = PlantupiaText.supporting, color = lensLight)
                    }
                }
                if (plants.size > 1 || subject == null) Box {
                    OutlinedButton(onClick = { menu = true }, enabled = plants.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()) { Text(subject?.name ?: "Choose a plant for inspection") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        plants.forEach { plant -> DropdownMenuItem(text = { Text(plant.name) },
                            onClick = { selected = plant.id; menu = false }) }
                    }
                }
                LensAction("Inspect a leaf", "Capture, outline and examine one visible part.",
                    enabled = subject?.canRecord == true) { task = LensTask.SCAN; capture = LensCapture.SCAN }
                LensAction("Identify a plant", "Compare a whole plant and a close-up. No record is changed.") {
                    task = LensTask.IDENTIFY; capture = LensCapture.WHOLE
                }
                LensAction("Compare over time", "Slide between your actual dated photographs.",
                    enabled = subject != null) { task = LensTask.TIME }
                if (subject == null) Text("Choose a plant to inspect or compare its records.", style = PlantupiaText.supporting)
            }
            LensTask.SCAN -> {
                val photo = scan
                if (photo == null) {
                    Text("Take a leaf photo or choose one from your phone.", style = PlantupiaText.body)
                    Button(onClick = { capture = LensCapture.SCAN }, modifier = Modifier.fillMaxWidth()) { Text("Open camera") }
                } else {
                    LensPhoto(photo, "Photograph for inspection", Modifier.fillMaxWidth().height(220.dp))
                    if (busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Preparing your private inspection…", style = PlantupiaText.body)
                    } else Button(onClick = { analyse(photo) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (savedId != null) "Open saved inspection" else "Retry analysis")
                    }
                }
                if (error.isNotBlank()) Text(error, style = PlantupiaText.supporting)
            }
            LensTask.IDENTIFY -> {
                Text("Two views of the same plant help the model compare shape and detail.", style = PlantupiaText.body)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LensPhotoSlot("1 · Whole plant", whole, Modifier.weight(1f)) { capture = LensCapture.WHOLE }
                    LensPhotoSlot("2 · Leaf or flower", detail, Modifier.weight(1f)) { capture = LensCapture.DETAIL }
                }
                val first = whole
                val second = detail
                if (first != null && second != null && first != second) {
                    key(first, second) {
                        PlantupiaSpeciesReview(analyse = { repository.identifyPlantPhotos(first, second) },
                            current = repository::isCurrentSession, prominent = true)
                    }
                } else Text("Add both distinct views to begin.", style = PlantupiaText.supporting)
                Text("Private analysis; the result will not change any plant identity.", style = PlantupiaText.supporting)
            }
            LensTask.TIME -> if (subject != null) LensTime(subject, repository)
        }
    }
    val target = capture
    if (target != null && repository.isCurrentSession()) PlantupiaCaptureStudio(
        plantName = if (target == LensCapture.WHOLE) "Whole plant" else if (target == LensCapture.DETAIL) "Leaf or flower" else subject?.name.orEmpty(),
        initialView = if (target == LensCapture.WHOLE) CaptureView.WHOLE else CaptureView.LEAF,
        startLiveOutline = target == LensCapture.SCAN,
        captureActionLabel = if (target == LensCapture.SCAN) "Analyse & save privately" else "Use this view",
        captureDescription = if (target == LensCapture.SCAN) "Saves the photo as a private observation, then opens its analysis." else "Used for this private comparison only.",
        onClose = { capture = null },
        onCapture = { uri ->
            if (!repository.isCurrentSession() || capture != target) false else when (target) {
                LensCapture.SCAN -> if (subject?.canRecord != true || busy) false else {
                    scan = uri; capture = null; analyse(uri); true
                }
                LensCapture.WHOLE -> if (uri == detail) false else { whole = uri; capture = null; true }
                LensCapture.DETAIL -> if (uri == whole) false else { detail = uri; capture = null; true }
            }
        },
    )
}

@Composable
private fun LensPhoto(uri: Uri, description: String, modifier: Modifier) {
    AsyncImage(model = uri, contentDescription = description, contentScale = ContentScale.Fit,
        modifier = modifier.clip(PlantupiaShapes.inset))
}

@Composable
private fun LensPhotoSlot(title: String, uri: Uri?, modifier: Modifier, onChoose: () -> Unit) {
    Surface(onClick = onChoose, modifier = modifier, shape = PlantupiaShapes.inset, color = PlantupiaColors.softSage) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (uri == null) LensPlate(Modifier.fillMaxWidth().height(118.dp))
            else LensPhoto(uri, title, Modifier.fillMaxWidth().height(118.dp))
            Text(title, style = PlantupiaText.itemTitle)
            Text(if (uri == null) "Add photo" else "Change photo", style = PlantupiaText.supporting)
        }
    }
}

@Composable
private fun LensTime(plant: PlantupiaPlant, repository: PlantupiaRepository) {
    var pair by remember(plant.id) { mutableStateOf<PlantupiaLoadedPhotoPair?>(null) }
    var loading by remember(plant.id) { mutableStateOf(true) }
    var error by remember(plant.id) { mutableStateOf("") }
    var retry by remember(plant.id) { mutableIntStateOf(0) }
    var older by remember(plant.id) { mutableStateOf(false) }
    var viewing by remember(plant.id) { mutableStateOf<PlantupiaCareEvent?>(null) }
    LaunchedEffect(plant.id, retry) {
        loading = true; error = ""; pair = null
        when (val result = repository.historyPage(plant.id)) {
            is ApiResult.Failure -> error = result.message
            is ApiResult.Success -> if (repository.isCurrentSession()) {
                older = result.value.nextOffset != null
                pair = plantupiaLoadedPhotoPair(result.value.events)
                if (pair == null && older) {
                    val latest = recordedPlantPhotos(result.value.events).firstOrNull()
                    if (latest != null) when (val past = repository.historyPage(plant.id, previousPhotoFor = UUID.fromString(latest.id))) {
                        is ApiResult.Failure -> error = past.message
                        is ApiResult.Success -> pair = plantupiaLoadedPhotoPair(past.value.events + latest)
                    }
                }
            }
        }
        loading = false
    }
    val photos = pair
    when {
        loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        error.isNotBlank() -> { Text(error, style = PlantupiaText.body); TextButton(onClick = { retry++ }) { Text("Try again") } }
        photos != null -> {
            PlantupiaPhotoTimeLens(photos, older, onOpenPhoto = { viewing = it }, alignmentEnabled = true,
                automaticAlignment = { marks -> repository.alignVisits(plant.id, photos.earlier.id, photos.later.id, marks) },
                isCurrentSession = repository::isCurrentSession)
            PlantupiaMeasuredVisits(repository, plant.id, photos)
        }
        else -> Text("Compare needs two distinct recorded photographs from different days. Return to inspect a new photo.", style = PlantupiaText.body)
    }
    viewing?.let { PlantupiaPhotoViewer(it.photoUrl, plant.name) { viewing = null } }
}
