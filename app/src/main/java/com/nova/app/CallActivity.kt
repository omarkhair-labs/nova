package com.nova.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nova.app.core.calls.NovaAudioRouteState
import com.nova.app.core.calls.NovaCallAudioRouter
import com.nova.app.core.calls.NovaCallController
import com.nova.app.core.calls.NovaCallKind
import com.nova.app.core.calls.NovaCallLaunchSpec
import com.nova.app.core.calls.NovaCallPerson
import com.nova.app.core.calls.NovaCallUiState
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaTheme
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack


class CallActivity : ComponentActivity() {
    private lateinit var controller: NovaCallController

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val audioGranted = result[Manifest.permission.RECORD_AUDIO]
            ?: hasPermission(Manifest.permission.RECORD_AUDIO)
        val cameraGranted = result[Manifest.permission.CAMERA]
            ?: hasPermission(Manifest.permission.CAMERA)
        if (::controller.isInitialized) {
            controller.onPermissionsResult(audioGranted, cameraGranted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val spec = parseLaunchSpec(intent) ?: run {
            finish()
            return
        }
        controller = NovaCallController(
            context = applicationContext,
            scope = lifecycleScope,
            launchSpec = spec,
            requestPermissions = ::requestCallPermissions,
            onFinished = {
                if (!isFinishing) finishAndRemoveTask()
            },
            onSessionExpired = {
                if (!isFinishing) finishAndRemoveTask()
            },
        )

        setContent {
            NovaTheme {
                val state by controller.state.collectAsState()
                val audioRoute by NovaCallAudioRouter.state.collectAsState()
                CallScreen(
                    state = state,
                    audioRoute = audioRoute,
                    onAnswer = controller::accept,
                    onDecline = controller::decline,
                    onHangUp = controller::hangUp,
                    onToggleMicrophone = controller::toggleMicrophone,
                    onToggleCamera = controller::toggleCamera,
                    onSwitchCamera = controller::switchCamera,
                    onToggleSpeaker = {
                        lifecycleScope.launch { NovaCallAudioRouter.toggleSpeaker() }
                    },
                    onMinimize = { moveTaskToBack(true) },
                )
            }
        }
        controller.start()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::controller.isInitialized) {
            controller.handleExternalAction(actionValue(intent.action))
        }
    }

    override fun onDestroy() {
        if (::controller.isInitialized) controller.release()
        super.onDestroy()
    }

