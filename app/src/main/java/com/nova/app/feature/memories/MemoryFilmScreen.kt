package com.nova.app.feature.memories

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nova.app.app.appContainer
import com.nova.app.feature.memories.domain.model.MemoryFilmPlan
import com.nova.app.feature.memories.domain.model.MemoryFilmScene
import com.nova.app.feature.memories.film.MemoryFilmWorker
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import java.io.File
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext


@Composable
fun MemoryFilmScreen(
    initialWeeksAgo: Int = 0,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = context.appContainer.memoryRepository
    val exporter = context.appContainer.memoryFilmExporter
    val scope = rememberCoroutineScope()
    val owner = remember(repository, exporter, scope) {
        MemoryFilmStateOwner(repository, exporter, scope)
    }
    val state = owner.state
    val workManager = remember(context) { WorkManager.getInstance(context.applicationContext) }
    var activeWorkId by remember { mutableStateOf<UUID?>(null) }

    BackHandler(onBack = onBack)
    LaunchedEffect(owner, initialWeeksAgo) {
        owner.loadPlan(
            utcOffsetMinutes = filmUtcOffsetMinutes(),
            weeksAgo = initialWeeksAgo,
            showSpinner = true,
        )
    }
    LaunchedEffect(state.sessionExpiryVersion) {
        if (state.sessionExpiryVersion > 0) onSessionExpired()
    }
    LaunchedEffect(state.plan?.selectionVersion) {
        val plan = state.plan ?: return@LaunchedEffect
        val existing = withContext(Dispatchers.IO) {
            workManager.getWorkInfosForUniqueWork(MemoryFilmWorker.uniqueName(plan)).get()
                .firstOrNull { it.state != WorkInfo.State.CANCELLED }
        }
        activeWorkId = existing?.id
    }
    LaunchedEffect(activeWorkId) {
        val workId = activeWorkId ?: return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            val info = withContext(Dispatchers.IO) { workManager.getWorkInfoById(workId).get() } ?: break
            when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED ->
                    owner.acceptBackgroundWork(
                        running = true,
                        progress = info.progress.getInt(MemoryFilmWorker.KEY_PROGRESS, 0),
                    )
                WorkInfo.State.SUCCEEDED -> owner.acceptBackgroundWork(
                    running = false,
                    progress = 100,
                    outputPath = info.outputData.getString(MemoryFilmWorker.KEY_OUTPUT_PATH),
                )
                WorkInfo.State.FAILED -> owner.acceptBackgroundWork(
                    running = false,
                    progress = 0,
                    error = info.outputData.getString(MemoryFilmWorker.KEY_ERROR)
                        ?: "Nova couldn't render this film.",
                )
                WorkInfo.State.CANCELLED -> owner.acceptBackgroundWork(
                    running = false,
                    progress = 0,
                )
            }
            if (info.state.isFinished) break
            delay(1_000)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = NovaBackground) {
        when {
            state.loadingPlan && state.plan == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = NovaAccent)
            }

            state.plan == null -> FilmLoadError(
                message = state.error ?: "Nova couldn't plan this film.",
                onBack = onBack,
                onRetry = {
                    owner.loadPlan(
                        filmUtcOffsetMinutes(),
                        state.weeksAgo,
                        showSpinner = true,
                    )
                },
            )

            else -> FilmContent(
                plan = state.plan,
                exporting = state.exporting,
                progress = state.progress,
                outputPath = state.outputPath,
                error = state.error,
                onBack = {
                    onBack()
                },
                onRender = {
                    state.plan?.let { plan ->
                        activeWorkId = MemoryFilmWorker.enqueue(context.applicationContext, plan)
                        owner.acceptBackgroundWork(running = true, progress = 0)
                    }
                },
                onCancel = {
                    activeWorkId?.let(workManager::cancelWorkById)
                    owner.acceptBackgroundWork(running = false, progress = 0)
                },
                onShare = { path -> shareFilm(context, path) },
                onOlder = {
                    owner.loadPlan(
                        filmUtcOffsetMinutes(),
                        (state.weeksAgo + 1).coerceAtMost(51),
                        showSpinner = true,
                    )
                },
                onNewer = if (state.weeksAgo > 0) {
                    {
                        owner.loadPlan(
                            filmUtcOffsetMinutes(),
                            state.weeksAgo - 1,
                            showSpinner = true,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}


@Composable
private fun FilmContent(
    plan: MemoryFilmPlan,
    exporting: Boolean,
    progress: Int,
    outputPath: String?,
    error: String?,
    onBack: () -> Unit,
    onRender: () -> Unit,
    onCancel: () -> Unit,
    onShare: (String) -> Unit,
    onOlder: () -> Unit,
    onNewer: (() -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 14.dp,
            bottom = 36.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Text(
                        text = "‹",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        color = NovaInk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Memory Film",
                        color = NovaInk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (plan.weeksAgo == 0) "Last week · ${moodLabel(plan.mood)}" else "${plan.weeksAgo} weeks ago · ${moodLabel(plan.mood)}",
                        color = NovaMuted,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item { FilmHero(plan = plan, outputPath = outputPath) }

        if (outputPath != null) {
            item {
                RenderedFilmPreview(path = outputPath)
            }
            item {
                Surface(
                    onClick = { onShare(outputPath) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = NovaAccent,
                ) {
                    Text(
                        text = "Share your film",
                        modifier = Modifier.padding(vertical = 13.dp),
                        color = NovaBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        } else if (!plan.filmReady) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = NovaSurface,
                    border = BorderStroke(1.dp, NovaBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("☾", color = NovaAccent, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This week needs a little more media.",
                            color = NovaInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Photos or videos from Pulse, Posts or Rooms will become film scenes.",
                            color = NovaMuted,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        } else {
            item {
                Surface(
                    onClick = if (exporting) onCancel else onRender,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = if (exporting) NovaAccentSoft else NovaAccent,
                    border = if (exporting) BorderStroke(1.dp, NovaAccent.copy(alpha = 0.25f)) else null,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 13.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (exporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NovaAccent,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                        }
                        Text(
                            text = if (exporting) "Rendering $progress% · tap to cancel" else "Make my film",
                            color = if (exporting) NovaAccent else NovaBackground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        if (plan.scenes.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Nova picked ${plan.scenes.size} scenes",
                        color = NovaInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${plan.totalDurationMs / 1000}s · chronological · 9:16",
                        color = NovaMuted,
                        fontSize = 9.sp,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(plan.scenes, key = { "${it.source}-${it.sourceId}" }) { scene ->
                            FilmSceneCard(scene)
                        }
                    }
                }
            }
        }

        if (!error.isNullOrBlank()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = NovaAccentSoft,
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = NovaMuted,
                        fontSize = 9.sp,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onNewer != null) {
                    FilmNavButton("Newer week", onNewer, Modifier.weight(1f))
                }
                FilmNavButton("Older week", onOlder, Modifier.weight(1f))
            }
        }
    }
}


@Composable
private fun FilmHero(
    plan: MemoryFilmPlan,
    outputPath: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF0D1018),
        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.28f)),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
            if (plan.coverMediaUrl.isNotBlank() && plan.scenes.firstOrNull()?.mediaType == "image") {
                NovaMediaImage(
                    source = plan.coverMediaUrl,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "Memory Film cover",
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f)),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = if (outputPath != null) "Your film is ready." else if (plan.filmReady) "Nova found your film." else "Not enough scenes yet.",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${moodLabel(plan.mood)} · ${plan.scenes.size} scenes · ${plan.totalDurationMs / 1000}s",
                    color = Color(0xFFC0C5D2),
                    fontSize = 10.sp,
                )
            }
        }
    }
}


@Composable
private fun FilmSceneCard(scene: MemoryFilmScene) {
    Surface(
        modifier = Modifier.width(132.dp),
        shape = RoundedCornerShape(18.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column {
            if (scene.mediaType == "image") {
                NovaMediaImage(
                    source = scene.mediaUrl,
                    modifier = Modifier.fillMaxWidth().height(116.dp),
                    contentDescription = "Film scene",
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                        .background(Color(0xFF11151E)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("▶", color = Color.White, fontSize = 23.sp)
                }
            }
            Column(modifier = Modifier.padding(9.dp)) {
                Text(
                    text = scene.caption.ifBlank { scene.source.replace('_', ' ') },
                    color = NovaInk,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${scene.durationMs / 1000}s",
                    color = NovaMuted,
                    fontSize = 8.sp,
                )
            }
        }
    }
}


@Composable
private fun RenderedFilmPreview(path: String) {
    val context = LocalContext.current
    val player = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp),
    )
}


@Composable
private fun FilmNavButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 11.dp),
            color = NovaAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}


@Composable
private fun FilmLoadError(
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = NovaMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = onBack, shape = RoundedCornerShape(16.dp), color = NovaSurface) {
                Text("Back", modifier = Modifier.padding(12.dp), color = NovaInk)
            }
            Surface(onClick = onRetry, shape = RoundedCornerShape(16.dp), color = NovaAccent) {
                Text("Retry", modifier = Modifier.padding(12.dp), color = NovaBackground)
            }
        }
    }
}


private fun shareFilm(context: Context, path: String) {
    val file = File(path)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.files",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("Nova Memory Film", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share your Nova Memory Film"))
}


private fun moodLabel(mood: String): String = when (mood) {
    "after_dark" -> "After dark"
    "together" -> "Together"
    "quiet" -> "Quiet week"
    "week_in_motion" -> "Week in motion"
    else -> "Your week"
}


private fun filmUtcOffsetMinutes(): Int =
    TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
