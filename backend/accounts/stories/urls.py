from django.urls import path

from . import (
    StoryDetailView,
    StoryFeedView,
    StoryReactionView,
    StoryReplyView,
    StoryViewedView,
    StoryViewersView,
)


urlpatterns = [
    path("stories/", StoryFeedView.as_view(), name="stories"),
    path("stories/<int:story_id>/", StoryDetailView.as_view(), name="story-detail"),
    path("stories/<int:story_id>/view/", StoryViewedView.as_view(), name="story-view"),
    path("stories/<int:story_id>/viewers/", StoryViewersView.as_view(), name="story-viewers"),
    path("stories/<int:story_id>/reaction/", StoryReactionView.as_view(), name="story-reaction"),
    path("stories/<int:story_id>/reply/", StoryReplyView.as_view(), name="story-reply"),
]
