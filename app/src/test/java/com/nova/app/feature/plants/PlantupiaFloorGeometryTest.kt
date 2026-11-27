package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlantupiaFloorGeometryTest {
    private val rectangle=listOf(FloorMark(1.0,0.0,2.0),FloorMark(5.0,0.0,2.0),
        FloorMark(5.0,0.0,5.0),FloorMark(1.0,0.0,5.0))
    @Test fun rectangularFloorReturnsMeasuredAxes() {
        val result=estimateRectangularFloor(rectangle)
        assertNotNull(result)
        assertEquals(4.0,result!!.width,.0001)
        assertEquals(3.0,result.length,.0001)
        assertEquals(0.0,result.closure,.0001)
        assertNotNull(estimateRectangularFloor(rectangle.reversed()))
    }
    @Test fun refusesCrossingNonPlanarDuplicateAndDistortedMarks() {
        assertNull(estimateRectangularFloor(listOf(rectangle[0],rectangle[2],rectangle[1],rectangle[3])))
        assertNull(estimateRectangularFloor(rectangle.mapIndexed { i,p -> if(i==2)p.copy(y=.2) else p }))
        assertNull(estimateRectangularFloor(rectangle.mapIndexed { i,p -> if(i==2)rectangle[1] else p }))
        assertNull(estimateRectangularFloor(rectangle.mapIndexed { i,p -> if(i==2)p.copy(x=6.0) else p }))
        assertNull(estimateRectangularFloor(rectangle.take(3)))
        assertNull(estimateRectangularFloor(rectangle.mapIndexed { i,p -> if(i==0)p.copy(x=Double.NaN) else p }))
    }
}
