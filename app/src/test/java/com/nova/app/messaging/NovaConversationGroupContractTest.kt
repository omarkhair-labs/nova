package com.nova.app.messaging

import com.nova.app.core.messaging.NovaConversation
import com.nova.app.core.network.NovaPostAuthor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class NovaConversationGroupContractTest {
    private val person = NovaPostAuthor(
        id = 7L,
        username = "alice",
        name = "Alice",
        avatarUrl = "",
    )

    @Test
    fun directConversationKeepsPersonIdentity() {
        val conversation = NovaConversation(
            id = 10L,
            otherUser = person,
            lastMessage = null,
            unreadCount = 0,
            createdAt = "",
            updatedAt = "",
        )

        assertFalse(conversation.isGroup)
        assertEquals("Alice", conversation.displayName)
        assertEquals("@alice", conversation.displaySubtitle)
        assertEquals(2, conversation.membersCount)
    }

    @Test
    fun groupConversationUsesTitleAndMemberCount() {
        val conversation = NovaConversation(
            id = 20L,
            otherUser = NovaPostAuthor(
                id = 0L,
                username = "group",
                name = "Nova Crew",
                avatarUrl = "",
            ),
            lastMessage = null,
            unreadCount = 3,
            createdAt = "",
            updatedAt = "",
            kind = "group",
            title = "Nova Crew",
            membersPreview = listOf(person),
            membersCount = 4,
            currentUserRole = "owner",
        )

        assertTrue(conversation.isGroup)
        assertEquals("Nova Crew", conversation.displayName)
        assertEquals("4 members", conversation.displaySubtitle)
        assertEquals("owner", conversation.currentUserRole)
    }
}
