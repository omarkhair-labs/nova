package com.nova.app.feature.people.data

import com.nova.app.feature.people.domain.model.NovaPerson
import org.json.JSONObject


internal fun parseNovaPerson(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): NovaPerson {
    return NovaPerson(
        id = json.optLong("id"),
        username = json.optString("username"),
        name = json.optString("name"),
        avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
        followersCount = json.optInt("followers_count", 0),
        followingCount = json.optInt("following_count", 0),
        postsCount = json.optInt("posts_count", 0),
        isFollowing = json.optBoolean("is_following", false),
    )
}
