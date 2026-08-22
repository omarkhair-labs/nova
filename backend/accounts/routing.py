from .calls.routing import websocket_urlpatterns as call_websocket_urlpatterns
from .messaging.routing import websocket_urlpatterns as messaging_websocket_urlpatterns


websocket_urlpatterns = [
    *messaging_websocket_urlpatterns,
    *call_websocket_urlpatterns,
]
