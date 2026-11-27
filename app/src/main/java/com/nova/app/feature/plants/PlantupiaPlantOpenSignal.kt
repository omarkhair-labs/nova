package com.nova.app.feature.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab

internal data class PlantupiaPlantDestination(
    val accountId: Long,
    val plantId: String,
    val chapter: PlantupiaDetailChapter? = null,
    val recordedDay: java.time.LocalDate? = null,
)

/** IDs are navigation hints only. Plants resolves them against its authenticated inventory. */
internal object PlantupiaPlantOpenSignal {
    var target by mutableStateOf<PlantupiaPlantDestination?>(null)
        private set

    fun open(
        accountId: Long, plantId: String,
        chapter: PlantupiaDetailChapter? = null,
        recordedDay: java.time.LocalDate? = null,
    ) {
        if (accountId <= 0 || runCatching { java.util.UUID.fromString(plantId) }.isFailure) return
        target = PlantupiaPlantDestination(accountId, plantId, chapter, recordedDay)
        NovaRootNavigationSignal.request(NovaRootTab.Plants)
    }

    fun consume() { target = null }
}
