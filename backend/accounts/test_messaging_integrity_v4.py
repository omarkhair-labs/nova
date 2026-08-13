from django.contrib.auth import get_user_model
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Conversation, Message


User = get_user_model()


class MessagingIntegrityV4Tests(APITestCase):
    def setUp(self):
        self.caller = User.objects.create_user(
            email="caller-v4@example.com",
            username="caller-v4",
            password="StrongNovaPass2026!",
            name="Caller",
        )
        self.callee = User.objects.create_user(
            email="callee-v4@example.com",
            username="callee-v4",
            password="StrongNovaPass2026!",
            name="Callee",
        )
        first, second = sorted((self.caller.pk, self.callee.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )

    def test_call_history_message_cannot_be_edited_but_can_be_deleted(self):
        message = Message.objects.create(
            conversation=self.conversation,
            sender=self.caller,
            recipient=self.callee,
            body="Voice call · 0:25",
            client_id="call:4242",
        )
        url = f"/api/v1/messages/{message.pk}/"
        self.client.force_authenticate(user=self.caller)

        edited = self.client.post(url, {"body": "Voice call · 0:10"}, format="json")
        self.assertEqual(edited.status_code, status.HTTP_409_CONFLICT)
        self.assertEqual(edited.data["detail"], "Call history can't be edited.")
        message.refresh_from_db()
        self.assertEqual(message.body, "Voice call · 0:25")
        self.assertIsNone(message.deleted_at)

        deleted = self.client.delete(url)
        self.assertEqual(deleted.status_code, status.HTTP_200_OK)
        message.refresh_from_db()
        self.assertIsNotNone(message.deleted_at)
