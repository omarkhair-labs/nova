package com.nova.app.feature.plants

import com.nova.app.feature.plants.data.PlantupiaCareEvent
import org.junit.Assert.*
import org.junit.Test

class PlantupiaSignatureJournalTest {
    private fun event(id: String, date: String, note: String = id, photo: String = "", kind: String = "observation", parent: String = "") =
        PlantupiaCareEvent(id = id, kind = kind, note = note, evidence = "observed",
            observedAt = date, photoUrl = photo, followUpOn = "", followUpCompletedAt = "", followUpFor = parent)

    @Test fun searchIncludesLinkedOutcomeWithoutBreakingItsParent() {
        val care = event("a", "2026-11-01T09:00:00Z", "Checked soil", kind = "care")
        val outcome = event("b", "2026-11-03T09:00:00Z", "Dry leaf", kind = "outcome", parent = "a")
        val unrelated = event("c", "2026-11-05T09:00:00Z", "Moved pot")
        val moments = groupPlantupiaHistory(listOf(care, outcome, unrelated))
        assertEquals(listOf("a"), searchLoadedPlantupiaHistory(moments, "DRY 2026-11-03").map { it.event.id })
        assertEquals("b", searchLoadedPlantupiaHistory(moments, "dry").single().outcomes.single().id)
        assertTrue(searchLoadedPlantupiaHistory(moments, "missing").isEmpty())
        assertEquals(moments, searchLoadedPlantupiaHistory(moments, "  "))
    }

    @Test fun customPairUsesOnlyDistinctLoadedPhotosAndOrdersRealDates() {
        val earlier = event("a", "2026-11-01T09:00:00Z", photo = "https://qa.invalid/a.jpg")
        val middle = event("b", "2026-11-04T09:00:00Z", photo = "https://qa.invalid/b.jpg")
        val latest = event("c", "2026-11-09T09:00:00Z", photo = "https://qa.invalid/c.jpg")
        val events = listOf(earlier, middle, latest)
        val pair = signatureComparisonPair(events, "c", "b")
        assertEquals("b", pair?.earlier?.id)
        assertEquals("c", pair?.later?.id)
        assertNull(signatureComparisonPair(events, "b", "b"))
        assertNull(signatureComparisonPair(events, "b", "unloaded"))
        assertNull(signatureComparisonPair(events, null, "a"))
        assertNull(signatureComparisonPair(events + earlier.copy(id = "repeat", observedAt = latest.observedAt), "a", "repeat"))
        assertNull(signatureComparisonPair(events + earlier.copy(id = "same-day", photoUrl = middle.photoUrl), "a", "same-day"))
        assertNull(signatureComparisonPair(events + earlier.copy(id = "bad-date", observedAt = "invalid", photoUrl = latest.photoUrl), "a", "bad-date"))
    }
}
