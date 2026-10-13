package com.nova.app.feature.plants.data

/** Only a member of the server-authorized collection may become the focused space. */
internal fun resolveFocusedSpace(
    spaces: List<PlantupiaSpace>,
    requestedId: String?,
): PlantupiaSpace? =
    spaces.firstOrNull { it.id == requestedId } ?: spaces.firstOrNull()
