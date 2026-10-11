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

    fun request(mode: PlantupiaCreateMode) {
        pending = mode
    }

    fun clear() {
        pending = null
    }
}
