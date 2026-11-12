package com.nova.app.feature.plants.data

/**
 * Human-reported care actions, not advice, predictions or inferred plant status.
 * Only the action actually selected by a person is written to private history.
 */
enum class PlantupiaCareAction(val label: String, val recordedText: String) {
    WATERED("Watered", "Watered the plant"),
    CHECKED_SOIL("Checked the soil", "Checked the soil"),
    PRUNED("Pruned", "Pruned the plant"),
    MOVED("Moved", "Moved the plant to another spot"),
    FERTILIZED("Fertilized", "Fertilized the plant"),
    REPOTTED("Repotted", "Repotted the plant"),
}

/** Compatible with the existing private care-event API; no fake metadata. */
fun buildPlantupiaCareNote(action: PlantupiaCareAction?, details: String): String {
    val humanNote = details.trim()
    if (action == null) return humanNote
    return if (humanNote.isEmpty()) action.recordedText
        else "${action.recordedText}\n$humanNote"
}
