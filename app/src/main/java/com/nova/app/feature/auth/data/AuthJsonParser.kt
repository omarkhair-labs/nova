package com.nova.app.feature.auth.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.auth.domain.model.AuthSession
import com.nova.app.feature.auth.domain.model.NovaUser
import org.json.JSONObject


internal fun parseAuthSession(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): ApiResult<AuthSession> {
    val access = json.optString("access")
    val refresh = json.optString("refresh")
    val userJson = json.optJSONObject("user")

    if (access.isBlank() || refresh.isBlank() || userJson == null) {
        return ApiResult.Failure("Nova returned an invalid authentication response.")
    }

    return ApiResult.Success(
        AuthSession(
            accessToken = access,
            refreshToken = refresh,
            user = parseNovaUser(userJson, resolveMediaUrl),
        ),
    )
}


internal fun parseNovaUser(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): NovaUser {
    return NovaUser(
        id = json.optLong("id"),
        email = json.optString("email"),
        username = json.optString("username"),
        name = json.optString("name"),
        avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
        bio = json.optString("bio"),
        location = json.optString("location"),
        link = json.optString("link"),
        interests = buildList {
            val values = json.optJSONArray("interests")
            if (values != null) {
                for (index in 0 until values.length()) {
                    values.optString(index).takeIf(String::isNotBlank)?.let(::add)
                }
            }
        },
        profileTheme = json.optString("profile_theme", "violet"),
        showOrbit = json.optBoolean("show_orbit", true),
        isVerified = json.optBoolean("is_verified", false),
        followersCount = json.optInt("followers_count", 0),
        followingCount = json.optInt("following_count", 0),
        postsCount = json.optInt("posts_count", 0),
    )
}
