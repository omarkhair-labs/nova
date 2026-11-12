package com.nova.app.feature.plants.data

/**
 * Only the latest detail request for the currently selected authorized plant
 * may update visible private history. The monotonic generation also rejects
 * stale results when the same plant is reopened after leaving its screen.
 */
internal fun isCurrentPlantDetailRequest(
    requestedPlantId: String,
    requestedGeneration: Int,
    selectedPlantId: String?,
    currentGeneration: Int,
): Boolean = requestedPlantId.isNotBlank() &&
    requestedPlantId == selectedPlantId &&
    requestedGeneration == currentGeneration
