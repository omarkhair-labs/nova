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
        .map { event ->
            PlantupiaHistoryMoment(
                event = event,
                outcomes = outcomesByParent[event.id].orEmpty().sortedBy { it.observedAt },
            )
        }
        // A completed check-in is a *new* moment. Surface the original
        // care + its outcome at the latest recorded activity date instead
        // of burying today's answer beneath months of later observations.
        .sortedWith(
            compareByDescending<PlantupiaHistoryMoment> {
                maxOf(it.event.observedAt, it.outcomes.lastOrNull()?.observedAt.orEmpty())
            }.thenByDescending { it.event.id },
        ).toList()
}

/**
 * A human can inspect separate types of events without losing provenance.
 * "Outcomes" intentionally shows each recorded result, even if All usually
 * attaches that result to its source care card.
 */
internal enum class PlantupiaHistoryFilter(val label: String) {
    ALL("All"),
    CARE("Care"),
    OBSERVATIONS("Notes"),
    CONCERNS("Concerns"),
    OUTCOMES("Outcomes"),
    PHOTOS("Photos"),
}

internal fun filteredPlantupiaHistory(
    events: List<PlantupiaCareEvent>,
    filter: PlantupiaHistoryFilter,
): List<PlantupiaHistoryMoment> {
    if (filter == PlantupiaHistoryFilter.ALL) return groupPlantupiaHistory(events)
    val selected = events.distinctBy { it.id }.filter { event ->
        when (filter) {
            PlantupiaHistoryFilter.ALL -> true
            PlantupiaHistoryFilter.CARE -> event.kind == "care"
            PlantupiaHistoryFilter.OBSERVATIONS -> event.kind == "observation"
            PlantupiaHistoryFilter.CONCERNS -> event.kind == "issue"
            PlantupiaHistoryFilter.OUTCOMES -> event.kind == "outcome"
            PlantupiaHistoryFilter.PHOTOS -> event.photoUrl.isNotBlank()
        }
    }
    return selected.sortedWith(
        compareByDescending<PlantupiaCareEvent> { it.observedAt }.thenByDescending { it.id },
    ).map { PlantupiaHistoryMoment(event = it, outcomes = emptyList()) }
}
