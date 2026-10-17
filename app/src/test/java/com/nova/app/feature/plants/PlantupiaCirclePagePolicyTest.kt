package com.nova.app.feature.plants

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fresh Circle results must never merge with a prior fetch, even if it finishes later. */
class PlantupiaCirclePagePolicyTest {
    @Test fun currentPageMayUpdateTheVisibleCircle() {
        assertTrue(isCurrentCirclePageRequest(requestRevision = 0, visibleRevision = 0))
        assertTrue(isCurrentCirclePageRequest(requestRevision = 12, visibleRevision = 12))
    }

    @Test fun previousPageCannotAppendAfterRefreshing() {
        assertFalse(isCurrentCirclePageRequest(requestRevision = 3, visibleRevision = 4))
    }

    @Test fun multipleRapidRefreshesDiscardEveryEarlierResult() {
        assertFalse(isCurrentCirclePageRequest(requestRevision = 5, visibleRevision = 7))
        assertFalse(isCurrentCirclePageRequest(requestRevision = 6, visibleRevision = 7))
        assertTrue(isCurrentCirclePageRequest(requestRevision = 7, visibleRevision = 7))
    }
}
