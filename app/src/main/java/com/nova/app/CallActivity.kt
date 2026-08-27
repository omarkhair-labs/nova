package com.nova.app

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nova.app.app.appContainer
import com.nova.app.core.calls.NovaAudioRouteState
import com.nova.app.core.calls.NovaCallAudioRouter
import com.nova.app.feature.calls.CallLaunchSpec
import com.nova.app.feature.calls.CallPhase
import com.nova.app.feature.calls.CallStateOwner
import com.nova.app.feature.calls.CallUiState
import com.nova.app.feature.calls.domain.model.NovaCallKind
import com.nova.app.feature.calls.domain.model.NovaCallPerson
import com.nova.app.ui.components.NovaMediaImage
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
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
    private lateinit var controller: CallStateOwner
    private var currentUiState: CallUiState? = null
    private var pictureInPictureMode by mutableStateOf(false)

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
        configureIncomingCallWindow()

        val spec = parseLaunchSpec(intent) ?: run {
            finish()
            return
        }
        val calls = applicationContext.appContainer
        controller = CallStateOwner(
            context = applicationContext,
            scope = lifecycleScope,
            launchSpec = spec,
            repository = calls.callRepository,
            signalingFactory = calls::callSignaling,
            webRtcFactory = calls::callWebRtcEngine,
            requestPermissions = ::requestCallPermissions,
            onFinished = {
                if (!isFinishing) finish()
            },
            onSessionExpired = {
                if (!isFinishing) finish()
            },
        )

        setContent {
            NovaTheme {
                val state by controller.state.collectAsState()
                val audioRoute by NovaCallAudioRouter.state.collectAsState()
                SideEffect {
                    currentUiState = state
                }
                CallScreen(
                    state = state,
                    audioRoute = audioRoute,
                    isPictureInPicture = pictureInPictureMode,
                    onAnswer = controller::accept,
                    onDecline = controller::decline,
                    onHangUp = controller::hangUp,
                    onToggleMicrophone = controller::toggleMicrophone,
                    onToggleCamera = controller::toggleCamera,
                    onSwitchCamera = controller::switchCamera,
                    onToggleSpeaker = {
                        lifecycleScope.launch { NovaCallAudioRouter.toggleSpeaker() }
                    },
                    onMinimize = { minimizeCall(state) },
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        currentUiState?.let(::enterVideoPictureInPicture)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pictureInPictureMode = isInPictureInPictureMode
    }

    override fun onDestroy() {
        currentUiState = null
        if (::controller.isInitialized) controller.release()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun configureIncomingCallWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    private fun minimizeCall(state: CallUiState) {
        if (!enterVideoPictureInPicture(state)) {
            moveTaskToBack(true)
        }
    }

    private fun enterVideoPictureInPicture(state: CallUiState): Boolean {
        if (
            pictureInPictureMode ||
            state.kind != NovaCallKind.Video ||
            !state.connected ||
            state.isTerminal ||
            state.remoteVideoTrack == null ||
            state.eglContext == null
        ) {
            return false
        }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(9, 16))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setSeamlessResizeEnabled(true)
        }
        val params = builder.build()
        return runCatching {
            setPictureInPictureParams(params)
            enterPictureInPictureMode(params)
        }.getOrDefault(false)
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

    private fun parseLaunchSpec(intent: Intent): CallLaunchSpec? {
        val mode = intent.getStringExtra(EXTRA_MODE).orEmpty()
        if (mode == MODE_OUTGOING) {
            val conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1L)
            if (conversationId <= 0L) return null
            return CallLaunchSpec.Outgoing(
                conversationId = conversationId,
                kind = NovaCallKind.fromWire(intent.getStringExtra(EXTRA_CALL_KIND).orEmpty()),
                peer = peerFromIntent(intent),
            )
        }

        val callId = intent.getStringExtra(EXTRA_CALL_ID).orEmpty()
        if (callId.isBlank()) return null
        return CallLaunchSpec.Existing(
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
            ACTION_ANSWER_CALL -> CallStateOwner.ACTION_ANSWER
            ACTION_DECLINE_CALL -> CallStateOwner.ACTION_DECLINE
            ACTION_END_CALL -> CallStateOwner.ACTION_END
            else -> CallStateOwner.ACTION_OPEN
        }
    }
}


