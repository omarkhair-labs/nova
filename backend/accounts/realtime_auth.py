from channels.db import database_sync_to_async
from django.contrib.auth.models import AnonymousUser
from rest_framework.exceptions import AuthenticationFailed
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError

from .account_security import NovaJWTAuthentication


class SecureJwtAuthMiddleware:
    """Authenticate Nova WebSockets with the same revocable JWT rules as REST."""

    def __init__(self, inner):
        self.inner = inner

    async def __call__(self, scope, receive, send):
        scope = dict(scope)
        scope["user"] = await self._authenticate(scope)
        return await self.inner(scope, receive, send)

    @database_sync_to_async
    def _authenticate(self, scope):
        headers = {key.lower(): value for key, value in scope.get("headers", [])}
        raw_header = headers.get(b"authorization", b"").decode("latin1").strip()
        if not raw_header.lower().startswith("bearer "):
            return AnonymousUser()

        raw_token = raw_header[7:].strip()
        if not raw_token:
            return AnonymousUser()

        authentication = NovaJWTAuthentication()
        try:
            validated = authentication.get_validated_token(raw_token)
            return authentication.get_user(validated)
        except (InvalidToken, TokenError, AuthenticationFailed):
            return AnonymousUser()
