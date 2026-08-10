import asyncio

from channels.db import database_sync_to_async
from channels.generic.websocket import AsyncJsonWebsocketConsumer
from django.contrib.auth.models import AnonymousUser
from django.db.models import Q
from django.utils import timezone
from rest_framework.exceptions import AuthenticationFailed
from rest_framework_simplejwt.authentication import JWTAuthentication
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError

from .messaging_models import GroupMembership
from .models import Conversation, Message, User, UserBlock
from .presence_store import is_online, refresh_lease, register_lease, unregister_lease


PRESENCE_REFRESH_SECONDS = 30


def conversation_group_name(conversation_id):
    return f"conversation.{conversation_id}"


def user_presence_group_name(user_id):
    return f"presence.user.{int(user_id)}"


@database_sync_to_async
def set_user_last_seen(user_id):
    now = timezone.now()
    User.objects.filter(pk=user_id).update(last_seen_at=now)
    return now.isoformat()


async def broadcast_presence(channel_layer, user, is_online_value, last_seen_at=None):
    await channel_layer.group_send(
        user_presence_group_name(user.pk),
        {
            "type": "presence.changed",
            "user_id": user.pk,
            "username": user.username,
            "is_online": bool(is_online_value),
            "last_seen_at": last_seen_at,
        },
    )


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
        headers = {key.lower(): value for key, value in scope.get("headers", [])}
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


class PresenceLeaseMixin:
    presence_registered = False
    presence_refresh_task = None

    async def start_presence_lease(self, user):
        self.presence_lease_id = f"{self.__class__.__name__}:{self.channel_name}"
        became_online, _ = await register_lease(user.pk, self.presence_lease_id)
        self.presence_registered = True
        self.presence_refresh_task = asyncio.create_task(self._presence_refresh_loop(user.pk))
        if became_online:
            await broadcast_presence(self.channel_layer, user, True, None)

    async def stop_presence_lease(self, user):
        task = getattr(self, "presence_refresh_task", None)
        if task is not None:
            task.cancel()
            try:
                await task
            except asyncio.CancelledError:
                pass
            self.presence_refresh_task = None

        if not getattr(self, "presence_registered", False):
            return
        self.presence_registered = False

        remaining = await unregister_lease(user.pk, self.presence_lease_id)
        if remaining == 0:
            last_seen_at = await set_user_last_seen(user.pk)
            await broadcast_presence(self.channel_layer, user, False, last_seen_at)

    async def _presence_refresh_loop(self, user_id):
        try:
            while True:
                await asyncio.sleep(PRESENCE_REFRESH_SECONDS)
                await refresh_lease(user_id, self.presence_lease_id)
        except asyncio.CancelledError:
            raise


class PresenceConsumer(PresenceLeaseMixin, AsyncJsonWebsocketConsumer):
    """One app-wide socket while Nova is in the foreground."""

    async def connect(self):
        user = self.scope.get("user")
        if not user or not user.is_authenticated:
            await self.close(code=4401)
            return

        await self.accept()
        await self.start_presence_lease(user)
        await self.send_json(
            {
                "type": "presence.ready",
                "user_id": user.pk,
                "username": user.username,
                "is_online": True,
            }
        )

    async def disconnect(self, close_code):
        user = self.scope.get("user")
        if user and user.is_authenticated:
            await self.stop_presence_lease(user)

    async def receive_json(self, content, **kwargs):
        if content.get("type") == "ping":
            await self.send_json({"type": "pong"})


