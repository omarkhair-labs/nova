from django.urls import path

from .account_security import (
    ChangePasswordView,
    PasswordResetConfirmView,
    PasswordResetRequestView,
    RevokeOtherSessionsView,
    SecureRegisterView,
    SecureTokenObtainPairView,
    SecureTokenRefreshView,
)
from .messaging_mutation_view import MessageMutationView
from .messaging_v9_views import (
    ConversationMediaView,
    ConversationMessageContextView,
    ConversationMessageSearchView,
    ConversationPreferenceView,
)
from .messaging_views import (
    ConversationMessagesView,
    ConversationReadView,
    ConversationsView,
    MessageReactionView,
)
from .views import (
    CommentDetailView,
    DevicePushTokenView,
    FeedView,
    FollowView,
    MeView,
    NotificationsReadView,
    NotificationsView,
    PeopleView,
    PersonPostsView,
    PersonView,
    PostCommentsView,
    PostDetailView,
    PostLikeView,
    PostsView,
)

urlpatterns = [
    path("auth/register/", SecureRegisterView.as_view(), name="register"),
    path("auth/login/", SecureTokenObtainPairView.as_view(), name="login"),
    path("auth/refresh/", SecureTokenRefreshView.as_view(), name="token-refresh"),
    path(
        "auth/password/reset/request/",
        PasswordResetRequestView.as_view(),
        name="password-reset-request",
    ),
    path(
        "auth/password/reset/confirm/",
        PasswordResetConfirmView.as_view(),
        name="password-reset-confirm",
    ),
    path(
        "auth/password/change/",
        ChangePasswordView.as_view(),
        name="password-change",
    ),
    path(
        "auth/sessions/revoke-others/",
        RevokeOtherSessionsView.as_view(),
        name="revoke-other-sessions",
    ),
    path("me/", MeView.as_view(), name="me"),
    path("people/", PeopleView.as_view(), name="people"),
    path("people/<str:username>/", PersonView.as_view(), name="person-detail"),
    path("people/<str:username>/posts/", PersonPostsView.as_view(), name="person-posts"),
    path("people/<str:username>/follow/", FollowView.as_view(), name="person-follow"),
    path("posts/", PostsView.as_view(), name="posts"),
    path("posts/<int:post_id>/", PostDetailView.as_view(), name="post-detail"),
    path("posts/<int:post_id>/like/", PostLikeView.as_view(), name="post-like"),
    path("posts/<int:post_id>/comments/", PostCommentsView.as_view(), name="post-comments"),
    path("comments/<int:comment_id>/", CommentDetailView.as_view(), name="comment-detail"),
    path("feed/", FeedView.as_view(), name="feed"),
    path("notifications/", NotificationsView.as_view(), name="notifications"),
    path("notifications/read/", NotificationsReadView.as_view(), name="notifications-read"),
    path("push/devices/", DevicePushTokenView.as_view(), name="push-devices"),
    path("conversations/", ConversationsView.as_view(), name="conversations"),
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
    path(
        "messages/<int:message_id>/",
        MessageMutationView.as_view(),
        name="message-detail",
    ),
    path(
        "messages/<int:message_id>/reaction/",
        MessageReactionView.as_view(),
        name="message-reaction",
    ),
]
