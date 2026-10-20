package com.nova.app.feature.plants

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import com.nova.app.feature.plants.data.PlantupiaHistoryPage

internal sealed interface PlantupiaPendingCheckInsState {
    data object Loading : PlantupiaPendingCheckInsState
    data class Unavailable(val message: String) : PlantupiaPendingCheckInsState
    data object Empty : PlantupiaPendingCheckInsState
    data class Loaded(val events: List<PlantupiaCareEvent>, val nextOffset: Int?) : PlantupiaPendingCheckInsState
}

internal fun pendingCheckInsFromPage(page: PlantupiaHistoryPage): PlantupiaPendingCheckInsState =
    if (page.events.isEmpty()) PlantupiaPendingCheckInsState.Empty
    else PlantupiaPendingCheckInsState.Loaded(page.events, page.nextOffset)

/** This retry owns only pending reviews, never history, selection, or the care draft. */
internal suspend fun loadPlantupiaPendingCheckIns(
    isCurrent: () -> Boolean,
    request: suspend () -> ApiResult<PlantupiaHistoryPage>,
    onState: (PlantupiaPendingCheckInsState) -> Unit,
) {
    if (!isCurrent()) return
    onState(PlantupiaPendingCheckInsState.Loading)
    val response = request()
    if (!isCurrent()) return
    onState(when (response) {
        is ApiResult.Success -> pendingCheckInsFromPage(response.value)
        is ApiResult.Failure -> PlantupiaPendingCheckInsState.Unavailable(response.message)
    })
}
