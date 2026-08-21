package com.nova.app.feature.auth.data.remote

import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.feature.auth.data.parseAuthSession
import com.nova.app.feature.auth.data.parseNovaUser
import com.nova.app.feature.auth.domain.model.AuthSession
import com.nova.app.feature.auth.domain.model.NovaUser
import org.json.JSONObject


class AuthRemoteDataSource(
    private val api: NovaApiClient,
) {
    suspend fun register(
        email: String,
        password: String,
        username: String,
        name: String,
    ): ApiResult<AuthSession> {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("username", username)
            .put("name", name)

        return when (val response = api.requestJson("auth/register/", "POST", body)) {
            is ApiResult.Success -> parseAuthSession(response.value, api::resolveMediaUrl)
            is ApiResult.Failure -> response
        }
    }

    suspend fun login(email: String, password: String): ApiResult<AuthSession> {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)

        return when (val response = api.requestJson("auth/login/", "POST", body)) {
            is ApiResult.Success -> parseAuthSession(response.value, api::resolveMediaUrl)
            is ApiResult.Failure -> response
        }
    }

    suspend fun me(accessToken: String): ApiResult<NovaUser> {
        return when (val response = api.requestJson("me/", bearerToken = accessToken)) {
            is ApiResult.Success -> ApiResult.Success(parseNovaUser(response.value, api::resolveMediaUrl))
            is ApiResult.Failure -> response
        }
    }

    suspend fun updateProfile(
        accessToken: String,
        name: String,
        username: String,
        avatar: UploadFile? = null,
    ): ApiResult<NovaUser> {
        return when (
            val response = api.requestMultipart(
                path = "me/",
                method = "PUT",
                fields = mapOf(
                    "name" to name,
                    "username" to username,
                ),
                fileField = "avatar",
                file = avatar,
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseNovaUser(response.value, api::resolveMediaUrl))
            is ApiResult.Failure -> response
        }
    }

    suspend fun refresh(refreshToken: String): ApiResult<String> {
        val body = JSONObject().put("refresh", refreshToken)

        return when (val response = api.requestJson("auth/refresh/", "POST", body)) {
            is ApiResult.Success -> {
                val access = response.value.optString("access")
                if (access.isBlank()) {
                    ApiResult.Failure("Nova returned an invalid session response.")
                } else {
                    ApiResult.Success(access)
                }
            }

            is ApiResult.Failure -> response
        }
    }
}
