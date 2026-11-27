package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaLiveLensTest {
    private val leaf = liveLensPixel(116, 91, 102)
    private val background = liveLensPixel(190, 128, 128)
    private val n = 80
    private fun ellipse(cx: Int = 40, cy: Int = 40, rx: Int = 22, ry: Int = 15,
                        inside: Int = leaf, outside: Int = background): IntArray =
        IntArray(n * n) { i ->
            val x = i % n
            val y = i / n
            if ((x - cx).toDouble() * (x - cx) / (rx * rx) +
                (y - cy).toDouble() * (y - cy) / (ry * ry) <= 1) inside else outside
        }
    private fun propose(data: IntArray) = proposeLiveLensRegion(n, n, data)

    @Test fun outlinesActualCentralRegionRatherThanAFixedGuide() {
        val data = ellipse()
        val result = requireNotNull(propose(data))
        var intersection = 0
        var union = 0
        for (y in 0 until n) for (x in 0 until n) {
            val expected = data[y * n + x] == leaf
            val selected = result.contains(x, y)
            if (expected && selected) intersection++
            if (expected || selected) union++
        }
        assertTrue(intersection.toDouble() / union > .98)
        assertTrue(result.contains(40, 40))
        assertTrue(!result.contains(0, 0))
        val shifted = requireNotNull(propose(ellipse(cx = 47)))
        assertTrue(!shifted.contains(19, 40))
        assertTrue(shifted.contains(68, 40))
    }

    @Test fun refusesUniformLowContrastDarkAndCroppedFrames() {
        assertNull(propose(IntArray(n*n) { leaf }))
        assertNull(propose(ellipse(outside = liveLensPixel(120, 95, 105))))
        assertNull(propose(ellipse(inside = liveLensPixel(20, 91, 102))))
        assertNull(propose(ellipse(rx = 41)))
        assertNull(propose(IntArray(n*n) { if ((it / n + it % n) % 2 == 0) leaf else background }))
    }

    @Test fun refusesSeveralSimilarlyColouredRegions() {
        val data = ellipse(rx = 13, ry = 13)
        for (y in 7..32) for (x in 5..18) data[y*n+x] = leaf
        assertNull(propose(data))
    }

    @Test fun requiresAgreementAndDropsLostOrChangedRegions() {
        val a = requireNotNull(propose(ellipse()))
        val b = requireNotNull(propose(ellipse(cx = 42)))
        val c = requireNotNull(propose(ellipse(rx = 10, ry = 10)))
        assertNull(stableLiveLensRegion(null, a))
        assertNotNull(stableLiveLensRegion(a, b))
        assertNull(stableLiveLensRegion(a, c))
        assertNull(stableLiveLensRegion(a, null))
        assertEquals(1f, a.overlap(a), 0f)
    }
}
