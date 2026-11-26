package com.nova.app.feature.plants

import com.nova.app.feature.plants.data.RoomPlant
import org.junit.Assert.*
import org.junit.Test

class PlantupiaRoomHitTest {
    private val plants = listOf(RoomPlant("a", 1.0, 2.0, ""), RoomPlant("b", 4.0, 3.0, ""))

    @Test fun mapsScreenPointToClosestRecordedPlant() {
        assertEquals("a", nearestRoomPlant(plants, 35f, 65f, 15f, 25f, 20f, 12f))
        assertEquals("b", nearestRoomPlant(plants, 95f, 85f, 15f, 25f, 20f, 12f))
    }

    @Test fun emptyAndOutOfRangeTapsDoNotSelectAnything() {
        assertNull(nearestRoomPlant(plants, 0f, 0f, 15f, 25f, 20f, 12f))
        assertNull(nearestRoomPlant(emptyList(), 35f, 65f, 15f, 25f, 20f, 12f))
        assertNull(nearestRoomPlant(plants, 35f, 65f, 15f, 25f, 0f, 12f))
    }
}
