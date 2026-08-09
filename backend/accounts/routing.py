from django.urls import re_path

from .realtime import ConversationConsumer


websocket_urlpatterns = [
    re_path(
        r"^ws/conversations/(?P<conversation_id>\d+)/$",
        ConversationConsumer.as_asgi(),
    ),
]
