from io import BytesIO

from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from PIL import Image
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Conversation, Message, MessageReaction


User = get_user_model()


def tiny_png(name="message.png"):
    buffer = BytesIO()
    Image.new("RGB", (8, 8), color=(80, 120, 180)).save(buffer, format="PNG")
    return SimpleUploadedFile(name, buffer.getvalue(), content_type="image/png")


class RichMessagingApiTests(APITestCase):
    def setUp(self):
        self.omar = User.objects.create_user(
            email="omar-rich@example.com",
            username="omar-rich",
            password="StrongNovaPass2026!",
            name="Omar",
        )
        self.maya = User.objects.create_user(
            email="maya-rich@example.com",
            username="maya-rich",
            password="StrongNovaPass2026!",
            name="Maya",
        )
        self.stranger = User.objects.create_user(
            email="stranger-rich@example.com",
            username="stranger-rich",
            password="StrongNovaPass2026!",
            name="Stranger",
        )
        first, second = sorted((self.omar.pk, self.maya.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )
        self.messages_url = f"/api/v1/conversations/{self.conversation.pk}/messages/"

    def authenticate(self, user):
        self.client.force_authenticate(user=user)

    def test_photo_only_message_and_reply_are_serialized(self):
        self.authenticate(self.omar)
        photo_response = self.client.post(
            self.messages_url,
            {
                "client_id": "photo-only-1",
                "body": "",
                "image": tiny_png(),
            },
            format="multipart",
        )
        self.assertEqual(photo_response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(photo_response.data["body"], "")
        self.assertTrue(photo_response.data["image_url"])
        self.assertIsNone(photo_response.data["reply_to"])

        photo_id = photo_response.data["id"]
        reply_response = self.client.post(
            self.messages_url,
            {
                "client_id": "reply-1",
                "body": "That photo is great",
                "reply_to_id": photo_id,
            },
            format="json",
        )
        self.assertEqual(reply_response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(reply_response.data["reply_to"]["id"], photo_id)
        self.assertEqual(reply_response.data["reply_to"]["sender"]["username"], self.omar.username)
        self.assertTrue(reply_response.data["reply_to"]["image_url"])

    def test_message_requires_text_or_image(self):
        self.authenticate(self.omar)
        response = self.client.post(
            self.messages_url,
            {"client_id": "empty-1", "body": ""},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_reply_must_belong_to_same_conversation(self):
        other = User.objects.create_user(
            email="other-rich@example.com",
            username="other-rich",
            password="StrongNovaPass2026!",
        )
        first, second = sorted((self.omar.pk, other.pk))
        other_conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )
        foreign_message = Message.objects.create(
            conversation=other_conversation,
            sender=self.omar,
            recipient=other,
            body="Foreign thread",
            client_id="foreign-1",
        )

        self.authenticate(self.omar)
        response = self.client.post(
            self.messages_url,
            {
                "client_id": "bad-reply-1",
                "body": "Nope",
                "reply_to_id": foreign_message.pk,
            },
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_reaction_is_one_per_user_and_can_change_or_remove(self):
        message = Message.objects.create(
            conversation=self.conversation,
            sender=self.omar,
            recipient=self.maya,
            body="React to me",
            client_id="reaction-message-1",
        )
        url = f"/api/v1/messages/{message.pk}/reaction/"

        self.authenticate(self.maya)
        first = self.client.post(url, {"emoji": "❤️"}, format="json")
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(MessageReaction.objects.filter(message=message, user=self.maya).count(), 1)
        self.assertEqual(first.data["reactions"][0]["emoji"], "❤️")
        self.assertTrue(first.data["reactions"][0]["reacted_by_me"])

        changed = self.client.post(url, {"emoji": "😂"}, format="json")
        self.assertEqual(changed.status_code, status.HTTP_200_OK)
        reaction = MessageReaction.objects.get(message=message, user=self.maya)
        self.assertEqual(reaction.emoji, "😂")
        self.assertEqual(MessageReaction.objects.filter(message=message, user=self.maya).count(), 1)

        removed = self.client.delete(url)
        self.assertEqual(removed.status_code, status.HTTP_200_OK)
        self.assertFalse(MessageReaction.objects.filter(message=message, user=self.maya).exists())
        self.assertEqual(removed.data["reactions"], [])

    def test_reaction_rejects_unsupported_emoji_and_nonparticipant(self):
        message = Message.objects.create(
            conversation=self.conversation,
            sender=self.omar,
            recipient=self.maya,
            body="Private",
            client_id="private-reaction-1",
        )
        url = f"/api/v1/messages/{message.pk}/reaction/"

        self.authenticate(self.maya)
        unsupported = self.client.post(url, {"emoji": "🔥"}, format="json")
        self.assertEqual(unsupported.status_code, status.HTTP_400_BAD_REQUEST)

        self.authenticate(self.stranger)
        forbidden = self.client.post(url, {"emoji": "❤️"}, format="json")
        self.assertEqual(forbidden.status_code, status.HTTP_404_NOT_FOUND)