@Composable
private fun CallScreen(
    state: CallUiState,
    audioRoute: NovaAudioRouteState,
    isPictureInPicture: Boolean,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onHangUp: () -> Unit,
    onToggleMicrophone: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onMinimize: () -> Unit,
) {
    BackHandler(enabled = !state.isTerminal && !isPictureInPicture, onBack = onMinimize)
    val peer = state.peer
    val isVideo = state.kind == NovaCallKind.Video
    val videoLive = isVideo && state.remoteVideoTrack != null && state.eglContext != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isVideo) Color(0xFF090A0C) else NovaBackground),
    ) {
        if (videoLive) {
            WebRtcVideo(
                track = state.remoteVideoTrack!!,
                eglContext = state.eglContext!!,
                mirror = false,
                overlay = false,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CallIdentity(
                peer = peer,
                stage = state.stage,
                phase = state.phase,
                isVideo = isVideo,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = if (isVideo) 118.dp else 104.dp, start = 28.dp, end = 28.dp),
            )
        }

        if (
            !isPictureInPicture &&
            isVideo &&
            state.localVideoTrack != null &&
            state.eglContext != null &&
            state.cameraEnabled
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 66.dp, end = 16.dp)
                    .size(width = 108.dp, height = 158.dp),
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

        if (!isPictureInPicture) {
            Surface(
                onClick = onMinimize,
                shape = CircleShape,
                color = if (isVideo) Color.Black.copy(alpha = 0.38f) else NovaSurface,
                border = if (isVideo) null else BorderStroke(1.dp, NovaBorder),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NovaIcon(
                        asset = NovaIconAsset.Back,
                        contentDescription = "Minimize call",
                        tint = if (isVideo) Color.White else NovaInk,
                        modifier = Modifier.size(23.dp).rotate(-90f),
                    )
                }
            }
        }

        if (!isPictureInPicture && videoLive) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 18.dp, start = 72.dp, end = 72.dp),
            ) {
                Text(
                    text = peer?.displayName ?: "Nova call",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = state.stage,
                    color = if (state.phase == CallPhase.Reconnecting) {
                        Color(0xFFB8AEFF)
                    } else {
                        Color.White.copy(alpha = 0.72f)
                    },
                    fontSize = 11.sp,
                    fontWeight = if (state.phase == CallPhase.Reconnecting) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }

        if (!isPictureInPicture) {
            state.error?.takeIf { it.isNotBlank() }?.let { message ->
                Surface(
                    color = if (isVideo) Color.Black.copy(alpha = 0.68f) else NovaSurface,
                    shape = RoundedCornerShape(15.dp),
                    border = if (isVideo) null else BorderStroke(1.dp, NovaBorder),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = if (videoLive) 78.dp else 60.dp, start = 24.dp, end = 24.dp),
                ) {
                    Text(
                        text = message,
                        color = if (isVideo) Color.White else NovaMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }

        if (!isPictureInPicture && state.permissionsPending) {
            CircularProgressIndicator(
                color = NovaAccent,
                strokeWidth = 2.5.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp),
            )
        }

        if (!isPictureInPicture) {
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
                    .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
            )
        }
    }
}


