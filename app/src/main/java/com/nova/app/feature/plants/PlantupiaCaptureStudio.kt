package com.nova.app.feature.plants

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import com.nova.app.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private enum class CaptureView(val title: String, val guidance: String, val frameWidth: Float, val frameHeight: Float) {
    WHOLE("Whole plant", "Include the whole plant and pot. For a future comparison, repeat this view at the same height.", .72f, .80f),
    LEAF("Leaf detail", "Keep one leaf still and include its edges. Tap the leaf in the preview to focus.", .68f, .56f),
    STEM("Stem", "Frame a section of stem with one node. Hold the camera level with that section.", .40f, .78f),
    FLOWER("Flower", "Face the flower and keep its edges in the frame.", .64f, .62f),
    ROOT("Exposed roots", "Photograph roots that are already visible, with the surrounding surface in view.", .70f, .55f),
    SOIL("Soil surface", "Hold the camera above the pot. Include the visible surface and a section of the rim.", .72f, .55f),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlantupiaCaptureStudio(
    plantName: String,
    onClose: () -> Unit,
    onCapture: (Uri) -> Boolean,
    reference: com.nova.app.feature.plants.data.PlantupiaCareEvent? = null,
    saveAsPortrait: Boolean = false,
    onVisitPart: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    var permission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    var view by remember { mutableStateOf(CaptureView.WHOLE) }
    var writing by remember { mutableStateOf(false) }
    var candidate by remember { mutableStateOf<Uri?>(null) }
    var rejected by remember { mutableStateOf(false) }
    // The preview owns a temporary file until the caller explicitly accepts it.
    // Activity/session disposal cannot publish or retain an abandoned candidate.
    val owned = remember(context) { AtomicReference<Uri?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked ->
        if (picked != null) {
            rejected = false
            candidate = picked
        }
    }
    fun discard() {
        owned.getAndSet(null)?.let { ownedCaptureFile(context.cacheDir, it)?.delete() }
        candidate = null
        rejected = false
    }
    DisposableEffect(owned) {
        onDispose { owned.getAndSet(null)?.let { ownedCaptureFile(context.cacheDir, it)?.delete() } }
    }
    PlantupiaLivingWorkspace("PRIVATE PHOTOGRAPH", plantName, "", writing,
        close = { if (candidate != null) discard() else onClose() },
        navigationKey = candidate != null) {
        val current = candidate
        if (current == null) {
            TextButton(enabled = !writing, onClick = { gallery.launch("image/*") }) { Text("Choose existing photo") }
        }
        if (current != null) {
            val fromCamera = owned.get() == current
            var previewReady by remember(current) { mutableStateOf(false) }
            var previewError by remember(current) { mutableStateOf(false) }
            AsyncImage(model = current, contentDescription = "Selected photograph preview",
                onSuccess = { previewReady = true; previewError = false },
                onError = { previewReady = false; previewError = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp)
                    .clip(PlantupiaShapes.card).background(PlantupiaColors.softSage),
                contentScale = ContentScale.Fit)
            Text("Keep this photograph?", style = PlantupiaText.editorialHeading)
            if (fromCamera) Text(view.title + " · " + view.guidance, style = PlantupiaText.supporting)
            if (previewError) Text("This photo could not be opened. Choose another photograph.")
            if (rejected) Text("The draft is no longer available. Return and try again.",
                color = PlantupiaColors.forestText)
            if (onVisitPart != null) {
                InspectionChoice("Photographed view", view.name.lowercase(), CaptureView.entries.map { it.name.lowercase() },
                    !writing) { name -> view = CaptureView.valueOf(name.uppercase(java.util.Locale.ROOT)) }
            }
            Button(enabled = previewReady, onClick = {
                onVisitPart?.invoke(view.name.lowercase(java.util.Locale.ROOT))
                if ((!fromCamera || ownedCaptureFile(context.cacheDir, current)?.isFile == true) &&
                    onCapture(current)) {
                    // Transfer to the private draft. The caller owns subsequent cleanup.
                    owned.compareAndSet(current, null)
                    candidate = null
                } else {
                    rejected = true
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (saveAsPortrait) "Save private portrait" else "Use photograph")
            }
            OutlinedButton(onClick = { discard() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (fromCamera) "Retake" else "Choose another photograph")
            }
            TextButton(onClick = { discard(); onClose() }) { Text("Cancel photograph") }
            Text(if (saveAsPortrait) "Saving updates this plant’s private portrait."
                else if (onVisitPart != null) "Review and save this view in the visit."
                else "This photo will be attached when you save the plant or care record.", style = PlantupiaText.supporting)
        } else if (!permission) {
            Text("Photograph your plant", style = PlantupiaText.editorialHeading)
            Text("Camera access is needed for a new photograph. You can still choose an existing photo instead.",
                style = PlantupiaText.body)
            Button(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                onClick = { requestPermission.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
        } else {
            var compare by remember(reference?.id) { mutableStateOf(false) }
            if (reference != null && reference.photoUrl.isNotBlank()) {
                TextButton(onClick = { compare = !compare }) {
                    Text(if (compare) "Hide earlier view" else "Match the view from " + reference.observedAt.take(10))
                }
            }
            var showFraming by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(enabled = !writing, onClick = { showFraming = true }) {
                    Text("Framing · " + view.title)
                }
                androidx.compose.material3.DropdownMenu(expanded = showFraming, onDismissRequest = { showFraming = false }) {
                    CaptureView.entries.forEach { option ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text(option.title) }, onClick = {
                            view = option
                            showFraming = false
                        })
                    }
                }
            }
            PlantupiaCameraPreview(view, reference?.photoUrl.takeIf { compare }, writing, { writing = it }) { photo ->
                if (ownedCaptureFile(context.cacheDir, photo)?.isFile != true) false
                else {
                    owned.getAndSet(photo)?.let { previous ->
                        if (previous != photo) ownedCaptureFile(context.cacheDir, previous)?.delete()
                    }
                    rejected = false
                    candidate = photo
                    true
                }
            }
        }
    }
}

@Composable
private fun PlantupiaCameraPreview(
    view: CaptureView,
    referenceUrl: String?,
    writing: Boolean,
    onWriting: (Boolean) -> Unit,
    onCapture: (Uri) -> Boolean,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val latestCapture = rememberUpdatedState(onCapture)
    val latestWriting = rememberUpdatedState(onWriting)
    val main = remember(context) { ContextCompat.getMainExecutor(context) }
    val controller = remember(context) { LifecycleCameraController(context.applicationContext) }
    val preview = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var ready by remember { mutableStateOf(false) }
    var quality by remember { mutableStateOf<PlantupiaCaptureQuality?>(null) }
    var error by remember { mutableStateOf("") }
    val active = remember(owner, controller) { AtomicBoolean(false) }
    val canPreviewOutline = BuildConfig.PLANTUPIA_LIVE_LENS_QA && view == CaptureView.LEAF && referenceUrl == null
    var outlineEnabled by remember(view, referenceUrl) { mutableStateOf(false) }
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    var liveReading by remember { mutableStateOf<LiveLensReading?>(null) }
    var selection by remember(view, referenceUrl) { mutableStateOf(0) }
    val lensAnalyzer = remember(active, main) {
        PlantupiaLiveLensAnalyzer(BuildConfig.PLANTUPIA_LIVE_LENS_QA, active, main,
            onQuality = { quality = it }, onLens = { liveReading = it })
    }
    val liveEnabled = canPreviewOutline && outlineEnabled && ready && !writing
    SideEffect { lensAnalyzer.configure(liveEnabled, previewSize.width, previewSize.height, selection) }
    LaunchedEffect(liveReading) {
        val reading = liveReading?.takeIf { it.region != null } ?: return@LaunchedEffect
        delay((550L - (SystemClock.elapsedRealtime() - reading.timestamp)).coerceAtLeast(0L))
        if (liveReading === reading) liveReading = reading.copy(phase = LiveLensPhase.LOST, region = null)
    }
    DisposableEffect(owner, controller, preview, lensAnalyzer) {
        active.set(true)
        val analysis = Executors.newSingleThreadExecutor()
        val stream = Observer<PreviewView.StreamState> {
            ready = it == PreviewView.StreamState.STREAMING
            if (!ready) {
                lensAnalyzer.configure(false, 0, 0)
                liveReading = null
                quality = null
            }
        }
        preview.previewStreamState.observe(owner, stream)
        try {
            controller.setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
            controller.setImageAnalysisBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            controller.setImageAnalysisAnalyzer(analysis, lensAnalyzer)
            preview.controller = controller
            controller.bindToLifecycle(owner)
            val initialized = controller.initializationFuture
            initialized.addListener({
                if (active.get() && runCatching { initialized.get() }.isFailure) {
                    error = "A compatible rear camera is unavailable. Choose an existing photo."
                }
            }, main)
        } catch (_: IllegalStateException) {
            error = "Camera preview could not start. Choose an existing photo."
        } catch (_: IllegalArgumentException) {
            error = "This camera cannot run the preview and inspection together. Choose an existing photo."
        } catch (_: SecurityException) {
            error = "Camera permission is unavailable. Choose an existing photo."
        }
        onDispose {
            active.set(false)
            lensAnalyzer.configure(false, 0, 0)
            preview.previewStreamState.removeObserver(stream)
            preview.controller = null
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            analysis.shutdown()
        }
    }
    if (canPreviewOutline) {
        FilterChip(selected = outlineEnabled, onClick = {
            outlineEnabled = !outlineEnabled
            if (outlineEnabled) { liveReading = null; selection++ }
        },
            label = { Text("Live outline preview") }, enabled = !writing)
        if (outlineEnabled) {
            Text("Centre one leaf against a contrasting background, then move slowly. Local tracking guide only.",
                style = PlantupiaText.supporting)
            TextButton(enabled = liveEnabled, onClick = { liveReading = null; selection++ }) { Text("Select another region") }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height((maxWidth * 1.02f).coerceIn(230.dp, 420.dp))
            .clip(PlantupiaShapes.card).background(Color(0xFF192820)).onSizeChanged { previewSize = it }) {
            AndroidView(factory = { preview }, modifier = Modifier.fillMaxSize())
            if (referenceUrl != null) AsyncImage(
                model = referenceUrl, contentDescription = "Earlier plant photograph over live preview",
                contentScale = ContentScale.Fit, alpha = .38f, modifier = Modifier.fillMaxSize(),
            )
            Canvas(Modifier.fillMaxSize()) {
                val width = size.width * view.frameWidth
                val height = size.height * view.frameHeight
                val corner = CornerRadius(18.dp.toPx())
                val start = Offset((size.width - width) / 2f, (size.height - height) / 2f)
                val shade = Color.Black.copy(alpha = .36f)
                drawRect(shade, Offset.Zero, Size(size.width, start.y))
                drawRect(shade, Offset(0f, start.y + height), Size(size.width, start.y))
                drawRect(shade, Offset(0f, start.y), Size(start.x, height))
                drawRect(shade, Offset(start.x + width, start.y), Size(start.x, height))
                drawRoundRect(PlantupiaColors.cream, start, Size(width, height), corner,
                    style = Stroke(1.5.dp.toPx()))
                val tick = 12.dp.toPx()
                val mid = size.width / 2f
                drawLine(PlantupiaColors.cream, Offset(mid - tick, start.y), Offset(mid + tick, start.y), 2.dp.toPx())
                drawLine(PlantupiaColors.cream, Offset(mid - tick, start.y + height),
                    Offset(mid + tick, start.y + height), 2.dp.toPx())
                if (liveEnabled) {
                    val region = liveReading?.region
                    if (region == null) {
                        drawCircle(PlantupiaColors.cream, 5.dp.toPx(), center = center,
                            style = Stroke(1.5.dp.toPx()))
                    } else if (previewSize.width == size.width.toInt() && previewSize.height == size.height.toInt()) {
                        val dx = size.width / region.width
                        val dy = size.height / region.height
                        val fill = Path()
                        val edge = Path()
                        fun segment(x1: Float, y1: Float, x2: Float, y2: Float) {
                            edge.moveTo(x1, y1)
                            edge.lineTo(x2, y2)
                        }
                        for (y in 0 until region.height) {
                            var run = -1
                            for (x in 0..region.width) {
                                val present = region.contains(x, y)
                                if (present && run == -1) run = x
                                if (!present && run != -1) {
                                    fill.addRect(Rect(run * dx, y * dy, x * dx, (y + 1) * dy))
                                    run = -1
                                }
                                if (!present) continue
                                val left = x * dx
                                val top = y * dy
                                val right = left + dx
                                val bottom = top + dy
                                if (!region.contains(x - 1, y)) segment(left, top, left, bottom)
                                if (!region.contains(x + 1, y)) segment(right, top, right, bottom)
                                if (!region.contains(x, y - 1)) segment(left, top, right, top)
                                if (!region.contains(x, y + 1)) segment(left, bottom, right, bottom)
                            }
                        }
                        val ink = Color(0xFFCDF2A4)
                        drawPath(fill, ink.copy(alpha = .12f))
                        drawPath(edge, Color(0xAA14281C), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(edge, ink, style = Stroke(1.7.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
    val guidance = when {
        error.isNotBlank() -> error
        !ready -> "Opening camera…"
        quality == PlantupiaCaptureQuality.DARK -> "Try brighter, even light."
        quality == PlantupiaCaptureQuality.GLARE -> "Change your angle to reduce glare."
        quality == PlantupiaCaptureQuality.LOW_DETAIL -> "Hold still and tap the subject to focus."
        else -> "Keep the subject inside the guide."
    }
    Text(guidance, style = PlantupiaText.itemTitle)
    if (liveEnabled) Text(when (liveReading?.phase) {
        LiveLensPhase.TRACKING -> "Following the selected region · preview only"
        LiveLensPhase.LOST -> "Track lost · centre the part and select again"
        else -> "Finding a distinct centred region…"
    }, style = PlantupiaText.supporting)
    Surface(
        enabled = ready && !writing,
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        color = if (ready && !writing) PlantupiaColors.forest else PlantupiaColors.softSage,
        shape = PlantupiaShapes.card,
        onClick = {
            val file = runCatching {
                val directory = File(context.cacheDir, "plantupia-captures").apply { mkdirs() }
                val expiry = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                directory.listFiles()?.filter { it.isFile && it.lastModified() < expiry }?.forEach { it.delete() }
                File.createTempFile("care-", ".jpg", directory)
            }.getOrNull()
            if (file == null) {
                error = "There is no space available for this photograph."
                return@Surface
            }
            error = ""
            latestWriting.value(true)
            try {
                controller.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), main,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            if (!active.get()) {
                                file.delete()
                                return
                            }
                            latestWriting.value(false)
                            if (file.length() in 1L..(10L * 1024 * 1024) && latestCapture.value(Uri.fromFile(file))) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                file.delete()
                                error = "This photograph couldn't be kept. Move back slightly and try again."
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            file.delete()
                            if (active.get()) {
                                latestWriting.value(false)
                                error = "The camera couldn't take a photo. Try again or choose an existing one."
                            }
                        }
                    },
                )
            } catch (_: IllegalStateException) {
                file.delete()
                latestWriting.value(false)
                error = "The camera is no longer ready. Reopen the preview to try again."
            }
        },
    ) {
        val shutterInk = if (ready && !writing) PlantupiaColors.paper else PlantupiaColors.secondaryText
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Canvas(Modifier.size(42.dp)) {
                drawCircle(shutterInk, radius = size.minDimension / 2f - 2.dp.toPx(),
                    style = Stroke(2.dp.toPx()))
                drawCircle(shutterInk, radius = size.minDimension / 2f - 8.dp.toPx())
            }
            Text(if (writing) "Keeping photograph…" else "Take photograph",
                style = PlantupiaText.itemTitle.copy(color = shutterInk), modifier = Modifier.weight(1f))
        }
    }
    Text(view.guidance, style = PlantupiaText.supporting)
    Text("Review the photograph before keeping it.", style = PlantupiaText.supporting)
}
