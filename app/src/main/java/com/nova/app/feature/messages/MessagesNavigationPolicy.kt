package com.nova.app.feature.messages


internal data class MessagesBackSnapshot(
    val newMessageOpen: Boolean,
    val newGroupOpen: Boolean,
    val createMenuOpen: Boolean,
    val conversationOpen: Boolean,
)


internal enum class MessagesBackAction {
    CloseNewMessage,
    CloseNewGroup,
    CloseCreateMenu,
    CloseConversation,
    Finish,
}


internal fun messagesBackAction(snapshot: MessagesBackSnapshot): MessagesBackAction = when {
    snapshot.newMessageOpen -> MessagesBackAction.CloseNewMessage
    snapshot.newGroupOpen -> MessagesBackAction.CloseNewGroup
    snapshot.createMenuOpen -> MessagesBackAction.CloseCreateMenu
    snapshot.conversationOpen -> MessagesBackAction.CloseConversation
    else -> MessagesBackAction.Finish
}


internal enum class ConversationExitAction {
    ReturnToInbox,
    Finish,
}


internal fun conversationExitAction(openedDirectly: Boolean): ConversationExitAction =
    if (openedDirectly) ConversationExitAction.Finish else ConversationExitAction.ReturnToInbox
