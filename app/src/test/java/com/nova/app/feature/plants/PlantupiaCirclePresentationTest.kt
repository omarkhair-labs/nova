package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Test

/** Labels show actual public replies, never imply advice or an outcome exists. */
class PlantupiaCirclePresentationTest {
    @Test fun replyCopyIsHonestAtAllInventorySizes() {
        assertEquals("0 replies", circleReplyLabel(0))
        assertEquals("1 reply", circleReplyLabel(1))
        assertEquals("2 replies", circleReplyLabel(2))
        assertEquals("100 replies", circleReplyLabel(100))
    }

    @Test fun malformedNegativeReplyCountNeverAppearsToPeople() {
        assertEquals("0 replies", circleReplyLabel(-1))
    }
}
