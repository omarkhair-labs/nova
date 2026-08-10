package com.nova.app.core.calls

import android.content.Context
import android.net.Uri
import android.telecom.DisconnectCause
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import kotlinx.coroutines.CancellationException
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
                        if (telecomReady) safeCallback(onSystemAnswer)
                    },
                    onDisconnect = {
                        // Some OEM Telecom stacks can emit callbacks while a call
                        // is still being registered or while their internal state
                        // is changing. Never let an OEM callback crash Nova.
                        if (telecomReady) safeCallback(onSystemDisconnect)
                    },
                    onSetActive = {
                        if (telecomReady) safeCallback(onSystemSetActive)
                    },
                    onSetInactive = {
                        if (telecomReady) safeCallback(onSystemSetInactive)
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
                    runCatching(onReady)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                onFailure(error)
            } finally {
                telecomReady = false
                callControlScope = null
                NovaCallAudioRouter.detach(this@NovaTelecomBridge)
            }
        }
    }

    suspend fun answer(): Boolean = safeControlAction { answer(callType) }

    suspend fun setActive(): Boolean = safeControlAction { setActive() }

    suspend fun setInactive(): Boolean = safeControlAction { setInactive() }

    suspend fun disconnect(reason: Int = DisconnectCause.LOCAL): Boolean =
        safeControlAction { disconnect(DisconnectCause(reason)) }

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

        return try {
            control.requestEndpointChange(target) is CallControlResult.Success
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            false
        }
    }

    fun release() {
        addCallJob?.cancel()
        addCallJob = null
        callControlScope = null
        NovaCallAudioRouter.detach(this)
    }

    private suspend fun safeControlAction(
        action: suspend CallControlScope.() -> Unit,
    ): Boolean {
        val control = callControlScope ?: return false
        return try {
            control.action()
            true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun safeCallback(callback: suspend () -> Unit) {
        try {
            callback()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // Telecom is an integration layer, not the source of truth for Nova's
            // call lifecycle. A device-specific callback failure must stay local.
        }
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
