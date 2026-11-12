package com.nova.app.feature.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

/**
 * A notification is only a navigation hint. The screen rechecks current account,
 * authenticated plant membership and source-reviewed plan before expanding.
 */
internal object PlantupiaInspectionOpenSignal {
    var target by mutableStateOf<Triple<Long, String, String>?>(null)
        private set

    fun open(accountId: Long, plantId: String, planId: String) {
        if (accountId <= 0L ||
            runCatching { UUID.fromString(plantId); UUID.fromString(planId) }.isFailure) return
        target = Triple(accountId, plantId, planId)
        PlantupiaPlantOpenSignal.open(accountId, plantId)
    }

    fun consume() { target = null }
}
