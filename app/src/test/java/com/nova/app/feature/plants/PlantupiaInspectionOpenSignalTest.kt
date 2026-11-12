package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PlantupiaInspectionOpenSignalTest {
    @Test fun malformedOrUnboundNotificationCannotQueuePrivateTask() {
        PlantupiaInspectionOpenSignal.consume()
        PlantupiaInspectionOpenSignal.open(0L, UUID.randomUUID().toString(), UUID.randomUUID().toString())
        assertNull(PlantupiaInspectionOpenSignal.target)
        PlantupiaInspectionOpenSignal.open(7L, "not-a-uuid", UUID.randomUUID().toString())
        assertNull(PlantupiaInspectionOpenSignal.target)
        PlantupiaInspectionOpenSignal.open(7L, UUID.randomUUID().toString(), "bad-plan-id")
        assertNull(PlantupiaInspectionOpenSignal.target)
    }

    @Test fun validPlanHintKeepsAccountAndExactPlantAndPlanIdsUntilConsumed() {
        PlantupiaInspectionOpenSignal.consume()
        val plant = UUID.randomUUID().toString()
        val plan = UUID.randomUUID().toString()
        PlantupiaInspectionOpenSignal.open(7L, plant, plan)
        assertEquals(Triple(7L, plant, plan), PlantupiaInspectionOpenSignal.target)
        PlantupiaInspectionOpenSignal.consume()
        assertNull(PlantupiaInspectionOpenSignal.target)
    }
}
