from django.urls import path

from ..group_management import GroupManagementDetailView, GroupMemberRoleView
from ..group_messaging import (
    GroupConversationCreateView,
    GroupConversationDetailView,
    GroupMemberDetailView,
    GroupMembersView,
)
from ..messaging_mutation_view import MessageMutationView
from ..messaging_paging import PaginatedConversationsView
from ..messaging_v9_views import (
    ConversationMediaView,
    ConversationMessageContextView,
    ConversationMessageSearchView,
    ConversationPreferenceView,
)
from ..messaging_views import ConversationMessagesView, ConversationReadView, MessageReactionView


urlpatterns = [
    path("conversations/", PaginatedConversationsView.as_view(), name="conversations"),
    path(
        "conversations/groups/",
        GroupConversationCreateView.as_view(),
        name="group-conversation-create",
    ),
    path(
        "conversations/<int:conversation_id>/group/",
        GroupConversationDetailView.as_view(),
        name="group-conversation-detail",
    ),
    path(
        "conversations/<int:conversation_id>/group/manage/",
        GroupManagementDetailView.as_view(),
        name="group-management-detail",
    ),
    path(
        "conversations/<int:conversation_id>/group/members/",
        GroupMembersView.as_view(),
        name="group-members",
    ),
    path(
        "conversations/<int:conversation_id>/group/members/<str:username>/",
        GroupMemberDetailView.as_view(),
        name="group-member-detail",
    ),
    path(
        "conversations/<int:conversation_id>/group/members/<str:username>/role/",
        GroupMemberRoleView.as_view(),
        name="group-member-role",
    ),
    path(
        "conversations/<int:conversation_id>/messages/",
        ConversationMessagesView.as_view(),
        name="conversation-messages",
    ),
    path(
        "conversations/<int:conversation_id>/messages/search/",
        ConversationMessageSearchView.as_view(),
        name="conversation-message-search",
    ),
    path(
        "conversations/<int:conversation_id>/messages/context/",
        ConversationMessageContextView.as_view(),
        name="conversation-message-context",
    ),
    path(
        "conversations/<int:conversation_id>/media/",
        ConversationMediaView.as_view(),
        name="conversation-media",
    ),
    path(
        "conversations/<int:conversation_id>/preferences/",
        ConversationPreferenceView.as_view(),
        name="conversation-preferences",
    ),
    path(
        "conversations/<int:conversation_id>/read/",
        ConversationReadView.as_view(),
        name="conversation-read",
    ),
    path("messages/<int:message_id>/", MessageMutationView.as_view(), name="message-detail"),
    path(
        "messages/<int:message_id>/reaction/",
        MessageReactionView.as_view(),
        name="message-reaction",
    ),
]
