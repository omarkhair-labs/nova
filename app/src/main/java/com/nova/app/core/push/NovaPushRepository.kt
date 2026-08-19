package com.nova.app.core.push

import android.content.Context
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class NovaPushRepository(
    context: Context,
    private val api: NovaPushApiClient = NovaPushApiClient(PRODUCTION_API_URL),
    private val authApi: NovaApiClient = NovaApiClient(PRODUCTION_API_URL),
) {
    private val sessionStore = NovaSessionStore(context.applicationContext)

    suspend fun register(installationId: String): ApiResult<Unit> {
        return authenticatedCall { accessToken ->
            api.register(accessToken, installationId)
        }
    }

    suspend fun remove(installationId: String): ApiResult<Unit> {
        return authenticatedCall { accessToken ->
            api.remove(accessToken, installationId)
        }
    }

    private suspend fun <T> authenticatedCall(
        call: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("No signed-in Nova session.", 401)

        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return first
            is ApiResult.Failure -> {
                if (first.statusCode != 401) return first
            }
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
                } else {
                    refreshed
                }
            }
        }
    }

    companion object {
        const val PRODUCTION_API_URL = "https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"
    }
}


class NovaPushApiClient(
    private val baseUrl: String = NovaPushRepository.PRODUCTION_API_URL,
) {
    suspend fun register(
        accessToken: String,
        installationId: String,
    ): ApiResult<Unit> {
        return request(
            method = "POST",
            accessToken = accessToken,
            installationId = installationId,
        )
    }

    suspend fun remove(
        accessToken: String,
        installationId: String,
    ): ApiResult<Unit> {
        return request(
            method = "DELETE",
            accessToken = accessToken,
            installationId = installationId,
        )
    }

    private suspend fun request(
        method: String,
        accessToken: String,
        installationId: String,
    ): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(baseUrl + "push/devices/").openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }

                val body = JSONObject()
                    .put("token", installationId)
                    .put("platform", "android")
                    .toString()

                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }

                val statusCode = connection.responseCode
                if (statusCode in 200..299) {
                    ApiResult.Success(Unit)
                } else {
                    val raw = connection.errorStream
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()
                    val detail = runCatching {
                        JSONObject(raw).optString("detail")
                    }.getOrDefault("")

                    ApiResult.Failure(
                        message = detail.ifBlank {
                            when (statusCode) {
                                401 -> "Your session expired. Please log in again."
                                in 500..599 -> "Nova couldn't update push notifications right now."
                                else -> "Nova couldn't update push notifications."
                            }
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
}
