package com.nova.app.feature.plants

/**
 * Zero successfully fetched records and a failed network fetch are different facts.
 * Never show an illustration claiming a blank field-book before a successful GET.
 */
internal enum class PlantupiaHistoryDisplayState {
    LOADING, UNAVAILABLE, EMPTY, RECORDED,
}

internal fun plantupiaHistoryDisplayState(
    loading: Boolean,
    failed: Boolean,
    recordCount: Int,
): PlantupiaHistoryDisplayState = when {
    loading -> PlantupiaHistoryDisplayState.LOADING
    failed -> PlantupiaHistoryDisplayState.UNAVAILABLE
    recordCount <= 0 -> PlantupiaHistoryDisplayState.EMPTY
    else -> PlantupiaHistoryDisplayState.RECORDED
}
