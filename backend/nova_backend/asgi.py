import os

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "nova_backend.settings")

from channels.routing import ProtocolTypeRouter, URLRouter
from django.core.asgi import get_asgi_application

# Initialize Django before importing app routing/consumers so model imports are safe.
django_asgi_application = get_asgi_application()

from accounts.realtime_auth import SecureJwtAuthMiddleware
from accounts.routing import websocket_urlpatterns


application = ProtocolTypeRouter(
    {
        "http": django_asgi_application,
        "websocket": SecureJwtAuthMiddleware(URLRouter(websocket_urlpatterns)),
    }
)
