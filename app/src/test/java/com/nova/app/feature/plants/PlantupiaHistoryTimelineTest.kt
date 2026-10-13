package com.nova.app.feature.plants

import com.nova.app.feature.plants.data.PlantupiaCareEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaHistoryTimelineTest {
    private fun event(id: String, kind: String, time: String, parent: String = "", photo: String = "") =
        PlantupiaCareEvent(
            id = id, kind = kind, note = "Observed fact: " + id,
            evidence = "observed", observedAt = time, photoUrl = photo,
            followUpOn = if (kind == "care") "2026-10-20" else "",
            followUpCompletedAt = "",
            followUpFor = parent,
        )

    @Test fun outcomeIsDisplayedUnderOriginalCareWhenBothAreLoaded() {
        val care = event("water", "care", "2026-10-01T08:00:00Z")
        val result = event("result", "outcome", "2026-10-21T08:00:00Z", parent = care.id)
        val moments = groupPlantupiaHistory(listOf(result, care))
        assertEquals(listOf("water"), moments.map { it.event.id })
        assertEquals(listOf("result"), moments.first().outcomes.map { it.id })
    }

    @Test fun orphanOutcomeStaysVisibleUntilPreviousHistoryPageIsLoaded() {
        val result = event("result", "outcome", "2026-10-21T08:00:00Z", parent = "older-page")
        val moments = groupPlantupiaHistory(listOf(result))
        assertEquals(listOf("result"), moments.map { it.event.id })
        assertTrue(moments.single().outcomes.isEmpty())
    }

    @Test fun neverHideEvidencePhotosOrUnlinkedOutcomes() {
        val original = event("care", "care", "2026-10-01T08:00:00Z")
        val photoOutcome = event("photo-result", "outcome", "2026-10-11T08:00:00Z", parent = "care", photo = "https://qa.invalid/one.jpg")
        val other = event("other", "outcome", "2026-10-15T08:00:00Z")
        val moments = groupPlantupiaHistory(listOf(original, photoOutcome, other))
        assertEquals(3, moments.size)
        assertTrue(moments.all { it.outcomes.isEmpty() })
    }

    @Test fun pagingDuplicatesDoNotCreateDuplicateTimelineMoments() {
        val original = event("care", "care", "2026-10-01T08:00:00Z")
        val outcome = event("end", "outcome", "2026-10-03T08:00:00Z", parent = "care")
        val moments = groupPlantupiaHistory(listOf(original, outcome, original, outcome))
        assertEquals(1, moments.size)
        assertEquals(1, moments.first().outcomes.size)
    }

    @Test fun unrelatedHistoryRemainsSortedByObservedDate() {
        val old = event("old", "observation", "2025-09-01T11:00:00Z")
        val recent = event("recent", "issue", "2026-10-02T11:00:00Z")
        val moments = groupPlantupiaHistory(listOf(old, recent))
        assertEquals(listOf("recent", "old"), moments.map { it.event.id })
    }
}
