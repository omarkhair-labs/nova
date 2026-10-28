package com.nova.app.feature.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A one-shot, account-scoped route to an authorized Plantupia support ticket. */
data class PlantupiaSupportPushTarget(
    val accountId: Long,
    val ticketId: String,
    val plantId: String,
)

object PlantupiaSupportPushSignal {
    var target by mutableStateOf<PlantupiaSupportPushTarget?>(null)
        private set

    fun offer(accountId: Long, ticketId: String, plantId: String) {
        if (accountId <= 0 || runCatching { java.util.UUID.fromString(ticketId); java.util.UUID.fromString(plantId) }.isFailure) return
        target = PlantupiaSupportPushTarget(accountId, ticketId, plantId)
    }

    fun consume() { target = null }
}
