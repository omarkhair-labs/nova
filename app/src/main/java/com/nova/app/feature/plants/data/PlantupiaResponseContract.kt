package com.nova.app.feature.plants.data

import com.nova.app.core.network.ApiResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A successful HTTP status is not proof of a valid Plantupia record/envelope. */
internal fun <T> parsePlantupiaResponse(body: JSONObject, parse: (JSONObject) -> T): ApiResult<T> =
    try {
        ApiResult.Success(parse(body))
    } catch (_: JSONException) {
        invalidPlantupiaResponse()
    } catch (_: IllegalArgumentException) {
        invalidPlantupiaResponse()
    }

private fun invalidPlantupiaResponse() =
    ApiResult.Failure("The Plantupia server returned an invalid response. Your records couldn't be verified. Please retry.")

internal fun requiredPlantupiaString(body: JSONObject, key: String): String =
    (body.opt(key) as? String)?.takeIf { it.isNotBlank() && it != "null" }
        ?: throw IllegalArgumentException("Missing string field")

internal fun requiredPlantupiaId(body: JSONObject, expected: String? = null): String =
    requiredPlantupiaString(body, "id").also { require(expected == null || it == expected) }

internal fun requiredPlantupiaLong(body: JSONObject, key: String): Long {
    val number = body.opt(key)
    require(number is Number)
    return number.toString().toLongOrNull()?.takeIf { it > 0L }
        ?: throw IllegalArgumentException("Invalid integer")
}

internal fun plantupiaRows(body: JSONObject): List<JSONObject> {
    val rows = body.opt("results") as? JSONArray ?: throw IllegalArgumentException("Missing results array")
    return List(rows.length()) { index ->
        (rows.opt(index) as? JSONObject ?: throw IllegalArgumentException("Invalid record"))
            .also { requiredPlantupiaId(it) }
    }
}

internal fun <T> parsePlantupiaList(body: JSONObject, parse: (JSONObject) -> T): ApiResult<List<T>> =
    parsePlantupiaResponse(body) { plantupiaRows(it).map(parse) }

/**
 * The care calendar endpoint returns aggregate rows, not PlantEvent records.
 * A row is keyed by (date, plant_id, kind), and deliberately has no "id".
 * Do not reuse plantupiaRows(), which requires an entity ID for every row.
 */
internal fun plantupiaCalendarHistoryRows(body: JSONObject): List<JSONObject> {
    val rows = body.opt("results") as? JSONArray ?: throw IllegalArgumentException("Missing calendar results")
    return List(rows.length()) { index ->
        val row = rows.opt(index) as? JSONObject ?: throw IllegalArgumentException("Malformed calendar row")
        val date = requiredPlantupiaString(row, "date")
        require(runCatching { java.time.LocalDate.parse(date) }.isSuccess) { "Invalid calendar date" }
        requiredPlantupiaString(row, "plant_id")
        requiredPlantupiaString(row, "plant_name")
        requiredPlantupiaString(row, "kind")
        require(requiredPlantupiaString(row, "state") == "recorded") { "Unknown calendar state" }
        val count = row.opt("count") as? Number ?: throw IllegalArgumentException("Missing calendar count")
        require(count.toDouble().isFinite() && count.toDouble() >= 1.0 && count.toDouble() <= Int.MAX_VALUE &&
            count.toDouble() == count.toInt().toDouble()) { "Invalid calendar count" }
        row
    }
}

internal fun plantupiaNextOffset(body: JSONObject, offset: Int): Int? {
    require(body.has("next_offset"))
    if (body.isNull("next_offset")) return null
    return requiredPlantupiaLong(body, "next_offset").also {
        require(it > offset && it <= Int.MAX_VALUE)
    }.toInt()
}

internal fun plantupiaNextCursor(body: JSONObject): String? {
    require(body.has("next_cursor"))
    if (body.isNull("next_cursor")) return null
    return requiredPlantupiaString(body, "next_cursor").also {
        require(it.toLongOrNull()?.let { value -> value > 0L } == true)
    }
}
