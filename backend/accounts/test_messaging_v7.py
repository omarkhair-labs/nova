from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Conversation, Message, MessageReaction


User = get_user_model()


def tiny_audio(name="voice.m4a", size=256):
    return SimpleUploadedFile(name, b"nova-audio" * max(size // 10, 1), content_type="audio/mp4")


class MessagingV7ApiTests(APITestCase):
    def setUp(self):
        self.omar = User.objects.create_user(
            email="omar-v7@example.com",
            username="omar-v7",
            password="StrongNovaPass2026!",
            name="Omar",
        )
        self.maya = User.objects.create_user(
            email="maya-v7@example.com",
            username="maya-v7",
            password="StrongNovaPass2026!",
            name="Maya",
        )
        first, second = sorted((self.omar.pk, self.maya.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )
        self.messages_url = f"/api/v1/conversations/{self.conversation.pk}/messages/"

    def authenticate(self, user):
        self.client.force_authenticate(user=user)

    def create_text_message(self, body="Original", client_id="v7-text-1"):
        return Message.objects.create(
            conversation=self.conversation,
            sender=self.omar,
            recipient=self.maya,
            body=body,
            client_id=client_id,
        )

    def test_voice_only_message_is_created_and_serialized(self):
        self.authenticate(self.omar)
        response = self.client.post(
            self.messages_url,
            {
                "client_id": "voice-only-1",
                "body": "",
                "audio": tiny_audio(),
                "audio_duration_ms": "2300",
            },
            format="multipart",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["body"], "")
        self.assertTrue(response.data["audio_url"])
        self.assertEqual(response.data["audio_duration_ms"], 2300)
        self.assertFalse(response.data["is_deleted"])

    def test_voice_message_rejects_invalid_duration(self):
        self.authenticate(self.omar)
        response = self.client.post(
            self.messages_url,
            {
                "client_id": "voice-too-long-1",
                "audio": tiny_audio(),
                "audio_duration_ms": str(5 * 60 * 1000 + 1),
            },
            format="multipart",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_sender_can_edit_but_recipient_cannot(self):
        message = self.create_text_message()
        url = f"/api/v1/messages/{message.pk}/"

        self.authenticate(self.maya)
        forbidden = self.client.patch(url, {"body": "Nope"}, format="json")
        self.assertEqual(forbidden.status_code, status.HTTP_403_FORBIDDEN)

        self.authenticate(self.omar)
        edited = self.client.patch(url, {"body": "Edited text"}, format="json")
        self.assertEqual(edited.status_code, status.HTTP_200_OK)
        self.assertEqual(edited.data["body"], "Edited text")
        self.assertTrue(edited.data["edited_at"])

        message.refresh_from_db()
        self.assertEqual(message.body, "Edited text")
        self.assertIsNotNone(message.edited_at)

    def test_delete_for_everyone_is_sender_only_and_idempotent(self):
        message = self.create_text_message()
        MessageReaction.objects.create(message=message, user=self.maya, emoji="❤️")
        url = f"/api/v1/messages/{message.pk}/"

        self.authenticate(self.maya)
        forbidden = self.client.delete(url)
        self.assertEqual(forbidden.status_code, status.HTTP_403_FORBIDDEN)

        self.authenticate(self.omar)
        deleted = self.client.delete(url)
        self.assertEqual(deleted.status_code, status.HTTP_200_OK)
        self.assertTrue(deleted.data["deleted_at"])

        repeated = self.client.delete(url)
        self.assertEqual(repeated.status_code, status.HTTP_200_OK)
        self.assertEqual(repeated.data["deleted_at"], deleted.data["deleted_at"])

        message.refresh_from_db()
        self.assertEqual(message.body, "")
        self.assertIsNotNone(message.deleted_at)
        self.assertFalse(MessageReaction.objects.filter(message=message).exists())

    def test_deleted_message_becomes_tombstone_and_reply_preview_survives(self):
        original = self.create_text_message(body="Secret text", client_id="v7-original")
        reply = Message.objects.create(
            conversation=self.conversation,
            sender=self.maya,
            recipient=self.omar,
            reply_to=original,
            body="Reply",
            client_id="v7-reply",
        )

        self.authenticate(self.omar)
        deleted = self.client.delete(f"/api/v1/messages/{original.pk}/")
        self.assertEqual(deleted.status_code, status.HTTP_200_OK)

        history = self.client.get(self.messages_url)
        self.assertEqual(history.status_code, status.HTTP_200_OK)
        by_id = {item["id"]: item for item in history.data["results"]}

        tombstone = by_id[original.pk]
        self.assertTrue(tombstone["is_deleted"])
        self.assertEqual(tombstone["body"], "")
        self.assertEqual(tombstone["image_url"], "")
        self.assertEqual(tombstone["audio_url"], "")
        self.assertEqual(tombstone["reactions"], [])

        reply_payload = by_id[reply.pk]["reply_to"]
        self.assertTrue(reply_payload["is_deleted"])
        self.assertEqual(reply_payload["body"], "Message deleted")

    def test_deleted_message_rejects_reaction_reply_and_edit(self):
        message = self.create_text_message(client_id="v7-delete-guards")
        self.authenticate(self.omar)
        self.client.delete(f"/api/v1/messages/{message.pk}/")

        edit = self.client.patch(
            f"/api/v1/messages/{message.pk}/",
            {"body": "Bring it back"},
            format="json",
        )
        self.assertEqual(edit.status_code, status.HTTP_409_CONFLICT)

        reaction = self.client.post(
            f"/api/v1/messages/{message.pk}/reaction/",
            {"emoji": "❤️"},
            format="json",
        )
        self.assertEqual(reaction.status_code, status.HTTP_409_CONFLICT)

        reply = self.client.post(
            self.messages_url,
            {
                "client_id": "v7-bad-reply",
                "body": "Can't reply",
                "reply_to_id": message.pk,
            },
            format="json",
        )
        self.assertEqual(reply.status_code, status.HTTP_400_BAD_REQUEST)
