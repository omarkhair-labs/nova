package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlantupiaVisitAlignmentTest {
    private val points = listOf(
        VisitPoint(-.32, -.22),
        VisitPoint(.25, -.21),
        VisitPoint(-.18, .25),
        VisitPoint(.30, .20),
    )

    @Test fun recoversRotationScaleAndTranslationWithAnIndependentCheck() {
        val known = VisitTransform(.93, .19, .07, -.04)
        val recovered = fitVisitAlignment(points.map(known::project), points)
        assertNotNull(recovered)
        val actual = requireNotNull(recovered)
        assertEquals(known.a, actual.a, 1e-10)
        assertEquals(known.b, actual.b, 1e-10)
        assertEquals(known.x, actual.x, 1e-10)
        assertEquals(known.y, actual.y, 1e-10)
    }

    @Test fun rejectsMovingReferencesUnstableGeometryAndDuplicatedCheck() {
        val displaced = points.dropLast(1) + VisitPoint(.13, .20)
        val line = points.map { VisitPoint(it.x, .05) }
        assertNull(fitVisitAlignment(points, displaced))
        assertNull(fitVisitAlignment(points, line))
        assertNull(fitVisitAlignment(points.dropLast(1) + points.first(), points))
        assertNull(fitVisitAlignment(points, points.take(2)))
    }
}
