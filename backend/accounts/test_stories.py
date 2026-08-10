import base64
import shutil
import tempfile
from datetime import timedelta

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Follow, Message, User, UserBlock
from .story_models import Story, StoryReaction, StoryView


class StoryFlowTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-story-test-media-")
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
            email="me@example.com",
            username="me",
            password="StrongNovaPass2026!",
            name="Me",
        )
        self.maya = User.objects.create_user(
            email="maya@example.com",
            username="maya",
            password="StrongNovaPass2026!",
            name="Maya",
        )
        self.lina = User.objects.create_user(
            email="lina@example.com",
            username="lina",
            password="StrongNovaPass2026!",
            name="Lina",
        )
        self.client.force_authenticate(user=self.me)

    def photo(self, name="story.png"):
        png = base64.b64decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC"
        )
        return SimpleUploadedFile(name, png, content_type="image/png")

    def video(self, name="story.mp4"):
        return SimpleUploadedFile(name, b"nova-fake-mp4", content_type="video/mp4")

    def create_story_as(self, user, media=None, caption=""):
        self.client.force_authenticate(user=user)
        return self.client.post(
            reverse("stories"),
            {"media": media or self.photo(), "caption": caption},
            format="multipart",
        )

    def test_story_feed_is_follow_graph_scoped_and_tracks_views(self):
        mine = self.create_story_as(self.me, self.photo("mine.png"), "Mine")
        maya_story = self.create_story_as(self.maya, self.photo("maya.png"), "Maya")
        self.create_story_as(self.lina, self.photo("lina.png"), "Lina")
        self.assertEqual(mine.status_code, status.HTTP_201_CREATED)
        self.assertEqual(maya_story.status_code, status.HTTP_201_CREATED)

        Follow.objects.create(follower=self.me, following=self.maya)
        self.client.force_authenticate(user=self.me)
        feed = self.client.get(reverse("stories"))
        self.assertEqual(feed.status_code, status.HTTP_200_OK)
        usernames = [group["author"]["username"] for group in feed.data["results"]]
        self.assertEqual(usernames, ["me", "maya"])
        self.assertTrue(feed.data["results"][0]["is_mine"])
        self.assertTrue(feed.data["results"][1]["has_unseen"])

        maya_id = maya_story.data["id"]
        viewed = self.client.post(reverse("story-view", kwargs={"story_id": maya_id}), {}, format="json")
        self.assertEqual(viewed.status_code, status.HTTP_200_OK)
        self.assertTrue(StoryView.objects.filter(story_id=maya_id, viewer=self.me).exists())

        feed_after = self.client.get(reverse("stories"))
        maya_group = next(group for group in feed_after.data["results"] if group["author"]["username"] == "maya")
        self.assertFalse(maya_group["has_unseen"])
        self.assertTrue(maya_group["stories"][0]["is_viewed"])

    def test_owner_can_see_viewers_and_reactions(self):
        story_response = self.create_story_as(self.maya, self.photo("reaction.png"))
        story_id = story_response.data["id"]
        Follow.objects.create(follower=self.me, following=self.maya)

        self.client.force_authenticate(user=self.me)
        reaction = self.client.post(
            reverse("story-reaction", kwargs={"story_id": story_id}),
            {"emoji": "🔥"},
            format="json",
        )
        self.assertEqual(reaction.status_code, status.HTTP_200_OK)
        self.assertEqual(reaction.data["reaction"], "🔥")
        self.assertTrue(StoryReaction.objects.filter(story_id=story_id, user=self.me, emoji="🔥").exists())
        self.assertTrue(StoryView.objects.filter(story_id=story_id, viewer=self.me).exists())

        self.client.force_authenticate(user=self.maya)
        viewers = self.client.get(reverse("story-viewers", kwargs={"story_id": story_id}))
        self.assertEqual(viewers.status_code, status.HTTP_200_OK)
        self.assertEqual(viewers.data["views_count"], 1)
        self.assertEqual(viewers.data["results"][0]["user"]["username"], "me")
        self.assertEqual(viewers.data["results"][0]["reaction"], "🔥")

        self.client.force_authenticate(user=self.lina)
        forbidden = self.client.get(reverse("story-viewers", kwargs={"story_id": story_id}))
        self.assertEqual(forbidden.status_code, status.HTTP_404_NOT_FOUND)

    def test_story_reply_creates_real_dm_message(self):
        story_response = self.create_story_as(self.maya, self.photo("reply.png"))
        story_id = story_response.data["id"]
        Follow.objects.create(follower=self.me, following=self.maya)

        self.client.force_authenticate(user=self.me)
        reply = self.client.post(
            reverse("story-reply", kwargs={"story_id": story_id}),
            {"body": "This is beautiful"},
            format="json",
        )
        self.assertEqual(reply.status_code, status.HTTP_201_CREATED)
        self.assertEqual(reply.data["conversation"]["other_user"]["username"], "maya")
        message = Message.objects.get(pk=reply.data["message"]["id"])
        self.assertEqual(message.sender, self.me)
        self.assertEqual(message.recipient, self.maya)
        self.assertEqual(message.body, "↳ Story reply\nThis is beautiful")

    def test_expired_and_blocked_stories_are_not_visible(self):
        maya_story = self.create_story_as(self.maya, self.photo("expired.png"))
        story = Story.objects.get(pk=maya_story.data["id"])
        story.expires_at = timezone.now() - timedelta(seconds=1)
        story.save(update_fields=("expires_at",))
        Follow.objects.create(follower=self.me, following=self.maya)

        self.client.force_authenticate(user=self.me)
        expired_feed = self.client.get(reverse("stories"))
        self.assertEqual(expired_feed.data["results"], [])

        story.expires_at = timezone.now() + timedelta(hours=1)
        story.save(update_fields=("expires_at",))
        UserBlock.objects.create(blocker=self.me, blocked=self.maya)
        blocked_feed = self.client.get(reverse("stories"))
        self.assertEqual(blocked_feed.data["results"], [])
        blocked_view = self.client.post(reverse("story-view", kwargs={"story_id": story.pk}), {}, format="json")
        self.assertEqual(blocked_view.status_code, status.HTTP_404_NOT_FOUND)

    def test_video_story_and_owner_delete(self):
        video_story = self.create_story_as(self.me, self.video(), "Video story")
        self.assertEqual(video_story.status_code, status.HTTP_201_CREATED)
        self.assertEqual(video_story.data["media_type"], "video")
        story_id = video_story.data["id"]

        self.client.force_authenticate(user=self.me)
        deleted = self.client.delete(reverse("story-detail", kwargs={"story_id": story_id}))
        self.assertEqual(deleted.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(Story.objects.filter(pk=story_id).exists())
