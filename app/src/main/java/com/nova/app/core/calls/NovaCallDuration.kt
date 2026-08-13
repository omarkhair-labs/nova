package com.nova.app.core.calls

import java.time.Instant


internal object NovaCallDuration {
    fun answeredAtEpochMs(value: String?): Long? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
    }

    fun label(startedAtEpochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): String {
        val totalSeconds = ((nowEpochMs - startedAtEpochMs).coerceAtLeast(0L) / 1_000L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
