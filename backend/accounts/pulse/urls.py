from django.urls import path

from .views import PulseDetailView, PulseFeedView


urlpatterns = [
    path("pulses/", PulseFeedView.as_view(), name="pulse-feed"),
    path("pulses/<int:pulse_id>/", PulseDetailView.as_view(), name="pulse-detail"),
]
