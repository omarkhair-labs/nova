package com.nova.app.feature.plants

import kotlin.math.hypot

internal data class VisitPoint(val x: Double, val y: Double)

internal data class VisitTransform(val a: Double, val b: Double, val x: Double, val y: Double) {
    fun project(point: VisitPoint) = VisitPoint(a * point.x - b * point.y + x, b * point.x + a * point.y + y)
}

/** Fit three static marks and validate against a fourth mark the fit never sees. */
internal fun fitVisitAlignment(earlier: List<VisitPoint>, later: List<VisitPoint>): VisitTransform? {
    if (earlier.size != 4 || later.size != 4) return null
    if ((earlier + later).any { !it.x.isFinite() || !it.y.isFinite() }) return null
    val target = earlier.take(3)
    val source = later.take(3)
    if (!spansView(earlier) || !spansView(later)) return null
    val targetCenter = VisitPoint(target.map { it.x }.average(), target.map { it.y }.average())
    val sourceCenter = VisitPoint(source.map { it.x }.average(), source.map { it.y }.average())
    var dot = 0.0
    var cross = 0.0
    var norm = 0.0
    source.zip(target).forEach { (from, to) ->
        val sx = from.x - sourceCenter.x
        val sy = from.y - sourceCenter.y
        val tx = to.x - targetCenter.x
        val ty = to.y - targetCenter.y
        dot += sx * tx + sy * ty
        cross += sx * ty - sy * tx
        norm += sx * sx + sy * sy
    }
    val a = dot / norm
    val b = cross / norm
    if (hypot(a, b) !in 0.65..1.55) return null
    val transform = VisitTransform(
        a, b,
        targetCenter.x - a * sourceCenter.x + b * sourceCenter.y,
        targetCenter.y - b * sourceCenter.x - a * sourceCenter.y,
    )
    // Coordinates are fractions of viewport width on both axes.
    if (source.zip(target).any { (input, expected) -> distance(transform.project(input), expected) > 0.025 }) return null
    if (distance(transform.project(later.last()), earlier.last()) > 0.035) return null
    return transform
}

private fun spansView(points: List<VisitPoint>): Boolean {
    val (a, b, c) = points.take(3)
    val twiceArea = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
    return kotlin.math.abs(twiceArea) >= 0.015 &&
        points.indices.all { i -> points.drop(i + 1).all { distance(points[i], it) >= 0.06 } }
}

private fun distance(a: VisitPoint, b: VisitPoint) = hypot(a.x - b.x, a.y - b.y)
