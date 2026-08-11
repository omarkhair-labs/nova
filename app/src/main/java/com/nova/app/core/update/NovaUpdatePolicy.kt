package com.nova.app.core.update

internal enum class NovaUpdateMode {
    None,
    Flexible,
    Immediate,
}

internal object NovaUpdatePolicy {
    const val HIGH_PRIORITY_THRESHOLD = 4
    const val FLEXIBLE_PROMPT_COOLDOWN_MS = 24L * 60L * 60L * 1000L
    const val IMMEDIATE_PROMPT_COOLDOWN_MS = 4L * 60L * 60L * 1000L

    fun chooseMode(
        updatePriority: Int,
        flexibleAllowed: Boolean,
        immediateAllowed: Boolean,
    ): NovaUpdateMode {
        return when {
            updatePriority >= HIGH_PRIORITY_THRESHOLD && immediateAllowed -> NovaUpdateMode.Immediate
            flexibleAllowed -> NovaUpdateMode.Flexible
            immediateAllowed -> NovaUpdateMode.Immediate
            else -> NovaUpdateMode.None
        }
    }

    fun promptCooldownMs(mode: NovaUpdateMode): Long {
        return when (mode) {
            NovaUpdateMode.Immediate -> IMMEDIATE_PROMPT_COOLDOWN_MS
            NovaUpdateMode.Flexible -> FLEXIBLE_PROMPT_COOLDOWN_MS
            NovaUpdateMode.None -> Long.MAX_VALUE
        }
    }
}
