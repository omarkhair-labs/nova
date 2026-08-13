package com.nova.app.feature.messages

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageActionPolicyV4Test {
    @Test
    fun callHistoryClientIds_areRecognizedAsImmutableSystemHistory() {
        assertTrue(isCallHistoryClientIdV4("call:42"))
        assertTrue(isCallHistoryClientIdV4("call:abc"))
        assertFalse(isCallHistoryClientIdV4("client-message-42"))
        assertFalse(isCallHistoryClientIdV4(""))
    }
}
