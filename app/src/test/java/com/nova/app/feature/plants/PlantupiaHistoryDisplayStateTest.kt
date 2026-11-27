package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Test

class PlantupiaHistoryDisplayStateTest {
    @Test fun failedFetchCannotMasqueradeAsNoRecords() {
        assertEquals(
            PlantupiaHistoryDisplayState.UNAVAILABLE,
            plantupiaHistoryDisplayState(loading = false, failed = true, recordCount = 0),
        )
    }

    @Test fun emptyIllustrationOnlyAfterAConfirmedEmptyFetch() {
        assertEquals(
            PlantupiaHistoryDisplayState.EMPTY,
            plantupiaHistoryDisplayState(loading = false, failed = false, recordCount = 0),
        )
        for (count in listOf(1, 7, 100)) {
            assertEquals(
                PlantupiaHistoryDisplayState.RECORDED,
                plantupiaHistoryDisplayState(loading = false, failed = false, recordCount = count),
            )
        }
    }

    @Test fun loadingDoesNotRenderThePreviousErrorOrStaleContent() {
        assertEquals(
            PlantupiaHistoryDisplayState.LOADING,
            plantupiaHistoryDisplayState(loading = true, failed = true, recordCount = 0),
        )
        assertEquals(
            PlantupiaHistoryDisplayState.LOADING,
            plantupiaHistoryDisplayState(loading = true, failed = false, recordCount = 4),
        )
    }

    @Test fun failureDoesNotMisrepresentPreviouslyLoadedEventsAsVerified() {
        assertEquals(
            PlantupiaHistoryDisplayState.UNAVAILABLE,
            plantupiaHistoryDisplayState(loading = false, failed = true, recordCount = 2),
        )
    }
}