    private fun requestCallPermissions(kind: NovaCallKind) {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (kind == NovaCallKind.Video) add(Manifest.permission.CAMERA)
        }
        val missing = permissions.filterNot(::hasPermission)
        if (missing.isEmpty()) {
            controller.onPermissionsResult(
                audioGranted = true,
                cameraGranted = kind != NovaCallKind.Video || hasPermission(Manifest.permission.CAMERA),
            )
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun parseLaunchSpec(intent: Intent): NovaCallLaunchSpec? {
        val mode = intent.getStringExtra(EXTRA_MODE).orEmpty()
        if (mode == MODE_OUTGOING) {
            val conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1L)
            if (conversationId <= 0L) return null
            return NovaCallLaunchSpec.Outgoing(
                conversationId = conversationId,
                kind = NovaCallKind.fromWire(intent.getStringExtra(EXTRA_CALL_KIND).orEmpty()),
                peer = peerFromIntent(intent),
            )
        }

        val callId = intent.getStringExtra(EXTRA_CALL_ID).orEmpty()
        if (callId.isBlank()) return null
        return NovaCallLaunchSpec.Existing(
            callId = callId,
            requestedAction = actionValue(intent.action),
            fallbackPeer = peerFromIntent(intent).takeIf { it.username.isNotBlank() || it.name.isNotBlank() },
            fallbackKind = NovaCallKind.fromWire(intent.getStringExtra(EXTRA_CALL_KIND).orEmpty()),
            fallbackConversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1L),
        )
    }

    private fun peerFromIntent(intent: Intent): NovaCallPerson = NovaCallPerson(
        id = intent.getLongExtra(EXTRA_PEER_ID, 0L),
        username = intent.getStringExtra(EXTRA_PEER_USERNAME).orEmpty(),
        name = intent.getStringExtra(EXTRA_PEER_NAME).orEmpty(),
        avatarUrl = intent.getStringExtra(EXTRA_PEER_AVATAR_URL).orEmpty(),
    )

    companion object {
        const val ACTION_OPEN_CALL = "com.nova.app.call.OPEN"
        const val ACTION_ANSWER_CALL = "com.nova.app.call.ANSWER"
        const val ACTION_DECLINE_CALL = "com.nova.app.call.DECLINE"
        const val ACTION_END_CALL = "com.nova.app.call.END"

        private const val EXTRA_MODE = "nova_call_mode"
        private const val MODE_OUTGOING = "outgoing"
        private const val MODE_EXISTING = "existing"
        private const val EXTRA_CALL_ID = "nova_call_id"
        private const val EXTRA_CONVERSATION_ID = "nova_call_conversation_id"
        private const val EXTRA_CALL_KIND = "nova_call_kind"
        private const val EXTRA_PEER_ID = "nova_call_peer_id"
        private const val EXTRA_PEER_USERNAME = "nova_call_peer_username"
        private const val EXTRA_PEER_NAME = "nova_call_peer_name"
        private const val EXTRA_PEER_AVATAR_URL = "nova_call_peer_avatar_url"

        fun outgoingIntent(
            context: Context,
            conversationId: Long,
            kind: NovaCallKind,
            peer: NovaCallPerson,
        ): Intent = Intent(context, CallActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_OUTGOING)
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
            putExtra(EXTRA_CALL_KIND, kind.wireValue)
            putPeer(peer)
            action = ACTION_OPEN_CALL
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        fun incomingIntent(
            context: Context,
            callId: String,
            conversationId: Long,
            callKind: NovaCallKind,
            callerUsername: String,
            callerName: String,
            callerAvatarUrl: String,
            action: String,
        ): Intent = Intent(context, CallActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_EXISTING)
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
            putExtra(EXTRA_CALL_KIND, callKind.wireValue)
            putExtra(EXTRA_PEER_USERNAME, callerUsername)
            putExtra(EXTRA_PEER_NAME, callerName)
            putExtra(EXTRA_PEER_AVATAR_URL, callerAvatarUrl)
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        fun existingCallIntent(context: Context, callId: String, action: String): Intent =
            Intent(context, CallActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_EXISTING)
                putExtra(EXTRA_CALL_ID, callId)
                this.action = action
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        private fun Intent.putPeer(peer: NovaCallPerson) {
            putExtra(EXTRA_PEER_ID, peer.id)
            putExtra(EXTRA_PEER_USERNAME, peer.username)
            putExtra(EXTRA_PEER_NAME, peer.name)
            putExtra(EXTRA_PEER_AVATAR_URL, peer.avatarUrl)
        }

        private fun actionValue(action: String?): String = when (action) {
            ACTION_ANSWER_CALL -> NovaCallController.ACTION_ANSWER
            ACTION_DECLINE_CALL -> NovaCallController.ACTION_DECLINE
            ACTION_END_CALL -> NovaCallController.ACTION_END
            else -> NovaCallController.ACTION_OPEN
        }
    }
}


@Composable
private fun CallScreen(
    state: NovaCallUiState,
    audioRoute: NovaAudioRouteState,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onHangUp: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onMinimize: () -> Unit,
) {
    BackHandler(enabled = !state.isTerminal, onBack = onMinimize)
    val peer = state.peer
    val isVideo = state.kind == NovaCallKind.Video

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isVideo) Color(0xFF090A0C) else NovaBackground),
    ) {
        if (isVideo && state.remoteVideoTrack != null && state.eglContext != null) {
            WebRtcVideo(
                track = state.remoteVideoTrack,
                eglContext = state.eglContext,
                mirror = false,
                overlay = false,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CallIdentity(
                peer = peer,
                stage = state.stage,
                isVideo = isVideo,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp),
            )
        }

        if (isVideo && state.localVideoTrack != null && state.eglContext != null && state.cameraEnabled) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                color = Color.Black,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 64.dp, end = 16.dp)
                    .size(width = 112.dp, height = 164.dp),
            ) {
                WebRtcVideo(
                    track = state.localVideoTrack,
                    eglContext = state.eglContext,
                    mirror = true,
                    overlay = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Surface(
            onClick = onMinimize,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.34f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Text(
                text = "⌄",
                color = Color.White,
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }

        if (isVideo && state.remoteVideoTrack != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 18.dp, start = 70.dp, end = 70.dp),
            ) {
                Text(
                    text = peer?.displayName ?: "Nova call",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = state.stage,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                )
            }
        }

        state.error?.takeIf { it.isNotBlank() }?.let { message ->
            Surface(
                color = Color.Black.copy(alpha = 0.62f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 78.dp, start = 24.dp, end = 24.dp),
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }

        if (state.permissionsPending) {
            CircularProgressIndicator(
                color = NovaAccent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 180.dp)
                    .size(28.dp),
            )
        }

        CallControls(
            state = state,
            audioRoute = audioRoute,
            onAnswer = onAnswer,
            onDecline = onDecline,
            onHangUp = onHangUp,
            onToggleMicrophone = onToggleMicrophone,
            onToggleCamera = onToggleCamera,
            onSwitchCamera = onSwitchCamera,
            onToggleSpeaker = onToggleSpeaker,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
        )
    }
}


