from django.urls import re_path

from ..call_reliability_realtime import ReliableCallConsumer


websocket_urlpatterns = [
    re_path(
        r"^ws/calls/(?P<call_id>[0-9a-fA-F-]+)/$",
        ReliableCallConsumer.as_asgi(),
    ),
]
