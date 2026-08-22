from django.urls import path

from .film_views import MemoryFilmPlanView
from .views import WeeklyMemoryView


urlpatterns = [
    path("memories/week/", WeeklyMemoryView.as_view(), name="memory-week"),
    path(
        "memories/week/film-plan/",
        MemoryFilmPlanView.as_view(),
        name="memory-film-plan",
    ),
]
