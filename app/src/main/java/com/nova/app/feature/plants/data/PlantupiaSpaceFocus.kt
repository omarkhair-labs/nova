package com.nova.app.feature.plants.data

/** Only a member of the server-authorized collection may become the focused space. */
internal fun resolveFocusedSpace(
    spaces: List<PlantupiaSpace>,
    requestedId: String?,
): PlantupiaSpace? =
    spaces.firstOrNull { it.id == requestedId } ?: spaces.firstOrNull()


/**
 * Prefer the currently focused real writable space for adding a plant.
 * A newly created server-returned UUID may be used while its list reloads.
 * Never use a stale/foreign 'preferred' value as authorization.
 */
internal fun chooseWritableSpaceId(
    writableSpaces: List<PlantupiaSpace>,
    preferredId: String?,
    currentId: String?,
    justCreatedId: String? = null,
): String? {
    if (!justCreatedId.isNullOrBlank() && justCreatedId == currentId) return justCreatedId
    return writableSpaces.firstOrNull { it.id == preferredId }?.id
        ?: writableSpaces.firstOrNull { it.id == currentId }?.id
        ?: writableSpaces.firstOrNull()?.id
}

/** Count only server-visible, active units in the selected space. */
internal fun spacePlantCountSummary(count: Int): String = when (count.coerceAtLeast(0)) {
    0 -> "No plants added yet"
    1 -> "1 plant in this space"
    else -> "${count} plants in this space"
}
