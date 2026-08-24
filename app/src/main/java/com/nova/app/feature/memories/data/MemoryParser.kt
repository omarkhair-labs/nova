package com.nova.app.feature.memories.data

import com.nova.app.feature.memories.domain.model.MemoryFilmPlan
import com.nova.app.feature.memories.domain.model.MemoryFilmScene
import com.nova.app.feature.memories.domain.model.MemoryDraft
import com.nova.app.feature.memories.domain.model.MemoryHighlight
import com.nova.app.feature.memories.domain.model.MemoryPerson
import com.nova.app.feature.memories.domain.model.MemoryPersonRow
import com.nova.app.feature.memories.domain.model.MemoryRoom
import com.nova.app.feature.memories.domain.model.MemoryRoomRow
import com.nova.app.feature.memories.domain.model.MemoryStats
import com.nova.app.feature.memories.domain.model.WeeklyMemory
import org.json.JSONArray
import org.json.JSONObject


internal fun parseMemoryDraft(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): MemoryDraft = MemoryDraft(
    id = json.optLong("id"),
    kind = json.optString("kind", "recap"),
    title = json.optString("title"),
    note = json.optString("note"),
    mediaUrl = resolveMediaUrl(json.optString("media_url")),
    mediaType = json.optString("media_type"),
    createdAt = json.optString("created_at"),
    updatedAt = json.optString("updated_at"),
)


internal fun parseWeeklyMemory(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): WeeklyMemory {
    val stats = json.optJSONObject("stats") ?: JSONObject()
    return WeeklyMemory(
        startsAt = json.optString("starts_at"),
        endsAt = json.optString("ends_at"),
        utcOffsetMinutes = json.optInt("utc_offset_minutes", 0),
        weeksAgo = json.optInt("weeks_ago", 0),
        generatedAt = json.optString("generated_at"),
        stats = MemoryStats(
            pulses = stats.optInt("pulses", 0),
            posts = stats.optInt("posts", 0),
            roomItems = stats.optInt("room_items", 0),
            rooms = stats.optInt("rooms", 0),
            people = stats.optInt("people", 0),
            nights = stats.optInt("nights", 0),
            highlights = stats.optInt("highlights", 0),
        ),
        highlights = parseHighlights(json.optJSONArray("highlights"), resolveMediaUrl),
        people = parsePeople(json.optJSONArray("people"), resolveMediaUrl),
        rooms = parseRooms(json.optJSONArray("rooms"), resolveMediaUrl),
    )
}


internal fun parseMemoryFilmPlan(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): MemoryFilmPlan {
    val rows = json.optJSONArray("scenes") ?: JSONArray()
    val scenes = buildList {
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            add(
                MemoryFilmScene(
                    index = row.optInt("index", index),
                    source = row.optString("source"),
                    sourceId = row.optLong("source_id"),
                    mediaType = row.optString("media_type"),
                    mediaUrl = resolveMediaUrl(row.optString("media_url")),
                    occurredAt = row.optString("occurred_at"),
                    durationMs = row.optLong("duration_ms", 0L).coerceAtLeast(0L),
                    trimStartMs = row.optLong("trim_start_ms", 0L).coerceAtLeast(0L),
                    caption = row.optString("caption"),
                    person = row.optJSONObject("person")?.let { parsePerson(it, resolveMediaUrl) },
                    room = row.optJSONObject("room")?.let { parseRoom(it, resolveMediaUrl) },
                )
            )
        }
    }
    return MemoryFilmPlan(
        renderVersion = json.optInt("render_version", 1),
        selectionVersion = json.optString("selection_version"),
        startsAt = json.optString("starts_at"),
        endsAt = json.optString("ends_at"),
        utcOffsetMinutes = json.optInt("utc_offset_minutes", 0),
        weeksAgo = json.optInt("weeks_ago", 0),
        filmReady = json.optBoolean("film_ready", false),
        mood = json.optString("mood"),
        targetDurationMs = json.optLong("target_duration_ms", 0L),
        totalDurationMs = json.optLong("total_duration_ms", 0L),
        coverMediaUrl = resolveMediaUrl(json.optString("cover_media_url")),
        scenes = scenes,
    )
}


private fun parseHighlights(
    rows: JSONArray?,
    resolveMediaUrl: (String) -> String,
): List<MemoryHighlight> = buildList {
    val source = rows ?: JSONArray()
    for (index in 0 until source.length()) {
        val row = source.optJSONObject(index) ?: continue
        add(
            MemoryHighlight(
                source = row.optString("source"),
                id = row.optLong("id"),
                occurredAt = row.optString("occurred_at"),
                title = row.optString("title"),
                text = row.optString("text"),
                url = row.optString("url"),
                mediaType = row.optString("media_type"),
                mediaUrl = resolveMediaUrl(row.optString("media_url")),
                person = row.optJSONObject("person")?.let { parsePerson(it, resolveMediaUrl) },
                room = row.optJSONObject("room")?.let { parseRoom(it, resolveMediaUrl) },
            )
        )
    }
}


private fun parsePeople(
    rows: JSONArray?,
    resolveMediaUrl: (String) -> String,
): List<MemoryPersonRow> = buildList {
    val source = rows ?: JSONArray()
    for (index in 0 until source.length()) {
        val row = source.optJSONObject(index) ?: continue
        val person = row.optJSONObject("person") ?: continue
        add(
            MemoryPersonRow(
                person = parsePerson(person, resolveMediaUrl),
                sharedCount = row.optInt("shared_count", 0),
            )
        )
    }
}


private fun parseRooms(
    rows: JSONArray?,
    resolveMediaUrl: (String) -> String,
): List<MemoryRoomRow> = buildList {
    val source = rows ?: JSONArray()
    for (index in 0 until source.length()) {
        val row = source.optJSONObject(index) ?: continue
        val room = row.optJSONObject("room") ?: continue
        add(
            MemoryRoomRow(
                room = parseRoom(room, resolveMediaUrl),
                sharedCount = row.optInt("shared_count", 0),
            )
        )
    }
}


private fun parsePerson(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): MemoryPerson = MemoryPerson(
    id = json.optLong("id"),
    username = json.optString("username"),
    name = json.optString("name"),
    avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
)


private fun parseRoom(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): MemoryRoom = MemoryRoom(
    id = json.optLong("id"),
    title = json.optString("title").ifBlank { "Nova Room" },
    avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
)
