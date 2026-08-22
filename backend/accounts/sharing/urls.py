from django.urls import path

from .views import MessageShareView, PostRepostView, SharingFeedView


urlpatterns = [
    path("posts/<int:post_id>/repost/", PostRepostView.as_view(), name="post-repost"),
    path("feed/", SharingFeedView.as_view(), name="feed"),
    path("shares/messages/", MessageShareView.as_view(), name="message-share"),
]
