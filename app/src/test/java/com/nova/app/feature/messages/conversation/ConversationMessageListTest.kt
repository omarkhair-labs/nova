package com.nova.app.feature.messages.conversation

import com.nova.app.feature.messages.domain.model.NovaMessage
import com.nova.app.feature.messages.domain.model.NovaReplyPreview
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class ConversationMessageListTest {
    private val utc = ZoneId.of("UTC")
    private val alice = NovaPostAuthor(1L, "alice", "Alice", "")
    private val bob = NovaPostAuthor(2L, "bob", "Bob", "")

    @Test
    fun rowContextPreservesDateSenderGroupingAndUnreadPlacement() {
        val messages = listOf(
            message(1L, alice, "2026-08-14T09:00:00Z"),
            message(2L, alice, "2026-08-14T09:01:00Z"),
            message(3L, bob, "2026-08-14T10:00:00Z"),
            message(4L, bob, "2026-08-15T10:00:00Z"),
        )

        val first = context(messages, index = 0, unreadAnchor = 2L, unreadCount = 2)
        val second = context(messages, index = 1, unreadAnchor = 2L, unreadCount = 2)
        val third = context(messages, index = 2, unreadAnchor = 2L, unreadCount = 2)
        val fourth = context(messages, index = 3, unreadAnchor = 2L, unreadCount = 2)

        assertEquals(LocalDate.of(2026, 8, 14), first.day)
        assertTrue(first.showDateDivider)
        assertFalse(first.compactTop)
        assertTrue(first.compactBottom)
        assertTrue(first.showSenderName)

        assertFalse(second.showDateDivider)
        assertTrue(second.compactTop)
        assertFalse(second.compactBottom)
        assertFalse(second.showSenderName)
        assertTrue(second.showUnreadDivider)

        assertEquals(LocalDate.of(2026, 8, 14), third.day)
        assertFalse(third.showDateDivider)
        assertFalse(third.compactTop)
        assertFalse(third.compactBottom)
        assertTrue(third.showSenderName)

        assertEquals(LocalDate.of(2026, 8, 15), fourth.day)
        assertTrue(fourth.showDateDivider)
        assertFalse(fourth.compactTop)
        assertFalse(fourth.compactBottom)
        assertTrue(fourth.showSenderName)
    }

    @Test
    fun unreadDividerRequiresTheCapturedPositiveCount() {
        val messages = listOf(message(1L, alice, "2026-08-14T09:00:00Z"))

        assertFalse(context(messages, 0, unreadAnchor = 1L, unreadCount = 0).showUnreadDivider)
        assertFalse(context(messages, 0, unreadAnchor = 2L, unreadCount = 3).showUnreadDivider)
        assertTrue(context(messages, 0, unreadAnchor = 1L, unreadCount = 3).showUnreadDivider)
    }

    @Test
    fun invalidTimestampKeepsTheEstablishedNoDividerFallback() {
        val messages = listOf(
            message(1L, alice, "invalid"),
            message(2L, alice, "also-invalid"),
        )

        val second = context(messages, index = 1, unreadAnchor = null, unreadCount = 0)

        assertNull(second.day)
        assertFalse(second.showDateDivider)
        assertTrue(second.compactTop)
    }

    @Test
    fun dateReplyAndVoiceLabelsKeepTheirExactFallbacks() {
        val today = LocalDate.of(2026, 8, 14)

        assertEquals("Today", dayLabel(today, today))
        assertEquals("Yesterday", dayLabel(today.minusDays(1), today))
        assertEquals("Aug 12, 2026", dayLabel(today.minusDays(2), today))
        assertEquals("5:00", formatVoiceDuration(5 * 60 * 1000L + 20_000L))
        assertEquals("0:00", formatVoiceDuration(-1L))
        assertEquals("Voice message", replyPreviewText(reply(audioUrl = "voice.mp4")))
        assertEquals("Photo", replyPreviewText(reply(imageUrl = "photo.jpg")))
        assertEquals("Message deleted", replyPreviewText(reply(isDeleted = true)))
    }

    private fun context(
        messages: List<NovaMessage>,
        index: Int,
        unreadAnchor: Long?,
        unreadCount: Int,
    ) = messageRowContext(
        messages = messages,
        index = index,
        isGroupConversation = true,
        unreadAnchorMessageId = unreadAnchor,
        unreadCountAtOpen = unreadCount,
        zoneId = utc,
    )

    private fun message(
        id: Long,
        sender: NovaPostAuthor,
        createdAt: String,
    ) = NovaMessage(
        id = id,
        clientId = "client-$id",
        sender = sender,
        body = "message-$id",
        imageUrl = "",
        replyTo = null,
        reactions = emptyList(),
        createdAt = createdAt,
        deliveredAt = null,
        readAt = null,
        isMine = false,
    )

    private fun reply(
        imageUrl: String = "",
        audioUrl: String = "",
        isDeleted: Boolean = false,
    ) = NovaReplyPreview(
        id = 1L,
        sender = alice,
        body = "",
        imageUrl = imageUrl,
        audioUrl = audioUrl,
        isDeleted = isDeleted,
    )
}
