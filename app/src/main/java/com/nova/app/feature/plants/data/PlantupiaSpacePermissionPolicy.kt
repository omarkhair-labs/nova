package com.nova.app.feature.plants.data

/**
 * Android never grants a plant write based on UI state alone.
 * A missing field from an older preview must not be misreported as a 403.
 */
internal fun addPlantAccessExplanation(spaces: List<PlantupiaSpace>): String = when {
    spaces.any { !it.canAddPlantPermissionReported } ->
        "This server didn't confirm plant-creation access. Refresh your spaces or update the preview API."
    spaces.isNotEmpty() ->
        "You can view these spaces, but can't add plants to them. Create a space you own or ask for manager access."
    else -> "Create your first private space to add a plant."
}
