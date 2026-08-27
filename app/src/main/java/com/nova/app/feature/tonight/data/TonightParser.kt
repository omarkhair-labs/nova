package com.nova.app.feature.tonight.data

import com.nova.app.feature.tonight.domain.model.TonightPerson
import com.nova.app.feature.tonight.domain.model.TonightPersonRow
import com.nova.app.feature.tonight.domain.model.TonightPulse
import com.nova.app.feature.tonight.domain.model.TonightSnapshot
import org.json.JSONArray
import org.json.JSONObject


internal fun parseTonightSnapshot(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): TonightSnapshot {
    val rows = json.optJSONArray("people") ?: JSONArray()
    return TonightSnapshot(
        isTonight = json.optBoolean("is_tonight", false),
        localHour = json.optInt("local_hour", 0),
        utcOffsetMinutes = json.optInt("utc_offset_minutes", 0),
        startsAt = json.optString("starts_at"),
        endsAt = json.optString("ends_at"),
        peopleCount = json.optInt("people_count", 0),
        momentsCount = json.optInt("moments_count", 0),
        myMomentsCount = json.optInt("my_moments_count", 0),
        people = buildList {
            for (index in 0 until rows.length()) {
                rows.optJSONObject(index)?.let { add(parsePersonRow(it, resolveMediaUrl)) }
            }
        },
    )
}


private fun parsePersonRow(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): TonightPersonRow = TonightPersonRow(
    person = parsePerson(json.optJSONObject("person") ?: JSONObject(), resolveMediaUrl),
    momentsCount = json.optInt("moments_count", 0),
    latestPulse = parsePulse(json.optJSONObject("latest_pulse") ?: JSONObject(), resolveMediaUrl),
)


private fun parsePerson(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): TonightPerson = TonightPerson(
    id = json.optLong("id"),
    username = json.optString("username"),
    name = json.optString("name"),
    avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
)


internal data class TonightPulseMediaUrls(
    val mediaUrl: String,
    val thumbnailUrl: String,
)


internal fun resolveTonightPulseMediaUrls(
    rawMediaUrl: String,
    rawThumbnailUrl: String,
    resolveMediaUrl: (String) -> String,
): TonightPulseMediaUrls = TonightPulseMediaUrls(
    mediaUrl = resolveMediaUrl(rawMediaUrl),
    thumbnailUrl = resolveMediaUrl(rawThumbnailUrl),
)


private fun parsePulse(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): TonightPulse {
    val media = resolveTonightPulseMediaUrls(
        rawMediaUrl = json.optString("media_url"),
        rawThumbnailUrl = json.optString("thumbnail_url"),
        resolveMediaUrl = resolveMediaUrl,
    )
    return TonightPulse(
        id = json.optLong("id"),
        author = parsePerson(json.optJSONObject("author") ?: JSONObject(), resolveMediaUrl),
        mediaUrl = media.mediaUrl,
        thumbnailUrl = media.thumbnailUrl,
        mediaType = json.optString("media_type"),
        audience = json.optString("audience"),
        note = json.optString("note"),
        createdAt = json.optString("created_at"),
        expiresAt = json.optString("expires_at"),
        isMine = json.optBoolean("is_mine", false),
    )
}
