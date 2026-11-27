package com.nova.app.feature.plants

/** Presentation policy only: never grants access, claims a delivery or verifies a specimen. */
internal fun signatureIdentityLabel(identity: String, qa: Boolean): String = when (identity) {
    "staff_verified" -> if (qa)
        "Company-recorded type · QA specimen. Confirm the photo and real identity."
        else "Botanical identity verified by Plantupia"
    "user_selected" -> "Species selected by you · individual plant not verified"
    else -> "Species not confirmed"
}

internal fun signatureTypeChosen(key: String?, unknown: Boolean): Boolean =
    !key.isNullOrBlank() || unknown

internal enum class SignatureDeliveryStage { SCAN, REVIEW, CONNECTED }

/** A preview may already say claimed; only an acknowledged connection has a space ID. */
internal fun signatureDeliveryStage(hasPreview: Boolean, claimed: Boolean, spaceId: String?): SignatureDeliveryStage =
    when {
        !hasPreview -> SignatureDeliveryStage.SCAN
        claimed && !spaceId.isNullOrBlank() -> SignatureDeliveryStage.CONNECTED
        else -> SignatureDeliveryStage.REVIEW
    }
