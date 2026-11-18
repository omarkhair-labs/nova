package com.nova.app.feature.plants.data

/**
 * Local, permission-scoped browsing of a space's living units. The API already
 * restricts the input list to spaces this account may view.
 *
 * Recent documented changes surface first, then names for stable ties. This
 * intentionally doesn't infer plant health from the presence of a photo.
 */
internal fun findSpacePlants(
    plants: List<PlantupiaPlant>,
    spaceId: String,
    search: String,
): List<PlantupiaPlant> {
    val phrase = search.trim()
    return plants.asSequence()
        .filter { it.active && it.spaceId == spaceId }
        .filter { plant ->
            phrase.isEmpty() || sequenceOf(
                plant.name,
                plant.label,
                plant.species,
                plant.placement,
            ).any { it.contains(phrase, ignoreCase = true) }
        }
        .sortedWith(
            compareByDescending<PlantupiaPlant> { it.lastEventAt }
                .thenBy { it.name.lowercase() }
                .thenBy { it.id }
        )
        .toList()
}

/** Archived records remain discoverable but never enter active care or Today. */
internal fun findArchivedPlants(
    plants: List<PlantupiaPlant>,
    spaceId: String,
    search: String,
): List<PlantupiaPlant> {
    val phrase = search.trim()
    return plants.asSequence()
        .filter { !it.active && it.spaceId == spaceId }
        .filter { plant ->
            phrase.isEmpty() || sequenceOf(
                plant.name, plant.label, plant.species, plant.placement,
            ).any { it.contains(phrase, ignoreCase = true) }
        }
        .sortedWith(compareBy<PlantupiaPlant> { it.name.lowercase() }.thenBy { it.id })
        .toList()
}
