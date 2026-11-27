package com.nova.app.feature.plants

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

internal data class FloorMark(val x: Double, val y: Double, val z: Double) {
    fun finite() = x.isFinite() && y.isFinite() && z.isFinite()
}
internal data class FloorSurvey(val width: Double, val length: Double, val closure: Double,
    val oppositeDisagreement: Double, val rightAngleError: Double)

/** Four tracked corners in order. All limits are consistency gates, not accuracy claims. */
internal fun estimateRectangularFloor(marks: List<FloorMark>): FloorSurvey? {
    if (marks.size != 4 || marks.any { !it.finite() }) return null
    if (marks.maxOf { it.y } - marks.minOf { it.y } > .07) return null
    val edges = marks.indices.map { i ->
        val a = marks[i]; val b = marks[(i+1)%4]
        (b.x-a.x) to (b.z-a.z)
    }
    val sides = edges.map { (x,z) -> hypot(x,z) }
    if (sides.any { it !in .3..30.0 }) return null
    val width = (sides[0]+sides[2])/2
    val length = (sides[1]+sides[3])/2
    val opposite = max(abs(sides[0]-sides[2])/width, abs(sides[1]-sides[3])/length)
    val cross = edges.indices.map { i ->
        val (x,z) = edges[i]; val (u,v) = edges[(i+1)%4]
        x*v-z*u
    }
    if (!(cross.all { it > 0 } || cross.all { it < 0 })) return null
    val angle = edges.indices.maxOf { i ->
        val (x,z) = edges[i]; val (u,v) = edges[(i+1)%4]
        abs(x*u+z*v)/(sides[i]*sides[(i+1)%4])
    }
    val predicted = FloorMark(marks[1].x+marks[3].x-marks[0].x, marks[0].y,
        marks[1].z+marks[3].z-marks[0].z)
    val closure = hypot(predicted.x-marks[2].x, predicted.z-marks[2].z)
    if (opposite > .06 || angle > .12 || closure > max(.10, .04*max(width,length))) return null
    return FloorSurvey(width,length,closure,opposite,angle)
}
