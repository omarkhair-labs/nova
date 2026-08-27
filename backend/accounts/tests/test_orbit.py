from datetime import timedelta
import tempfile

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from ..models import Comment, Follow, Like, Post, User, UserBlock
from ..privacy_models import AccountPrivacy
from ..pulse_models import Pulse
from ..sharing_models import Repost


class OrbitApiTests(APITestCase):
    def setUp(self):
        self.media_dir = tempfile.TemporaryDirectory(prefix="nova-orbit-tests-")
        self.media_override = override_settings(MEDIA_ROOT=self.media_dir.name)
        self.media_override.enable()
        self.viewer = self.user("viewer")
        self.actor = self.user("actor")
        self.target = self.user("target")
        self.stranger = self.user("stranger")
        Follow.objects.create(follower=self.viewer, following=self.actor)
        self.url = reverse("orbit-feed")

    def tearDown(self):
        self.media_override.disable()
        self.media_dir.cleanup()
        super().tearDown()

    def user(self, stem):
        return User.objects.create_user(
            email=f"{stem}@orbit.example.com",
            username=f"{stem}_orbit",
            password="StrongNovaPass2026!",
            name=stem.title(),
        )

    def post(self, author, index=1):
        return Post.objects.create(
            author=author,
            image=SimpleUploadedFile(
                f"orbit-{index}.jpg",
                f"image-{index}".encode(),
                content_type="image/jpeg",
            ),
            caption=f"Post {index}",
        )

    def authenticate(self):
        self.client.force_authenticate(user=self.viewer)

    def test_orbit_requires_authentication(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_orbit_surfaces_followed_actor_social_movement(self):
        post = self.post(self.target)
        Like.objects.create(post=post, user=self.actor)
        Comment.objects.create(post=post, author=self.actor, body="This is moving")
        Repost.objects.create(post=post, user=self.actor)
        Follow.objects.create(follower=self.actor, following=self.target)
        root = Pulse.objects.create(
            author=self.viewer,
            media_type=Pulse.MediaType.TEXT,
            note="Where are we going?",
            expires_at=timezone.now() + timedelta(hours=1),
        )
        Pulse.objects.create(
            author=self.actor,
            reply_to=root,
            media_type=Pulse.MediaType.TEXT,
            note="On my way",
            expires_at=timezone.now() + timedelta(hours=1),
        )

        self.authenticate()
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        kinds = {item["kind"] for item in response.data["results"]}
        self.assertEqual(kinds, {"like", "comment", "repost", "follow", "pulse_reply"})
        for item in response.data["results"]:
            self.assertEqual(item["actor"]["username"], self.actor.username)

    def test_video_post_activity_keeps_thumbnail_and_media_contract(self):
        post = Post.objects.create(
            author=self.target,
            video=SimpleUploadedFile(
                "orbit-video.mp4",
                b"nova-compatible-video",
                content_type="video/mp4",
            ),
            thumbnail=SimpleUploadedFile(
                "orbit-video-thumbnail.jpg",
                b"nova-thumbnail",
                content_type="image/jpeg",
            ),
            media_type=Post.MediaType.VIDEO,
            caption="Video movement",
        )
        Like.objects.create(post=post, user=self.actor)

        self.authenticate()
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        payload = response.data["results"][0]["post"]
        self.assertEqual(payload["media_type"], "video")
        self.assertTrue(payload["media_url"].endswith(".mp4"))
        self.assertTrue(payload["thumbnail_url"].endswith(".jpg"))
        self.assertEqual(payload["image_url"], "")

    def test_stranger_activity_is_not_in_viewers_orbit(self):
        post = self.post(self.target)
        Like.objects.create(post=post, user=self.stranger)

        self.authenticate()
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"], [])

    def test_blocked_followed_actor_is_hidden(self):
        post = self.post(self.target)
        Like.objects.create(post=post, user=self.actor)
        UserBlock.objects.create(blocker=self.viewer, blocked=self.actor)

        self.authenticate()
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"], [])

    def test_activity_on_private_post_viewer_cannot_access_is_hidden(self):
        private_author = self.user("private_author")
        AccountPrivacy.objects.create(user=private_author, is_private=True)
        private_post = self.post(private_author)
        Like.objects.create(post=private_post, user=self.actor)

        self.authenticate()
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"], [])

    def test_follow_to_private_target_is_hidden_until_viewer_can_access_target(self):
        private_target = self.user("private_target")
        AccountPrivacy.objects.create(user=private_target, is_private=True)
        Follow.objects.create(follower=self.actor, following=private_target)

        self.authenticate()
        hidden = self.client.get(self.url)
        self.assertEqual(hidden.status_code, status.HTTP_200_OK)
        self.assertNotIn("follow", {item["kind"] for item in hidden.data["results"]})

        Follow.objects.create(follower=self.viewer, following=private_target)
        visible = self.client.get(self.url)
        follow_events = [item for item in visible.data["results"] if item["kind"] == "follow"]
        self.assertEqual(len(follow_events), 1)
        self.assertEqual(follow_events[0]["person"]["username"], private_target.username)

    def test_pulse_reply_does_not_reveal_hidden_parent(self):
        private_target = self.user("private_pulse")
        root = Pulse.objects.create(
            author=private_target,
            media_type=Pulse.MediaType.TEXT,
            note="Hidden root",
            expires_at=timezone.now() + timedelta(hours=1),
        )
        Pulse.objects.create(
            author=self.actor,
            reply_to=root,
            media_type=Pulse.MediaType.TEXT,
            note="Visible actor but hidden context",
            expires_at=timezone.now() + timedelta(hours=1),
        )

        self.authenticate()
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertNotIn("pulse_reply", {item["kind"] for item in response.data["results"]})

    def test_cursor_pages_without_repeating_social_events(self):
        for index in range(30):
            post = self.post(self.target, index=index + 1)
            Like.objects.create(post=post, user=self.actor)

        self.authenticate()
        first = self.client.get(self.url)
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(len(first.data["results"]), 24)
        self.assertTrue(first.data["next_cursor"])

        second = self.client.get(self.url, {"cursor": first.data["next_cursor"]})
        self.assertEqual(second.status_code, status.HTTP_200_OK)
        self.assertEqual(len(second.data["results"]), 6)

        first_ids = {item["id"] for item in first.data["results"]}
        second_ids = {item["id"] for item in second.data["results"]}
        self.assertFalse(first_ids & second_ids)
        self.assertEqual(len(first_ids | second_ids), 30)

    def test_invalid_cursor_is_rejected(self):
        self.authenticate()
        response = self.client.get(self.url, {"cursor": "not-a-real-orbit-cursor"})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
