package com.nova.app.feature.rooms.data

import com.nova.app.feature.rooms.domain.model.RoomConversation
import com.nova.app.feature.rooms.domain.model.RoomDetail
import com.nova.app.feature.rooms.domain.model.RoomItem
import com.nova.app.feature.rooms.domain.model.RoomItemPage
import com.nova.app.feature.rooms.domain.model.RoomMember
import com.nova.app.feature.rooms.domain.model.RoomPerson
import com.nova.app.feature.rooms.domain.model.RoomSections
import com.nova.app.feature.rooms.domain.model.RoomSummary
import org.json.JSONArray
import org.json.JSONObject


internal fun parseRooms(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): List<RoomSummary> {
    val rows = json.optJSONArray("rooms") ?: JSONArray()
    return buildList {
        for (index in 0 until rows.length()) {
            rows.optJSONObject(index)?.let { add(parseRoomSummary(it, resolveMediaUrl)) }
        }
    }
}


internal fun parseRoomDetail(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): RoomDetail {
    val room = json.optJSONObject("room") ?: JSONObject()
    val members = json.optJSONArray("members") ?: JSONArray()
    return RoomDetail(
        conversation = parseRoomConversation(
            json.optJSONObject("conversation") ?: JSONObject(),
            resolveMediaUrl,
        ),
        description = room.optString("description"),
        sections = parseSections(room.optJSONObject("sections") ?: JSONObject()),
        members = buildList {
            for (index in 0 until members.length()) {
                members.optJSONObject(index)?.let { row ->
                    val person = row.optJSONObject("user") ?: JSONObject()
                    add(
                        RoomMember(
                            person = parseRoomPerson(person, resolveMediaUrl),
                            role = row.optString("role"),
                            joinedAt = row.optString("joined_at"),
                        )
                    )
                }
            }
        },
    )
}


internal fun parseRoomItemPage(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): RoomItemPage = RoomItemPage(
    pinned = parseItems(json.optJSONArray("pinned"), resolveMediaUrl),
    items = parseItems(json.optJSONArray("items"), resolveMediaUrl),
    nextBefore = json.optLong("next_before").takeIf { json.has("next_before") && !json.isNull("next_before") },
)


private fun parseRoomSummary(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): RoomSummary = RoomSummary(
    conversation = parseRoomConversation(
        json.optJSONObject("conversation") ?: JSONObject(),
        resolveMediaUrl,
    ),
    description = json.optString("description"),
)


private fun parseRoomConversation(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): RoomConversation = RoomConversation(
    id = json.optLong("id"),
    title = json.optString("title").ifBlank { "Nova Room" },
    avatarUrl = resolveMediaUrl(json.optString("group_avatar_url")),
    membersCount = json.optInt("members_count", 0),
    currentUserRole = json.optString("current_user_role"),
    unreadCount = json.optInt("unread_count", 0),
    updatedAt = json.optString("updated_at"),
)


private fun parseSections(json: JSONObject): RoomSections = RoomSections(
    all = json.optInt("all", 0),
    note = json.optInt("note", 0),
    photo = json.optInt("photo", 0),
    video = json.optInt("video", 0),
    music = json.optInt("music", 0),
    plan = json.optInt("plan", 0),
    saved = json.optInt("saved", 0),
)


private fun parseItems(
    rows: JSONArray?,
    resolveMediaUrl: (String) -> String,
): List<RoomItem> = buildList {
    val source = rows ?: JSONArray()
    for (index in 0 until source.length()) {
        source.optJSONObject(index)?.let { add(parseRoomItem(it, resolveMediaUrl)) }
    }
}


private fun parseRoomItem(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): RoomItem {
    val creator = json.optJSONObject("created_by")
    return RoomItem(
        id = json.optLong("id"),
        kind = json.optString("kind"),
        createdBy = creator?.let { parseRoomPerson(it, resolveMediaUrl) },
        title = json.optString("title"),
        body = json.optString("body"),
        url = json.optString("url"),
        mediaUrl = resolveMediaUrl(json.optString("media_url")),
        scheduledFor = json.optString("scheduled_for").takeIf {
            json.has("scheduled_for") && !json.isNull("scheduled_for") && it.isNotBlank()
        },
        pinned = json.optBoolean("pinned", false),
        createdAt = json.optString("created_at"),
        updatedAt = json.optString("updated_at"),
    )
}


private fun parseRoomPerson(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): RoomPerson = RoomPerson(
    id = json.optLong("id"),
    username = json.optString("username"),
    name = json.optString("name"),
    avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
)
