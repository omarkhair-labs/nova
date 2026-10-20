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

    @Test fun newReviewOutcomeBringsItsCareMomentToTopOfTimeline() {
        val care = event("old-care", "care", "2026-09-01T08:00:00Z")
        val mid = event("later-observation", "observation", "2026-10-01T08:00:00Z")
        val outcome = event("just-reviewed", "outcome", "2026-10-13T08:00:00Z", parent = care.id)
        val moments = groupPlantupiaHistory(listOf(care, mid, outcome))
        assertEquals(listOf("old-care", "later-observation"), moments.map { it.event.id })
        assertEquals(listOf("just-reviewed"), moments.first().outcomes.map { it.id })
    }

    @Test fun unrelatedHistoryRemainsSortedByObservedDate() {
        val old = event("old", "observation", "2025-09-01T11:00:00Z")
        val recent = event("recent", "issue", "2026-10-02T11:00:00Z")
        val moments = groupPlantupiaHistory(listOf(old, recent))
        assertEquals(listOf("recent", "old"), moments.map { it.event.id })
    }
    @Test fun historyFiltersKeepLinkedOutcomesVisibleInTheirOwnCategory() {
        val care = event("care", "care", "2026-10-01T08:00:00Z")
        val outcome = event("outcome", "outcome", "2026-10-05T09:00:00Z", parent = care.id)
        val issue = event("issue", "issue", "2026-10-02T08:00:00Z")
        val notes = event("note", "observation", "2026-10-03T08:00:00Z")
        val entries = listOf(care, outcome, issue, notes)
        assertEquals(1, filteredPlantupiaHistory(entries, PlantupiaHistoryFilter.ALL).count {
            it.event.id == "care"
        })
        assertEquals(listOf("care"),
            filteredPlantupiaHistory(entries, PlantupiaHistoryFilter.CARE).map { it.event.id })
        assertEquals(listOf("outcome"),
            filteredPlantupiaHistory(entries, PlantupiaHistoryFilter.OUTCOMES).map { it.event.id })
        assertEquals(listOf("issue"),
            filteredPlantupiaHistory(entries, PlantupiaHistoryFilter.CONCERNS).map { it.event.id })
        assertEquals(listOf("note"),
            filteredPlantupiaHistory(entries, PlantupiaHistoryFilter.OBSERVATIONS).map { it.event.id })
    }

    @Test fun photoFilterNeverClaimsOrHidesAUserUploadedImage() {
        val withPhoto = event("actual-photo", "issue", "2026-10-04T09:00:00Z", photo = "https://qa.invalid/real.jpg")
        val withoutPhoto = event("plain", "observation", "2026-10-05T09:00:00Z")
        val result = filteredPlantupiaHistory(listOf(withoutPhoto, withPhoto), PlantupiaHistoryFilter.PHOTOS)
        assertEquals(listOf("actual-photo"), result.map { it.event.id })
        assertTrue(result.single().outcomes.isEmpty())
    }

    @Test fun pagingFilterDoesNotInventContentForNoMatches() {
        val care = event("care", "care", "2026-10-01T08:00:00Z")
        assertTrue(filteredPlantupiaHistory(listOf(care), PlantupiaHistoryFilter.CONCERNS).isEmpty())
        assertTrue(filteredPlantupiaHistory(emptyList(), PlantupiaHistoryFilter.ALL).isEmpty())
    }

    @Test fun photoIndexShowsOnlyLoadedRealImagesNewestFirst() {
        val older = event("older", "observation", "2026-01-01T11:00:00Z", photo = "https://qa.invalid/older.jpg")
        val newer = event("newer", "care", "2026-10-01T09:00:00Z", photo = "https://qa.invalid/newer.jpg")
        val note = event("note", "observation", "2026-10-02T09:00:00Z")
        assertEquals(
            listOf("newer", "older"),
            recordedPlantPhotos(listOf(older, newer, note, newer)).map { it.id },
        )
    }

    @Test fun twoDatePhotoComparisonUsesRealDistinctLoadedImages() {
        val oldest = event("early", "observation", "2026-02-01T09:00:00Z", photo = "https://qa.invalid/early.jpg")
        val newest = event("latest", "care", "2026-10-03T12:00:00Z", photo = "https://qa.invalid/latest.jpg")
        val middle = event("middle", "observation", "2026-04-01T09:00:00Z", photo = "https://qa.invalid/middle.jpg")
        val pair = plantupiaLoadedPhotoPair(listOf(middle, newest, oldest, newest))
        assertEquals("early", pair?.earlier?.id)
        assertEquals("latest", pair?.later?.id)
    }

    @Test fun duplicatedImagesAndSameDaySnapshotsNeverClaimAComparison() {
        val first = event("first", "observation", "2026-10-03T08:00:00Z", photo = "https://qa.invalid/same.jpg")
        val repeated = event("repeated", "care", "2026-10-05T09:00:00Z", photo = "https://qa.invalid/same.jpg")
        assertEquals(null, plantupiaLoadedPhotoPair(listOf(first, repeated)))
        val other = event("second", "observation", "2026-10-05T19:00:00Z", photo = "https://qa.invalid/other.jpg")
        assertEquals(null, plantupiaLoadedPhotoPair(listOf(repeated, other)))
        assertEquals(null, plantupiaLoadedPhotoPair(listOf(first)))
    }

    @Test fun malformedHistoryDatesCannotCreateAFalseTimeComparison() {
        val fakeDate = event("bad-date", "care", "not-a-date", photo = "https://qa.invalid/a.jpg")
        val actual = event("actual", "observation", "2026-10-19T08:00:00Z", photo = "https://qa.invalid/b.jpg")
        assertEquals(null, plantupiaLoadedPhotoPair(listOf(fakeDate, actual)))
    }

    @Test fun photoComparisonDoesNotInferPlantGrowthFromCareRecords() {
        val withoutPhoto = event("note", "observation", "2025-01-01T08:00:00Z")
        val later = event("late", "care", "2026-10-11T08:00:00Z", photo = "https://qa.invalid/real.jpg")
        assertEquals(null, plantupiaLoadedPhotoPair(listOf(withoutPhoto, later)))
    }

    @Test fun photoIndexCanTruthfullyBeEmptyOrHaveOnlyOnePhoto() {
        val noPhoto = event("text", "care", "2026-10-02T09:00:00Z")
        val image = event("photo", "care", "2026-10-03T09:00:00Z", photo = "https://qa.invalid/photo.jpg")
        assertTrue(recordedPlantPhotos(listOf(noPhoto)).isEmpty())
        assertEquals(1, recordedPlantPhotos(listOf(noPhoto, image)).size)
    }

}
