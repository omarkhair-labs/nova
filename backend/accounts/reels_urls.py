from django.urls import path

from .comment_threads import (
    ReelCommentReplyDetailView,
    ThreadReelCommentDetailView,
    ThreadReelCommentsView,
)
from .profile_reels import ProfileReelsView
from .reels import (
    ReelDetailView,
    ReelFeedView,
    ReelLikeView,
    ReelRepostView,
    ReelWatchView,
)


urlpatterns = [
    path("reels/", ReelFeedView.as_view(), name="reels"),
    path("reels/profile/<str:username>/", ProfileReelsView.as_view(), name="profile-reels"),
    path("reels/<int:reel_id>/", ReelDetailView.as_view(), name="reel-detail"),
    path("reels/<int:reel_id>/watch/", ReelWatchView.as_view(), name="reel-watch"),
    path("reels/<int:reel_id>/like/", ReelLikeView.as_view(), name="reel-like"),
    path("reels/<int:reel_id>/repost/", ReelRepostView.as_view(), name="reel-repost"),
    path("reels/<int:reel_id>/comments/", ThreadReelCommentsView.as_view(), name="reel-comments"),
    path(
        "reel-comments/<int:comment_id>/",
        ThreadReelCommentDetailView.as_view(),
        name="reel-comment-detail",
    ),
    path(
        "reel-comment-replies/<int:reply_id>/",
        ReelCommentReplyDetailView.as_view(),
        name="reel-comment-reply-detail",
    ),
]
