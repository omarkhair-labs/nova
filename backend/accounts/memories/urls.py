from django.urls import path

from .film_views import MemoryFilmPlanView
from .views import WeeklyMemoryView
from .draft_views import MemoryDraftDetailView, MemoryDraftListView


urlpatterns = [
    path("memories/drafts/", MemoryDraftListView.as_view(), name="memory-drafts"),
    path("memories/drafts/<int:draft_id>/", MemoryDraftDetailView.as_view(), name="memory-draft-detail"),
    path("memories/week/", WeeklyMemoryView.as_view(), name="memory-week"),
    # Keep the client-facing route compatible with the current Android build.
    path(
        "memories/film-plan/",
        MemoryFilmPlanView.as_view(),
        name="memory-film-plan-client",
    ),
    path(
        "memories/week/film-plan/",
        MemoryFilmPlanView.as_view(),
        name="memory-film-plan",
    ),
]
