package com.nova.app.feature.memories

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.memories.data.MemoryRepository
import com.nova.app.feature.memories.domain.model.MemoryFilmPlan
import com.nova.app.feature.memories.domain.model.MemoryFilmScene
import com.nova.app.feature.memories.domain.model.WeeklyMemory
import com.nova.app.feature.memories.film.MemoryFilmExport
import com.nova.app.feature.memories.film.MemoryFilmExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class MemoryFilmStateOwnerTest {
    @Test
    fun `plan load stores requested week and bounds index`() = runBlocking {
        val expected = filmPlan(weeksAgo = 51)
        val repository = FakeFilmRepository(
            planResults = mutableListOf(ApiResult.Success(expected)),
        )
        val exporter = FakeFilmExporter()
        val owner = MemoryFilmStateOwner(
            repository,
            exporter,
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadPlanNow(utcOffsetMinutes = 180, weeksAgo = 80, showSpinner = true)

        assertEquals(expected, owner.state.plan)
        assertEquals(51, owner.state.weeksAgo)
        assertEquals(listOf(180 to 51), repository.planRequests)
        assertFalse(owner.state.loadingPlan)
        assertNull(owner.state.error)
    }

    @Test
    fun `film plan 401 emits terminal session expiry`() = runBlocking {
        val repository = FakeFilmRepository(
            planResults = mutableListOf(ApiResult.Failure("expired", 401)),
        )
        val owner = MemoryFilmStateOwner(
            repository,
            FakeFilmExporter(),
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.loadPlanNow(utcOffsetMinutes = 0, weeksAgo = 0, showSpinner = true)

        assertEquals(1, owner.state.sessionExpiryVersion)
        assertFalse(owner.state.loadingPlan)
        assertNull(owner.state.error)
    }

    @Test
    fun `successful export stores mp4 path and completed progress`() = runBlocking {
        val plan = filmPlan()
        val repository = FakeFilmRepository(
            planResults = mutableListOf(ApiResult.Success(plan)),
        )
        val exporter = FakeFilmExporter(
            exportResult = Result.success(
                MemoryFilmExport(
                    filePath = "/cache/nova-memory.mp4",
                    durationMs = plan.totalDurationMs,
                )
            ),
            progressValues = listOf(12, 64, 99),
        )
        val owner = MemoryFilmStateOwner(
            repository,
            exporter,
            CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadPlanNow(0, 0, showSpinner = true)

        owner.exportNow(plan)

        assertFalse(owner.state.exporting)
        assertEquals(100, owner.state.progress)
        assertEquals("/cache/nova-memory.mp4", owner.state.outputPath)
        assertNull(owner.state.error)
        assertEquals(listOf(plan), exporter.exportedPlans)
    }

    @Test
    fun `failed export releases render state and surfaces error`() = runBlocking {
        val plan = filmPlan()
        val exporter = FakeFilmExporter(
            exportResult = Result.failure(IllegalStateException("codec failed")),
        )
        val owner = MemoryFilmStateOwner(
            FakeFilmRepository(),
            exporter,
            CoroutineScope(Dispatchers.Unconfined),
        )

        owner.exportNow(plan)

        assertFalse(owner.state.exporting)
        assertEquals(0, owner.state.progress)
        assertNull(owner.state.outputPath)
        assertEquals("codec failed", owner.state.error)
    }

    @Test
    fun `cancel stops active exporter and releases progress state`() = runBlocking {
        val plan = filmPlan()
        val exporter = FakeFilmExporter(hangDuringExport = true)
        val repository = FakeFilmRepository(
            planResults = mutableListOf(ApiResult.Success(plan)),
        )
        val owner = MemoryFilmStateOwner(
            repository,
            exporter,
            CoroutineScope(Dispatchers.Unconfined),
        )
        owner.loadPlanNow(0, 0, showSpinner = true)

        owner.export()
        assertTrue(owner.state.exporting)

        owner.cancelExport()

        assertEquals(1, exporter.cancelCalls)
        assertFalse(owner.state.exporting)
        assertEquals(0, owner.state.progress)
    }
}


private class FakeFilmRepository(
    private val planResults: MutableList<ApiResult<MemoryFilmPlan>> = mutableListOf(),
) : MemoryRepository {
    val planRequests = mutableListOf<Pair<Int, Int>>()

    override suspend fun week(
        utcOffsetMinutes: Int,
        weeksAgo: Int,
    ): ApiResult<WeeklyMemory> = ApiResult.Failure("unused", 500)

    override suspend fun filmPlan(
        utcOffsetMinutes: Int,
        weeksAgo: Int,
    ): ApiResult<MemoryFilmPlan> {
        planRequests += utcOffsetMinutes to weeksAgo
        return planResults.removeFirst()
    }
}


private class FakeFilmExporter(
    private val exportResult: Result<MemoryFilmExport> = Result.failure(
        IllegalStateException("unused")
    ),
    private val progressValues: List<Int> = emptyList(),
    private val hangDuringExport: Boolean = false,
) : MemoryFilmExporter {
    val exportedPlans = mutableListOf<MemoryFilmPlan>()
    var cancelCalls: Int = 0
        private set

    override suspend fun export(
        plan: MemoryFilmPlan,
        onProgress: (Int) -> Unit,
    ): Result<MemoryFilmExport> {
        exportedPlans += plan
        progressValues.forEach(onProgress)
        if (hangDuringExport) {
            suspendCancellableCoroutine<Unit> { }
            error("unreachable")
        }
        return exportResult
    }

    override fun cancel() {
        cancelCalls += 1
    }
}


private fun filmPlan(weeksAgo: Int = 0): MemoryFilmPlan = MemoryFilmPlan(
    renderVersion = 1,
    selectionVersion = "smart-v1",
    startsAt = "2026-08-10T00:00:00Z",
    endsAt = "2026-08-17T00:00:00Z",
    utcOffsetMinutes = 0,
    weeksAgo = weeksAgo,
    filmReady = true,
    mood = "after_dark",
    targetDurationMs = 45_000,
    totalDurationMs = 8_000,
    coverMediaUrl = "https://cdn.example.com/cover.jpg",
    scenes = listOf(
        MemoryFilmScene(
            index = 0,
            source = "post",
            sourceId = 10,
            mediaType = "image",
            mediaUrl = "https://cdn.example.com/a.jpg",
            occurredAt = "2026-08-11T20:00:00Z",
            durationMs = 3_000,
            trimStartMs = 0,
            caption = "First scene",
            person = null,
            room = null,
        ),
        MemoryFilmScene(
            index = 1,
            source = "pulse",
            sourceId = 11,
            mediaType = "video",
            mediaUrl = "https://cdn.example.com/b.mp4",
            occurredAt = "2026-08-12T21:00:00Z",
            durationMs = 5_000,
            trimStartMs = 0,
            caption = "Second scene",
            person = null,
            room = null,
        ),
    ),
)
