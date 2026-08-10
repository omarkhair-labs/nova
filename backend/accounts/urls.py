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
from .call_reliability_view import ReliableCallSessionCreateView
from .calls import (
    CallIceConfigView,
    CallSessionActionView,
    CallSessionDetailView,
)
from .group_management import GroupManagementDetailView, GroupMemberRoleView
from .group_messaging import (
    GroupConversationCreateView,
    GroupConversationDetailView,
    GroupMemberDetailView,
    GroupMembersView,
)
from .messaging_mutation_view import MessageMutationView
from .messaging_paging import PaginatedConversationsView
from .messaging_v9_views import (
    ConversationMediaView,
    ConversationMessageContextView,
    ConversationMessageSearchView,
    ConversationPreferenceView,
)
from .messaging_views import (
    ConversationMessagesView,
    ConversationReadView,
    MessageReactionView,
)
from .privacy_views import (
    AccountPrivacyView,
    CloseFriendDetailView,
    CloseFriendsView,
    FollowRequestAcceptView,
    FollowRequestDeclineView,
    FollowRequestsView,
)
from .sharing_views import MessageShareView, PostRepostView, SharingFeedView
from .social_paging import (
    FollowersView,
    FollowingView,
    PaginatedPeopleView,
    PaginatedPersonPostsView,
)
from .stories import (
    StoryDetailView,
    StoryFeedView,
    StoryReactionView,
    StoryReplyView,
    StoryViewedView,
    StoryViewersView,
)
from .trust_safety import BlockedUsersView, DeleteAccountView, UserBlockView, UserReportView
from .views import (
    CommentDetailView,
    DevicePushTokenView,
    FollowView,
    MeView,
    NotificationsReadView,
    NotificationsView,
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
    path("auth/account/delete/", DeleteAccountView.as_view(), name="account-delete"),
    path("auth/blocks/", BlockedUsersView.as_view(), name="blocked-users"),
    path("me/", MeView.as_view(), name="me"),
    path("privacy/", AccountPrivacyView.as_view(), name="account-privacy"),
    path("follow-requests/", FollowRequestsView.as_view(), name="follow-requests"),
    path(
        "follow-requests/<int:request_id>/accept/",
        FollowRequestAcceptView.as_view(),
        name="follow-request-accept",
    ),
    path(
        "follow-requests/<int:request_id>/decline/",
        FollowRequestDeclineView.as_view(),
        name="follow-request-decline",
    ),
    path("close-friends/", CloseFriendsView.as_view(), name="close-friends"),
    path(
        "close-friends/<str:username>/",
        CloseFriendDetailView.as_view(),
        name="close-friend-detail",
    ),
    path("people/", PaginatedPeopleView.as_view(), name="people"),
    path("people/<str:username>/", PersonView.as_view(), name="person-detail"),
    path(
        "people/<str:username>/posts/",
        PaginatedPersonPostsView.as_view(),
        name="person-posts",
    ),
    path(
        "people/<str:username>/followers/",
        FollowersView.as_view(),
        name="person-followers",
    ),
    path(
        "people/<str:username>/following/",
        FollowingView.as_view(),
        name="person-following-list",
    ),
    path("people/<str:username>/follow/", FollowView.as_view(), name="person-follow"),
    path("people/<str:username>/block/", UserBlockView.as_view(), name="person-block"),
    path("people/<str:username>/report/", UserReportView.as_view(), name="person-report"),
    path("stories/", StoryFeedView.as_view(), name="stories"),
    path("stories/<int:story_id>/", StoryDetailView.as_view(), name="story-detail"),
    path("stories/<int:story_id>/view/", StoryViewedView.as_view(), name="story-view"),
    path("stories/<int:story_id>/viewers/", StoryViewersView.as_view(), name="story-viewers"),
    path("stories/<int:story_id>/reaction/", StoryReactionView.as_view(), name="story-reaction"),
    path("stories/<int:story_id>/reply/", StoryReplyView.as_view(), name="story-reply"),
    path("posts/", PostsView.as_view(), name="posts"),
    path("posts/<int:post_id>/", PostDetailView.as_view(), name="post-detail"),
    path("posts/<int:post_id>/like/", PostLikeView.as_view(), name="post-like"),
    path("posts/<int:post_id>/repost/", PostRepostView.as_view(), name="post-repost"),
    path("posts/<int:post_id>/comments/", PostCommentsView.as_view(), name="post-comments"),
    path("comments/<int:comment_id>/", CommentDetailView.as_view(), name="comment-detail"),
    path("feed/", SharingFeedView.as_view(), name="feed"),
    path("shares/messages/", MessageShareView.as_view(), name="message-share"),
    path("notifications/", NotificationsView.as_view(), name="notifications"),
    path("notifications/read/", NotificationsReadView.as_view(), name="notifications-read"),
    path("push/devices/", DevicePushTokenView.as_view(), name="push-devices"),
    path("calls/", ReliableCallSessionCreateView.as_view(), name="call-create"),
    path("calls/ice/", CallIceConfigView.as_view(), name="call-ice-config"),
    path("calls/<uuid:call_id>/", CallSessionDetailView.as_view(), name="call-detail"),
    path("calls/<uuid:call_id>/action/", CallSessionActionView.as_view(), name="call-action"),
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
