package com.nova.app.feature.orbit.data

import com.nova.app.feature.orbit.domain.model.OrbitEvent
import com.nova.app.feature.orbit.domain.model.OrbitPage
import com.nova.app.feature.orbit.domain.model.OrbitPerson
import com.nova.app.feature.orbit.domain.model.OrbitPost
import com.nova.app.feature.orbit.domain.model.OrbitPulse
import org.json.JSONArray
import org.json.JSONObject


internal fun parseOrbitPage(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): OrbitPage {
    val rows = json.optJSONArray("results") ?: JSONArray()
    return OrbitPage(
        events = buildList {
            for (index in 0 until rows.length()) {
                rows.optJSONObject(index)?.let { add(parseOrbitEvent(it, resolveMediaUrl)) }
            }
        },
        nextCursor = json.optString("next_cursor")
            .takeIf { it.isNotBlank() && it != "null" },
    )
}


private fun parseOrbitEvent(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): OrbitEvent = OrbitEvent(
    id = json.optString("id"),
    kind = json.optString("kind"),
    actor = parsePerson(json.optJSONObject("actor") ?: JSONObject(), resolveMediaUrl),
    createdAt = json.optString("created_at"),
    post = json.optJSONObject("post")?.let { parsePost(it, resolveMediaUrl) },
    person = json.optJSONObject("person")?.let { parsePerson(it, resolveMediaUrl) },
    pulse = json.optJSONObject("pulse")?.let { parsePulse(it, resolveMediaUrl) },
    commentPreview = json.optString("comment_preview"),
)


private fun parsePerson(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): OrbitPerson = OrbitPerson(
    id = json.optLong("id"),
    username = json.optString("username"),
    name = json.optString("name"),
    avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
)


private fun parsePost(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): OrbitPost = OrbitPost(
    id = json.optLong("id"),
    author = parsePerson(json.optJSONObject("author") ?: JSONObject(), resolveMediaUrl),
    imageUrl = resolveMediaUrl(json.optString("image_url")),
    caption = json.optString("caption"),
)


private fun parsePulse(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): OrbitPulse = OrbitPulse(
    id = json.optLong("id"),
    author = parsePerson(json.optJSONObject("author") ?: JSONObject(), resolveMediaUrl),
    mediaUrl = resolveMediaUrl(json.optString("media_url")),
    mediaType = json.optString("media_type"),
    note = json.optString("note"),
    replyToId = json.optNullableLong("reply_to_id"),
    chainRootId = json.optNullableLong("chain_root_id"),
)


private fun JSONObject.optNullableLong(name: String): Long? = when (val value = opt(name)) {
    null, JSONObject.NULL -> null
    is Number -> value.toLong()
    else -> value.toString().toLongOrNull()
}
