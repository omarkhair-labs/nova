"""Temporary URL adapter until calls.py moves into its domain package in #176."""

from django.urls import path

from .call_reliability_view import ReliableCallSessionCreateView
from .calls import CallIceConfigView, CallSessionActionView, CallSessionDetailView


urlpatterns = [
    path("calls/", ReliableCallSessionCreateView.as_view(), name="call-create"),
    path("calls/ice/", CallIceConfigView.as_view(), name="call-ice-config"),
    path("calls/<uuid:call_id>/", CallSessionDetailView.as_view(), name="call-detail"),
    path("calls/<uuid:call_id>/action/", CallSessionActionView.as_view(), name="call-action"),
]
