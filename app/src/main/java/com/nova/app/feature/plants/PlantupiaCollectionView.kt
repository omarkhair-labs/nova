package com.nova.app.feature.plants

import java.time.LocalDate

/** View-only filters over authorized active plants; a missing portrait is not a health signal. */
internal fun plantupiaCollectionIncludes(filter: String, nextFollowUpOn: String, coverPhoto: String, today: LocalDate): Boolean =
    when (filter) {
        "due" -> runCatching { !LocalDate.parse(nextFollowUpOn).isAfter(today) }.getOrDefault(false)
        "no_photo" -> coverPhoto.isBlank()
        else -> true
    }
