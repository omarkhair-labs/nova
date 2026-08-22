from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Message


User = get_user_model()


class MessagingDeliveryTests(APITestCase):
    def setUp(self):
        self.omar = User.objects.create_user(
            email="omar-delivery@example.com",
            username="omar-delivery",
            password="StrongNovaPass2026!",
            name="Omar",
        )
        self.maya = User.objects.create_user(
            email="maya-delivery@example.com",
            username="maya-delivery",
            password="StrongNovaPass2026!",
            name="Maya",
        )
        self.client.force_authenticate(self.omar)
        created = self.client.post(
            reverse("conversations"),
            {"username": self.maya.username},
            format="json",
        )
        self.assertEqual(created.status_code, status.HTTP_201_CREATED)
        self.conversation_id = created.data["id"]
        self.messages_url = reverse(
            "conversation-messages",
            kwargs={"conversation_id": self.conversation_id},
        )

    @patch("accounts.messaging.messaging_views.send_message_push")
    @patch("accounts.messaging.messaging_views.broadcast_message_created")
    def test_new_message_broadcasts_and_pushes_once(self, broadcast, push):
        payload = {"body": "Realtime hello", "client_id": "delivery-1"}

        first = self.client.post(self.messages_url, payload, format="json")
        self.assertEqual(first.status_code, status.HTTP_201_CREATED)
        self.assertIsNone(first.data["delivered_at"])
        broadcast.assert_called_once()
        push.assert_called_once()

        repeated = self.client.post(self.messages_url, payload, format="json")
        self.assertEqual(repeated.status_code, status.HTTP_200_OK)
        self.assertEqual(repeated.data["id"], first.data["id"])
        broadcast.assert_called_once()
        push.assert_called_once()

    @patch("accounts.messaging.messaging_views.broadcast_messages_delivered")
    def test_loading_received_history_marks_message_delivered(self, broadcast_delivered):
        sent = self.client.post(
            self.messages_url,
            {"body": "Deliver me", "client_id": "delivery-history-1"},
            format="json",
        )
        self.assertEqual(sent.status_code, status.HTTP_201_CREATED)

        self.client.force_authenticate(self.maya)
        history = self.client.get(self.messages_url)
        self.assertEqual(history.status_code, status.HTTP_200_OK)
        self.assertEqual(len(history.data["results"]), 1)
        self.assertIsNotNone(history.data["results"][0]["delivered_at"])

        message = Message.objects.get(pk=sent.data["id"])
        self.assertIsNotNone(message.delivered_at)
        self.assertIsNone(message.read_at)

        broadcast_delivered.assert_called_once()
        kwargs = broadcast_delivered.call_args.kwargs
        self.assertEqual(kwargs["conversation_id"], self.conversation_id)
        self.assertEqual(kwargs["recipient_id"], self.maya.pk)
        self.assertEqual(kwargs["message_ids"], [sent.data["id"]])

        self.client.get(self.messages_url)
        broadcast_delivered.assert_called_once()

    @patch("accounts.messaging.messaging_views.broadcast_messages_delivered")
    @patch("accounts.messaging.messaging_views.broadcast_conversation_read")
    def test_mark_read_guarantees_delivery_and_broadcasts_exact_ids(
        self,
        broadcast_read,
        broadcast_delivered,
    ):
        sent = self.client.post(
            self.messages_url,
            {"body": "Read me", "client_id": "delivery-read-1"},
            format="json",
        )
        self.assertEqual(sent.status_code, status.HTTP_201_CREATED)

        self.client.force_authenticate(self.maya)
        read = self.client.post(
            reverse(
                "conversation-read",
                kwargs={"conversation_id": self.conversation_id},
            ),
            {},
            format="json",
        )
        self.assertEqual(read.status_code, status.HTTP_200_OK)
        self.assertEqual(read.data["marked_read"], 1)

        message = Message.objects.get(pk=sent.data["id"])
        self.assertIsNotNone(message.delivered_at)
        self.assertIsNotNone(message.read_at)
        self.assertLessEqual(message.delivered_at, message.read_at)

        broadcast_delivered.assert_called_once()
        delivery_kwargs = broadcast_delivered.call_args.kwargs
        self.assertEqual(delivery_kwargs["conversation_id"], self.conversation_id)
        self.assertEqual(delivery_kwargs["recipient_id"], self.maya.pk)
        self.assertEqual(delivery_kwargs["message_ids"], [sent.data["id"]])

        broadcast_read.assert_called_once()
        read_kwargs = broadcast_read.call_args.kwargs
        self.assertEqual(read_kwargs["conversation_id"], self.conversation_id)
        self.assertEqual(read_kwargs["reader_id"], self.maya.pk)
        self.assertEqual(read_kwargs["message_ids"], [sent.data["id"]])
