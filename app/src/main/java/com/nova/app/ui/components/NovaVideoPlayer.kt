package com.nova.app.ui.components

import android.graphics.Color
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nova.app.R
import com.nova.app.ui.theme.NovaMuted


@Composable
fun NovaVideoPlayer(
    source: String,
    modifier: Modifier = Modifier,
    thumbnailSource: String = "",
    autoplay: Boolean = false,
    repeat: Boolean = false,
    muted: Boolean = false,
    useController: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    description: String = "Video",
) {
    val context = LocalContext.current
    val player = remember(source) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                !muted,
            )
            repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = if (muted) 0f else 1f
            setMediaItem(MediaItem.fromUri(source))
            prepare()
            playWhenReady = autoplay
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    NovaPlayerSurface(
        player = player,
        modifier = modifier,
        thumbnailSource = thumbnailSource,
        useController = useController,
        resizeMode = resizeMode,
        description = description,
    )
}


@Composable
fun NovaPlayerSurface(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    thumbnailSource: String = "",
    useController: Boolean = false,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    description: String = "Video",
) {
    var firstFrameRendered by remember(player) { mutableStateOf(player.currentPosition > 0L) }
    var playbackError by remember(player) { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
                playbackError = false
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    Box(
        modifier = modifier
            .background(ComposeColor.Black)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailSource.isNotBlank() && !firstFrameRendered) {
            NovaMediaImage(
                source = thumbnailSource,
                modifier = Modifier.fillMaxSize(),
                contentDescription = description,
                contentScale = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                    ContentScale.Fit
                } else {
                    ContentScale.Crop
                },
            )
        }

        AndroidView(
            factory = { viewContext ->
                (LayoutInflater.from(viewContext)
                    .inflate(R.layout.nova_video_player, null, false) as PlayerView).apply {
                    setShutterBackgroundColor(Color.TRANSPARENT)
                    setKeepContentOnPlayerReset(true)
                    this.player = player
                }
            },
            update = { view ->
                view.player = player
                view.useController = useController
                view.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (firstFrameRendered || thumbnailSource.isBlank()) 1f else 0.01f),
        )

        if (playbackError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Video couldn't play.",
                    color = NovaMuted,
                    fontSize = 12.sp,
                )
                Surface(
                    onClick = {
                        playbackError = false
                        firstFrameRendered = false
                        player.prepare()
                        player.play()
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    color = ComposeColor.White.copy(alpha = 0.14f),
                    shape = androidx.compose.material3.MaterialTheme.shapes.small,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "Try again", color = ComposeColor.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
