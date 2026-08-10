import base64
import shutil
import tempfile

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Follow, Post, User, UserBlock
from .privacy_models import AccountPrivacy, CloseFriend, FollowRequest
from .story_models import Story


class PrivacyAndCloseFriendsTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-privacy-test-media-")
        cls._media_override = override_settings(MEDIA_ROOT=cls._media_dir)
        cls._media_override.enable()
        super().setUpClass()

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        cls._media_override.disable()
        shutil.rmtree(cls._media_dir, ignore_errors=True)

    def setUp(self):
        self.me = self.user("me")
        self.private = self.user("private")
        self.follower = self.user("follower")
        self.other = self.user("other")
        AccountPrivacy.objects.create(user=self.private, is_private=True)

    def user(self, username):
        return User.objects.create_user(
            email=f"{username}@example.com",
            username=username,
            name=username.title(),
            password="StrongNovaPass2026!",
        )

    def auth(self, user):
        self.client.force_authenticate(user=user)

    def image(self, name="privacy.png"):
        png = base64.b64decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC"
        )
        return SimpleUploadedFile(name, png, content_type="image/png")

    def post(self, author):
        return Post.objects.create(author=author, image=self.image(f"post-{author.username}.png"))

    def test_private_follow_creates_request_and_hides_content(self):
        post = self.post(self.private)
        self.auth(self.me)

        response = self.client.post(
            reverse("person-follow", kwargs={"username": self.private.username}),
            {},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_202_ACCEPTED)
        self.assertFalse(response.data["is_following"])
        self.assertTrue(response.data["follow_requested"])
        self.assertFalse(response.data["can_view_content"])
        self.assertFalse(Follow.objects.filter(follower=self.me, following=self.private).exists())
        self.assertTrue(FollowRequest.objects.filter(requester=self.me, target=self.private).exists())

        posts = self.client.get(
            reverse("person-posts", kwargs={"username": self.private.username})
        )
        self.assertEqual(posts.status_code, status.HTTP_403_FORBIDDEN)
        detail = self.client.get(reverse("post-detail", kwargs={"post_id": post.pk}))
        self.assertEqual(detail.status_code, status.HTTP_404_NOT_FOUND)

    def test_accept_follow_request_unlocks_private_content(self):
        post = self.post(self.private)
        request_row = FollowRequest.objects.create(requester=self.me, target=self.private)

        self.auth(self.private)
        response = self.client.post(
            reverse("follow-request-accept", kwargs={"request_id": request_row.pk}),
            {},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(Follow.objects.filter(follower=self.me, following=self.private).exists())
        self.assertFalse(FollowRequest.objects.filter(pk=request_row.pk).exists())

        self.auth(self.me)
        person = self.client.get(
            reverse("person-detail", kwargs={"username": self.private.username})
        )
        self.assertTrue(person.data["is_following"])
        self.assertTrue(person.data["can_view_content"])
        posts = self.client.get(
            reverse("person-posts", kwargs={"username": self.private.username})
        )
        self.assertEqual(posts.status_code, status.HTTP_200_OK)
        self.assertEqual(posts.data["results"][0]["id"], post.pk)

    def test_decline_and_cancel_follow_request(self):
        request_row = FollowRequest.objects.create(requester=self.me, target=self.private)
        self.auth(self.private)
        declined = self.client.post(
            reverse("follow-request-decline", kwargs={"request_id": request_row.pk}),
            {},
            format="json",
        )
        self.assertEqual(declined.status_code, status.HTTP_200_OK)
        self.assertFalse(FollowRequest.objects.filter(pk=request_row.pk).exists())

        self.auth(self.me)
        self.client.post(
            reverse("person-follow", kwargs={"username": self.private.username}),
            {},
            format="json",
        )
        canceled = self.client.delete(
            reverse("person-follow", kwargs={"username": self.private.username})
        )
        self.assertEqual(canceled.status_code, status.HTTP_200_OK)
        self.assertFalse(canceled.data["follow_requested"])

    def test_switching_private_account_public_accepts_pending_requests(self):
        FollowRequest.objects.create(requester=self.me, target=self.private)
        FollowRequest.objects.create(requester=self.other, target=self.private)
        self.auth(self.private)

        response = self.client.post(
            reverse("account-privacy"),
            {"is_private": False},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["is_private"])
        self.assertEqual(response.data["accepted_pending_requests"], 2)
        self.assertEqual(Follow.objects.filter(following=self.private).count(), 2)
        self.assertEqual(FollowRequest.objects.filter(target=self.private).count(), 0)

    def test_private_connections_are_hidden_until_approved(self):
        Follow.objects.create(follower=self.follower, following=self.private)
        self.auth(self.me)
        hidden = self.client.get(
            reverse("person-followers", kwargs={"username": self.private.username})
        )
        self.assertEqual(hidden.status_code, status.HTTP_403_FORBIDDEN)

        Follow.objects.create(follower=self.me, following=self.private)
        visible = self.client.get(
            reverse("person-followers", kwargs={"username": self.private.username})
        )
        self.assertEqual(visible.status_code, status.HTTP_200_OK)
        usernames = {item["username"] for item in visible.data["results"]}
        self.assertIn(self.follower.username, usernames)

    def test_private_post_share_requires_recipient_access(self):
        private_post = self.post(self.private)
        Follow.objects.create(follower=self.me, following=self.private)
        self.auth(self.me)

        rejected = self.client.post(
            reverse("message-share"),
            {
                "recipient_username": self.other.username,
                "kind": "post",
                "post_id": private_post.pk,
            },
            format="json",
        )
        self.assertEqual(rejected.status_code, status.HTTP_403_FORBIDDEN)

        Follow.objects.create(follower=self.other, following=self.private)
        accepted = self.client.post(
            reverse("message-share"),
            {
                "recipient_username": self.other.username,
                "kind": "post",
                "post_id": private_post.pk,
            },
            format="json",
        )
        self.assertEqual(accepted.status_code, status.HTTP_201_CREATED)

    def test_private_post_from_someone_else_cannot_be_added_to_story(self):
        private_post = self.post(self.private)
        Follow.objects.create(follower=self.me, following=self.private)
        self.auth(self.me)

        response = self.client.post(
            reverse("stories"),
            {"shared_post_id": private_post.pk},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(Story.objects.count(), 0)

    def test_close_friends_story_only_reaches_selected_follower(self):
        Follow.objects.create(follower=self.follower, following=self.me)
        Follow.objects.create(follower=self.other, following=self.me)
        self.auth(self.me)

        added = self.client.post(
            reverse("close-friends"),
            {"username": self.follower.username},
            format="json",
        )
        self.assertEqual(added.status_code, status.HTTP_201_CREATED)
        self.assertTrue(CloseFriend.objects.filter(owner=self.me, member=self.follower).exists())

        created = self.client.post(
            reverse("stories"),
            {
                "media": self.image("story.png"),
                "audience": "close_friends",
                "caption": "inner circle",
            },
            format="multipart",
        )
        self.assertEqual(created.status_code, status.HTTP_201_CREATED)
        self.assertEqual(created.data["audience"], "close_friends")

        self.auth(self.follower)
        close_feed = self.client.get(reverse("stories"))
        self.assertEqual(len(close_feed.data["results"]), 1)
        self.assertEqual(close_feed.data["results"][0]["stories"][0]["audience"], "close_friends")

        self.auth(self.other)
        regular_feed = self.client.get(reverse("stories"))
        self.assertEqual(regular_feed.data["results"], [])

    def test_close_friends_requires_follower_and_block_clears_privacy_edges(self):
        self.auth(self.me)
        rejected = self.client.post(
            reverse("close-friends"),
            {"username": self.other.username},
            format="json",
        )
        self.assertEqual(rejected.status_code, status.HTTP_400_BAD_REQUEST)

        Follow.objects.create(follower=self.other, following=self.me)
        CloseFriend.objects.create(owner=self.me, member=self.other)
        FollowRequest.objects.create(requester=self.me, target=self.private)
        self.client.post(
            reverse("person-block", kwargs={"username": self.private.username}),
            {},
            format="json",
        )
        self.assertFalse(FollowRequest.objects.filter(requester=self.me, target=self.private).exists())

        UserBlock.objects.filter(blocker=self.me, blocked=self.private).delete()
        self.client.post(
            reverse("person-block", kwargs={"username": self.other.username}),
            {},
            format="json",
        )
        self.assertFalse(CloseFriend.objects.filter(owner=self.me, member=self.other).exists())
