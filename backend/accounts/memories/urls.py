from django.urls import path

from .views import WeeklyMemoryView


urlpatterns = [
    path("memories/week/", WeeklyMemoryView.as_view(), name="memory-week"),
]
