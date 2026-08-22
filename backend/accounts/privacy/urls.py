from django.urls import path

from .views import (
    AccountPrivacyView,
    CloseFriendDetailView,
    CloseFriendsView,
    FollowRequestAcceptView,
    FollowRequestDeclineView,
    FollowRequestsView,
)


urlpatterns = [
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
]
