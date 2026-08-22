from django.urls import path

from .views import OrbitFeedView


urlpatterns = [
    path("orbit/", OrbitFeedView.as_view(), name="orbit-feed"),
]
