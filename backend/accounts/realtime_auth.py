from channels.db import database_sync_to_async
from django.contrib.auth.models import AnonymousUser
from rest_framework.exceptions import AuthenticationFailed
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError

from .account_security import NovaJWTAuthentication


class SecureJwtAuthMiddleware:
    """Apply the same password-bound session rules to REST and WebSockets.

    Authentication is checked when the socket opens and again before an
    application frame is processed or delivered. A password reset/change or
    "log out other devices" therefore cuts an old live socket off at its next
    message instead of waiting indefinitely for a reconnect.
    """

    def __init__(self, inner):
        self.inner = inner

    async def __call__(self, scope, receive, send):
        scope = dict(scope)
        raw_token = self._raw_token(scope)
        user = await self._authenticate_token(raw_token)
        scope["user"] = user
        revoked = False

        async def session_is_current():
            if not user or not user.is_authenticated or not raw_token:
                return False
            checked = await self._authenticate_token(raw_token)
            return bool(
                checked
                and checked.is_authenticated
                and checked.pk == user.pk
            )

        async def secure_receive():
            nonlocal revoked
            message = await receive()
            if (
                message.get("type") == "websocket.receive"
                and user
                and user.is_authenticated
                and not revoked
                and not await session_is_current()
            ):
                revoked = True
                await send({"type": "websocket.close", "code": 4401})
                return {"type": "websocket.disconnect", "code": 4401}
            return message

        async def secure_send(message):
            nonlocal revoked
            if revoked:
                return
            if (
                message.get("type") == "websocket.send"
                and user
                and user.is_authenticated
                and not await session_is_current()
            ):
                revoked = True
                await send({"type": "websocket.close", "code": 4401})
                return
            await send(message)

        return await self.inner(scope, secure_receive, secure_send)

    @staticmethod
    def _raw_token(scope):
        headers = {key.lower(): value for key, value in scope.get("headers", [])}
        raw_header = headers.get(b"authorization", b"").decode("latin1").strip()
        if not raw_header.lower().startswith("bearer "):
            return ""
        return raw_header[7:].strip()

    @database_sync_to_async
    def _authenticate_token(self, raw_token):
        if not raw_token:
            return AnonymousUser()

        authentication = NovaJWTAuthentication()
        try:
            validated = authentication.get_validated_token(raw_token)
            return authentication.get_user(validated)
        except (InvalidToken, TokenError, AuthenticationFailed):
            return AnonymousUser()

    async def _authenticate(self, scope):
        """Test/helper compatibility for direct middleware authentication."""
        return await self._authenticate_token(self._raw_token(scope))
