package com.nova.app.feature.people.data.remote

import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.people.data.parseNovaPerson
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.posts.data.parseNovaPosts
import com.nova.app.feature.posts.domain.model.NovaPost
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder


class PeopleRemoteDataSource(
    private val api: NovaApiClient,
) {
    suspend fun people(
        accessToken: String,
        query: String = "",
    ): ApiResult<List<NovaPerson>> {
        val cleanQuery = query.trim()
        val path = if (cleanQuery.isBlank()) {
            "people/"
        } else {
            "people/?q=${encode(cleanQuery)}"
        }

        return when (val response = api.requestJson(path, bearerToken = accessToken)) {
            is ApiResult.Success -> {
                val array = response.value.optJSONArray("results") ?: JSONArray()
                val people = buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { add(parseNovaPerson(it, api::resolveMediaUrl)) }
                    }
                }
                ApiResult.Success(people)
            }

            is ApiResult.Failure -> response
        }
    }

    suspend fun person(
        accessToken: String,
        username: String,
    ): ApiResult<NovaPerson> {
        return when (
            val response = api.requestJson(
                path = "people/${encode(username.trim().lowercase())}/",
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseNovaPerson(response.value, api::resolveMediaUrl))
            is ApiResult.Failure -> response
        }
    }

    suspend fun personPosts(
        accessToken: String,
        username: String,
    ): ApiResult<List<NovaPost>> {
        return when (
            val response = api.requestJson(
                path = "people/${encode(username.trim().lowercase())}/posts/",
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseNovaPosts(response.value, api::resolveMediaUrl))
            is ApiResult.Failure -> response
        }
    }

    suspend fun setFollowing(
        accessToken: String,
        username: String,
        follow: Boolean,
    ): ApiResult<NovaPerson> {
        val path = "people/${encode(username.trim().lowercase())}/follow/"
        val response = if (follow) {
            api.requestJson(path, method = "POST", body = JSONObject(), bearerToken = accessToken)
        } else {
            api.requestJson(path, method = "DELETE", bearerToken = accessToken)
        }

        return when (response) {
            is ApiResult.Success -> ApiResult.Success(parseNovaPerson(response.value, api::resolveMediaUrl))
            is ApiResult.Failure -> response
        }
    }

    suspend fun setBlocked(
        accessToken: String,
        username: String,
        blocked: Boolean,
    ): ApiResult<Unit> {
        val path = "people/${encode(username.trim().lowercase())}/block/"
        val response = if (blocked) {
            api.requestJson(path, method = "POST", body = JSONObject(), bearerToken = accessToken)
        } else {
            api.requestJson(path, method = "DELETE", bearerToken = accessToken)
        }
        return when (response) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> response
        }
    }

    suspend fun reportPerson(
        accessToken: String,
        username: String,
        reason: String,
        details: String = "",
    ): ApiResult<String> {
        val body = JSONObject()
            .put("reason", reason)
            .put("details", details)
        return when (
            val response = api.requestJson(
                path = "people/${encode(username.trim().lowercase())}/report/",
                method = "POST",
                body = body,
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                response.value.optString("detail").ifBlank { "Report submitted for review." },
            )
            is ApiResult.Failure -> response
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