@Composable
private fun CallIdentity(
    peer: NovaCallPerson?,
    stage: String,
    phase: CallPhase,
    isVideo: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = if (isVideo) "NOVA VIDEO" else "NOVA VOICE",
            color = if (isVideo) Color.White.copy(alpha = 0.56f) else NovaMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            shape = CircleShape,
            color = if (isVideo) Color.White.copy(alpha = 0.10f) else NovaAccentSoft,
            modifier = Modifier.size(146.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (peer?.avatarUrl?.isNotBlank() == true) {
                    NovaMediaImage(
                        source = peer.avatarUrl,
                        contentDescription = peer.displayName,
                        modifier = Modifier
                            .size(126.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(126.dp)
                            .clip(CircleShape)
                            .background(if (isVideo) Color.White.copy(alpha = 0.08f) else NovaSurface),
                    ) {
                        Text(
                            text = peer?.displayName?.firstOrNull()?.uppercase() ?: "N",
                            color = if (isVideo) Color.White else NovaAccent,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = peer?.displayName ?: "Nova call",
            color = if (isVideo) Color.White else NovaInk,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        peer?.username?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "@$it",
                color = if (isVideo) Color.White.copy(alpha = 0.58f) else NovaMuted,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(15.dp))
        val reconnecting = phase == CallPhase.Reconnecting
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = when {
                isVideo && reconnecting -> NovaAccent.copy(alpha = 0.28f)
                isVideo -> Color.White.copy(alpha = 0.10f)
                reconnecting -> NovaAccentSoft
                else -> NovaSurface
            },
            border = if (isVideo) null else BorderStroke(1.dp, if (reconnecting) NovaAccent else NovaBorder),
        ) {
            Text(
                text = stage,
                color = when {
                    isVideo -> Color.White.copy(alpha = 0.88f)
                    reconnecting -> NovaAccent
                    else -> NovaMuted
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}


@Composable
private fun CallControls(
    state: CallUiState,
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
    Surface(
        color = if (dark) Color.Black.copy(alpha = 0.58f) else NovaSurface,
        shape = RoundedCornerShape(28.dp),
        border = if (dark) null else BorderStroke(1.dp, NovaBorder),
        shadowElevation = if (dark) 0.dp else 5.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        when {
            state.isIncomingRinging -> {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp, vertical = 18.dp),
                ) {
                    RoundCallButton(
                        icon = NovaIconAsset.CallEnd,
                        label = "Decline",
                        background = Color(0xFFE2444E),
                        iconColor = Color.White,
                        labelColor = if (dark) Color.White.copy(alpha = 0.82f) else NovaMuted,
                        onClick = onDecline,
                    )
                    RoundCallButton(
                        icon = NovaIconAsset.CallAudio,
                        label = "Answer",
                        background = Color(0xFF2EAE68),
                        iconColor = Color.White,
                        labelColor = if (dark) Color.White.copy(alpha = 0.82f) else NovaMuted,
                        onClick = onAnswer,
                    )
                }
            }

            state.kind == NovaCallKind.Video -> {
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
                            icon = NovaIconAsset.Microphone,
                            label = if (state.microphoneEnabled) "Mute" else "Unmute",
                            background = if (state.microphoneEnabled) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.28f),
                            iconColor = Color.White,
                            labelColor = Color.White.copy(alpha = 0.82f),
                            onClick = onToggleMicrophone,
                        )
                        if (audioRoute.canToggleSpeaker) {
                            RoundCallButton(
                                icon = NovaIconAsset.VolumeOn,
                                label = if (audioRoute.speakerEnabled) "Speaker" else audioRoute.name.take(9),
                                background = if (audioRoute.speakerEnabled) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.14f),
                                iconColor = Color.White,
                                labelColor = Color.White.copy(alpha = 0.82f),
                                onClick = onToggleSpeaker,
                            )
                        }
                        RoundCallButton(
                            icon = NovaIconAsset.CallVideo,
                            label = if (state.cameraEnabled) "Camera" else "Camera off",
                            background = if (state.cameraEnabled) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.28f),
                            iconColor = Color.White,
                            labelColor = Color.White.copy(alpha = 0.82f),
                            onClick = onToggleCamera,
                        )
                        RoundCallButton(
                            icon = NovaIconAsset.Refresh,
                            label = "Flip",
                            background = Color.White.copy(alpha = 0.14f),
                            iconColor = Color.White,
                            labelColor = Color.White.copy(alpha = 0.82f),
                            onClick = onSwitchCamera,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    RoundCallButton(
                        icon = NovaIconAsset.CallEnd,
                        label = if (state.session?.status?.wireValue == "ringing") "Cancel" else "End",
                        background = Color(0xFFE2444E),
                        iconColor = Color.White,
                        labelColor = Color.White.copy(alpha = 0.82f),
                        onClick = onHangUp,
                    )
                }
            }

            else -> {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 17.dp),
                ) {
                    RoundCallButton(
                        icon = NovaIconAsset.Microphone,
                        label = if (state.microphoneEnabled) "Mute" else "Unmute",
                        background = if (state.microphoneEnabled) NovaBackground else NovaAccentSoft,
                        iconColor = if (state.microphoneEnabled) NovaInk else NovaAccent,
                        labelColor = NovaMuted,
                        borderColor = NovaBorder,
                        onClick = onToggleMicrophone,
                    )
                    if (audioRoute.canToggleSpeaker) {
                        RoundCallButton(
                            icon = NovaIconAsset.VolumeOn,
                            label = if (audioRoute.speakerEnabled) "Speaker" else audioRoute.name.take(9),
                            background = if (audioRoute.speakerEnabled) NovaAccentSoft else NovaBackground,
                            iconColor = if (audioRoute.speakerEnabled) NovaAccent else NovaInk,
                            labelColor = NovaMuted,
                            borderColor = NovaBorder,
                            onClick = onToggleSpeaker,
                        )
                    }
                    RoundCallButton(
                        icon = NovaIconAsset.CallEnd,
                        label = if (state.session?.status?.wireValue == "ringing") "Cancel" else "End",
                        background = Color(0xFFE2444E),
                        iconColor = Color.White,
                        labelColor = NovaMuted,
                        onClick = onHangUp,
                    )
                }
            }
        }
    }
}


@Composable
private fun RoundCallButton(
    icon: NovaIconAsset,
    label: String,
    background: Color,
    iconColor: Color,
    labelColor: Color,
    onClick: () -> Unit,
    borderColor: Color? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = background,
            border = borderColor?.let { BorderStroke(1.dp, it) },
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                NovaIcon(
                    asset = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(23.dp),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
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
