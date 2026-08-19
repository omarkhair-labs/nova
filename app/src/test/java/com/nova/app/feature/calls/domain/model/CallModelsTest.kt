package com.nova.app.feature.calls.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class CallModelsTest {
    @Test
    fun callKindWireMappingPreservesLegacyFallback() {
        assertEquals(NovaCallKind.Audio, NovaCallKind.fromWire("audio"))
        assertEquals(NovaCallKind.Video, NovaCallKind.fromWire("video"))
        assertEquals(NovaCallKind.Video, NovaCallKind.fromWire("VIDEO"))
        assertEquals(NovaCallKind.Audio, NovaCallKind.fromWire("unknown"))
        assertEquals("audio", NovaCallKind.Audio.wireValue)
        assertEquals("video", NovaCallKind.Video.wireValue)
    }

    @Test
    fun callStatusWireMappingAndTerminalSemanticsStayExact() {
        assertEquals(NovaCallStatus.Ringing, NovaCallStatus.fromWire("ringing"))
        assertEquals(NovaCallStatus.Active, NovaCallStatus.fromWire("active"))
        assertEquals(NovaCallStatus.Declined, NovaCallStatus.fromWire("declined"))
        assertEquals(NovaCallStatus.Canceled, NovaCallStatus.fromWire("canceled"))
        assertEquals(NovaCallStatus.Ended, NovaCallStatus.fromWire("ended"))
        assertEquals(NovaCallStatus.Missed, NovaCallStatus.fromWire("missed"))
        assertEquals(NovaCallStatus.Failed, NovaCallStatus.fromWire("failed"))
        assertEquals(NovaCallStatus.Failed, NovaCallStatus.fromWire("unexpected"))

        assertFalse(NovaCallStatus.Ringing.isTerminal)
        assertFalse(NovaCallStatus.Active.isTerminal)
        assertTrue(NovaCallStatus.Declined.isTerminal)
        assertTrue(NovaCallStatus.Canceled.isTerminal)
        assertTrue(NovaCallStatus.Ended.isTerminal)
        assertTrue(NovaCallStatus.Missed.isTerminal)
        assertTrue(NovaCallStatus.Failed.isTerminal)
    }

    @Test
    fun callPersonDisplayNameKeepsNameThenUsernameFallback() {
        assertEquals(
            "Omar",
            NovaCallPerson(1L, "omar", "Omar", "").displayName,
        )
        assertEquals(
            "omar",
            NovaCallPerson(1L, "omar", "", "").displayName,
        )
    }
}
