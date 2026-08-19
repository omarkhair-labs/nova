package com.nova.app.feature.messages.details.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test


class ConversationToolsRemoteRepositoryTest {
    @Test
    fun mediaTypeNormalizationPreservesSupportedValuesAndFallsBackToAll() {
        assertEquals("all", normalizeConversationMediaType("all"))
        assertEquals("image", normalizeConversationMediaType("image"))
        assertEquals("audio", normalizeConversationMediaType("audio"))
        assertEquals("all", normalizeConversationMediaType("video"))
        assertEquals("all", normalizeConversationMediaType(""))
    }

    @Test
    fun replyPreviewKeepsTheExistingPriorityOrder() {
        assertEquals("Message deleted", conversationReplyPreview(true, "text", "voice", "photo"))
        assertEquals("text", conversationReplyPreview(false, "text", "voice", "photo"))
        assertEquals("🎤 Voice message", conversationReplyPreview(false, "", "voice", "photo"))
        assertEquals("📷 Photo", conversationReplyPreview(false, "", "", "photo"))
        assertEquals("Message", conversationReplyPreview(false, "", "", ""))
    }

    @Test
    fun mediaUrlResolutionPreservesAbsoluteUrlsAndUsesTheApiOriginForRelativePaths() {
        val baseUrl = "https://example.test/api/v1/"
        assertEquals("", resolveConversationMediaUrl(baseUrl, ""))
        assertEquals("", resolveConversationMediaUrl(baseUrl, "null"))
        assertEquals(
            "https://cdn.example.test/photo.jpg",
            resolveConversationMediaUrl(baseUrl, "https://cdn.example.test/photo.jpg"),
        )
        assertEquals(
            "https://example.test/media/photo.jpg",
            resolveConversationMediaUrl(baseUrl, "/media/photo.jpg"),
        )
    }
}
