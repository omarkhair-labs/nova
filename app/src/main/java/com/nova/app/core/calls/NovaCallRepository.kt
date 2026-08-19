package com.nova.app.core.calls

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


enum class NovaCallKind(val wireValue: String) {
    Audio("audio"),
    Video("video");

    companion object {
        fun fromWire(value: String): NovaCallKind =
            if (value.equals("video", ignoreCase = true)) Video else Audio
    }
}


enum class NovaCallStatus(val wireValue: String) {
    Ringing("ringing"),
    Active("active"),
    Declined("declined"),
    Canceled("canceled"),
    Ended("ended"),
    Missed("missed"),
    Failed("failed");

    val isTerminal: Boolean
        get() = this !in setOf(Ringing, Active)

    companion object {
        fun fromWire(value: String): NovaCallStatus =
            entries.firstOrNull { it.wireValue == value } ?: Failed
    }
}


data class NovaCallPerson(
    val id: Long,
    val username: String,
    val name: String,
    val avatarUrl: String,
) {
    val displayName: String
        get() = name.ifBlank { username }
}


data class NovaCallSession(
    val id: String,
    val conversationId: Long,
    val kind: NovaCallKind,
    val status: NovaCallStatus,
    val caller: NovaCallPerson,
    val callee: NovaCallPerson,
    val peer: NovaCallPerson,
    val isCaller: Boolean,
    val createdAt: String,
    val answeredAt: String?,
    val endedAt: String?,
    val endReason: String,
    val ringTimeoutSeconds: Int,
)


data class NovaIceServer(
    val urls: List<String>,
    val username: String = "",
    val credential: String = "",
)


data class NovaIceConfig(
    val servers: List<NovaIceServer>,
    val turnConfigured: Boolean,
)


class NovaCallRepository(
    context: Context,
    private val api: NovaCallsApiClient = NovaCallsApiClient(PRODUCTION_API_URL),
    private val authApi: NovaApiClient = NovaApiClient(PRODUCTION_API_URL),
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

    suspend fun createCall(conversationId: Long, kind: NovaCallKind): ApiResult<NovaCallSession> {
        return authenticatedCall { token -> api.createCall(token, conversationId, kind) }
    }

    suspend fun call(callId: String): ApiResult<NovaCallSession> {
        return authenticatedCall { token -> api.call(token, callId) }
    }

    suspend fun callAction(callId: String, action: String): ApiResult<NovaCallSession> {
        return authenticatedCall { token -> api.callAction(token, callId, action) }
    }

    suspend fun iceConfig(): ApiResult<NovaIceConfig> {
        return authenticatedCall { token -> api.iceConfig(token) }
    }

    suspend fun realtimeAccessToken(): ApiResult<String> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        return when (val refreshed = authApi.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                ApiResult.Success(refreshed.value)
            }
            is ApiResult.Failure -> {
                if (refreshed.statusCode == 400 || refreshed.statusCode == 401) {
                    sessionStore.clear()
                    ApiResult.Failure("Your session expired. Please log in again.", 401)
                } else {
                    ApiResult.Success(stored.accessToken)
                }
            }
        }
    }

    private suspend fun <T> authenticatedCall(
        block: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        val first = block(stored.accessToken)
        if (first !is ApiResult.Failure || first.statusCode != 401) return first

        return when (val refreshed = authApi.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                block(refreshed.value)
            }
            is ApiResult.Failure -> {
                sessionStore.clear()
                ApiResult.Failure("Your session expired. Please log in again.", 401)
            }
        }
    }

    companion object {
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
        const val PRODUCTION_WS_URL = "wss://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/ws/"
    }
}


object NovaCallActionDispatcher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun dispatch(context: Context, callId: String, action: String) {
        val appContext = context.applicationContext
        scope.launch {
            val repository = NovaCallRepository(appContext)
            val retryDelays = longArrayOf(0L, 800L, 2_000L, 5_000L)
            for (waitMs in retryDelays) {
                if (waitMs > 0) delay(waitMs)
                when (val result = repository.callAction(callId, action)) {
                    is ApiResult.Success -> return@launch
                    is ApiResult.Failure -> {
                        if (result.statusCode == 400 || result.statusCode == 401 || result.statusCode == 404) {
                            return@launch
                        }
                    }
                }
            }
        }
    }
}


