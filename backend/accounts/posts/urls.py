from django.urls import path

from .comments import (
    PostCommentReplyDetailView,
    PostCommentLikeView,
    PostCommentReplyLikeView,
    ThreadCommentDetailView,
    ThreadPostCommentsView,
)
from .views import PostDetailView, PostLikeView, PostsView


urlpatterns = [
    path("posts/", PostsView.as_view(), name="posts"),
    path("posts/<int:post_id>/", PostDetailView.as_view(), name="post-detail"),
    path("posts/<int:post_id>/like/", PostLikeView.as_view(), name="post-like"),
    path(
        "posts/<int:post_id>/comments/",
        ThreadPostCommentsView.as_view(),
        name="post-comments",
    ),
    path("comments/<int:comment_id>/", ThreadCommentDetailView.as_view(), name="comment-detail"),
    path(
        "comments/<int:comment_id>/like/",
        PostCommentLikeView.as_view(),
        name="comment-like",
    ),
    path(
        "comment-replies/<int:reply_id>/",
        PostCommentReplyDetailView.as_view(),
        name="comment-reply-detail",
    ),
    path(
        "comment-replies/<int:reply_id>/like/",
        PostCommentReplyLikeView.as_view(),
        name="comment-reply-like",
    ),
]
