package com.nova.app.feature.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Explicit private vs public creation intent across the existing Nova tabs.
 * This is UI-only state. No private observation is uploaded or published by it.
 */
enum class PlantupiaCreateMode { OBSERVATION, QUESTION }

object PlantupiaCreateIntent {
    var pending by mutableStateOf<PlantupiaCreateMode?>(null)
        private set
    var pendingPlantId by mutableStateOf<String?>(null)
        private set
    var pendingAddPlant by mutableStateOf(false)
        private set

    fun request(mode: PlantupiaCreateMode) {
        pendingPlantId = null
        pendingAddPlant = false
        pending = mode
    }

    fun requestOpenPlant(plantId: String, mode: PlantupiaCreateMode? = null) {
        pendingPlantId = plantId
        pendingAddPlant = false
        pending = mode
    }

    fun requestAddPlant() {
        pending = null
        pendingPlantId = null
        pendingAddPlant = true
    }

    fun consumeAddPlant() { pendingAddPlant = false }

    fun clear() {
        pending = null
        pendingPlantId = null
    }
}
