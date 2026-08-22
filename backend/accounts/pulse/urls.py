from django.urls import path

from .views import PulseChainView, PulseDetailView, PulseFeedView, PulseReplyView


urlpatterns = [
    path("pulses/", PulseFeedView.as_view(), name="pulse-feed"),
    path("pulses/<int:pulse_id>/", PulseDetailView.as_view(), name="pulse-detail"),
    path("pulses/<int:pulse_id>/reply/", PulseReplyView.as_view(), name="pulse-reply"),
    path("pulses/<int:pulse_id>/chain/", PulseChainView.as_view(), name="pulse-chain"),
]
