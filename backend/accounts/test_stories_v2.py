import shutil
import tempfile
import uuid

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Follow, User, UserBlock
from .privacy_models import AccountPrivacy
from .reels_models import Reel
from .story_models import Story


class StoriesV2Tests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-stories-v2-media-")
        cls._media_override = override_settings(MEDIA_ROOT=cls._media_dir)
        cls._media_override.enable()
        super().setUpClass()

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        cls._media_override.disable()
        shutil.rmtree(cls._media_dir, ignore_errors=True)

    def setUp(self):
        self.owner = self.user("owner")
        self.viewer = self.user("viewer")
        Follow.objects.create(follower=self.viewer, following=self.owner)

    def user(self, username):
        return User.objects.create_user(
            email=f"{username}@example.com",
            username=username,
            password="StrongNovaPass2026!",
            name=username.title(),
        )

    def auth(self, user):
        self.client.force_authenticate(user=user)

    def reel(self, author, caption="A Nova Reel"):
        return Reel.objects.create(
            author=author,
            video=SimpleUploadedFile(
                f"{author.username}.mp4",
                b"nova-reel-video",
                content_type="video/mp4",
            ),
            caption=caption,
        )

    def story_payload(self, response, story_id):
        for group in response.data["results"]:
            for story in group["stories"]:
                if story["id"] == story_id:
                    return story
        self.fail(f"Story {story_id} not found in feed")

    def test_text_story_create_and_feed_payload(self):
        self.auth(self.owner)
        response = self.client.post(
            reverse("stories"),
            {
                "media_type": "text",
                "caption": "Nova after midnight",
                "background_style": "ocean",
                "audience": "followers",
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["media_type"], "text")
        self.assertEqual(response.data["media_url"], "")
        self.assertEqual(response.data["background_style"], "ocean")
        self.assertIsNone(response.data["shared_post"])
        self.assertIsNone(response.data["shared_reel"])

        story_id = response.data["id"]
        self.auth(self.viewer)
        feed = self.client.get(reverse("stories"))
        self.assertEqual(feed.status_code, status.HTTP_200_OK)
        payload = self.story_payload(feed, story_id)
        self.assertEqual(payload["caption"], "Nova after midnight")
        self.assertEqual(payload["background_style"], "ocean")

    def test_text_story_requires_non_blank_text(self):
        self.auth(self.owner)
        response = self.client.post(
            reverse("stories"),
            {
                "media_type": "text",
                "caption": "   ",
                "background_style": "midnight",
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(Story.objects.count(), 0)

    def test_reel_can_be_shared_to_story_and_keeps_original_target(self):
        reel_author = self.user("creator")
        reel = self.reel(reel_author, caption="Original Reel")

        self.auth(self.owner)
        response = self.client.post(
            reverse("stories"),
            {
                "shared_reel_id": reel.pk,
                "caption": "Watch this",
                "audience": "followers",
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data["media_type"], "video")
        self.assertEqual(response.data["shared_reel"]["id"], reel.pk)
        self.assertEqual(response.data["shared_reel"]["author"]["username"], reel_author.username)
        self.assertEqual(response.data["shared_reel"]["caption"], "Original Reel")
        self.assertIn(".mp4", response.data["media_url"])

        story = Story.objects.get(pk=response.data["id"])
        self.assertEqual(story.shared_reel_id, reel.pk)
        self.assertFalse(bool(story.media))

        self.auth(self.viewer)
        feed = self.client.get(reverse("stories"))
        payload = self.story_payload(feed, story.pk)
        self.assertEqual(payload["shared_reel"]["id"], reel.pk)

    def test_private_account_reel_cannot_be_reshared_by_follower(self):
        private_creator = self.user("privatecreator")
        AccountPrivacy.objects.create(user=private_creator, is_private=True)
        Follow.objects.create(follower=self.owner, following=private_creator)
        reel = self.reel(private_creator)

        self.auth(self.owner)
        response = self.client.post(
            reverse("stories"),
            {"shared_reel_id": reel.pk, "audience": "followers"},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(Story.objects.count(), 0)

    def test_blocking_original_reel_creator_hides_shared_reel_story(self):
        reel_author = self.user("blockedcreator")
        reel = self.reel(reel_author)
        self.auth(self.owner)
        created = self.client.post(
            reverse("stories"),
            {"shared_reel_id": reel.pk, "audience": "followers"},
            format="json",
        )
        self.assertEqual(created.status_code, status.HTTP_201_CREATED)
        story_id = created.data["id"]

        UserBlock.objects.create(blocker=self.viewer, blocked=reel_author)
        self.auth(self.viewer)
        feed = self.client.get(reverse("stories"))
        self.assertEqual(feed.status_code, status.HTTP_200_OK)
        returned_ids = {
            story["id"]
            for group in feed.data["results"]
            for story in group["stories"]
        }
        self.assertNotIn(story_id, returned_ids)

    def test_story_rejects_multiple_sources(self):
        reel = self.reel(self.owner)
        self.auth(self.owner)
        response = self.client.post(
            reverse("stories"),
            {
                "media_type": "text",
                "caption": "two sources",
                "shared_reel_id": reel.pk,
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(Story.objects.count(), 0)

    def test_video_story_accepts_compatible_mp4_thumbnail_and_is_idempotent(self):
        publish_id = str(uuid.uuid4())
        self.auth(self.owner)

        def payload():
            return {
                "media": SimpleUploadedFile("story.mp4", b"nova-story-video", content_type="video/mp4"),
                "thumbnail": SimpleUploadedFile("story.jpg", b"nova-story-thumb", content_type="image/jpeg"),
                "caption": "Right now",
                "audience": "close_friends",
                "client_publish_id": publish_id,
            }

        created = self.client.post(reverse("stories"), payload(), format="multipart")
        duplicate = self.client.post(reverse("stories"), payload(), format="multipart")

        self.assertEqual(created.status_code, status.HTTP_201_CREATED)
        self.assertEqual(duplicate.status_code, status.HTTP_200_OK)
        self.assertEqual(created.data["id"], duplicate.data["id"])
        self.assertEqual(created.data["media_type"], "video")
        self.assertIn(".mp4", created.data["media_url"])
        self.assertIn(".jpg", created.data["thumbnail_url"])
        self.assertEqual(Story.objects.filter(author=self.owner).count(), 1)

    def test_story_publish_identity_is_scoped_per_account(self):
        publish_id = str(uuid.uuid4())
        for user in (self.owner, self.viewer):
            self.auth(user)
            response = self.client.post(
                reverse("stories"),
                {
                    "media": SimpleUploadedFile(
                        f"{user.username}.jpg",
                        b"nova-story-image",
                        content_type="image/jpeg",
                    ),
                    "client_publish_id": publish_id,
                    "audience": "followers",
                },
                format="multipart",
            )
            self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        self.assertEqual(Story.objects.filter(client_publish_id=publish_id).count(), 2)
