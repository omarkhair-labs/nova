from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Conversation, Message

User = get_user_model()


class MessagingApiTests(APITestCase):
    def setUp(self):
        self.omar = User.objects.create_user(
            email="omar@example.com",
            username="omar",
            password="StrongNovaPass2026!",
            name="Omar",
        )
        self.maya = User.objects.create_user(
            email="maya@example.com",
            username="maya",
            password="StrongNovaPass2026!",
            name="Maya",
        )
        self.ziad = User.objects.create_user(
            email="ziad@example.com",
            username="ziad",
            password="StrongNovaPass2026!",
            name="Ziad",
        )

    def authenticate(self, user):
        self.client.force_authenticate(user=user)

    def create_conversation(self, username="maya"):
        self.authenticate(self.omar)
        return self.client.post(
            reverse("conversations"),
            {"username": username},
            format="json",
        )

    def test_direct_conversation_is_unique_and_cannot_target_self(self):
        created = self.create_conversation()
        self.assertEqual(created.status_code, status.HTTP_201_CREATED)
        self.assertEqual(created.data["other_user"]["username"], "maya")
        conversation_id = created.data["id"]

        self.authenticate(self.maya)
        same = self.client.post(
            reverse("conversations"),
            {"username": "omar"},
            format="json",
        )
        self.assertEqual(same.status_code, status.HTTP_200_OK)
        self.assertEqual(same.data["id"], conversation_id)
        self.assertEqual(Conversation.objects.count(), 1)

        self.authenticate(self.omar)
        self_message = self.client.post(
            reverse("conversations"),
            {"username": "omar"},
            format="json",
        )
        self.assertEqual(self_message.status_code, status.HTTP_400_BAD_REQUEST)

    def test_message_is_persistent_idempotent_and_readable(self):
        created = self.create_conversation()
        conversation_id = created.data["id"]
        messages_url = reverse(
            "conversation-messages",
            kwargs={"conversation_id": conversation_id},
        )

        first = self.client.post(
            messages_url,
            {"body": "Hey Maya", "client_id": "android-message-1"},
            format="json",
        )
        self.assertEqual(first.status_code, status.HTTP_201_CREATED)
        self.assertEqual(first.data["body"], "Hey Maya")
        self.assertTrue(first.data["is_mine"])

        retried = self.client.post(
            messages_url,
            {"body": "Hey Maya", "client_id": "android-message-1"},
            format="json",
        )
        self.assertEqual(retried.status_code, status.HTTP_200_OK)
        self.assertEqual(retried.data["id"], first.data["id"])
        self.assertEqual(Message.objects.count(), 1)

        self.authenticate(self.maya)
        inbox = self.client.get(reverse("conversations"))
        self.assertEqual(inbox.status_code, status.HTTP_200_OK)
        self.assertEqual(inbox.data["unread_count"], 1)
        self.assertEqual(inbox.data["results"][0]["unread_count"], 1)
        self.assertEqual(inbox.data["results"][0]["last_message"]["body"], "Hey Maya")

        thread = self.client.get(messages_url)
        self.assertEqual(thread.status_code, status.HTTP_200_OK)
        self.assertEqual(len(thread.data["results"]), 1)
        self.assertFalse(thread.data["results"][0]["is_mine"])

        marked = self.client.post(
            reverse("conversation-read", kwargs={"conversation_id": conversation_id}),
            {},
            format="json",
        )
        self.assertEqual(marked.status_code, status.HTTP_200_OK)
        self.assertEqual(marked.data["marked_read"], 1)

        inbox_after = self.client.get(reverse("conversations"))
        self.assertEqual(inbox_after.data["unread_count"], 0)
        self.assertEqual(inbox_after.data["results"][0]["unread_count"], 0)

    def test_stranger_cannot_read_or_send_into_conversation(self):
        created = self.create_conversation()
        conversation_id = created.data["id"]
        messages_url = reverse(
            "conversation-messages",
            kwargs={"conversation_id": conversation_id},
        )

        self.authenticate(self.ziad)
        hidden = self.client.get(messages_url)
        self.assertEqual(hidden.status_code, status.HTTP_404_NOT_FOUND)

        blocked = self.client.post(
            messages_url,
            {"body": "I should not get in", "client_id": "ziad-1"},
            format="json",
        )
        self.assertEqual(blocked.status_code, status.HTTP_404_NOT_FOUND)
        self.assertEqual(Message.objects.count(), 0)

    def test_conversation_search_only_returns_matching_other_user(self):
        maya_conversation = self.create_conversation("maya").data["id"]
        ziad_conversation = self.create_conversation("ziad").data["id"]
        self.assertNotEqual(maya_conversation, ziad_conversation)

        self.authenticate(self.omar)
        search = self.client.get(reverse("conversations"), {"q": "may"})
        self.assertEqual(search.status_code, status.HTTP_200_OK)
        self.assertEqual(len(search.data["results"]), 1)
        self.assertEqual(search.data["results"][0]["other_user"]["username"], "maya")

    def test_message_cursor_paginates_older_history_without_duplicates(self):
        created = self.create_conversation()
        conversation_id = created.data["id"]
        conversation = Conversation.objects.get(pk=conversation_id)

        Message.objects.bulk_create(
            [
                Message(
                    conversation=conversation,
                    sender=self.omar,
                    recipient=self.maya,
                    body=f"Message {index}",
                    client_id=f"bulk-{index}",
                )
                for index in range(55)
            ]
        )

        self.authenticate(self.maya)
        messages_url = reverse(
            "conversation-messages",
            kwargs={"conversation_id": conversation_id},
        )
        first = self.client.get(messages_url)
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(len(first.data["results"]), 50)
        self.assertIsNotNone(first.data["next_cursor"])

        second = self.client.get(
            messages_url,
            {"cursor": first.data["next_cursor"]},
        )
        self.assertEqual(second.status_code, status.HTTP_200_OK)
        self.assertEqual(len(second.data["results"]), 5)
        self.assertIsNone(second.data["next_cursor"])

        ids = [item["id"] for item in first.data["results"] + second.data["results"]]
        self.assertEqual(len(ids), 55)
        self.assertEqual(len(set(ids)), 55)

        invalid = self.client.get(messages_url, {"cursor": "bad"})
        self.assertEqual(invalid.status_code, status.HTTP_400_BAD_REQUEST)
