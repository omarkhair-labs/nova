from channels.db import database_sync_to_async
from channels.testing import WebsocketCommunicator
from django.contrib.auth import get_user_model
from django.test import TransactionTestCase
from rest_framework_simplejwt.tokens import AccessToken

from nova_backend.asgi import application

from .models import Conversation, Message


User = get_user_model()


class MessagingRealtimeSocketTests(TransactionTestCase):
    reset_sequences = True

    def setUp(self):
        self.omar = User.objects.create_user(
            email="omar-realtime@example.com",
            username="omar-realtime",
            password="StrongNovaPass2026!",
            name="Omar",
        )
        self.maya = User.objects.create_user(
            email="maya-realtime@example.com",
            username="maya-realtime",
            password="StrongNovaPass2026!",
            name="Maya",
        )
        self.stranger = User.objects.create_user(
            email="stranger-realtime@example.com",
            username="stranger-realtime",
            password="StrongNovaPass2026!",
            name="Stranger",
        )
        first, second = sorted((self.omar.pk, self.maya.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )
        self.path = f"/ws/conversations/{self.conversation.pk}/"

    def communicator(self, user):
        token = str(AccessToken.for_user(user))
        return WebsocketCommunicator(
            application,
            self.path,
            headers=[(b"authorization", f"Bearer {token}".encode("ascii"))],
        )

    async def connect_participant(self, user):
        communicator = self.communicator(user)
        connected, _ = await communicator.connect()
        self.assertTrue(connected)
        ready = await communicator.receive_json_from(timeout=1)
        self.assertEqual(ready["type"], "ready")
        self.assertEqual(ready["conversation_id"], self.conversation.pk)
        return communicator

    async def test_nonparticipant_is_rejected(self):
        communicator = self.communicator(self.stranger)
        connected, close_code = await communicator.connect()
        self.assertFalse(connected)
        self.assertEqual(close_code, 4403)

    async def test_typing_is_ephemeral_and_not_echoed_to_sender(self):
        omar_socket = await self.connect_participant(self.omar)
        maya_socket = await self.connect_participant(self.maya)

        try:
            await omar_socket.send_json_to({"type": "typing", "is_typing": True})
            event = await maya_socket.receive_json_from(timeout=1)
            self.assertEqual(event["type"], "typing")
            self.assertEqual(event["user_id"], self.omar.pk)
            self.assertEqual(event["username"], self.omar.username)
            self.assertTrue(event["is_typing"])
            self.assertTrue(await omar_socket.receive_nothing(timeout=0.15))

            await omar_socket.send_json_to({"type": "typing", "is_typing": False})
            stopped = await maya_socket.receive_json_from(timeout=1)
            self.assertEqual(stopped["type"], "typing")
            self.assertEqual(stopped["user_id"], self.omar.pk)
            self.assertFalse(stopped["is_typing"])
        finally:
            await omar_socket.disconnect()
            await maya_socket.disconnect()

    async def test_only_recipient_can_ack_delivery_and_ack_is_idempotent(self):
        message = await database_sync_to_async(Message.objects.create)(
            conversation=self.conversation,
            sender=self.omar,
            recipient=self.maya,
            body="Delivered through the socket",
            client_id="socket-delivery-1",
        )
        omar_socket = await self.connect_participant(self.omar)
        maya_socket = await self.connect_participant(self.maya)

        try:
            # The sender cannot mark their own outgoing message delivered.
            await omar_socket.send_json_to(
                {"type": "message.delivered", "message_id": message.pk}
            )
            self.assertTrue(await omar_socket.receive_nothing(timeout=0.15))
            unchanged = await database_sync_to_async(Message.objects.get)(pk=message.pk)
            self.assertIsNone(unchanged.delivered_at)

            # The actual recipient can acknowledge it.
            await maya_socket.send_json_to(
                {"type": "message.delivered", "message_id": message.pk}
            )
            event = await omar_socket.receive_json_from(timeout=1)
            self.assertEqual(event["type"], "messages.delivered")
            self.assertEqual(event["recipient_id"], self.maya.pk)
            self.assertEqual(event["message_ids"], [message.pk])
            self.assertTrue(event["delivered_at"])

            delivered = await database_sync_to_async(Message.objects.get)(pk=message.pk)
            self.assertIsNotNone(delivered.delivered_at)

            # A retry is a no-op and must not emit another delivery transition.
            await maya_socket.send_json_to(
                {"type": "message.delivered", "message_id": message.pk}
            )
            self.assertTrue(await omar_socket.receive_nothing(timeout=0.15))
        finally:
            await omar_socket.disconnect()
            await maya_socket.disconnect()
