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

from .models import Follow, Post, User, UserBlock
from .sharing_models import MessageShare, Repost
from .story_models import Story


class SharingAndRepostTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-sharing-test-media-")
        cls._media_override = override_settings(MEDIA_ROOT=cls._media_dir)
        cls._media_override.enable()
        super().setUpClass()

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        cls._media_override.disable()
        shutil.rmtree(cls._media_dir, ignore_errors=True)

    def setUp(self):
        self.me = self.user("me", "Me")
        self.friend = self.user("friend", "Friend")
        self.other_friend = self.user("other", "Other Friend")
        self.author = self.user("author", "Author")
        self.recipient = self.user("recipient", "Recipient")

    def user(self, username, name):
        return User.objects.create_user(
            email=f"{username}@example.com",
            username=username,
            name=name,
            password="StrongNovaPass2026!",
        )

    def image(self, name="share.png"):
        png = base64.b64decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC"
        )
        return SimpleUploadedFile(name, png, content_type="image/png")

    def post(self, author=None, caption="Share me"):
        return Post.objects.create(
            author=author or self.author,
            image=self.image(f"post-{Post.objects.count()}.png"),
            caption=caption,
        )

    def auth(self, user):
        self.client.force_authenticate(user=user)

    def test_repost_surfaces_unfollowed_author_once_in_feed(self):
        post = self.post()
        Follow.objects.create(follower=self.me, following=self.friend)
        Follow.objects.create(follower=self.me, following=self.other_friend)

        earlier = Repost.objects.create(user=self.friend, post=post)
        Repost.objects.filter(pk=earlier.pk).update(created_at=timezone.now() - timedelta(minutes=2))
        Repost.objects.create(user=self.other_friend, post=post)

        self.auth(self.me)
        response = self.client.get(reverse("feed"))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data["results"]), 1)
        item = response.data["results"][0]
        self.assertEqual(item["id"], post.pk)
        self.assertEqual(item["reposted_by"]["username"], "other")
        self.assertEqual(item["reposts_count"], 2)
        self.assertFalse(item["is_reposted"])

    def test_unrepost_removes_repost_only_post_from_follower_feed(self):
        post = self.post()
        Follow.objects.create(follower=self.me, following=self.friend)
        Repost.objects.create(user=self.friend, post=post)

        self.auth(self.me)
        self.assertEqual(len(self.client.get(reverse("feed")).data["results"]), 1)

        self.auth(self.friend)
        response = self.client.delete(reverse("post-repost", kwargs={"post_id": post.pk}))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["is_reposted"])

        self.auth(self.me)
        self.assertEqual(self.client.get(reverse("feed")).data["results"], [])

    def test_block_hides_repost_and_shared_author_content(self):
        post = self.post()
        Follow.objects.create(follower=self.me, following=self.friend)
        Repost.objects.create(user=self.friend, post=post)
        UserBlock.objects.create(blocker=self.me, blocked=self.author)

        self.auth(self.me)
        response = self.client.get(reverse("feed"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["results"], [])

    def test_post_share_creates_real_rich_dm(self):
        post = self.post()
        self.auth(self.me)
        response = self.client.post(
            reverse("message-share"),
            {
                "recipient_username": self.recipient.username,
                "kind": "post",
                "post_id": post.pk,
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        message = response.data["message"]
        self.assertEqual(message["share"]["kind"], "post")
        self.assertTrue(message["share"]["available"])
        self.assertEqual(message["share"]["post"]["id"], post.pk)
        self.assertEqual(MessageShare.objects.count(), 1)

        conversation_id = response.data["conversation"]["id"]
        self.auth(self.recipient)
        messages = self.client.get(
            reverse("conversation-messages", kwargs={"conversation_id": conversation_id})
        )
        self.assertEqual(messages.status_code, status.HTTP_200_OK)
        self.assertEqual(messages.data["results"][0]["share"]["post"]["id"], post.pk)

    def test_profile_share_creates_rich_dm(self):
        self.auth(self.me)
        response = self.client.post(
            reverse("message-share"),
            {
                "recipient_username": self.recipient.username,
                "kind": "profile",
                "profile_username": self.friend.username,
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        share = response.data["message"]["share"]
        self.assertEqual(share["kind"], "profile")
        self.assertEqual(share["profile"]["username"], self.friend.username)

    def test_recipient_block_with_shared_target_rejects_share(self):
        post = self.post()
        UserBlock.objects.create(blocker=self.recipient, blocked=self.author)

        self.auth(self.me)
        response = self.client.post(
            reverse("message-share"),
            {
                "recipient_username": self.recipient.username,
                "kind": "post",
                "post_id": post.pk,
            },
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(MessageShare.objects.count(), 0)

    def test_deleted_shared_post_becomes_unavailable_in_history(self):
        post = self.post()
        self.auth(self.me)
        response = self.client.post(
            reverse("message-share"),
            {
                "recipient_username": self.recipient.username,
                "kind": "post",
                "post_id": post.pk,
            },
            format="json",
        )
        conversation_id = response.data["conversation"]["id"]
        post.delete()

        self.auth(self.recipient)
        messages = self.client.get(
            reverse("conversation-messages", kwargs={"conversation_id": conversation_id})
        )
        share = messages.data["results"][0]["share"]
        self.assertFalse(share["available"])
        self.assertIsNone(share["post"])

    def test_post_can_be_shared_to_story_and_remains_linked(self):
        post = self.post()
        Follow.objects.create(follower=self.friend, following=self.me)

        self.auth(self.me)
        created = self.client.post(
            reverse("stories"),
            {"shared_post_id": post.pk, "caption": "Worth sharing"},
            format="json",
        )
        self.assertEqual(created.status_code, status.HTTP_201_CREATED)
        self.assertEqual(created.data["media_type"], "image")
        self.assertEqual(created.data["shared_post"]["id"], post.pk)
        story = Story.objects.get(pk=created.data["id"])
        self.assertEqual(story.shared_post_id, post.pk)
        self.assertFalse(bool(story.media))

        self.auth(self.friend)
        feed = self.client.get(reverse("stories"))
        stories = [story for group in feed.data["results"] for story in group["stories"]]
        self.assertEqual(stories[0]["shared_post"]["id"], post.pk)

        UserBlock.objects.create(blocker=self.friend, blocked=self.author)
        hidden = self.client.get(reverse("stories"))
        stories_after_block = [story for group in hidden.data["results"] for story in group["stories"]]
        self.assertEqual(stories_after_block, [])
