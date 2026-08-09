from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase


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

    @patch("accounts.messaging_views.send_message_push")
    @patch("accounts.messaging_views.broadcast_message_created")
    def test_new_message_broadcasts_and_pushes_once(self, broadcast, push):
        payload = {"body": "Realtime hello", "client_id": "delivery-1"}

        first = self.client.post(self.messages_url, payload, format="json")
        self.assertEqual(first.status_code, status.HTTP_201_CREATED)
        broadcast.assert_called_once()
        push.assert_called_once()

        repeated = self.client.post(self.messages_url, payload, format="json")
        self.assertEqual(repeated.status_code, status.HTTP_200_OK)
        self.assertEqual(repeated.data["id"], first.data["id"])
        broadcast.assert_called_once()
        push.assert_called_once()

    @patch("accounts.messaging_views.broadcast_conversation_read")
    def test_mark_read_broadcasts_exact_message_ids(self, broadcast_read):
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
        broadcast_read.assert_called_once()
        kwargs = broadcast_read.call_args.kwargs
        self.assertEqual(kwargs["conversation_id"], self.conversation_id)
        self.assertEqual(kwargs["reader_id"], self.maya.pk)
        self.assertEqual(kwargs["message_ids"], [sent.data["id"]])
