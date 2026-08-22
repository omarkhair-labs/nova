package com.nova.app.feature.pulse.data

import com.nova.app.feature.pulse.domain.model.NovaPulse
import com.nova.app.feature.pulse.domain.model.NovaPulseAuthor
import org.json.JSONObject


internal fun parseNovaPulse(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): NovaPulse {
    val author = json.optJSONObject("author") ?: JSONObject()
    return NovaPulse(
        id = json.optLong("id"),
        author = NovaPulseAuthor(
            id = author.optLong("id"),
            username = author.optString("username"),
            name = author.optString("name"),
            avatarUrl = resolveMediaUrl(author.optString("avatar_url")),
        ),
        mediaUrl = resolveMediaUrl(json.optString("media_url")),
        mediaType = json.optString("media_type"),
        audience = json.optString("audience"),
        note = json.optString("note"),
        createdAt = json.optString("created_at"),
        expiresAt = json.optString("expires_at"),
        isMine = json.optBoolean("is_mine", false),
        replyToId = json.optNullableLong("reply_to_id"),
        chainRootId = json.optNullableLong("chain_root_id"),
    )
}


private fun JSONObject.optNullableLong(name: String): Long? {
    val value = opt(name)
    return when (value) {
        null, JSONObject.NULL -> null
        is Number -> value.toLong()
        else -> value.toString().toLongOrNull()
    }
}