@Composable
private fun CallIdentity(
    peer: NovaCallPerson?,
    stage: String,
    isVideo: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        if (peer?.avatarUrl?.isNotBlank() == true) {
            NovaMediaImage(
                source = peer.avatarUrl,
                contentDescription = peer.displayName,
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .background(NovaSurface),
            ) {
                Text(
                    text = peer?.displayName?.firstOrNull()?.uppercase() ?: "N",
                    color = NovaAccent,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = peer?.displayName ?: "Nova call",
            color = if (isVideo) Color.White else NovaInk,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        peer?.username?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = "@$it",
                color = if (isVideo) Color.White.copy(alpha = 0.62f) else NovaMuted,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stage,
            color = if (isVideo) Color.White.copy(alpha = 0.78f) else NovaMuted,
            fontSize = 14.sp,
        )
    }
}


@Composable
private fun CallControls(
    state: NovaCallUiState,
    audioRoute: NovaAudioRouteState,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onHangUp: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = state.kind == NovaCallKind.Video
    val background = if (dark) Color.Black.copy(alpha = 0.52f) else NovaSurface
    Surface(
        color = background,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (state.isIncomingRinging) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                RoundCallButton("✕", "Decline", Color(0xFFDD3D47), onDecline)
                RoundCallButton("✓", "Answer", Color(0xFF2BAA66), onAnswer)
            }
        } else if (state.kind == NovaCallKind.Video) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RoundCallButton(
                        icon = if (state.microphoneEnabled) "🎙" else "⊘",
                        label = if (state.microphoneEnabled) "Mute" else "Unmute",
                        background = Color.White.copy(alpha = 0.16f),
                        onClick = onToggleMicrophone,
                    )
                    if (audioRoute.canToggleSpeaker) {
                        RoundCallButton(
                            icon = if (audioRoute.speakerEnabled) "🔊" else "◖",
                            label = if (audioRoute.speakerEnabled) "Speaker" else audioRoute.name.take(10),
                            background = Color.White.copy(alpha = 0.16f),
                            onClick = onToggleSpeaker,
                        )
                    }
                    RoundCallButton(
                        icon = if (state.cameraEnabled) "▣" else "□",
                        label = if (state.cameraEnabled) "Camera" else "Camera off",
                        background = Color.White.copy(alpha = 0.16f),
                        onClick = onToggleCamera,
                    )
                    RoundCallButton(
                        icon = "↻",
                        label = "Flip",
                        background = Color.White.copy(alpha = 0.16f),
                        onClick = onSwitchCamera,
                    )
                }
                Spacer(Modifier.height(12.dp))
                RoundCallButton("✕", if (state.session?.status?.wireValue == "ringing") "Cancel" else "End", Color(0xFFDD3D47), onHangUp)
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 18.dp),
            ) {
                RoundCallButton(
                    icon = if (state.microphoneEnabled) "🎙" else "⊘",
                    label = if (state.microphoneEnabled) "Mute" else "Unmute",
                    background = NovaBackground,
                    onClick = onToggleMicrophone,
                )
                if (audioRoute.canToggleSpeaker) {
                    RoundCallButton(
                        icon = if (audioRoute.speakerEnabled) "🔊" else "◖",
                        label = if (audioRoute.speakerEnabled) "Speaker" else audioRoute.name.take(10),
                        background = NovaBackground,
                        onClick = onToggleSpeaker,
                    )
                }
                RoundCallButton(
                    "✕",
                    if (state.session?.status?.wireValue == "ringing") "Cancel" else "End",
                    Color(0xFFDD3D47),
                    onHangUp,
                )
            }
        }
    }
}


@Composable
private fun RoundCallButton(
    icon: String,
    label: String,
    background: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = background,
            border = if (background == NovaBackground) androidx.compose.foundation.BorderStroke(1.dp, NovaBorder) else null,
            modifier = Modifier.size(58.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = icon,
                    color = if (background == NovaBackground) NovaInk else Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            color = if (background == NovaBackground) NovaMuted else Color.White.copy(alpha = 0.82f),
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}


@Composable
private fun WebRtcVideo(
    track: VideoTrack,
    eglContext: EglBase.Context,
    mirror: Boolean,
    overlay: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val renderer = remember(track, eglContext) {
        SurfaceViewRenderer(context).apply {
            init(eglContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            setEnableHardwareScaler(true)
            setMirror(mirror)
            if (overlay) setZOrderMediaOverlay(true)
        }
    }

    DisposableEffect(track, renderer) {
        track.addSink(renderer)
        onDispose {
            runCatching { track.removeSink(renderer) }
            runCatching { renderer.release() }
        }
    }

    AndroidView(
        factory = { renderer },
        modifier = modifier,
    )
}
