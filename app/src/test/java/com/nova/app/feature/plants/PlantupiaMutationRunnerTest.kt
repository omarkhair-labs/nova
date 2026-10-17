package com.nova.app.feature.plants

import com.nova.app.core.network.ApiResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PlantupiaMutationRunnerTest {
    private class Screen {
        var account = "A-login-1"
        var plant: String? = "plant-A"
        var generation = 1
        var busy = false
        var writes = 0
        var committed = 0
        var error: String? = null
        val lifetime = PlantupiaCallbackLifetime { account == "A-login-1" }
        fun current() = lifetime.isCurrent() && plant == "plant-A" && generation == 1
        suspend fun save(response: CompletableDeferred<ApiResult<String>>) = runPlantupiaMutation(
            isAccountCurrent = lifetime::isCurrent,
            isDetailCurrent = ::current,
            request = { response.await() },
            onStarted = { busy = true },
            onCommitted = { committed++ },
            onSuccess = { writes++ },
            onFailure = { error = it.message },
            onFinished = { busy = false },
        )
    }

    @Test fun confirmedSaveUpdatesCurrentScreen() = runBlocking {
        val screen = Screen()
        val response = CompletableDeferred<ApiResult<String>>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { screen.save(response) }
        assertTrue(screen.busy)
        response.complete(ApiResult.Success("server-event"))
        job.join()
        assertEquals(1, screen.committed)
        assertEquals(1, screen.writes)
        assertFalse(screen.busy)
    }

    @Test fun backKeepsServerCommitButCannotReopenPlantOrClearNewBusyState() = runBlocking {
        val screen = Screen()
        val response = CompletableDeferred<ApiResult<String>>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { screen.save(response) }
        screen.plant = null
        screen.generation++
        screen.busy = true // A newer operation owns the current screen.
        response.complete(ApiResult.Success("saved-after-back"))
        job.join()
        assertEquals(1, screen.committed)
        assertEquals(0, screen.writes)
        assertNull(screen.plant)
        assertTrue(screen.busy)
    }

    @Test fun switchAndReopenRejectBothLateSuccessAndFailure() = runBlocking {
        for (reopen in listOf(false, true)) {
            for (result in listOf(ApiResult.Success("saved"), ApiResult.Failure("old-error"))) {
                val screen = Screen()
                val response = CompletableDeferred<ApiResult<String>>()
                val job = launch(start = CoroutineStart.UNDISPATCHED) { screen.save(response) }
                screen.plant = if (reopen) "plant-A" else "plant-B"
                screen.generation += 2
                response.complete(result)
                job.join()
                assertEquals(0, screen.writes)
                assertNull(screen.error)
            }
        }
    }

    @Test fun accountSwitchAndNewLoginRejectReminderSideEffectsAndUi() = runBlocking {
        for (newAccount in listOf("B-login-1", "A-login-2", "signed-out")) {
            val screen = Screen()
            val response = CompletableDeferred<ApiResult<String>>()
            val job = launch(start = CoroutineStart.UNDISPATCHED) { screen.save(response) }
            screen.account = newAccount
            response.complete(ApiResult.Success("A-event"))
            job.join()
            assertEquals(0, screen.committed)
            assertEquals(0, screen.writes)
        }
    }

    @Test fun disposedQuestionPanelCannotNavigateOrRefreshParent() = runBlocking {
        val screen = Screen()
        val response = CompletableDeferred<ApiResult<String>>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { screen.save(response) }
        screen.lifetime.dispose()
        response.complete(ApiResult.Success("public-post"))
        job.join()
        assertEquals(0, screen.committed)
        assertEquals(0, screen.writes)
    }

    @Test fun currentFailurePreservesDraftAndReleasesBusy() = runBlocking {
        val screen = Screen()
        screen.save(CompletableDeferred(ApiResult.Failure("offline")))
        assertEquals("offline", screen.error)
        assertEquals(0, screen.writes)
        assertFalse(screen.busy)
    }

    @Test fun cancelledRequestReleasesOnlyItsCurrentBusyState() = runBlocking {
        val screen = Screen()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { screen.save(CompletableDeferred()) }
        job.cancel()
        job.join()
        assertFalse(screen.busy)
        assertEquals(0, screen.writes)
    }

    @Test fun staleOperationDoesNotStartARequest() = runBlocking {
        var requested = false
        runPlantupiaMutation(
            isAccountCurrent = { false }, isDetailCurrent = { true },
            request = { requested = true; ApiResult.Success(Unit) },
            onStarted = { fail("must not start") }, onSuccess = { fail("must not apply") },
            onFailure = { fail("must not apply") }, onFinished = { fail("must not finish") },
        )
        assertFalse(requested)
    }
}
