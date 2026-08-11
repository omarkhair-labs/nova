import shutil
import tempfile

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Follow, User
from .privacy_models import AccountPrivacy
from .reels_models import Reel, ReelComment, ReelLike


class ReelFlowTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-reel-test-media-")
        cls._media_override = override_settings(MEDIA_ROOT=cls._media_dir)
        cls._media_override.enable()
        super().setUpClass()

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        cls._media_override.disable()
        shutil.rmtree(cls._media_dir, ignore_errors=True)

    def setUp(self):
        self.me = User.objects.create_user(
            email="me-reels@example.com",
            username="me_reels",
            password="StrongNovaPass2026!",
            name="Me Reels",
        )
        self.maya = User.objects.create_user(
            email="maya-reels@example.com",
            username="maya_reels",
            password="StrongNovaPass2026!",
            name="Maya Reels",
        )
        self.client.force_authenticate(user=self.me)

    def video(self, name="reel.mp4"):
        return SimpleUploadedFile(name, b"nova-v3-fake-mp4", content_type="video/mp4")

    def create_reel_as(self, user, caption="Nova V3"):
        self.client.force_authenticate(user=user)
        return self.client.post(
            reverse("reels"),
            {"video": self.video(f"{user.username}.mp4"), "caption": caption},
            format="multipart",
        )

    def test_video_upload_returns_reel_and_feed(self):
        created = self.create_reel_as(self.me, "First Nova Reel")
        self.assertEqual(created.status_code, status.HTTP_201_CREATED)
        self.assertEqual(created.data["caption"], "First Nova Reel")
        self.assertEqual(created.data["author"]["username"], self.me.username)
        self.assertTrue(created.data["video_url"].endswith(".mp4"))
        self.assertTrue(Reel.objects.filter(pk=created.data["id"]).exists())

        self.client.force_authenticate(user=self.me)
        feed = self.client.get(reverse("reels"))
        self.assertEqual(feed.status_code, status.HTTP_200_OK)
        self.assertEqual(len(feed.data["results"]), 1)
        self.assertEqual(feed.data["results"][0]["id"], created.data["id"])

    def test_like_is_idempotent_and_can_be_removed(self):
        created = self.create_reel_as(self.maya)
        reel_id = created.data["id"]

        self.client.force_authenticate(user=self.me)
        first = self.client.post(reverse("reel-like", kwargs={"reel_id": reel_id}), {}, format="json")
        second = self.client.post(reverse("reel-like", kwargs={"reel_id": reel_id}), {}, format="json")
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(second.status_code, status.HTTP_200_OK)
        self.assertEqual(second.data["likes_count"], 1)
        self.assertTrue(second.data["is_liked"])
        self.assertEqual(ReelLike.objects.filter(reel_id=reel_id, user=self.me).count(), 1)

        removed = self.client.delete(reverse("reel-like", kwargs={"reel_id": reel_id}))
        self.assertEqual(removed.status_code, status.HTTP_200_OK)
        self.assertEqual(removed.data["likes_count"], 0)
        self.assertFalse(removed.data["is_liked"])

    def test_comment_create_updates_reel_count_and_owner_can_delete(self):
        created = self.create_reel_as(self.maya)
        reel_id = created.data["id"]

        self.client.force_authenticate(user=self.me)
        response = self.client.post(
            reverse("reel-comments", kwargs={"reel_id": reel_id}),
            {"body": "This feels like Nova."},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["comment"]["body"], "This feels like Nova.")
        self.assertEqual(response.data["reel"]["comments_count"], 1)
        comment_id = response.data["comment"]["id"]
        self.assertTrue(ReelComment.objects.filter(pk=comment_id, author=self.me).exists())

        deleted = self.client.delete(reverse("reel-comment-detail", kwargs={"comment_id": comment_id}))
        self.assertEqual(deleted.status_code, status.HTTP_200_OK)
        self.assertEqual(deleted.data["reel"]["comments_count"], 0)

    def test_private_creator_requires_follow_relationship(self):
        AccountPrivacy.objects.create(user=self.maya, is_private=True)
        created = self.create_reel_as(self.maya, "Private Reel")
        reel_id = created.data["id"]

        self.client.force_authenticate(user=self.me)
        hidden_feed = self.client.get(reverse("reels"))
        self.assertEqual(hidden_feed.status_code, status.HTTP_200_OK)
        self.assertNotIn(reel_id, [row["id"] for row in hidden_feed.data["results"]])
        hidden_detail = self.client.get(reverse("reel-detail", kwargs={"reel_id": reel_id}))
        self.assertEqual(hidden_detail.status_code, status.HTTP_404_NOT_FOUND)

        Follow.objects.create(follower=self.me, following=self.maya)
        visible_feed = self.client.get(reverse("reels"))
        self.assertIn(reel_id, [row["id"] for row in visible_feed.data["results"]])
