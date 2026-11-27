package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaAmbientLightTest {
    @Test fun actualEventsOnlyAndZeroIsAValidReading() {
        val w = AmbientLightWindow(1000L,20000f)
        assertNull(w.accept(120f,999L,3))
        assertNull(w.accept(Float.NaN,1000L,3))
        assertNull(w.accept(-1f,1000L,3))
        assertNull(w.accept(20001f,1000L,3))
        assertEquals(0f,w.accept(0f,1000L,3)!!.lastLux,0f)
        assertNull(w.accept(5f,1000L,3))
        val last=w.accept(120f,2000L,3)!!
        assertEquals(2,last.count)
        assertEquals(0f,last.minLux,0f)
        assertEquals(120f,last.maxLux,0f)
        assertFalse(last.lowAccuracy)
        assertFalse(last.nearSensorLimit)
    }

    @Test fun marksLimitsAndRejectsExpiredOrUnboundedSamples() {
        val w=AmbientLightWindow(1000L,1000f)
        assertTrue(w.accept(1000f,1000L,1)!!.nearSensorLimit)
        val second=w.accept(200f,2000L,3)!!
        assertTrue(second.lowAccuracy)
        assertTrue(second.nearSensorLimit)
        assertNull(w.accept(10f,6_000_001_001L,3))
        val bounded=AmbientLightWindow(0L,100f)
        repeat(120) { assertNotNull(bounded.accept(1f,it.toLong(),3)) }
        assertNull(bounded.accept(1f,120L,3))
    }
}
