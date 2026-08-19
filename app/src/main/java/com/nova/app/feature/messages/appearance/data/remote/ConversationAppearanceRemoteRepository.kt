package com.nova.app.feature.messages.appearance.data.remote

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.messages.appearance.data.ConversationAppearanceRepository
import com.nova.app.feature.messages.appearance.model.ConversationPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class ConversationAppearanceRemoteRepository(
    context: Context,
    private val baseUrl: String = NovaMessagingRepository.PRODUCTION_API_URL,
) : ConversationAppearanceRepository {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)
    private val authApi = NovaApiClient(baseUrl)
    private val themePreferences = appContext.getSharedPreferences(
        THEME_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override suspend fun preference(conversationId: Long): ApiResult<ConversationPreference> {
        val localTheme = localTheme(conversationId)
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/preferences/",
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> {
                    val preference = parseConversationPreference(response.value, localTheme)
                    if (response.value.has("theme_key")) {
                        rememberLocalTheme(conversationId, preference.themeKey)
                    }
                    ApiResult.Success(preference)
                }
                is ApiResult.Failure -> response
            }
        }
    }

    suspend fun setMuted(
        conversationId: Long,
        muted: Boolean,
    ): ApiResult<ConversationPreference> {
        return update(
            conversationId = conversationId,
            body = JSONObject().put("muted", muted),
            fallbackThemeKey = localTheme(conversationId),
        )
    }

    override suspend fun setTheme(
        conversationId: Long,
        themeKey: String,
    ): ApiResult<ConversationPreference> {
        val cleanTheme = normalizeConversationThemeKey(themeKey)
        rememberLocalTheme(conversationId, cleanTheme)

        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/preferences/",
                    method = "POST",
                    body = JSONObject().put("theme_key", cleanTheme),
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> {
                    val preference = parseConversationPreference(response.value, cleanTheme)
                    rememberLocalTheme(conversationId, preference.themeKey)
                    ApiResult.Success(preference.copy(themeKey = cleanTheme))
                }
                is ApiResult.Failure -> {
                    if (response.statusCode == 400 && response.message.contains("muted must be true or false", ignoreCase = true)) {
                        setThemeAgainstLegacyBackend(
                            conversationId = conversationId,
                            cleanTheme = cleanTheme,
                            bearerToken = token,
                        )
                    } else {
                        response
                    }
                }
            }
        }
    }

    private suspend fun setThemeAgainstLegacyBackend(
        conversationId: Long,
        cleanTheme: String,
        bearerToken: String,
    ): ApiResult<ConversationPreference> {
        val current = requestJson(
            path = "conversations/$conversationId/preferences/",
            bearerToken = bearerToken,
        )
        if (current is ApiResult.Failure) return current

        val currentJson = (current as ApiResult.Success).value
        val muted = currentJson.optBoolean("muted", false)
        return when (
            val response = requestJson(
                path = "conversations/$conversationId/preferences/",
                method = "POST",
                body = JSONObject()
                    .put("muted", muted)
                    .put("theme_key", cleanTheme),
                bearerToken = bearerToken,
            )
        ) {
            is ApiResult.Success -> {
                // Older production backends ignore theme_key and return only muted.
                // Keep the selected theme locally so appearance still works now;
                // once the new backend is deployed, the same request syncs it remotely.
                rememberLocalTheme(conversationId, cleanTheme)
                ApiResult.Success(
                    ConversationPreference(
                        muted = response.value.optBoolean("muted", muted),
                        themeKey = cleanTheme,
                    )
                )
            }
            is ApiResult.Failure -> response
        }
    }

    private suspend fun update(
        conversationId: Long,
        body: JSONObject,
        fallbackThemeKey: String,
    ): ApiResult<ConversationPreference> {
        return authenticatedCall { token ->
            when (
                val response = requestJson(
                    path = "conversations/$conversationId/preferences/",
                    method = "POST",
                    body = body,
                    bearerToken = token,
                )
            ) {
                is ApiResult.Success -> ApiResult.Success(
                    parseConversationPreference(response.value, fallbackThemeKey)
                )
                is ApiResult.Failure -> response
            }
        }
    }

    private fun localTheme(conversationId: Long): String {
        return themePreferences.getString(themePreferenceKey(conversationId), "nova")
            ?.let(::normalizeConversationThemeKey)
            ?: "nova"
    }

    private fun rememberLocalTheme(conversationId: Long, themeKey: String) {
        themePreferences.edit()
            .putString(themePreferenceKey(conversationId), normalizeConversationThemeKey(themeKey))
            .apply()
    }

    private fun themePreferenceKey(conversationId: Long) = "conversation_$conversationId"

    private suspend fun <T> authenticatedCall(
        call: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return first
            is ApiResult.Failure -> if (first.statusCode != 401) return first
        }

        return when (val refreshed = authApi.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                when (val retried = call(refreshed.value)) {
                    is ApiResult.Success -> retried
                    is ApiResult.Failure -> {
                        if (retried.statusCode == 401) sessionStore.clear()
                        retried
                    }
                }
            }
            is ApiResult.Failure -> {
                if (refreshed.statusCode == 400 || refreshed.statusCode == 401) {
                    sessionStore.clear()
                    ApiResult.Failure("Your session expired. Please log in again.", 401)
                } else refreshed
            }
        }
    }

    private suspend fun requestJson(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        bearerToken: String,
    ): ApiResult<JSONObject> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $bearerToken")
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
                    }
                }

                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = raw.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()

                if (statusCode in 200..299) {
                    ApiResult.Success(json)
                } else {
                    ApiResult.Failure(
                        message = when (statusCode) {
                            400 -> json.optString("detail").ifBlank { "Nova couldn't update that conversation." }
                            401 -> "Your session expired. Please log in again."
                            403 -> "You can't access that conversation."
                            404 -> "That conversation is no longer available."
                            in 500..599 -> "Nova's server had a problem. Try again in a moment."
                            else -> json.optString("detail").ifBlank { "Something went wrong. Please try again." }
                        },
                        statusCode = statusCode,
                    )
                }
            } catch (_: Exception) {
                ApiResult.Failure("Can't reach Nova right now. Check your connection and try again.")
            } finally {
                connection?.disconnect()
            }
        }
    }

    private companion object {
        const val THEME_PREFERENCES_NAME = "nova_conversation_themes"
    }
}


internal fun normalizeConversationThemeKey(themeKey: String): String =
    themeKey.trim().lowercase().ifBlank { "nova" }


internal fun parseConversationPreference(
    json: JSONObject,
    fallbackThemeKey: String = "nova",
): ConversationPreference = ConversationPreference(
    muted = json.optBoolean("muted", false),
    themeKey = if (json.has("theme_key")) {
        json.optString("theme_key", fallbackThemeKey).ifBlank { fallbackThemeKey }
    } else {
        fallbackThemeKey
    },
)
