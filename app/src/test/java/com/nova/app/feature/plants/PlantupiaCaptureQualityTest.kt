package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlantupiaCaptureQualityTest {
    @Test fun classifiesExposureAndDetailIndependently() {
        assertEquals(PlantupiaCaptureQuality.DARK, captureQuality(100, 80) { _, _ -> 24 })
        assertEquals(PlantupiaCaptureQuality.GLARE, captureQuality(100, 80) { _, _ -> 250 })
        assertEquals(PlantupiaCaptureQuality.LOW_DETAIL, captureQuality(100, 80) { _, _ -> 110 })
        assertEquals(PlantupiaCaptureQuality.READY, captureQuality(100, 80) { x, y ->
            if ((x + y) % 2 == 0) 90 else 150
        })
    }

    @Test fun ignoresOuterFrameAndAbstainsOnTinyImages() {
        val result = captureQuality(100, 100) { x, y ->
            if (x in 20 until 80 && y in 20 until 80) 100 else 0
        }
        assertEquals(PlantupiaCaptureQuality.LOW_DETAIL, result)
        assertNull(captureQuality(2, 3) { _, _ -> error("No pixels should be read") })
    }
}
