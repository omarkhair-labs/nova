import shutil
import tempfile

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import User, UserBlock
from .privacy_models import AccountPrivacy
from .reels_models import Reel
from .sharing_models import MessageShare


class ReelMessageSharingV3Tests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-reel-sharing-test-media-")
        cls._media_override = override_settings(MEDIA_ROOT=cls._media_dir)
        cls._media_override.enable()
        super().setUpClass()

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        cls._media_override.disable()
        shutil.rmtree(cls._media_dir, ignore_errors=True)

    def setUp(self):
        self.author = self.user("author")
        self.sender = self.user("sender")
        self.recipient = self.user("recipient")
        self.reel = Reel.objects.create(
            author=self.author,
            video=SimpleUploadedFile("shared.mp4", b"nova-reel", content_type="video/mp4"),
            caption="Share this Reel",
        )

    def user(self, username):
        return User.objects.create_user(
            email=f"{username}@example.com",
            username=username,
            password="StrongNovaPass2026!",
            name=username.title(),
        )

    def auth(self, user):
        self.client.force_authenticate(user=user)

    def share(self, **destination):
        return self.client.post(
            reverse("message-share"),
            {
                **destination,
                "kind": "reel",
                "reel_id": self.reel.pk,
            },
            format="json",
        )

    def test_reel_share_creates_rich_direct_message(self):
        self.auth(self.sender)
        response = self.share(recipient_username=self.recipient.username)

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        message = response.data["message"]
        self.assertEqual(message["share"]["kind"], "reel")
        self.assertTrue(message["share"]["available"])
        self.assertEqual(message["share"]["reel"]["id"], self.reel.pk)
        self.assertEqual(message["share"]["reel"]["author"]["username"], self.author.username)
        self.assertEqual(message["share"]["reel"]["caption"], self.reel.caption)
        share = MessageShare.objects.get()
        self.assertEqual(share.reel_id, self.reel.pk)

        conversation_id = response.data["conversation"]["id"]
        self.auth(self.recipient)
        messages = self.client.get(
            reverse("conversation-messages", kwargs={"conversation_id": conversation_id})
        )
        self.assertEqual(messages.status_code, status.HTTP_200_OK)
        self.assertEqual(messages.data["results"][0]["share"]["reel"]["id"], self.reel.pk)

    def test_private_reel_cannot_be_shared_to_recipient_without_access(self):
        AccountPrivacy.objects.create(user=self.author, is_private=True)
        self.auth(self.author)
        response = self.share(recipient_username=self.recipient.username)

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(MessageShare.objects.count(), 0)

    def test_recipient_blocking_reel_author_rejects_share(self):
        UserBlock.objects.create(blocker=self.recipient, blocked=self.author)
        self.auth(self.sender)
        response = self.share(recipient_username=self.recipient.username)

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(MessageShare.objects.count(), 0)

    def test_deleted_shared_reel_becomes_unavailable_in_history(self):
        self.auth(self.sender)
        response = self.share(recipient_username=self.recipient.username)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        conversation_id = response.data["conversation"]["id"]

        self.reel.delete()
        self.auth(self.recipient)
        messages = self.client.get(
            reverse("conversation-messages", kwargs={"conversation_id": conversation_id})
        )
        self.assertEqual(messages.status_code, status.HTTP_200_OK)
        share = messages.data["results"][0]["share"]
        self.assertEqual(share["kind"], "reel")
        self.assertFalse(share["available"])
        self.assertIsNone(share["reel"])
