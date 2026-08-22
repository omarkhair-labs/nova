from datetime import timedelta
import tempfile

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from ..models import Follow, User, UserBlock
from ..privacy_models import CloseFriend
from ..pulse_models import PULSE_DURATION, Pulse


class PulseApiTests(APITestCase):
    def setUp(self):
        self.media_dir = tempfile.TemporaryDirectory(prefix="nova-pulse-tests-")
        self.media_override = override_settings(MEDIA_ROOT=self.media_dir.name)
        self.media_override.enable()
        self.me = User.objects.create_user(
            email="omar-pulse@example.com",
            username="omar_pulse",
            password="StrongNovaPass2026!",
            name="Omar",
        )
        self.friend = User.objects.create_user(
            email="maya-pulse@example.com",
            username="maya_pulse",
            password="StrongNovaPass2026!",
            name="Maya",
        )
        self.feed_url = reverse("pulse-feed")

    def tearDown(self):
        self.media_override.disable()
        self.media_dir.cleanup()
        super().tearDown()

    def authenticate(self, user=None):
        self.client.force_authenticate(user=user or self.me)

    def image(self, name="pulse.jpg"):
        return SimpleUploadedFile(name, b"pulse-image", content_type="image/jpeg")

    def test_feed_requires_authentication(self):
        response = self.client.get(self.feed_url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_create_text_pulse_sets_server_controlled_twelve_hour_expiry(self):
        self.authenticate()
        before = timezone.now()
        response = self.client.post(
            self.feed_url,
            {"note": "Late coffee", "audience": "followers"},
            format="json",
        )
        after = timezone.now()

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["media_type"], Pulse.MediaType.TEXT)
        self.assertEqual(response.data["note"], "Late coffee")
        self.assertEqual(response.data["media_url"], "")
        self.assertTrue(response.data["is_mine"])

        pulse = Pulse.objects.get(pk=response.data["id"])
        self.assertGreaterEqual(pulse.expires_at, before + PULSE_DURATION)
        self.assertLessEqual(pulse.expires_at, after + PULSE_DURATION)

    def test_create_photo_pulse_infers_media_type(self):
        self.authenticate()
        response = self.client.post(
            self.feed_url,
            {
                "media": self.image(),
                "note": "Outside right now",
                "audience": "followers",
            },
            format="multipart",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["media_type"], Pulse.MediaType.IMAGE)
        self.assertTrue(response.data["media_url"].endswith(".jpg"))

    def test_blank_or_mismatched_content_is_rejected(self):
        self.authenticate()
        blank = self.client.post(self.feed_url, {}, format="json")
        self.assertEqual(blank.status_code, status.HTTP_400_BAD_REQUEST)

        mismatch = self.client.post(
            self.feed_url,
            {"media": self.image(), "media_type": "video"},
            format="multipart",
        )
        self.assertEqual(mismatch.status_code, status.HTTP_400_BAD_REQUEST)

    def test_feed_shows_followed_live_pulses_and_hides_strangers_expired_and_blocked(self):
        stranger = User.objects.create_user(
            email="stranger-pulse@example.com",
            username="stranger_pulse",
            password="StrongNovaPass2026!",
        )
        blocked = User.objects.create_user(
            email="blocked-pulse@example.com",
            username="blocked_pulse",
            password="StrongNovaPass2026!",
        )
        Follow.objects.create(follower=self.me, following=self.friend)
        Follow.objects.create(follower=self.me, following=blocked)
        UserBlock.objects.create(blocker=self.me, blocked=blocked)

        mine = Pulse.objects.create(
            author=self.me,
            media_type=Pulse.MediaType.TEXT,
            note="Mine",
            expires_at=timezone.now() + timedelta(hours=1),
        )
        friend = Pulse.objects.create(
            author=self.friend,
            media_type=Pulse.MediaType.TEXT,
            note="Friend",
            expires_at=timezone.now() + timedelta(hours=1),
        )
        Pulse.objects.create(
            author=stranger,
            media_type=Pulse.MediaType.TEXT,
            note="Stranger",
            expires_at=timezone.now() + timedelta(hours=1),
        )
        Pulse.objects.create(
            author=self.friend,
            media_type=Pulse.MediaType.TEXT,
            note="Expired",
            expires_at=timezone.now() - timedelta(seconds=1),
        )
        Pulse.objects.create(
            author=blocked,
            media_type=Pulse.MediaType.TEXT,
            note="Blocked",
            expires_at=timezone.now() + timedelta(hours=1),
        )

        self.authenticate()
        response = self.client.get(self.feed_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = {item["id"] for item in response.data["results"]}
        self.assertEqual(ids, {mine.pk, friend.pk})

    def test_close_friends_pulse_requires_membership(self):
        Follow.objects.create(follower=self.me, following=self.friend)
        pulse = Pulse.objects.create(
            author=self.friend,
            media_type=Pulse.MediaType.TEXT,
            audience=Pulse.Audience.CLOSE_FRIENDS,
            note="Inner circle",
            expires_at=timezone.now() + timedelta(hours=1),
        )

        self.authenticate()
        hidden = self.client.get(self.feed_url)
        self.assertNotIn(pulse.pk, {item["id"] for item in hidden.data["results"]})

        CloseFriend.objects.create(owner=self.friend, member=self.me)
        visible = self.client.get(self.feed_url)
        self.assertIn(pulse.pk, {item["id"] for item in visible.data["results"]})

    def test_detail_respects_visibility_and_only_owner_can_delete(self):
        Follow.objects.create(follower=self.me, following=self.friend)
        pulse = Pulse.objects.create(
            author=self.friend,
            media_type=Pulse.MediaType.TEXT,
            note="Live",
            expires_at=timezone.now() + timedelta(hours=1),
        )
        detail_url = reverse("pulse-detail", kwargs={"pulse_id": pulse.pk})

        self.authenticate()
        visible = self.client.get(detail_url)
        self.assertEqual(visible.status_code, status.HTTP_200_OK)
        denied_delete = self.client.delete(detail_url)
        self.assertEqual(denied_delete.status_code, status.HTTP_404_NOT_FOUND)

        self.authenticate(self.friend)
        deleted = self.client.delete(detail_url)
        self.assertEqual(deleted.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Pulse.objects.filter(pk=pulse.pk).exists())

    def test_account_deletion_removes_pulses(self):
        pulse = Pulse.objects.create(
            author=self.me,
            media_type=Pulse.MediaType.IMAGE,
            media=self.image("delete-me.jpg"),
            note="Temporary",
            expires_at=timezone.now() + timedelta(hours=1),
        )
        self.authenticate()
        response = self.client.post(
            reverse("account-delete"),
            {"current_password": "StrongNovaPass2026!"},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(Pulse.objects.filter(pk=pulse.pk).exists())
