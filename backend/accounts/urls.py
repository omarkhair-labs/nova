from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

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
    NovaTokenObtainPairView,
    PeopleView,
    PersonPostsView,
    PersonView,
    PostCommentsView,
    PostDetailView,
    PostLikeView,
    PostsView,
    RegisterView,
)

urlpatterns = [
    path("auth/register/", RegisterView.as_view(), name="register"),
    path("auth/login/", NovaTokenObtainPairView.as_view(), name="login"),
    path("auth/refresh/", TokenRefreshView.as_view(), name="token-refresh"),
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
        "conversations/<int:conversation_id>/read/",
        ConversationReadView.as_view(),
        name="conversation-read",
    ),
    path(
        "messages/<int:message_id>/reaction/",
        MessageReactionView.as_view(),
        name="message-reaction",
    ),
]
