from unittest.mock import patch

from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .messaging_models import ConversationPreference
from .models import Conversation, Message
from .push import send_message_push


User = get_user_model()


class MessagingV9ApiTests(APITestCase):
    def setUp(self):
        self.omar = User.objects.create_user(
            email="omar-v9@example.com",
            username="omar-v9",
            password="StrongNovaPass2026!",
            name="Omar",
        )
        self.maya = User.objects.create_user(
            email="maya-v9@example.com",
            username="maya-v9",
            password="StrongNovaPass2026!",
            name="Maya",
        )
        self.stranger = User.objects.create_user(
            email="stranger-v9@example.com",
            username="stranger-v9",
            password="StrongNovaPass2026!",
            name="Stranger",
        )
        first, second = sorted((self.omar.pk, self.maya.pk))
        self.conversation = Conversation.objects.create(
            participant_one_id=first,
            participant_two_id=second,
        )

    def authenticate(self, user):
        self.client.force_authenticate(user=user)

    def message(self, body, index, **kwargs):
        sender = kwargs.pop("sender", self.omar)
        recipient = self.maya if sender == self.omar else self.omar
        return Message.objects.create(
            conversation=self.conversation,
            sender=sender,
            recipient=recipient,
            body=body,
            client_id=f"v9-{index}",
            **kwargs,
        )

    def test_search_matches_message_text_and_excludes_deleted(self):
        match = self.message("The hidden blue notebook", 1)
        self.message("Nothing relevant here", 2, sender=self.maya)
        deleted = self.message("blue should not appear", 3)
        deleted.deleted_at = timezone.now()
        deleted.body = ""
        deleted.save(update_fields=("deleted_at", "body"))

        self.authenticate(self.omar)
        response = self.client.get(
            f"/api/v1/conversations/{self.conversation.pk}/messages/search/?q=blue"
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual([item["id"] for item in response.data["results"]], [match.pk])

    def test_context_returns_target_with_messages_on_both_sides(self):
        items = [self.message(f"Message {index}", index) for index in range(1, 8)]
        target = items[3]

        self.authenticate(self.maya)
        response = self.client.get(
            f"/api/v1/conversations/{self.conversation.pk}/messages/context/?message_id={target.pk}"
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [item["id"] for item in response.data["results"]]
        self.assertIn(target.pk, ids)
        self.assertEqual(response.data["target_message_id"], target.pk)
        self.assertEqual(ids, sorted(ids))

    def test_shared_media_filters_images_and_audio(self):
        photo = self.message(
            "photo caption",
            10,
            image=SimpleUploadedFile("photo.jpg", b"fake-jpeg", content_type="image/jpeg"),
        )
        voice = self.message(
            "voice caption",
            11,
            audio=SimpleUploadedFile("voice.m4a", b"fake-audio", content_type="audio/mp4"),
            audio_duration_ms=2100,
        )
        self.message("text only", 12)

        self.authenticate(self.omar)
        all_media = self.client.get(
            f"/api/v1/conversations/{self.conversation.pk}/media/"
        )
        self.assertEqual(all_media.status_code, status.HTTP_200_OK)
        self.assertEqual(
            {item["id"] for item in all_media.data["results"]},
            {photo.pk, voice.pk},
        )

        photos = self.client.get(
            f"/api/v1/conversations/{self.conversation.pk}/media/?type=image"
        )
        self.assertEqual([item["id"] for item in photos.data["results"]], [photo.pk])

        voices = self.client.get(
            f"/api/v1/conversations/{self.conversation.pk}/media/?type=audio"
        )
        self.assertEqual([item["id"] for item in voices.data["results"]], [voice.pk])

    def test_each_participant_has_independent_mute_preference(self):
        url = f"/api/v1/conversations/{self.conversation.pk}/preferences/"

        self.authenticate(self.omar)
        initial = self.client.get(url)
        self.assertEqual(initial.status_code, status.HTTP_200_OK)
        self.assertFalse(initial.data["muted"])

        muted = self.client.post(url, {"muted": True}, format="json")
        self.assertEqual(muted.status_code, status.HTTP_200_OK)
        self.assertTrue(muted.data["muted"])

        self.authenticate(self.maya)
        maya = self.client.get(url)
        self.assertFalse(maya.data["muted"])

    def test_nonparticipant_cannot_use_v9_conversation_tools(self):
        self.authenticate(self.stranger)
        base = f"/api/v1/conversations/{self.conversation.pk}"
        self.assertEqual(self.client.get(f"{base}/messages/search/?q=x").status_code, 404)
        self.assertEqual(self.client.get(f"{base}/media/").status_code, 404)
        self.assertEqual(self.client.get(f"{base}/preferences/").status_code, 404)

    @patch("accounts.push._firebase_app")
    def test_muted_recipient_short_circuits_message_push(self, firebase_app):
        message = self.message("Muted push", 30)
        ConversationPreference.objects.create(
            conversation=self.conversation,
            user=self.maya,
            muted=True,
        )

        sent = send_message_push(message)
        self.assertEqual(sent, 0)
        firebase_app.assert_not_called()
