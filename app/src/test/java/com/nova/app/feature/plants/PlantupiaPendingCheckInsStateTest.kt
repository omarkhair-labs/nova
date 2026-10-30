package com.nova.app.feature.plants

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaHistoryPage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PlantupiaPendingCheckInsStateTest {
    private fun page() = PlantupiaHistoryPage(listOf(PlantupiaCareEvent(
        id = "review", kind = "care", note = "Checked soil", evidence = "observed",
        observedAt = "2026-10-18T10:00:00Z", photoUrl = "", followUpOn = "2026-10-20",
        followUpCompletedAt = "",
    )), 20)

    @Test fun unavailableIsNeverEmpty() = runBlocking {
        val states = mutableListOf<PlantupiaPendingCheckInsState>()
        loadPlantupiaPendingCheckIns({ true }, { ApiResult.Failure("offline") }, states::add)
        assertEquals(listOf(PlantupiaPendingCheckInsState.Loading, PlantupiaPendingCheckInsState.Unavailable("offline")), states)
        assertFalse(states.contains(PlantupiaPendingCheckInsState.Empty))
    }

    @Test fun onlySuccessfulEmptyResponseShowsEmpty() = runBlocking {
        val states = mutableListOf<PlantupiaPendingCheckInsState>()
        loadPlantupiaPendingCheckIns({ true }, { ApiResult.Success(PlantupiaHistoryPage(emptyList(), null)) }, states::add)
        assertEquals(PlantupiaPendingCheckInsState.Empty, states.last())
    }

    @Test fun retryLoadsReviewsAndCursor() = runBlocking {
        var pending: PlantupiaPendingCheckInsState = PlantupiaPendingCheckInsState.Unavailable("offline")
        loadPlantupiaPendingCheckIns({ true }, { ApiResult.Success(page()) }, { pending = it })
        assertEquals(PlantupiaPendingCheckInsState.Loaded(page().events, 20), pending)
    }

    @Test fun backSwitchReopenOrAccountChangeRejectsLateRetry() = runBlocking {
        for (result in listOf(ApiResult.Success(page()), ApiResult.Failure("late error"))) {
            var current = true
            val response = CompletableDeferred<ApiResult<PlantupiaHistoryPage>>()
            val states = mutableListOf<PlantupiaPendingCheckInsState>()
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                loadPlantupiaPendingCheckIns({ current }, { response.await() }, states::add)
            }
            assertEquals(listOf(PlantupiaPendingCheckInsState.Loading), states)
            current = false
            response.complete(result)
            job.join()
            assertEquals(listOf(PlantupiaPendingCheckInsState.Loading), states)
        }
    }

    @Test fun staleRetryDoesNotEvenClearCurrentPendingState() = runBlocking {
        loadPlantupiaPendingCheckIns({ false }, { fail("must not request"); ApiResult.Success(page()) }, { fail("must not mutate") })
    }
}
