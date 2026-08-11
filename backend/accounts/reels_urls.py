from django.urls import path

from .reels import (
    ReelCommentDetailView,
    ReelCommentsView,
    ReelDetailView,
    ReelFeedView,
    ReelLikeView,
)


urlpatterns = [
    path("reels/", ReelFeedView.as_view(), name="reels"),
    path("reels/<int:reel_id>/", ReelDetailView.as_view(), name="reel-detail"),
    path("reels/<int:reel_id>/like/", ReelLikeView.as_view(), name="reel-like"),
    path("reels/<int:reel_id>/comments/", ReelCommentsView.as_view(), name="reel-comments"),
    path(
        "reel-comments/<int:comment_id>/",
        ReelCommentDetailView.as_view(),
        name="reel-comment-detail",
    ),
]
