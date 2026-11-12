package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaCareConversationTest {
    @Test fun ordinaryQuestionsKeepTheirDialogueWhilePrivateTurnsRequireNewConsent() {
        val bubbles = listOf(CareChatBubble("general question", true), CareChatBubble("general answer", false, "general"),
            CareChatBubble("private question", true, "plant_context"), CareChatBubble("private answer", false, "plant_context"))
        assertEquals(listOf("general question", "general answer"), contextSafeAssistantConversation(bubbles, false).map { it.content })
        assertEquals(4, contextSafeAssistantConversation(bubbles, true).size)
        assertEquals(listOf("user", "assistant"), contextSafeAssistantConversation(bubbles, false).map { it.role })
    }
    @Test fun dialogueUsesOnlyEightBoundedTurnsAndHonestTeamStatus() {
        val turns = contextSafeAssistantConversation(List(12) { CareChatBubble("x".repeat(1500), true) }, false)
        assertEquals(8, turns.size)
        assertTrue(turns.all { it.content.length == 1200 })
        assertEquals("Team replied", supportStatusLabel("replied"))
        assertEquals("Closed · read-only", supportStatusLabel("closed"))
        assertEquals("Awaiting a team reply", supportStatusLabel("open"))
    }
}
