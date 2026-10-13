package com.nova.app.feature.plants

import com.nova.app.feature.plants.data.PlantupiaCareEvent

/** One real private history event and outcomes that explicitly point back to it. */
internal data class PlantupiaHistoryMoment(
    val event: PlantupiaCareEvent,
    val outcomes: List<PlantupiaCareEvent>,
)

/**
 * Join an outcome only when its parent care event is on the *loaded page*.
 * If the parent isn't loaded, keep that outcome as a visible standalone item.
 * Paginated histories must never silently hide real observations.
 *
 * An outcome with a photo remains separate, so its full evidence stays
 * visible and zoomable in the existing image viewer.
 */
internal fun groupPlantupiaHistory(events: List<PlantupiaCareEvent>): List<PlantupiaHistoryMoment> {
    val unique = events.distinctBy { it.id }
    val byId = unique.associateBy { it.id }
    val outcomesByParent = unique
        .filter { item ->
            item.kind == "outcome" &&
                item.followUpFor.isNotBlank() &&
                item.photoUrl.isBlank() &&
                byId[item.followUpFor]?.kind?.let { it != "outcome" } == true
        }
        .groupBy { it.followUpFor }
    val attachedIds = outcomesByParent.values.flatten().mapTo(hashSetOf()) { it.id }
    return unique
        .asSequence()
        .filterNot { it.id in attachedIds }
        .sortedWith(compareByDescending<PlantupiaCareEvent> { it.observedAt }.thenByDescending { it.id })
        .map { event ->
            PlantupiaHistoryMoment(
                event = event,
                outcomes = outcomesByParent[event.id].orEmpty().sortedBy { it.observedAt },
            )
        }.toList()
}
