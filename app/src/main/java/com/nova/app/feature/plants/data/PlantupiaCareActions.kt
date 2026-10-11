package com.nova.app.feature.plants.data

/**
 * Human-reported care actions, not advice, predictions or inferred plant status.
 * Only the action actually selected by a person is written to private history.
 */
enum class PlantupiaCareAction(val label: String, val recordedText: String) {
    WATERED("رويت", "رويت النبات"),
    CHECKED_SOIL("فحصت التربة", "فحصت التربة"),
    PRUNED("قلّمت", "قلّمت النبات"),
    MOVED("غيّرت المكان", "نقلت النبات لمكان آخر"),
    FERTILIZED("سمّدت", "سمّدت النبات"),
    REPOTTED("غيّرت الأصيص", "غيّرت أصيص النبات"),
}

/** Compatible with the existing private care-event API; no fake metadata. */
fun buildPlantupiaCareNote(action: PlantupiaCareAction?, details: String): String {
    val humanNote = details.trim()
    if (action == null) return humanNote
    return if (humanNote.isEmpty()) action.recordedText
        else "${action.recordedText}\n$humanNote"
}
