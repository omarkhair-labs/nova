package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Test

class PlantupiaPhotoGeometryTest {
    private fun limits(
        viewportWidth: Float, viewportHeight: Float,
        imageWidth: Float, imageHeight: Float, zoom: Float,
    ) = plantupiaPhotoPanLimits(viewportWidth, viewportHeight, imageWidth, imageHeight, zoom)

    @Test fun fitPortraitCannotBeDraggedSidewaysUntilItActuallyFillsWidth() {
        val l = limits(400f, 800f, 400f, 1200f, 2f)
        assertEquals(66.666f, l.maxX, .01f)
        // Original 400x1200 fits as 266.67x800; 2x becomes 533.33x1600.
        // Compare a *taller* portrait which remains narrower at 2x.
        val tall = limits(400f, 800f, 400f, 2000f, 2f)
        assertEquals(0f, tall.maxX, .001f)
        assertEquals(400f, tall.maxY, .001f)
    }

    @Test fun landscapePhotoGetsIndependentPanBounds() {
        val l = limits(400f, 800f, 1600f, 800f, 2f)
        assertEquals(200f, l.maxX, .001f)
        assertEquals(0f, l.maxY, .001f)
    }

    @Test fun squarePhotoClampsAtActualRenderedExtentNotMagicPixels() {
        val l = limits(360f, 640f, 1000f, 1000f, 3f)
        assertEquals(360f, l.maxX, .001f)
        assertEquals(220f, l.maxY, .001f)
        assertEquals(360f, plantupiaClampPhotoPan(900f, l.maxX), .001f)
        assertEquals(-220f, plantupiaClampPhotoPan(-1000f, l.maxY), .001f)
    }

    @Test fun unzoomedAndInvalidSizesHaveNoPan() {
        assertEquals(PlantupiaPhotoPanLimits(0f, 0f), limits(400f, 800f, 800f, 400f, 1f))
        assertEquals(PlantupiaPhotoPanLimits(0f, 0f), limits(0f, 800f, 800f, 400f, 3f))
        assertEquals(PlantupiaPhotoPanLimits(0f, 0f), limits(400f, 800f, -1f, 400f, 2f))
        assertEquals(PlantupiaPhotoPanLimits(0f, 0f), limits(400f, 800f, 800f, 400f, Float.NaN))
        assertEquals(0f, plantupiaClampPhotoPan(Float.NaN, 100f), .001f)
    }
}
