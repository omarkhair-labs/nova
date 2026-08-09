from django.urls import re_path

from .call_realtime import CallConsumer
from .realtime import ConversationConsumer, PresenceConsumer


websocket_urlpatterns = [
    re_path(r"^ws/presence/$", PresenceConsumer.as_asgi()),
    re_path(
        r"^ws/conversations/(?P<conversation_id>\d+)/$",
        ConversationConsumer.as_asgi(),
    ),
    re_path(
        r"^ws/calls/(?P<call_id>[0-9a-fA-F-]+)/$",
        CallConsumer.as_asgi(),
    ),
]
