package com.nova.app.core.calls

import android.content.Context
import android.net.Uri
import android.telecom.DisconnectCause
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


object NovaTelecomRegistration {
    fun register(context: Context) {
        runCatching {
            CallsManager(context.applicationContext).registerAppWithTelecom(
                CallsManager.CAPABILITY_BASELINE or CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING
            )
        }
    }
}


data class NovaAudioRouteState(
    val name: String = "Phone",
    val type: Int = CallEndpointCompat.TYPE_UNKNOWN,
    val speakerAvailable: Boolean = false,
    val canToggleSpeaker: Boolean = false,
) {
    val speakerEnabled: Boolean
        get() = type == CallEndpointCompat.TYPE_SPEAKER
}


object NovaCallAudioRouter {
    private val mutableState = MutableStateFlow(NovaAudioRouteState())
    val state = mutableState.asStateFlow()

    @Volatile
    private var activeBridge: NovaTelecomBridge? = null

    internal fun attach(bridge: NovaTelecomBridge) {
        activeBridge = bridge
    }

    internal fun detach(bridge: NovaTelecomBridge) {
        if (activeBridge === bridge) {
            activeBridge = null
            mutableState.value = NovaAudioRouteState()
        }
    }

    internal fun publish(state: NovaAudioRouteState) {
        mutableState.value = state
    }

    suspend fun toggleSpeaker(): Boolean = activeBridge?.toggleSpeaker() == true
}


class NovaTelecomBridge(
    context: Context,
) {
    private val callsManager = CallsManager(context.applicationContext)
    private var callControlScope: CallControlScope? = null
    private var addCallJob: Job? = null
    private var callType: Int = CallAttributesCompat.CALL_TYPE_AUDIO_CALL
    private var currentEndpoint: CallEndpointCompat? = null
    private var previousNonSpeakerEndpoint: CallEndpointCompat? = null
    private var availableAudioEndpoints: List<CallEndpointCompat> = emptyList()

    fun start(
        scope: CoroutineScope,
        call: NovaCallSession,
        onSystemAnswer: suspend () -> Unit,
        onSystemDisconnect: suspend () -> Unit,
        onSystemSetActive: suspend () -> Unit,
        onSystemSetInactive: suspend () -> Unit,
        onReady: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    ) {
        if (addCallJob?.isActive == true) return
        callType = if (call.kind == NovaCallKind.Video) {
            CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        } else {
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL
        }
        val direction = if (call.isCaller) {
            CallAttributesCompat.DIRECTION_OUTGOING
        } else {
            CallAttributesCompat.DIRECTION_INCOMING
        }
        val attributes = CallAttributesCompat(
            displayName = call.peer.displayName,
            address = Uri.parse("sip:${call.peer.username}@nova"),
            direction = direction,
            callType = callType,
            callCapabilities = CallAttributesCompat.SUPPORTS_SET_INACTIVE,
        )

        NovaCallAudioRouter.attach(this)
        addCallJob = scope.launch {
            var telecomReady = false
            try {
                callsManager.addCall(
                    callAttributes = attributes,
                    onAnswer = {
                        if (telecomReady) onSystemAnswer()
                    },
                    onDisconnect = {
                        // A few OEM Telecom stacks can emit disconnect while the
                        // call is still being registered. Treat it as user/system
                        // hangup only after addCall has actually handed us control.
                        if (telecomReady) onSystemDisconnect()
                    },
                    onSetActive = {
                        if (telecomReady) onSystemSetActive()
                    },
                    onSetInactive = {
                        if (telecomReady) onSystemSetInactive()
                    },
                ) {
                    callControlScope = this
                    telecomReady = true
                    launch {
                        currentCallEndpoint.collect { endpoint ->
                            currentEndpoint = endpoint
                            if (endpoint.type != CallEndpointCompat.TYPE_SPEAKER) {
                                previousNonSpeakerEndpoint = endpoint
                            }
                            publishAudioRoute()
                        }
                    }
                    launch {
                        availableEndpoints.collect { endpoints ->
                            availableAudioEndpoints = endpoints
                            publishAudioRoute()
                        }
                    }
                    onReady()
                }
            } catch (error: Throwable) {
                onFailure(error)
            } finally {
                telecomReady = false
                callControlScope = null
                NovaCallAudioRouter.detach(this@NovaTelecomBridge)
            }
        }
    }

    suspend fun answer() {
        callControlScope?.answer(callType)
    }

    suspend fun setActive() {
        callControlScope?.setActive()
    }

    suspend fun setInactive() {
        callControlScope?.setInactive()
    }

    suspend fun disconnect(reason: Int = DisconnectCause.LOCAL) {
        callControlScope?.disconnect(DisconnectCause(reason))
    }

    internal suspend fun toggleSpeaker(): Boolean {
        val control = callControlScope ?: return false
        val endpoints = availableAudioEndpoints
        val current = currentEndpoint
        val target = if (current?.type == CallEndpointCompat.TYPE_SPEAKER) {
            previousNonSpeakerEndpoint
                ?.takeIf { previous -> endpoints.any { it.identifier == previous.identifier } }
                ?: endpoints.firstOrNull { it.type == CallEndpointCompat.TYPE_EARPIECE }
                ?: endpoints.firstOrNull { it.type == CallEndpointCompat.TYPE_WIRED_HEADSET }
                ?: endpoints.firstOrNull { it.type == CallEndpointCompat.TYPE_BLUETOOTH }
        } else {
            endpoints.firstOrNull { it.type == CallEndpointCompat.TYPE_SPEAKER }
        } ?: return false

        return control.requestEndpointChange(target) is CallControlResult.Success
    }

    fun release() {
        addCallJob?.cancel()
        addCallJob = null
        callControlScope = null
        NovaCallAudioRouter.detach(this)
    }

    private fun publishAudioRoute() {
        val current = currentEndpoint
        val hasSpeaker = availableAudioEndpoints.any { it.type == CallEndpointCompat.TYPE_SPEAKER }
        val hasNonSpeaker = availableAudioEndpoints.any { it.type != CallEndpointCompat.TYPE_SPEAKER }
        NovaCallAudioRouter.publish(
            NovaAudioRouteState(
                name = current?.name?.toString().orEmpty().ifBlank { "Phone" },
                type = current?.type ?: CallEndpointCompat.TYPE_UNKNOWN,
                speakerAvailable = hasSpeaker,
                canToggleSpeaker = hasSpeaker && hasNonSpeaker,
            )
        )
    }
}
