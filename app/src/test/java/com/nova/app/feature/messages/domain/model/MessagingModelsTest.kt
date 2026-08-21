package com.nova.app.feature.messages.domain.model

import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class MessagingModelsTest {
    private val person = NovaPostAuthor(
        id = 7L,
        username = "alice",
        name = "Alice",
        avatarUrl = "",
    )

    @Test
    fun directConversationKeepsPersonIdentity() {
        val conversation = conversation()

        assertFalse(conversation.isGroup)
        assertEquals("Alice", conversation.displayName)
        assertEquals("@alice", conversation.displaySubtitle)
        assertEquals(2, conversation.membersCount)
    }

    @Test
    fun groupConversationUsesTitleAndMemberCount() {
        val conversation = conversation(
            kind = "group",
            title = "Nova Crew",
            membersCount = 4,
            currentUserRole = "owner",
        )

        assertTrue(conversation.isGroup)
        assertEquals("Nova Crew", conversation.displayName)
        assertEquals("4 members", conversation.displaySubtitle)
        assertEquals("owner", conversation.currentUserRole)
    }

    @Test
    fun blankGroupTitleKeepsTheEstablishedFallback() {
        val conversation = conversation(kind = "group", membersCount = 1)

        assertEquals("Nova group", conversation.displayName)
        assertEquals("1 member", conversation.displaySubtitle)
    }

    @Test
    fun messageDeletionStateStillComesFromDeletedTimestamp() {
        assertFalse(message(deletedAt = null).isDeleted)
        assertTrue(message(deletedAt = "2026-08-14T00:00:00Z").isDeleted)
    }

    private fun conversation(
        kind: String = "direct",
        title: String = "",
        membersCount: Int = 2,
        currentUserRole: String = "",
    ) = NovaConversation(
        id = 10L,
        otherUser = person,
        lastMessage = null,
        unreadCount = 0,
        createdAt = "",
        updatedAt = "",
        kind = kind,
        title = title,
        membersPreview = listOf(person),
        membersCount = membersCount,
        currentUserRole = currentUserRole,
    )

    private fun message(deletedAt: String?) = NovaMessage(
        id = 30L,
        clientId = "client-30",
        sender = person,
        body = "Hello",
        imageUrl = "",
        replyTo = null,
        reactions = emptyList(),
        createdAt = "2026-08-14T00:00:00Z",
        deliveredAt = null,
        readAt = null,
        isMine = true,
        deletedAt = deletedAt,
    )
}
