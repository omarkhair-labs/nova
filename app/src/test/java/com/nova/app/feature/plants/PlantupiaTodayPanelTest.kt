package com.nova.app.feature.plants

import com.nova.app.feature.plants.data.PlantupiaPlant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaTodayPanelTest {
    private fun plant(id: String, date: String, active: Boolean = true) = PlantupiaPlant(
        id = id, spaceId = "office", label = id, name = id,
        species = "", placement = "", coverPhoto = "",
        active = active, canRecord = true, lastEventKind = "",
        lastEventAt = "", nextFollowUpOn = date,
    )

    @Test fun onlyActualActiveScheduledCheckInsAppear() {
        val entries = listOf(
            plant("upcoming", "2026-10-20"),
            plant("due", "2026-10-13"),
            plant("retired", "2026-10-01", active = false),
            plant("unknown", ""),
            plant("corrupt", "tomorrow"),
        )
        assertEquals(listOf("due", "upcoming"), scheduledPlantCheckIns(entries).map { it.id })
    }

    @Test fun noSyntheticScheduleWhenNoFollowupsExist() {
        assertTrue(scheduledPlantCheckIns(listOf(plant("new", ""))).isEmpty())
    }

    @Test fun dueCheckInsCanBeRevealedBeyondTheFirstThreeInLargeSpaces() {
        val records = (1..100).map { plant("plant-$it", "2026-10-13") }
        val due = scheduledPlantCheckIns(records)
        assertEquals(100, due.size)
        assertEquals(3, visibleDueCheckInCount(due.size, 3))
        assertEquals(11, visibleDueCheckInCount(due.size, 11))
        assertEquals(100, visibleDueCheckInCount(due.size, 107))
        assertEquals(100, due.take(visibleDueCheckInCount(due.size, 107)).distinctBy { it.id }.size)
    }

    @Test fun dueCountClampsEmptyAndSmallInventories() {
        assertEquals(0, visibleDueCheckInCount(0, 3))
        assertEquals(1, visibleDueCheckInCount(1, 3))
        assertEquals(2, visibleDueCheckInCount(2, -20))
    }

}
