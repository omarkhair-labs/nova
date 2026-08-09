package com.nova.app.core.calls

import android.content.Context
import android.net.Uri
import android.telecom.DisconnectCause
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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


class NovaTelecomBridge(
    context: Context,
) {
    private val callsManager = CallsManager(context.applicationContext)
    private var callControlScope: CallControlScope? = null
    private var addCallJob: Job? = null
    private var callType: Int = CallAttributesCompat.CALL_TYPE_AUDIO_CALL

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

        addCallJob = scope.launch {
            try {
                callsManager.addCall(
                    callAttributes = attributes,
                    onAnswer = {
                        onSystemAnswer()
                    },
                    onDisconnect = {
                        onSystemDisconnect()
                    },
                    onSetActive = {
                        onSystemSetActive()
                    },
                    onSetInactive = {
                        onSystemSetInactive()
                    },
                ) {
                    callControlScope = this
                    onReady()
                }
            } catch (error: Throwable) {
                onFailure(error)
            } finally {
                callControlScope = null
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

    fun release() {
        addCallJob?.cancel()
        addCallJob = null
        callControlScope = null
    }
}
