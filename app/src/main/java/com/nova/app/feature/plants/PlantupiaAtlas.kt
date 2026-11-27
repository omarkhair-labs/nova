package com.nova.app.feature.plants

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

private val atlasNight = Color(0xFF10241D)
private val atlasMint = Color(0xFFDAEBC9)
private val atlasLime = Color(0xFFB9FF84)
private enum class AtlasMode(val label: String) { NOW("Now"), TIME("Through time"), PLACE("In space") }

/** The home is an actual living entity, not a collection of engine destinations. */
@Composable
internal fun PlantupiaAtlasHome(
    plants: List<PlantupiaPlant>, spaces: List<PlantupiaSpace>, repository: PlantupiaRepository,
    refreshVersion: Int, onRecordChanged: () -> Unit,
    onOpenPlant: (PlantupiaPlant) -> Unit, onOpenPlace: (PlantupiaSpace) -> Unit,
) {
    if (plants.isEmpty()) return
    var selected by remember(repository) { mutableStateOf(todayCollectionFocus(plants)?.id ?: plants.first().id) }
    val plant = plants.firstOrNull { it.id == selected } ?: plants.first()
    val place = spaces.firstOrNull { it.id == plant.spaceId }
    var mode by remember(repository) { mutableStateOf(AtlasMode.NOW) }
    var events by remember(repository, plant.id) { mutableStateOf(emptyList<PlantupiaCareEvent>()) }
    var nextOffset by remember(repository, plant.id) { mutableStateOf<Int?>(null) }
    var loading by remember(repository, plant.id) { mutableStateOf(true) }
    var error by remember(repository, plant.id) { mutableStateOf("") }
    var retry by remember(repository, plant.id) { mutableIntStateOf(0) }
    var dateIndex by remember(repository, plant.id) { mutableIntStateOf(0) }
    var room by remember(repository, place?.id) { mutableStateOf<PlantupiaRoom?>(null) }
    var roomError by remember(repository, place?.id) { mutableStateOf("") }
    var roomRetry by remember(repository, place?.id) { mutableIntStateOf(0) }
    var capture by remember(repository) { mutableStateOf(false) }
    var observe by remember(repository) { mutableStateOf<Pair<PlantupiaPlant,Uri>?>(null) }
    var original by remember(repository) { mutableStateOf<String?>(null) }
    var comparison by remember(repository, plant.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(repository, plant.id, refreshVersion, retry) {
        loading = true
        error = ""
        when (val response = repository.historyPage(plant.id)) {
            is ApiResult.Failure -> error = response.message
            is ApiResult.Success -> if (repository.isCurrentSession()) {
                events = response.value.events
                nextOffset = response.value.nextOffset
                dateIndex = 0
            }
        }
        loading = false
    }
    LaunchedEffect(repository, place?.id, mode, refreshVersion, roomRetry) {
        if (mode != AtlasMode.PLACE || place == null) return@LaunchedEffect
        roomError = ""
        when (val response = repository.room(place.id)) {
            is ApiResult.Failure -> { room = null; roomError = response.message }
            is ApiResult.Success -> if (repository.isCurrentSession()) room = response.value
        }
    }
    val photos = recordedPlantPhotos(events)
    val latest = photos.firstOrNull()
    val visible = if (mode == AtlasMode.TIME) photos.getOrNull(dateIndex.coerceIn(0, (photos.size-1).coerceAtLeast(0))) else latest
    val source = visible?.photoUrl ?: plant.coverPhoto
    Surface(color = atlasNight, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("PLANTUPIA / LIVING ATLAS", color = atlasMint, style = PlantupiaText.sectionLabel.copy(letterSpacing = 2.sp))
                    Text(place?.name.orEmpty(), color = Color.White, style = PlantupiaText.supporting, maxLines = 2)
                }
                Text("${plants.indexOf(plant)+1} / ${plants.size}", color = atlasMint, style = PlantupiaText.supporting)
            }
            // A photograph is the anchor. Date, identity and place refer to the same selected subject.
            if (mode != AtlasMode.PLACE) {
                Box(Modifier.fillMaxWidth().height(354.dp).clip(RoundedCornerShape(16.dp))) {
                    Crossfade(targetState = source, animationSpec = tween(240), label = "recorded photograph") { url ->
                        PlantupiaEvidencePhoto(url, "Photograph of ${plant.name}", Modifier.fillMaxSize(),
                            cornerRadius = 0.dp, framing = PlantupiaPhotoFraming.COVER,
                            onOpenPhoto = { original = url })
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xE610241D)))))
                    Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (visible != null) visible.observedAt.take(10) + " / RECORDED" else if(source.isBlank()) "AWAITING PHOTOGRAPH" else "PRIVATE PORTRAIT",
                            color = atlasMint, style = PlantupiaText.sectionLabel)
                        Text(plant.name, color = Color.White, style = PlantupiaText.display.copy(fontFamily = FontFamily.Serif, fontSize = 32.sp, lineHeight = 36.sp),
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (plant.species.isNotBlank() && plant.identityState != "unknown") Text(plant.species, color = atlasMint, style = PlantupiaText.supporting)
                        if (mode == AtlasMode.TIME && visible != null && visible.note.isNotBlank())
                            Text(visible.note, color = Color.White, style = PlantupiaText.supporting, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (source.isNotBlank()) Surface(color = Color(0xD010241D), shape = CircleShape, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                        TextButton(onClick = { original = source }) { Text("View photo", color = Color.White) }
                    }
                }
            } else {
                val layout = room
                val width = layout?.width
                val length = layout?.length
                if (layout != null && width != null && length != null) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PlantupiaColors.paper).padding(9.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        PlantupiaRecordedSpatialPlan(layout, width, length, plant.id, immersive = true) { id ->
                            plants.firstOrNull { it.id == id && it.spaceId == place?.id }?.let { selected = it.id }
                        }
                        Text("Recorded geometry · $width × $length m", style = PlantupiaText.itemTitle)
                    }
                } else Box(Modifier.fillMaxWidth().height(262.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF203B2E)),
                    contentAlignment = Alignment.Center) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (roomError.isNotBlank()) "Place unavailable" else if (layout == null) "Opening your place" else "A place to grow.",
                            color = Color.White, style = PlantupiaText.display)
                        Text(if (roomError.isNotBlank()) roomError else if (layout == null) "Reading saved geometry…" else "There is no recorded room geometry yet. Add the dimensions when you need a spatial view.",
                            color = atlasMint, style = PlantupiaText.supporting)
                        if (roomError.isNotBlank()) TextButton(onClick = { roomRetry++ }) { Text("Retry", color = atlasMint) }
                        if (layout != null && place != null) TextButton(onClick = { onOpenPlace(place) }) { Text("Open place", color = atlasMint) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AtlasMode.entries.forEach { m ->
                    Surface(onClick = { mode = m }, color = if (mode == m) atlasMint else Color(0xFF284236),
                        shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Box(Modifier.padding(horizontal = 5.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(m.label, color = if (mode == m) atlasNight else Color.White, style = PlantupiaText.supporting,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
            if (mode == AtlasMode.TIME) {
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = atlasLime)
                if (error.isNotBlank()) TextButton(onClick = { retry++ }) { Text("Retry history", color = atlasMint) }
                if (photos.size > 1) {
                    Text("Move through your recorded photographs", color = atlasMint, style = PlantupiaText.supporting)
                    Slider(value = dateIndex.toFloat().coerceIn(0f, photos.lastIndex.toFloat()),
                        onValueChange = { dateIndex = it.roundToInt().coerceIn(0, photos.lastIndex) },
                        valueRange = 0f..photos.lastIndex.toFloat(), steps = photos.size - 2,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Recorded photograph date" },
                        colors = SliderDefaults.colors(thumbColor = atlasLime, activeTrackColor = atlasMint))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(photos.first().observedAt.take(10), color = atlasMint, style = PlantupiaText.supporting)
                        Text(photos.last().observedAt.take(10), color = atlasMint, style = PlantupiaText.supporting)
                    }
                } else if (!loading) Text("Record another visit to begin a visual history.", color = atlasMint, style = PlantupiaText.supporting)
                val pair = plantupiaLoadedPhotoPair(events)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pair != null) TextButton(onClick = { comparison = true }) { Text("Compare dates", color = atlasMint) }
                    nextOffset?.let { offset ->
                        TextButton(enabled = !loading, onClick = {
                            loading = true
                            scope.launch {
                                when (val response = repository.historyPage(plant.id, offset = offset)) {
                                    is ApiResult.Failure -> error = response.message
                                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                                        events = (events + response.value.events).distinctBy { it.id }
                                        nextOffset = response.value.nextOffset
                                    }
                                }
                                loading = false
                            }
                        }) { Text("Earlier visits", color = atlasMint) }
                    }
                }
            }
            if (plants.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(plants, key = { it.id }) { item ->
                    Surface(onClick = { selected = item.id }, color = if (item.id == plant.id) atlasMint else Color(0xFF284236),
                        shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.width(96.dp).padding(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            PlantupiaEvidencePhoto(item.coverPhoto, item.name, Modifier.fillMaxWidth().height(66.dp), cornerRadius = 9.dp, compact = true)
                            Text(item.name, color = if(item.id == plant.id) atlasNight else Color.White, style = PlantupiaText.supporting,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (mode == AtlasMode.PLACE && place != null) {
                Button(onClick = { onOpenPlace(place) }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text("Explore this place") }
            } else if (plant.canRecord) {
                Button(onClick = { capture = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = atlasMint, contentColor = atlasNight)) { Text("Observe your plant") }
            }
            TextButton(onClick = { onOpenPlant(plant) }, modifier = Modifier.fillMaxWidth()) { Text("Plant record & care", color = atlasMint) }
        }
    }
    if (capture && repository.isCurrentSession()) PlantupiaAtlasCapture(plant.name, "Record privately & reveal",
        "Saves this photograph as a private observation. The boundary is not saved until you review it.",
        onClose = { capture = false }, onCapture = { uri ->
            if (!repository.isCurrentSession() || !plant.canRecord) false else {
                observe = plant to uri
                capture = false
                true
            }
        })
    observe?.let { (subject, uri) ->
        if (repository.isCurrentSession()) AtlasObservation(subject, uri, repository, close = { changed ->
            observe = null
            if (changed) { retry++; onRecordChanged() }
        })
    }
    original?.let { PlantupiaPhotoViewer(it, plant.name) { original = null } }
    if (comparison) plantupiaLoadedPhotoPair(events)?.let { pair ->
        PlantupiaLivingWorkspace("THROUGH TIME", plant.name, "Recorded photographs", false, close = { comparison = false }) {
            PlantupiaPhotoTimeLens(pair, nextOffset != null, onOpenPhoto = { original = it.photoUrl }, alignmentEnabled = true,
                automaticAlignment = { marks -> repository.alignVisits(plant.id, pair.earlier.id, pair.later.id, marks) },
                isCurrentSession = repository::isCurrentSession)
            PlantupiaMeasuredVisits(repository, plant.id, pair)
        }
    }
}

private fun atlasPoints(points: List<IntOffset>) = JSONArray().apply { points.forEach { put(JSONArray().put(it.x).put(it.y)) } }

/** In-place evidence reveal on the actual retained image. Existing signed server contract is preserved. */
@Composable
private fun AtlasObservation(plant: PlantupiaPlant, uri: Uri, repository: PlantupiaRepository, close: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var eventId by remember { mutableStateOf<String?>(null) }
    var record by remember { mutableStateOf<PlantupiaCareEvent?>(null) }
    var evidence by remember { mutableStateOf<JSONObject?>(null) }
    var proposal by remember { mutableStateOf<OutlineProposal?>(null) }
    var region by remember { mutableStateOf<FieldRegion?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf("Keeping your observation") }
    var attempt by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    var foreground by remember { mutableStateOf<IntOffset?>(null) }
    var background by remember { mutableStateOf<IntOffset?>(null) }
    var refining by remember { mutableStateOf(false) }
    var part by remember { mutableStateOf("leaf") }
    val submission = remember(proposal?.token, part) { UUID.randomUUID().toString() }
    val width = evidence?.optJSONObject("quality")?.optInt("width") ?: 0
    val height = evidence?.optJSONObject("quality")?.optInt("height") ?: 0
    DisposableEffect(uri) {
        val file = ownedCaptureFile(context.cacheDir, uri)
        onDispose { file?.delete() }
    }
    suspend fun suggest(body: JSONObject) {
        val id = eventId ?: return
        phase = "Finding the visible boundary"
        message = ""
        proposal = null
        when (val response = repository.fieldEvidence(plant.id, id, "suggest-region/", body)) {
            is ApiResult.Failure -> message = response.message
            is ApiResult.Success -> if (repository.isCurrentSession()) {
                proposal = parseOutlineProposal(response.value, width, height)
                if (proposal == null) { message = "No distinct boundary found. Tap the part, then its background."; refining = true }
            }
        }
    }
    LaunchedEffect(attempt) {
        busy = true
        message = ""
        try {
            if (eventId == null) {
                when (val result = repository.recordWithPhoto(plant.id, "observation", "", uri)) {
                    is ApiResult.Failure -> { message = result.message; return@LaunchedEffect }
                    is ApiResult.Success -> {
                        if (!repository.isCurrentSession()) return@LaunchedEffect
                        eventId = result.value
                        repository.confirmSubmission(plant.id, "event", result.value)
                    }
                }
            }
            val id = eventId ?: return@LaunchedEffect
            if (record == null) when (val result = repository.historyPage(plant.id)) {
                is ApiResult.Failure -> { message = result.message; return@LaunchedEffect }
                is ApiResult.Success -> if (repository.isCurrentSession()) record = result.value.events.firstOrNull { it.id == id }
            }
            if (record?.photoUrl.isNullOrBlank()) { message = "Photo saved. Open the observation again to load its evidence."; return@LaunchedEffect }
            when (val result = repository.fieldEvidence(plant.id, id)) {
                is ApiResult.Failure -> { message = result.message; return@LaunchedEffect }
                is ApiResult.Success -> if (repository.isCurrentSession()) {
                    evidence = result.value
                    val quality = result.value.optJSONObject("quality")
                    val w = quality?.optInt("width") ?: 0
                    val h = quality?.optInt("height") ?: 0
                    if (w > 0 && h > 0 && result.value.optBoolean("can_suggest_region")) {
                        phase = "Finding the visible boundary"
                        when (val answer = repository.fieldEvidence(plant.id, id, "suggest-region/", JSONObject().put("mode", "automatic"))) {
                            is ApiResult.Failure -> message = answer.message
                            is ApiResult.Success -> if (repository.isCurrentSession()) {
                                proposal = parseOutlineProposal(answer.value, w, h)
                                if (proposal == null) { message = "Tap the part, then its background."; refining = true }
                            }
                        }
                    } else message = "Photo saved. Outline analysis is unavailable for this photograph."
                }
            }
        } finally { busy = false }
    }
    val outline = region?.vertices ?: proposal?.vertices.orEmpty()
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(proposal?.token, region?.id) {
        if (outline.isNotEmpty()) { reveal.snapTo(0f); reveal.animateTo(1f, tween(650)) }
        else reveal.snapTo(1f)
    }
    val url = record?.photoUrl.orEmpty()
    Dialog(onDismissRequest = { if (!busy) close(eventId != null) },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false, dismissOnBackPress = !busy)) {
        Surface(color = atlasNight, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("OBSERVATION", color = atlasMint, style = PlantupiaText.sectionLabel)
                        Text(plant.name, color = Color.White, style = PlantupiaText.itemTitle)
                    }
                    TextButton(enabled = !busy, onClick = { close(eventId != null) }) { Text("Done", color = atlasMint) }
                }
                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF203B2E))) {
                    if (url.isNotBlank()) PlantupiaEvidencePhoto(url, "Recorded photograph", Modifier.fillMaxSize(), cornerRadius = 0.dp,
                        framing = PlantupiaPhotoFraming.FULL_RECORD, onPhotoLoadChanged = { loaded = it })
                    else coil3.compose.AsyncImage(model = uri, contentDescription = "Photograph being recorded", modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                    if (loaded && width > 0 && height > 0) Canvas(Modifier.fillMaxSize().pointerInput(width,height,refining,busy,foreground,background) {
                        detectTapGestures { point ->
                            if (!refining || busy) return@detectTapGestures
                            val scale = minOf(size.width.toFloat()/width,size.height.toFloat()/height)
                            val origin = Offset((size.width-width*scale)/2,(size.height-height*scale)/2)
                            val p = (point-origin)/scale
                            if (p.x >= 0 && p.x < width && p.y >= 0 && p.y < height) {
                                val mark = IntOffset(p.x.toInt(),p.y.toInt())
                                if (foreground == null || background != null) { foreground = mark; background = null }
                                else background = mark
                            }
                        }
                    }) {
                        val scale = minOf(size.width/width,size.height/height)
                        val origin = Offset((size.width-width*scale)/2,(size.height-height*scale)/2)
                        fun project(p: IntOffset) = origin + Offset(p.x*scale,p.y*scale)
                        if (outline.size >= 3) {
                            val path = Path().apply { val first=project(outline.first()); moveTo(first.x,first.y)
                                outline.drop(1).forEach { val p=project(it);lineTo(p.x,p.y) };close() }
                            drawPath(path, atlasLime.copy(alpha = .22f*reveal.value))
                            val measure = PathMeasure().apply { setPath(path,false) }
                            val visiblePath = Path()
                            measure.getSegment(0f, measure.length*reveal.value, visiblePath, true)
                            drawPath(visiblePath, atlasLime, style = Stroke(2.5.dp.toPx()))
                        }
                        foreground?.let { drawCircle(atlasLime,7.dp.toPx(),project(it)); drawCircle(Color.White,9.dp.toPx(),project(it),style=Stroke(1.dp.toPx())) }
                        background?.let { drawCircle(Color(0xFFF1BA91),7.dp.toPx(),project(it)); drawCircle(Color.White,9.dp.toPx(),project(it),style=Stroke(1.dp.toPx())) }
                    }
                    if (busy) Surface(color = Color(0xE010241D), shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(Modifier.fillMaxWidth(), color = atlasLime)
                            Text(phase, color = Color.White, style = PlantupiaText.itemTitle)
                        }
                    }
                }
                // Actions live immediately beside the result, never below a history form.
                Column(Modifier.fillMaxWidth().heightIn(max = 246.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    val saved = region
                    val candidate = proposal
                    when {
                        saved != null -> {
                            Text("Recorded boundary", color = atlasMint, style = PlantupiaText.sectionLabel)
                            Text("${saved.pixels} px²", color = Color.White, style = PlantupiaText.display)
                            Text("Visible image area, not physical size", color = atlasMint, style = PlantupiaText.supporting)
                            if (saved.colors.isNotEmpty()) {
                                val palette = listOf(Color(0xFF86B778),Color(0xFFE4C66B),Color(0xFFA78066),Color(0xFF9DA59B))
                                Canvas(Modifier.fillMaxWidth().height(15.dp).clip(RoundedCornerShape(6.dp))) {
                                    var x=0f
                                    saved.colors.forEachIndexed { i,f -> val w=(size.width*f).toFloat(); drawRect(palette[i],Offset(x,0f),androidx.compose.ui.geometry.Size(w,size.height));x+=w }
                                }
                                Text(listOf("Green-like","Yellow-like","Brown-like","Other").zip(saved.colors).joinToString(" · ") { (name,f) -> "$name ${(f*100).roundToInt()}%" },
                                    color = Color.White, style = PlantupiaText.supporting)
                                Text("Appearance in this photograph. Not a diagnosis.", color = atlasMint, style = PlantupiaText.supporting)
                            }
                        }
                        candidate != null && !refining -> {
                            Text("Is this the part you meant?", color = Color.White, style = PlantupiaText.editorialHeading)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("leaf","plant","flower").forEach { p ->
                                    FilterChip(selected = part == p, enabled = !busy, onClick = { part = p }, label = { Text(p.replaceFirstChar { it.uppercase() }) })
                                }
                            }
                            Button(enabled = !busy && loaded, modifier = Modifier.fillMaxWidth(), onClick = {
                                val id = eventId ?: return@Button
                                busy = true; phase = "Measuring the accepted boundary"; message = ""
                                scope.launch {
                                    try {
                                        val body = JSONObject().put("submission_id",submission).put("part",part)
                                            .put("vertices",atlasPoints(candidate.vertices)).put("suggestion_token",candidate.token).put("outline_method",candidate.method)
                                        when (val response = repository.fieldEvidence(plant.id,id,"regions/",body)) {
                                            is ApiResult.Failure -> message = response.message
                                            is ApiResult.Success -> if (repository.isCurrentSession()) {
                                                val savedId = response.value.optLong("id")
                                                val row = JSONObject().put("id",savedId).put("annotation",JSONObject().put("part",part).put("vertices",atlasPoints(candidate.vertices)))
                                                    .put("measurements",response.value.optJSONObject("measurements"))
                                                region = fieldRegions(JSONArray().put(row),width,height).firstOrNull()
                                                if (region == null) message = "Boundary saved. Reopen the record to see its measurements."
                                                else { proposal=null;foreground=null;background=null }
                                            }
                                        }
                                    } finally { busy=false }
                                }
                            }) { Text("Keep boundary") }
                            TextButton(enabled = !busy, onClick = { refining=true; foreground=null; background=null; proposal=null }) { Text("Choose another part", color = atlasMint) }
                        }
                        refining -> {
                            Text(if(foreground==null) "Tap the part you want." else if(background==null) "Tap outside the part." else "Ready to refine.", color = Color.White, style = PlantupiaText.editorialHeading)
                            val fg = foreground; val bg = background
                            if (fg != null && bg != null) Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                                busy=true;scope.launch { try {
                                    val body=JSONObject().put("foreground",atlasPoints(listOf(fg))).put("background",atlasPoints(listOf(bg)))
                                    if(evidence?.optBoolean("can_neural_suggest_region")==true) body.put("mode","neural")
                                    suggest(body)
                                    refining=proposal==null
                                } finally {busy=false} }
                            }) { Text("Reveal this part") }
                            TextButton(enabled = !busy, onClick = {foreground=null;background=null}) { Text("Reset selection",color=atlasMint) }
                        }
                        !busy -> {
                            if (message.isNotBlank()) Text(message,color=Color.White,style=PlantupiaText.supporting)
                            if (evidence?.optBoolean("can_suggest_region")==true && width>0 && height>0)
                                TextButton(onClick={refining=true;foreground=null;background=null}) {Text("Choose a visible part",color=atlasMint)}
                            else TextButton(onClick={attempt++}) {Text("Retry",color=atlasMint)}
                        }
                    }
                    if (message.isNotBlank() && (candidate!=null || refining || saved!=null)) Text(message,color=atlasMint,style=PlantupiaText.supporting)
                }
            }
        }
    }
}
