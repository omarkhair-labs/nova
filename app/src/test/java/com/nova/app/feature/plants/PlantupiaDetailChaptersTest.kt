package com.nova.app.feature.plants

import com.nova.app.feature.plants.data.PlantupiaCareTopic
import org.junit.Assert.*
import org.junit.Test

class PlantupiaDetailChaptersTest {
    @Test fun plantOpensAtOverviewUnlessExplicitObservationIntent() {
        assertEquals(PlantupiaDetailChapter.OVERVIEW, plantupiaOpeningChapter(false))
        assertEquals(PlantupiaDetailChapter.CARE, plantupiaOpeningChapter(true))
    }

    @Test fun chaptersSelectRealFocusWithoutInventingAnEventOrNewIdentity() {
        assertEquals(
            PlantupiaDetailChapter.HISTORY,
            plantupiaNextChapter(PlantupiaDetailChapter.OVERVIEW,
                PlantupiaDetailChapter.HISTORY),
        )
        assertEquals(
            PlantupiaDetailChapter.CARE,
            plantupiaNextChapter(PlantupiaDetailChapter.CARE,
                PlantupiaDetailChapter.CARE),
        )
        assertEquals(3, PlantupiaDetailChapter.entries.size)
    }

    private fun reviewed(key: String) = PlantupiaCareTopic(
        topic = key, title = "Reviewed " + key,
        instructions = "Human-approved chapter content.",
        contextNotes = "Approved context only.",
        sourceUrl = "https://example.org/reviewed", sourceReference = "Botanical source",
        position = 1,
    )

    @Test fun aReaderCanOnlyOpenActualLoadedReviewedTopics() {
        val available = listOf(reviewed("water"), reviewed("light"))
        assertEquals("light", selectedReviewedCareTopic(available, "light")?.topic)
        assertEquals("water", selectedReviewedCareTopic(available, null)?.topic)
        assertEquals("water", selectedReviewedCareTopic(available, "no_such_chapter")?.topic)
        assertNull(selectedReviewedCareTopic(emptyList(), "water"))
    }
}
