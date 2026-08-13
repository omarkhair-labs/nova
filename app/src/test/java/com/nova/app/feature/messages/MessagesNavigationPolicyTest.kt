package com.nova.app.feature.messages

import org.junit.Assert.assertEquals
import org.junit.Test

class MessagesNavigationPolicyTest {
    @Test
    fun backClosesOnlyTheTopmostMessagesLayer() {
        val cases = listOf(
            MessagesBackSnapshot(
                newMessageOpen = true,
                newGroupOpen = true,
                createMenuOpen = true,
                conversationOpen = true,
            ) to MessagesBackAction.CloseNewMessage,
            MessagesBackSnapshot(
                newMessageOpen = false,
                newGroupOpen = true,
                createMenuOpen = true,
                conversationOpen = true,
            ) to MessagesBackAction.CloseNewGroup,
            MessagesBackSnapshot(
                newMessageOpen = false,
                newGroupOpen = false,
                createMenuOpen = true,
                conversationOpen = true,
            ) to MessagesBackAction.CloseCreateMenu,
            MessagesBackSnapshot(
                newMessageOpen = false,
                newGroupOpen = false,
                createMenuOpen = false,
                conversationOpen = true,
            ) to MessagesBackAction.CloseConversation,
            MessagesBackSnapshot(
                newMessageOpen = false,
                newGroupOpen = false,
                createMenuOpen = false,
                conversationOpen = false,
            ) to MessagesBackAction.Finish,
        )

        cases.forEach { (snapshot, expected) ->
            assertEquals(expected, messagesBackAction(snapshot))
        }
    }

    @Test
    fun directConversationBackFinishesItsSpecialEntry() {
        assertEquals(ConversationExitAction.Finish, conversationExitAction(openedDirectly = true))
    }

    @Test
    fun inboxConversationBackReturnsToInbox() {
        assertEquals(
            ConversationExitAction.ReturnToInbox,
            conversationExitAction(openedDirectly = false),
        )
    }
}
