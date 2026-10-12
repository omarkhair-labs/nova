package com.nova.app.feature.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * UI-only navigation intent between Nova Create and the real authenticated
 * Plantupia living home. This never creates or publishes a record by itself.
 */
enum class PlantupiaCreateMode { OBSERVATION, QUESTION, ADD_PLANT, ADD_SPACE }

internal enum class PlantupiaCreationForm { NONE, SPACE, PLANT }

/**
 * A first-time owner cannot create a plant without an owned space.
 * Keep this deterministic so the first-plant onboarding can be unit tested.
 */
internal fun PlantupiaCreateMode?.creationForm(hasSpaces: Boolean): PlantupiaCreationForm =
    when (this) {
        PlantupiaCreateMode.ADD_SPACE -> PlantupiaCreationForm.SPACE
        PlantupiaCreateMode.ADD_PLANT ->
            if (hasSpaces) PlantupiaCreationForm.PLANT else PlantupiaCreationForm.SPACE
        else -> PlantupiaCreationForm.NONE
    }

internal fun PlantupiaCreateMode?.needsPlantSelection(): Boolean =
    this == PlantupiaCreateMode.OBSERVATION || this == PlantupiaCreateMode.QUESTION

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
