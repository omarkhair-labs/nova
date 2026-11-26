package com.nova.app.feature.plants.data

import org.json.JSONArray
import org.json.JSONObject

internal data class RoomPlant(val id: String, val x: Double, val y: Double, val latest: String)
internal data class RoomWindow(val id: String, val x: Double, val y: Double)
internal data class PlantupiaRoom(
    val revision: Int, val width: Double?, val length: Double?,
    val plants: List<RoomPlant>, val windows: List<RoomWindow>,
) {
    fun submission(width: Double, length: Double, placements: List<RoomPlant>) = JSONObject()
        .put("base_revision", revision).put("width_m", width).put("length_m", length)
        .put("plants", JSONArray().apply {
            placements.forEach { put(JSONObject().put("plant_id", it.id).put("x_m", it.x).put("y_m", it.y)) }
        })
        .put("windows", JSONArray().apply {
            windows.forEach { put(JSONObject().put("id", it.id).put("x_m", it.x).put("y_m", it.y)) }
        })
}

internal fun parseRoom(body: JSONObject, spaceId: String): PlantupiaRoom {
    require(requiredPlantupiaString(body, "space_id") == spaceId)
    val revision = body.getInt("revision")
    require(revision >= 0 && body.has("room"))
    if (body.isNull("room")) {
        require(revision == 0)
        return PlantupiaRoom(0, null, null, emptyList(), emptyList())
    }
    val geometry = body.getJSONObject("room")
    require(requiredPlantupiaString(geometry, "room_id") == spaceId && revision > 0)
    val width = geometry.getDouble("width_m")
    val length = geometry.getDouble("length_m")
    require(width > 0 && length > 0 && width.isFinite() && length.isFinite())
    val plants = geometry.getJSONArray("plants")
    val windows = geometry.getJSONArray("windows")
    return PlantupiaRoom(revision, width, length, List(plants.length()) { i ->
        val p = plants.getJSONObject(i)
        RoomPlant(requiredPlantupiaString(p, "plant_id"), p.getDouble("x_m"), p.getDouble("y_m"),
            p.optString("latest_observed_at").takeUnless { it == "null" }.orEmpty())
    }, List(windows.length()) { i ->
        val w = windows.getJSONObject(i)
        RoomWindow(requiredPlantupiaString(w, "id"), w.getDouble("x_m"), w.getDouble("y_m"))
    })
}
