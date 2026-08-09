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
        self.assertIn("presence", ready)
        return communicator, ready

    async def connect_pair(self):
        omar_socket, omar_ready = await self.connect_participant(self.omar)
        maya_socket, maya_ready = await self.connect_participant(self.maya)
        maya_online = await omar_socket.receive_json_from(timeout=1)
        self.assertEqual(maya_online["type"], "presence")
        self.assertEqual(maya_online["user_id"], self.maya.pk)
        self.assertTrue(maya_online["is_online"])
        return omar_socket, maya_socket, omar_ready, maya_ready

    async def test_nonparticipant_is_rejected(self):
        communicator = self.communicator(self.stranger)
        connected, close_code = await communicator.connect()
        self.assertFalse(connected)
        self.assertEqual(close_code, 4403)

    async def test_ready_and_disconnect_publish_presence_with_last_seen(self):
        omar_socket, omar_ready = await self.connect_participant(self.omar)
        self.assertEqual(omar_ready["presence"]["user_id"], self.maya.pk)
        self.assertFalse(omar_ready["presence"]["is_online"])

        maya_socket, maya_ready = await self.connect_participant(self.maya)
        self.assertEqual(maya_ready["presence"]["user_id"], self.omar.pk)
        self.assertTrue(maya_ready["presence"]["is_online"])

        online = await omar_socket.receive_json_from(timeout=1)
        self.assertEqual(online["type"], "presence")
        self.assertEqual(online["user_id"], self.maya.pk)
        self.assertTrue(online["is_online"])
        self.assertIsNone(online["last_seen_at"])

        second_maya_socket, _ = await self.connect_participant(self.maya)
        self.assertTrue(await omar_socket.receive_nothing(timeout=0.15))

        await maya_socket.disconnect()
        self.assertTrue(await omar_socket.receive_nothing(timeout=0.15))

        await second_maya_socket.disconnect()
        offline = await omar_socket.receive_json_from(timeout=1)
        self.assertEqual(offline["type"], "presence")
        self.assertEqual(offline["user_id"], self.maya.pk)
        self.assertFalse(offline["is_online"])
        self.assertTrue(offline["last_seen_at"])

        refreshed = await database_sync_to_async(User.objects.get)(pk=self.maya.pk)
        self.assertIsNotNone(refreshed.last_seen_at)
        await omar_socket.disconnect()

    async def test_typing_is_ephemeral_and_not_echoed_to_sender(self):
        omar_socket, maya_socket, _, _ = await self.connect_pair()

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
        omar_socket, maya_socket, _, _ = await self.connect_pair()

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
