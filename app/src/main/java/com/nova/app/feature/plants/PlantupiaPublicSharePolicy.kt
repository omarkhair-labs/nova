package com.nova.app.feature.plants

/**
 * Pure local UI validation for explicit public publishing. A Nova post
 * currently requires an image; do not imply text-only support until the
 * existing media engine and backend explicitly support that format.
 */
internal data class PlantupiaPublicShareDraft(
    val topic: String,
    val question: String,
    val hasPhoto: Boolean,
) {
    val cleanTopic: String get() = topic.trim()
    val cleanQuestion: String get() = question.trim()

    val canReview: Boolean
        get() = hasPhoto &&
            cleanTopic.length in 1..80 &&
            cleanQuestion.length in 1..500
}