class NovaCallsApiClient(
    private val baseUrl: String,
) {
    suspend fun createCall(
        accessToken: String,
        conversationId: Long,
        kind: NovaCallKind,
    ): ApiResult<NovaCallSession> {
        val body = JSONObject()
            .put("conversation_id", conversationId)
            .put("kind", kind.wireValue)
        return when (val response = requestJson("calls/", "POST", body, accessToken)) {
            is ApiResult.Success -> ApiResult.Success(parseCall(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun call(accessToken: String, callId: String): ApiResult<NovaCallSession> {
        return when (val response = requestJson("calls/$callId/", bearerToken = accessToken)) {
            is ApiResult.Success -> ApiResult.Success(parseCall(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun callAction(
        accessToken: String,
        callId: String,
        action: String,
    ): ApiResult<NovaCallSession> {
        val body = JSONObject().put("action", action)
        return when (
            val response = requestJson(
                path = "calls/$callId/action/",
                method = "POST",
                body = body,
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseCall(response.value))
            is ApiResult.Failure -> response
        }
    }

    suspend fun iceConfig(accessToken: String): ApiResult<NovaIceConfig> {
        return when (val response = requestJson("calls/ice/", bearerToken = accessToken)) {
            is ApiResult.Success -> {
                val array = response.value.optJSONArray("ice_servers") ?: JSONArray()
                val servers = buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val rawUrls = item.optJSONArray("urls") ?: JSONArray()
                        val urls = buildList {
                            for (urlIndex in 0 until rawUrls.length()) {
                                rawUrls.optString(urlIndex).takeIf { it.isNotBlank() }?.let(::add)
                            }
                        }
                        if (urls.isNotEmpty()) {
                            add(
                                NovaIceServer(
                                    urls = urls,
                                    username = item.optString("username"),
                                    credential = item.optString("credential"),
                                )
                            )
                        }
                    }
                }
                ApiResult.Success(
                    NovaIceConfig(
                        servers = servers,
                        turnConfigured = response.value.optBoolean("turn_configured", false),
                    )
                )
            }
            is ApiResult.Failure -> response
        }
    }

    private suspend fun requestJson(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        bearerToken: String? = null,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                bearerToken?.takeIf { it.isNotBlank() }?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
                if (body != null) {
                    doOutput = true
                    outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text.ifBlank { "{}" }) }.getOrElse { JSONObject() }
            if (statusCode in 200..299) {
                ApiResult.Success(json)
            } else {
                ApiResult.Failure(
                    message = json.optString("detail").ifBlank { "Nova couldn't complete that call request." },
                    statusCode = statusCode,
                )
            }
        } catch (_: Exception) {
            ApiResult.Failure("Nova couldn't reach the call service. Check your connection and try again.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseCall(json: JSONObject): NovaCallSession {
        return NovaCallSession(
            id = json.optString("id"),
            conversationId = json.optLong("conversation_id"),
            kind = NovaCallKind.fromWire(json.optString("kind")),
            status = NovaCallStatus.fromWire(json.optString("status")),
            caller = parsePerson(json.optJSONObject("caller") ?: JSONObject()),
            callee = parsePerson(json.optJSONObject("callee") ?: JSONObject()),
            peer = parsePerson(json.optJSONObject("peer") ?: JSONObject()),
            isCaller = json.optBoolean("is_caller"),
            createdAt = json.optString("created_at"),
            answeredAt = json.optString("answered_at").takeIf { it.isNotBlank() && it != "null" },
            endedAt = json.optString("ended_at").takeIf { it.isNotBlank() && it != "null" },
            endReason = json.optString("end_reason"),
            ringTimeoutSeconds = json.optInt("ring_timeout_seconds", 45),
        )
    }

    private fun parsePerson(json: JSONObject): NovaCallPerson {
        return NovaCallPerson(
            id = json.optLong("id"),
            username = json.optString("username"),
            name = json.optString("name"),
            avatarUrl = json.optString("avatar_url"),
        )
    }
}
