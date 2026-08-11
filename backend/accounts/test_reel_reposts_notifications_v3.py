import shutil
import tempfile
from unittest.mock import patch

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Notification, User
from .push import _notification_reel_id, _title_and_body
from .reels_models import Reel, ReelRepost


class ReelRepostNotificationTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-reel-repost-test-")
        cls._media_override = override_settings(MEDIA_ROOT=cls._media_dir)
        cls._media_override.enable()
        super().setUpClass()

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        cls._media_override.disable()
        shutil.rmtree(cls._media_dir, ignore_errors=True)

    def setUp(self):
        self.owner = User.objects.create_user(
            email="reel-owner@example.com",
            username="reel_owner",
            password="StrongNovaPass2026!",
            name="Reel Owner",
        )
        self.viewer = User.objects.create_user(
            email="reel-viewer@example.com",
            username="reel_viewer",
            password="StrongNovaPass2026!",
            name="Reel Viewer",
        )
        self.reel = Reel.objects.create(
            author=self.owner,
            video=SimpleUploadedFile(
                "notification-reel.mp4",
                b"nova-v3-reel-repost-video",
                content_type="video/mp4",
            ),
            caption="Repost this Reel",
        )
        self.client.force_authenticate(self.viewer)

    def test_repost_is_idempotent_and_can_be_undone(self):
        url = reverse("reel-repost", kwargs={"reel_id": self.reel.pk})
        with patch("accounts.reels.send_notification_push"):
            first = self.client.post(url, {}, format="json")
            second = self.client.post(url, {}, format="json")

        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(second.status_code, status.HTTP_200_OK)
        self.assertTrue(second.data["is_reposted"])
        self.assertEqual(second.data["reposts_count"], 1)
        self.assertEqual(
            ReelRepost.objects.filter(reel=self.reel, user=self.viewer).count(),
            1,
        )
        self.assertEqual(
            Notification.objects.filter(
                recipient=self.owner,
                actor=self.viewer,
                kind="reel_repost",
            ).count(),
            1,
        )

        removed = self.client.delete(url)
        self.assertEqual(removed.status_code, status.HTTP_200_OK)
        self.assertFalse(removed.data["is_reposted"])
        self.assertEqual(removed.data["reposts_count"], 0)
        self.assertFalse(ReelRepost.objects.filter(reel=self.reel, user=self.viewer).exists())

        with patch("accounts.reels.send_notification_push"):
            repeated = self.client.post(url, {}, format="json")
        self.assertTrue(repeated.data["is_reposted"])
        self.assertEqual(
            Notification.objects.filter(
                recipient=self.owner,
                actor=self.viewer,
                kind="reel_repost",
            ).count(),
            1,
        )

    def test_like_comment_and_repost_create_activity_rows(self):
        with patch("accounts.reels.send_notification_push"):
            liked = self.client.post(
                reverse("reel-like", kwargs={"reel_id": self.reel.pk}),
                {},
                format="json",
            )
            commented = self.client.post(
                reverse("reel-comments", kwargs={"reel_id": self.reel.pk}),
                {"body": "Strong V3 Reel"},
                format="json",
            )
            reposted = self.client.post(
                reverse("reel-repost", kwargs={"reel_id": self.reel.pk}),
                {},
                format="json",
            )

        self.assertEqual(liked.status_code, status.HTTP_200_OK)
        self.assertEqual(commented.status_code, status.HTTP_201_CREATED)
        self.assertEqual(reposted.status_code, status.HTTP_200_OK)
        self.assertEqual(
            set(
                Notification.objects.filter(recipient=self.owner).values_list(
                    "kind",
                    flat=True,
                )
            ),
            {"reel_like", "reel_comment", "reel_repost"},
        )

        self.client.force_authenticate(self.owner)
        activity = self.client.get(reverse("notifications"))
        self.assertEqual(activity.status_code, status.HTTP_200_OK)
        self.assertEqual(
            {row["kind"] for row in activity.data["results"]},
            {"reel_like", "reel_comment", "reel_repost"},
        )
        self.assertEqual(activity.data["unread_count"], 3)
        for row in activity.data["results"]:
            self.assertEqual(row["reel_id"], self.reel.pk)
            self.assertEqual(row["reel_author_username"], self.owner.username)
            self.assertIsNone(row["post_id"])

    def test_reel_notification_copy_and_target_are_derived_from_dedupe_key(self):
        notification = Notification.objects.create(
            recipient=self.owner,
            actor=self.viewer,
            kind="reel_repost",
            dedupe_key=f"reel_repost:{self.viewer.pk}:{self.reel.pk}",
        )
        self.assertEqual(_notification_reel_id(notification), self.reel.pk)
        self.assertEqual(
            _title_and_body(notification),
            ("New Reel repost", "Reel Viewer reposted your Reel"),
        )

    def test_own_reel_interactions_do_not_notify_self(self):
        self.client.force_authenticate(self.owner)
        with patch("accounts.reels.send_notification_push"):
            self.client.post(
                reverse("reel-like", kwargs={"reel_id": self.reel.pk}),
                {},
                format="json",
            )
            self.client.post(
                reverse("reel-comments", kwargs={"reel_id": self.reel.pk}),
                {"body": "My own comment"},
                format="json",
            )
            self.client.post(
                reverse("reel-repost", kwargs={"reel_id": self.reel.pk}),
                {},
                format="json",
            )
        self.assertFalse(Notification.objects.filter(recipient=self.owner).exists())

    def test_deleting_reel_clears_reel_activity_targets(self):
        with patch("accounts.reels.send_notification_push"):
            self.client.post(
                reverse("reel-like", kwargs={"reel_id": self.reel.pk}),
                {},
                format="json",
            )
            self.client.post(
                reverse("reel-comments", kwargs={"reel_id": self.reel.pk}),
                {"body": "Will be removed"},
                format="json",
            )
            self.client.post(
                reverse("reel-repost", kwargs={"reel_id": self.reel.pk}),
                {},
                format="json",
            )
        self.assertEqual(Notification.objects.filter(recipient=self.owner).count(), 3)

        self.client.force_authenticate(self.owner)
        deleted = self.client.delete(
            reverse("reel-detail", kwargs={"reel_id": self.reel.pk})
        )
        self.assertEqual(deleted.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Notification.objects.filter(recipient=self.owner).exists())
