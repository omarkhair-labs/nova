package com.nova.app.feature.plants.data

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

internal data class RoomDaylightScenario(
    val revision: Int,
    val windowId: String,
    val x: Double,
    val y: Double,
    val height: Double,
    val orientation: Double,
    val openingWidth: Double,
    val sill: Double,
    val head: Double,
    val latitude: Double,
    val longitude: Double,
    val times: List<OffsetDateTime>,
    val historical: Boolean = false,
    val screen: List<Double>? = null,
) {
    fun body(): JSONObject = JSONObject()
        .put("revision", revision)
        .put("historical", historical)
        .apply { screen?.let { put("screen", JSONArray(it)) } }
        .put("window_id", windowId)
        .put("point", JSONArray(listOf(x, y, height)))
        .put("top_normal_deg", orientation)
        .put("width_m", openingWidth)
        .put("sill_m", sill)
        .put("head_m", head)
        .put("latitude", latitude)
        .put("longitude", longitude)
        .put("timestamps", JSONArray(times.map { it.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) }))
}

internal data class RoomDaylightSample(
    val at: OffsetDateTime,
    val direct: Boolean,
    val elevation: Double,
    val footprint: List<Pair<Double, Double>>,
    val shadow: List<Pair<Double, Double>> = emptyList(),
)

internal fun daylightSampleTimes(date: LocalDate, zone: ZoneId): List<OffsetDateTime> {
    val start = date.atStartOfDay(zone)
    val end = date.plusDays(1).atStartOfDay(zone)
    return generateSequence(start) { it.plusMinutes(30) }
        .takeWhile { it.isBefore(end) }.take(96).map { it.toOffsetDateTime() }.toList()
}

internal fun parseDaylight(body: JSONObject, scenario: RoomDaylightScenario): List<RoomDaylightSample> {
    require(body.getString("source") == "simulation" && body.getInt("revision") == scenario.revision)
    require(body.optBoolean("historical") == scenario.historical)
    require(body.isNull("illuminance_lux"))
    require(body.getDouble("plane_height_m") == scenario.height)
    val samples = body.getJSONArray("samples")
    require(samples.length() == scenario.times.size)
    return List(samples.length()) { index ->
        val sample = samples.getJSONObject(index)
        val at = OffsetDateTime.parse(sample.getString("at"))
        require(at == scenario.times[index])
        fun polygon(vertices: JSONArray, maximum: Int): List<Pair<Double, Double>> {
            require(vertices.length() == 0 || vertices.length() in 3..maximum)
            return List(vertices.length()) { vertex ->
                val point = vertices.getJSONArray(vertex)
                require(point.length() == 2)
                val x = point.getDouble(0)
                val y = point.getDouble(1)
                require(x.isFinite() && y.isFinite() && x >= -1e-8 && y >= -1e-8)
                x.coerceAtLeast(0.0) to y.coerceAtLeast(0.0)
            }
        }
        RoomDaylightSample(
            at = at,
            direct = sample.getBoolean("direct_ray"),
            elevation = sample.getDouble("sun_elevation_deg").also { require(it in -90.0..90.0) },
            shadow = polygon(sample.optJSONArray("shadow_xy_m") ?: JSONArray(), 16),
            footprint = polygon(sample.getJSONArray("footprint_xy_m"), 8),
        )
    }
}


/** The server projects a convex polygon. Test the same plane at another point in metres. */
internal fun inDirectSunFootprint(
    point: Pair<Double, Double>,
    polygon: List<Pair<Double, Double>>,
): Boolean {
    if (polygon.size < 3 || !point.first.isFinite() || !point.second.isFinite() ||
        polygon.any { !it.first.isFinite() || !it.second.isFinite() }) return false
    val (x, y) = point
    var followsClockwise = false
    var followsCounterclockwise = false
    var twiceArea = 0.0
    polygon.forEachIndexed { index, a ->
        val b = polygon[(index + 1) % polygon.size]
        val cross = (b.first - a.first) * (y - a.second) - (b.second - a.second) * (x - a.first)
        twiceArea += a.first * b.second - b.first * a.second
        if (cross > 1e-10) followsClockwise = true
        if (cross < -1e-10) followsCounterclockwise = true
        if (followsClockwise && followsCounterclockwise) return false
    }
    return kotlin.math.abs(twiceArea) > 1e-10
}


/** Same time samples, aperture, plane and reported screen. Counts are not hours or lux. */
internal fun sampledDirectRay(sample: RoomDaylightSample, point: Pair<Double, Double>,
                              submittedPoint: Pair<Double, Double>?): Boolean =
    if (sample.elevation <= 0) false
    else if (point == submittedPoint) sample.direct
    else inDirectSunFootprint(point, sample.footprint) && !inDirectSunFootprint(point, sample.shadow)

internal data class DaylightPointComparison(val referenceHits: Int, val candidateHits: Int, val sampleCount: Int)

internal fun compareDaylightPoints(samples: List<RoomDaylightSample>, reference: Pair<Double, Double>,
                                   candidate: Pair<Double, Double>, submittedPoint: Pair<Double, Double>?): DaylightPointComparison? {
    if (samples.isEmpty() || samples.size > 96 || samples.zipWithNext().any { (a,b) -> !a.at.isBefore(b.at) } ||
        listOf(reference,candidate).any { !it.first.isFinite() || !it.second.isFinite() }) return null
    return DaylightPointComparison(
        samples.count { sampledDirectRay(it,reference,submittedPoint) },
        samples.count { sampledDirectRay(it,candidate,submittedPoint) },
        samples.size,
    )
}
