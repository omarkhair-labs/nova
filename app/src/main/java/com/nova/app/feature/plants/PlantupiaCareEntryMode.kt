package com.nova.app.feature.plants

/** A person's explicit record intent, never inferred from a guide or a due date. */
internal enum class PlantupiaCareEntryMode(val eventKind: String, val label: String, val saveLabel: String) {
    CARE("care", "Care action", "Save care"),
    NOTE("observation", "Observation", "Save observation"),
    CONCERN("issue", "Concern", "Record concern"),
}

internal fun plantupiaCanSaveCareEntry(
    mode: PlantupiaCareEntryMode,
    careActionSelected: Boolean,
    noteOrPhotoPresent: Boolean,
    validFollowUpDate: Boolean,
): Boolean = validFollowUpDate && when (mode) {
    PlantupiaCareEntryMode.CARE -> careActionSelected
    PlantupiaCareEntryMode.NOTE, PlantupiaCareEntryMode.CONCERN -> noteOrPhotoPresent
}
