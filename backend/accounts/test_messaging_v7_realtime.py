from channels.db import database_sync_to_async
from channels.testing import WebsocketCommunicator
from django.contrib.auth import get_user_model
from django.test import TransactionTestCase
from django.utils import timezone
from rest_framework_simplejwt.tokens import AccessToken

from nova_backend.asgi import application

from .messaging_realtime import broadcast_message_deleted, broadcast_message_updated
from .models import Conversation, Message


User = get_user_model()


class MessagingV7RealtimeTests(TransactionTestCase):
    reset_sequences = True

    def setUp(self):
        self.omar = User.objects.create_user(
            email="omar-v7-socket@example.com",
            username="omar-v7-socket",
            password="StrongNovaPass2026!",
        )
        self.maya = User.objects.create_user(
            email="maya-v7-socket@example.com",
            username="maya-v7-socket",
            password="StrongNovaPass2026!",
        )
        first, second = sorted((self.omar.pk, self.maya.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )
        self.message = Message.objects.create(
            conversation=self.conversation,
            sender=self.omar,
            recipient=self.maya,
            body="Original",
            client_id="v7-socket-message",
        )
        self.path = f"/ws/conversations/{self.conversation.pk}/"

    def communicator(self, user):
        token = str(AccessToken.for_user(user))
        return WebsocketCommunicator(
            application,
            self.path,
            headers=[(b"authorization", f"Bearer {token}".encode("ascii"))],
        )

    async def connect_pair(self):
        omar_socket = self.communicator(self.omar)
        maya_socket = self.communicator(self.maya)

        connected, _ = await omar_socket.connect()
        self.assertTrue(connected)
        ready = await omar_socket.receive_json_from(timeout=1)
        self.assertEqual(ready["type"], "ready")

        connected, _ = await maya_socket.connect()
        self.assertTrue(connected)
        ready = await maya_socket.receive_json_from(timeout=1)
        self.assertEqual(ready["type"], "ready")

        maya_online = await omar_socket.receive_json_from(timeout=1)
        self.assertEqual(maya_online["type"], "presence")
        return omar_socket, maya_socket

    async def test_edit_and_delete_events_reach_both_participants(self):
        omar_socket, maya_socket = await self.connect_pair()
        try:
            edited_at = timezone.now()
            await database_sync_to_async(broadcast_message_updated)(
                conversation_id=self.conversation.pk,
                message_id=self.message.pk,
                body="Edited live",
                edited_at=edited_at,
            )

            omar_edit = await omar_socket.receive_json_from(timeout=1)
            maya_edit = await maya_socket.receive_json_from(timeout=1)
            self.assertEqual(omar_edit["type"], "message.updated")
            self.assertEqual(maya_edit["type"], "message.updated")
            self.assertEqual(omar_edit["message_id"], self.message.pk)
            self.assertEqual(maya_edit["body"], "Edited live")
            self.assertEqual(omar_edit["edited_at"], edited_at.isoformat())

            deleted_at = timezone.now()
            await database_sync_to_async(broadcast_message_deleted)(
                conversation_id=self.conversation.pk,
                message_id=self.message.pk,
                deleted_at=deleted_at,
            )

            omar_delete = await omar_socket.receive_json_from(timeout=1)
            maya_delete = await maya_socket.receive_json_from(timeout=1)
            self.assertEqual(omar_delete["type"], "message.deleted")
            self.assertEqual(maya_delete["type"], "message.deleted")
            self.assertEqual(omar_delete["message_id"], self.message.pk)
            self.assertEqual(maya_delete["deleted_at"], deleted_at.isoformat())
        finally:
            await omar_socket.disconnect()
            await maya_socket.disconnect()
