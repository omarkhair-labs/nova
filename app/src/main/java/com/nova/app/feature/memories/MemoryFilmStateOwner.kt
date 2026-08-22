package com.nova.app.feature.memories

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.memories.data.MemoryRepository
import com.nova.app.feature.memories.domain.model.MemoryFilmPlan
import com.nova.app.feature.memories.film.MemoryFilmExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class MemoryFilmUiState(
    val plan: MemoryFilmPlan? = null,
    val weeksAgo: Int = 0,
    val loadingPlan: Boolean = true,
    val exporting: Boolean = false,
    val progress: Int = 0,
    val outputPath: String? = null,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
)


class MemoryFilmStateOwner(
    private val repository: MemoryRepository,
    private val exporter: MemoryFilmExporter,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(MemoryFilmUiState())
        private set

    fun loadPlan(
        utcOffsetMinutes: Int,
        weeksAgo: Int = state.weeksAgo,
        showSpinner: Boolean = false,
    ) {
        scope.launch { loadPlanNow(utcOffsetMinutes, weeksAgo, showSpinner) }
    }

    internal suspend fun loadPlanNow(
        utcOffsetMinutes: Int,
        weeksAgo: Int = state.weeksAgo,
        showSpinner: Boolean = false,
    ) {
        val boundedWeek = weeksAgo.coerceIn(0, 51)
        if (showSpinner) {
            state = state.copy(
                loadingPlan = true,
                outputPath = null,
                error = null,
            )
        }
        when (val result = repository.filmPlan(utcOffsetMinutes, boundedWeek)) {
            is ApiResult.Success -> state = state.copy(
                plan = result.value,
                weeksAgo = boundedWeek,
                loadingPlan = false,
                outputPath = null,
                error = null,
            )
            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        loadingPlan = false,
                        error = null,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(loadingPlan = false, error = result.message)
                }
            }
        }
    }

    fun export() {
        val plan = state.plan ?: return
        if (!plan.filmReady || state.exporting) return
        scope.launch { exportNow(plan) }
    }

    internal suspend fun exportNow(plan: MemoryFilmPlan) {
        state = state.copy(
            exporting = true,
            progress = 0,
            outputPath = null,
            error = null,
        )
        val result = exporter.export(plan) { value ->
            state = state.copy(progress = value.coerceIn(0, 100))
        }
        state = result.fold(
            onSuccess = { output ->
                state.copy(
                    exporting = false,
                    progress = 100,
                    outputPath = output.filePath,
                    error = null,
                )
            },
            onFailure = { error ->
                state.copy(
                    exporting = false,
                    progress = 0,
                    outputPath = null,
                    error = error.message ?: "Nova couldn't render this film.",
                )
            },
        )
    }

    fun cancelExport() {
        exporter.cancel()
        state = state.copy(exporting = false, progress = 0)
    }
}
