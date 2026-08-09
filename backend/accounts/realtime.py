from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncJsonWebsocketConsumer
from django.contrib.auth.models import AnonymousUser
from django.db.models import Q
from django.utils import timezone
from rest_framework.exceptions import AuthenticationFailed
from rest_framework_simplejwt.authentication import JWTAuthentication
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError

from .models import Conversation, Message


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
        self.joined_group = False

        user = self.scope.get("user")
        if not user or not user.is_authenticated:
            await self.close(code=4401)
            return

        if not await self._can_access_conversation(user.pk, self.conversation_id):
            await self.close(code=4403)
            return

        await self.channel_layer.group_add(self.group_name, self.channel_name)
        self.joined_group = True
        await self.accept()
        await self.send_json({"type": "ready", "conversation_id": self.conversation_id})

    async def disconnect(self, close_code):
        group_name = getattr(self, "group_name", None)
        if group_name and getattr(self, "joined_group", False):
            user = self.scope.get("user")
            if user and user.is_authenticated:
                await self.channel_layer.group_send(
                    group_name,
                    {
                        "type": "conversation.typing",
                        "user_id": user.pk,
                        "username": user.username,
                        "is_typing": False,
                    },
                )
            await self.channel_layer.group_discard(group_name, self.channel_name)
            self.joined_group = False

    async def receive_json(self, content, **kwargs):
        if not getattr(self, "joined_group", False):
            return

        event_type = content.get("type")

        if event_type == "ping":
            await self.send_json({"type": "pong"})
            return

        user = self.scope.get("user")
        if not user or not user.is_authenticated:
            return

        if event_type == "typing":
            await self.channel_layer.group_send(
                self.group_name,
                {
                    "type": "conversation.typing",
                    "user_id": user.pk,
                    "username": user.username,
                    "is_typing": bool(content.get("is_typing")),
                },
            )
            return

        if event_type == "message.delivered":
            try:
                message_id = int(content.get("message_id"))
            except (TypeError, ValueError):
                return

            delivered = await self._mark_delivered(user.pk, message_id)
            if delivered is not None:
                await self.channel_layer.group_send(
                    self.group_name,
                    {
                        "type": "messages.delivered",
                        "recipient_id": user.pk,
                        "delivered_at": delivered["delivered_at"],
                        "message_ids": [delivered["message_id"]],
                    },
                )

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

    async def messages_delivered(self, event):
        await self.send_json(
            {
                "type": "messages.delivered",
                "recipient_id": event["recipient_id"],
                "delivered_at": event["delivered_at"],
                "message_ids": event["message_ids"],
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

    async def conversation_typing(self, event):
        user = self.scope.get("user")
        if user and event["user_id"] == user.pk:
            return

        await self.send_json(
            {
                "type": "typing",
                "user_id": event["user_id"],
                "username": event["username"],
                "is_typing": event["is_typing"],
            }
        )

    @database_sync_to_async
    def _can_access_conversation(self, user_id, conversation_id):
        return Conversation.objects.filter(pk=conversation_id).filter(
            Q(participant_one_id=user_id) | Q(participant_two_id=user_id)
        ).exists()

    @database_sync_to_async
    def _mark_delivered(self, user_id, message_id):
        message = Message.objects.filter(
            pk=message_id,
            conversation_id=self.conversation_id,
            recipient_id=user_id,
        ).first()
        if message is None or message.delivered_at is not None:
            return None

        delivered_at = message.read_at or timezone.now()
        updated = Message.objects.filter(
            pk=message.pk,
            recipient_id=user_id,
            delivered_at__isnull=True,
        ).update(delivered_at=delivered_at)
        if not updated:
            return None

        return {
            "message_id": message.pk,
            "delivered_at": delivered_at.isoformat(),
        }