class ConversationConsumer(PresenceLeaseMixin, AsyncJsonWebsocketConsumer):
    async def connect(self):
        self.conversation_id = int(self.scope["url_route"]["kwargs"]["conversation_id"])
        self.group_name = conversation_group_name(self.conversation_id)
        self.joined_group = False
        self.watching_presence_group = None
        self.is_typing = False

        user = self.scope.get("user")
        if not user or not user.is_authenticated:
            await self.close(code=4401)
            return

        if not await self._can_access_conversation(user.pk, self.conversation_id):
            await self.close(code=4403)
            return

        other_presence = await self._other_participant_presence(user.pk, self.conversation_id)
        if other_presence is not None:
            self.watching_presence_group = user_presence_group_name(other_presence["user_id"])

        await self.channel_layer.group_add(self.group_name, self.channel_name)
        if self.watching_presence_group:
            await self.channel_layer.group_add(self.watching_presence_group, self.channel_name)
        self.joined_group = True
        await self.start_presence_lease(user)

        await self.accept()
        ready = {
            "type": "ready",
            "conversation_id": self.conversation_id,
        }
        if other_presence is not None:
            ready["presence"] = {
                **other_presence,
                "is_online": await is_online(other_presence["user_id"]),
            }
        await self.send_json(ready)

    async def disconnect(self, close_code):
        group_name = getattr(self, "group_name", None)
        user = self.scope.get("user")

        if group_name and getattr(self, "joined_group", False):
            if user and user.is_authenticated:
                if getattr(self, "is_typing", False):
                    self.is_typing = False
                    await self.channel_layer.group_send(
                        group_name,
                        {
                            "type": "conversation.typing",
                            "user_id": user.pk,
                            "username": user.username,
                            "is_typing": False,
                        },
                    )
                await self.stop_presence_lease(user)

            presence_group = getattr(self, "watching_presence_group", None)
            if presence_group:
                await self.channel_layer.group_discard(presence_group, self.channel_name)
            await self.channel_layer.group_discard(group_name, self.channel_name)
            self.joined_group = False

    async def _allow_realtime_event(self):
        user = self.scope.get("user")
        if not user or not user.is_authenticated:
            await self.close(code=4401)
            return False
        if await self._can_access_conversation(user.pk, self.conversation_id):
            return True
        await self.close(code=4403)
        return False

    async def receive_json(self, content, **kwargs):
        if not getattr(self, "joined_group", False):
            return
        if not await self._allow_realtime_event():
            return

        event_type = content.get("type")
        if event_type == "ping":
            await self.send_json({"type": "pong"})
            return

        user = self.scope.get("user")
        if event_type == "typing":
            self.is_typing = bool(content.get("is_typing"))
            await self.channel_layer.group_send(
                self.group_name,
                {
                    "type": "conversation.typing",
                    "user_id": user.pk,
                    "username": user.username,
                    "is_typing": self.is_typing,
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
        if not await self._allow_realtime_event():
            return
        message = dict(event["message"])
        sender = message.get("sender") or {}
        user = self.scope.get("user")
        message["is_mine"] = bool(user and sender.get("id") == user.pk)
        await self.send_json({"type": "message.created", "message": message})

    async def message_updated(self, event):
        if not await self._allow_realtime_event():
            return
        await self.send_json(
            {
                "type": "message.updated",
                "message_id": event["message_id"],
                "body": event["body"],
                "edited_at": event["edited_at"],
            }
        )

    async def message_deleted(self, event):
        if not await self._allow_realtime_event():
            return
        await self.send_json(
            {
                "type": "message.deleted",
                "message_id": event["message_id"],
                "deleted_at": event["deleted_at"],
            }
        )

    async def message_reaction(self, event):
        if not await self._allow_realtime_event():
            return
        user = self.scope.get("user")
        await self.send_json(
            {
                "type": "message.reaction",
                "message_id": event["message_id"],
                "user_id": event["user_id"],
                "emoji": event["emoji"],
                "active": event["active"],
                "count": event["count"],
                "is_mine": bool(user and event["user_id"] == user.pk),
            }
        )

    async def messages_delivered(self, event):
        if not await self._allow_realtime_event():
            return
        await self.send_json(
            {
                "type": "messages.delivered",
                "recipient_id": event["recipient_id"],
                "delivered_at": event["delivered_at"],
                "message_ids": event["message_ids"],
            }
        )

    async def conversation_read(self, event):
        if not await self._allow_realtime_event():
            return
        await self.send_json(
            {
                "type": "conversation.read",
                "reader_id": event["reader_id"],
                "read_at": event["read_at"],
                "message_ids": event["message_ids"],
            }
        )

    async def conversation_typing(self, event):
        if not await self._allow_realtime_event():
            return
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

    async def presence_changed(self, event):
        if not await self._allow_realtime_event():
            return
        await self.send_json(
            {
                "type": "presence",
                "user_id": event["user_id"],
                "username": event["username"],
                "is_online": event["is_online"],
                "last_seen_at": event.get("last_seen_at"),
            }
        )

    @database_sync_to_async
    def _can_access_conversation(self, user_id, conversation_id):
        conversation = Conversation.objects.select_related(
            "participant_one", "participant_two"
        ).filter(pk=conversation_id).first()
        if conversation is None:
            return False
        if conversation.kind == Conversation.Kind.GROUP:
            return GroupMembership.objects.filter(
                conversation_id=conversation_id,
                user_id=user_id,
                user__is_active=True,
            ).exists()

        if user_id not in (conversation.participant_one_id, conversation.participant_two_id):
            return False
        other = (
            conversation.participant_two
            if conversation.participant_one_id == user_id
            else conversation.participant_one
        )
        if other is None or not other.is_active:
            return False
        return not UserBlock.objects.filter(
            Q(blocker_id=user_id, blocked_id=other.pk)
            | Q(blocker_id=other.pk, blocked_id=user_id)
        ).exists()

    @database_sync_to_async
    def _other_participant_presence(self, user_id, conversation_id):
        conversation = Conversation.objects.select_related(
            "participant_one",
            "participant_two",
        ).get(pk=conversation_id)
        if conversation.kind != Conversation.Kind.DIRECT:
            return None
        other = (
            conversation.participant_two
            if conversation.participant_one_id == user_id
            else conversation.participant_one
        )
        if other is None:
            return None
        return {
            "user_id": other.pk,
            "username": other.username,
            "last_seen_at": other.last_seen_at.isoformat() if other.last_seen_at else None,
        }

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
