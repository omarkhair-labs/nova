package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlantupiaPhotoWipeTest {
    @Test fun physicalDragAndAccessibleFractionAgreeInBothDirections() {
        assertEquals(.25f, photoWipeFraction(100f, 400f, false)!!, .0001f)
        assertEquals(.75f, photoWipeFraction(100f, 400f, true)!!, .0001f)
    }

    @Test fun draggingBeyondTheImageClampsWithoutInvalidState() {
        assertEquals(0f, photoWipeFraction(-10f, 400f, false)!!, .0001f)
        assertEquals(1f, photoWipeFraction(450f, 400f, false)!!, .0001f)
        assertEquals(1f, photoWipeFraction(-10f, 400f, true)!!, .0001f)
        assertEquals(0f, photoWipeFraction(450f, 400f, true)!!, .0001f)
        assertNull(photoWipeFraction(0f, 0f, false))
        assertNull(photoWipeFraction(Float.NaN, 400f, false))
    }
}
