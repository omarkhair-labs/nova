package com.nova.app.core.presence

import android.content.Context
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit


object NovaAppPresence {
    private var client: NovaAppPresenceClient? = null
    private var isForeground = false

    fun initialize(context: Context) {
        if (client == null) {
            client = NovaAppPresenceClient(context.applicationContext)
        }
    }

    fun enterForeground() {
        isForeground = true
        client?.start()
    }

    fun leaveForeground() {
        isForeground = false
        client?.stop()
    }

    fun sessionChanged() {
        if (isForeground) client?.restart()
    }
}


private class NovaAppPresenceClient(context: Context) {
    private val repository = NovaMessagingRepository(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var connectionJob: Job? = null
    private var stopped = true
    private var reconnectAttempt = 0

    @Synchronized
    fun start() {
        if (!stopped) return
        stopped = false
        reconnectAttempt = 0
        connect()
    }

    @Synchronized
    fun stop() {
        if (stopped) return
        stopped = true
        reconnectJob?.cancel()
        reconnectJob = null
        connectionJob?.cancel()
        connectionJob = null
        val oldSocket = socket
        socket = null
        oldSocket?.close(1000, "Nova backgrounded")
    }

    @Synchronized
    fun restart() {
        if (stopped) return
        reconnectJob?.cancel()
        reconnectJob = null
        connectionJob?.cancel()
        connectionJob = null
        val oldSocket = socket
        socket = null
        oldSocket?.close(1000, "Nova session changed")
        reconnectAttempt = 0
        connect()
    }

    private fun connect() {
        if (stopped) return
        connectionJob?.cancel()
        connectionJob = scope.launch {
            when (val token = repository.realtimeAccessToken()) {
                is ApiResult.Success -> openSocket(token.value)
                is ApiResult.Failure -> scheduleReconnect(loggedOut = token.statusCode == 401)
            }
        }
    }

    private fun openSocket(accessToken: String) {
        if (stopped) return
        val request = Request.Builder()
            .url("${NovaMessagingRepository.PRODUCTION_WS_URL}presence/")
            .header("Authorization", "Bearer $accessToken")
            .build()

        val newSocket = sharedClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (socket !== webSocket || stopped) return
                    reconnectAttempt = 0
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (socket !== webSocket) return
                    socket = null
                    if (!stopped) scheduleReconnect(loggedOut = false)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (socket !== webSocket) return
                    socket = null
                    if (!stopped) scheduleReconnect(loggedOut = false)
                }
            },
        )
        if (stopped) {
            newSocket.close(1000, "Nova backgrounded")
        } else {
            socket = newSocket
        }
    }

    private fun scheduleReconnect(loggedOut: Boolean) {
        if (stopped || reconnectJob?.isActive == true) return
        val delayMs = if (loggedOut) {
            10_000L
        } else {
            when (reconnectAttempt.coerceAtMost(4)) {
                0 -> 1_000L
                1 -> 2_000L
                2 -> 4_000L
                3 -> 7_000L
                else -> 10_000L
            }
        }
        if (!loggedOut) reconnectAttempt += 1
        reconnectJob = scope.launch {
            delay(delayMs)
            reconnectJob = null
            connect()
        }
    }

    private companion object {
        val sharedClient: OkHttpClient = OkHttpClient.Builder()
            .pingInterval(25, TimeUnit.SECONDS)
            .build()
    }
}
