package com.nova.app.feature.plants

import java.time.LocalDate

/** Selected human check-ins, not predicted watering or sensor events. */
internal fun selectedFollowUpDate(
    customDate: LocalDate?,
    presetDays: Int?,
    today: LocalDate,
): String? =
    customDate?.toString()
        ?: presetDays?.takeIf { it > 0 }?.let { today.plusDays(it.toLong()).toString() }

/** A user may record care without a follow-up; chosen dates cannot be past. */
internal fun isValidCheckInDate(customDate: LocalDate?, today: LocalDate): Boolean =
    customDate == null || !customDate.isBefore(today)
