package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaAiReadabilityTest {
    @Test fun arabicAndEnglishBulletsRenderWithoutLeakingLiteralMarkdownMarkers() {
        val output = plantupiaReadableAssistantText(
            "1. **موقع الزرع**\n- **الإضاءة:** افحص مكان النبات\n## Next step\n* Check the soil"
        )
        assertTrue(output.text.contains("موقع الزرع"))
        assertTrue(output.text.contains("• الإضاءة:"))
        assertTrue(output.text.contains("Next step"))
        assertTrue(output.text.contains("• Check the soil"))
        assertFalse(output.text.contains("**"))
        assertFalse(output.text.contains("##"))
        assertTrue(output.spanStyles.size >= 2)
    }

    @Test fun optingOutExcludesEarlierPrivateTurnsFromLaterConversationPayload() {
        val chat = listOf(
            CareChatBubble("My plant notes", fromUser = true, scope = "plant_context"),
            CareChatBubble("Context-based answer", fromUser = false, scope = "plant_context"),
            CareChatBubble("General follow-up", fromUser = true, scope = "general"),
        )
        val withoutContext = contextSafeAssistantConversation(chat, includeContext = false)
        assertEquals(1, withoutContext.size)
        val withContext = contextSafeAssistantConversation(chat, includeContext = true)
        assertEquals(3, withContext.size)
    }
}
