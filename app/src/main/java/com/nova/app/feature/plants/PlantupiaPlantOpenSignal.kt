package com.nova.app.feature.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab

/** IDs are navigation hints only. Plants resolves them against its authenticated inventory. */
internal object PlantupiaPlantOpenSignal {
    var target by mutableStateOf<Pair<Long, String>?>(null)
        private set

    fun open(accountId: Long, plantId: String) {
        if (accountId <= 0 || runCatching { java.util.UUID.fromString(plantId) }.isFailure) return
        target = accountId to plantId
        NovaRootNavigationSignal.request(NovaRootTab.Plants)
    }

    fun consume() { target = null }
}
