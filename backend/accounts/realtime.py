from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncJsonWebsocketConsumer
from django.contrib.auth.models import AnonymousUser
from django.db.models import Q
from rest_framework.exceptions import AuthenticationFailed
from rest_framework_simplejwt.authentication import JWTAuthentication
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError

from .models import Conversation


def conversation_group_name(conversation_id):
    return f"conversation.{conversation_id}"


class JwtAuthMiddleware:
    """Authenticate Nova WebSockets from the same Bearer JWT used by the REST API."""

    def __init__(self, inner):
        self.inner = inner

    async def __call__(self, scope, receive, send):
        scope = dict(scope)
        scope["user"] = await self._authenticate(scope)
        return await self.inner(scope, receive, send)

    @database_sync_to_async
    def _authenticate(self, scope):
        headers = {
            key.lower(): value
            for key, value in scope.get("headers", [])
        }
        raw_header = headers.get(b"authorization", b"").decode("latin1").strip()
        if not raw_header.lower().startswith("bearer "):
            return AnonymousUser()

        raw_token = raw_header[7:].strip()
        if not raw_token:
            return AnonymousUser()

        authentication = JWTAuthentication()
        try:
            validated = authentication.get_validated_token(raw_token)
            return authentication.get_user(validated)
        except (InvalidToken, TokenError, AuthenticationFailed):
            return AnonymousUser()


class ConversationConsumer(AsyncJsonWebsocketConsumer):
    async def connect(self):
        self.conversation_id = int(self.scope["url_route"]["kwargs"]["conversation_id"])
        self.group_name = conversation_group_name(self.conversation_id)

        user = self.scope.get("user")
        if not user or not user.is_authenticated:
            await self.close(code=4401)
            return

        if not await self._can_access_conversation(user.pk, self.conversation_id):
            await self.close(code=4403)
            return

        await self.channel_layer.group_add(self.group_name, self.channel_name)
        await self.accept()
        await self.send_json({"type": "ready", "conversation_id": self.conversation_id})

    async def disconnect(self, close_code):
        group_name = getattr(self, "group_name", None)
        if group_name:
            await self.channel_layer.group_discard(group_name, self.channel_name)

    async def receive_json(self, content, **kwargs):
        if content.get("type") == "ping":
            await self.send_json({"type": "pong"})

    async def message_created(self, event):
        message = dict(event["message"])
        sender = message.get("sender") or {}
        user = self.scope.get("user")
        message["is_mine"] = bool(user and sender.get("id") == user.pk)
        await self.send_json(
            {
                "type": "message.created",
                "message": message,
            }
        )

    async def conversation_read(self, event):
        await self.send_json(
            {
                "type": "conversation.read",
                "reader_id": event["reader_id"],
                "read_at": event["read_at"],
                "message_ids": event["message_ids"],
            }
        )

    @database_sync_to_async
    def _can_access_conversation(self, user_id, conversation_id):
        return Conversation.objects.filter(pk=conversation_id).filter(
            Q(participant_one_id=user_id) | Q(participant_two_id=user_id)
        ).exists()
