package com.nova.app.feature.calls

import com.nova.app.feature.calls.domain.model.NovaCallKind
import com.nova.app.feature.calls.domain.model.NovaCallPerson
import com.nova.app.feature.calls.domain.model.NovaCallSession
import com.nova.app.feature.calls.domain.model.NovaCallStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class CallUiStateTest {
    private val person = NovaCallPerson(1L, "peer", "Peer", "")

    @Test
    fun `incoming ringing state is classified without changing session semantics`() {
        val state = CallUiState(session = session(NovaCallStatus.Ringing, isCaller = false))

        assertEquals(CallPhase.RingingIncoming, state.phase)
        assertTrue(state.isIncomingRinging)
        assertFalse(state.isTerminal)
    }

    @Test
    fun `outgoing ringing state remains distinct from incoming`() {
        val state = CallUiState(session = session(NovaCallStatus.Ringing, isCaller = true))

        assertEquals(CallPhase.RingingOutgoing, state.phase)
        assertFalse(state.isIncomingRinging)
    }

    @Test
    fun `active state distinguishes connecting connected and recovery`() {
        val active = session(NovaCallStatus.Active, isCaller = true)

        assertEquals(CallPhase.Connecting, CallUiState(session = active, connected = false).phase)
        assertEquals(CallPhase.Active, CallUiState(session = active, connected = true).phase)
        assertEquals(
            CallPhase.Reconnecting,
            CallUiState(session = active, connected = false, stage = "Reconnecting…").phase,
        )
    }

    @Test
    fun `all terminal call statuses classify as terminal`() {
        listOf(
            NovaCallStatus.Declined,
            NovaCallStatus.Canceled,
            NovaCallStatus.Ended,
            NovaCallStatus.Missed,
            NovaCallStatus.Failed,
        ).forEach { status ->
            val state = CallUiState(session = session(status, isCaller = true))
            assertEquals(status.wireValue, CallPhase.Terminal, state.phase)
            assertTrue(status.wireValue, state.isTerminal)
        }
    }

    private fun session(status: NovaCallStatus, isCaller: Boolean) = NovaCallSession(
        id = "call-1",
        conversationId = 7L,
        kind = NovaCallKind.Video,
        status = status,
        caller = person,
        callee = person,
        peer = person,
        isCaller = isCaller,
        createdAt = "",
        answeredAt = null,
        endedAt = null,
        endReason = "",
        ringTimeoutSeconds = 45,
    )
}
