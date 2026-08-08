import base64
import shutil
import tempfile

from django.contrib.auth import get_user_model
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Post


class AuthFlowTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-test-media-")
        cls._media_override = override_settings(MEDIA_ROOT=cls._media_dir)
        cls._media_override.enable()
        super().setUpClass()

    @classmethod
    def tearDownClass(cls):
        super().tearDownClass()
        cls._media_override.disable()
        shutil.rmtree(cls._media_dir, ignore_errors=True)

    def setUp(self):
        self.register_url = reverse("register")
        self.login_url = reverse("login")
        self.me_url = reverse("me")
        self.payload = {
            "email": "omar@example.com",
            "password": "StrongNovaPass2026!",
            "username": "omar",
            "name": "Omar Khair",
        }

    def register(self, payload):
        return self.client.post(self.register_url, payload, format="json")

    def authenticate(self, token):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")

    def image(self, name="moment.png"):
        png = base64.b64decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC"
        )
        return SimpleUploadedFile(name, png, content_type="image/png")

    def test_register_login_and_me_flow(self):
        register_response = self.register(self.payload)
        self.assertEqual(register_response.status_code, status.HTTP_201_CREATED)
        self.assertIn("access", register_response.data)
        self.assertIn("refresh", register_response.data)
        self.assertEqual(register_response.data["user"]["username"], "omar")

        self.authenticate(register_response.data["access"])
        me_response = self.client.get(self.me_url)
        self.assertEqual(me_response.status_code, status.HTTP_200_OK)
        self.assertEqual(me_response.data["email"], "omar@example.com")
        self.assertEqual(me_response.data["followers_count"], 0)
        self.assertEqual(me_response.data["following_count"], 0)
        self.assertEqual(me_response.data["posts_count"], 0)

        self.client.credentials()
        login_response = self.client.post(
            self.login_url,
            {
                "email": self.payload["email"],
                "password": self.payload["password"],
            },
            format="json",
        )
        self.assertEqual(login_response.status_code, status.HTTP_200_OK)
        self.assertIn("access", login_response.data)
        self.assertEqual(login_response.data["user"]["name"], "Omar Khair")

    def test_authenticated_profile_can_be_edited(self):
        register_response = self.register(self.payload)
        self.assertEqual(register_response.status_code, status.HTTP_201_CREATED)

        self.authenticate(register_response.data["access"])
        response = self.client.patch(
            self.me_url,
            {"name": "Omar Nova", "username": "omar.nova"},
            format="json",
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["name"], "Omar Nova")
        self.assertEqual(response.data["username"], "omar.nova")
        self.assertEqual(response.data["avatar_url"], "")

    def test_duplicate_username_is_rejected(self):
        first = self.register(self.payload)
        self.assertEqual(first.status_code, status.HTTP_201_CREATED)

        duplicate = {
            **self.payload,
            "email": "another@example.com",
        }
        second = self.register(duplicate)
        self.assertEqual(second.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("username", second.data)

    def test_people_discovery_follow_and_unfollow(self):
        me = self.register(self.payload)
        other = self.register(
            {
                "email": "maya@example.com",
                "password": "StrongNovaPass2026!",
                "username": "maya",
                "name": "Maya Noor",
            }
        )
        self.assertEqual(me.status_code, status.HTTP_201_CREATED)
        self.assertEqual(other.status_code, status.HTTP_201_CREATED)

        self.authenticate(me.data["access"])

        people = self.client.get(reverse("people"))
        self.assertEqual(people.status_code, status.HTTP_200_OK)
        self.assertEqual(len(people.data["results"]), 1)
        self.assertEqual(people.data["results"][0]["username"], "maya")
        self.assertFalse(people.data["results"][0]["is_following"])

        search = self.client.get(reverse("people"), {"q": "may"})
        self.assertEqual(len(search.data["results"]), 1)

        follow_url = reverse("person-follow", kwargs={"username": "maya"})
        followed = self.client.post(follow_url, {}, format="json")
        self.assertEqual(followed.status_code, status.HTTP_200_OK)
        self.assertTrue(followed.data["is_following"])
        self.assertEqual(followed.data["followers_count"], 1)

        me_after_follow = self.client.get(self.me_url)
        self.assertEqual(me_after_follow.data["following_count"], 1)

        detail = self.client.get(reverse("person-detail", kwargs={"username": "maya"}))
        self.assertTrue(detail.data["is_following"])

        unfollowed = self.client.delete(follow_url)
        self.assertEqual(unfollowed.status_code, status.HTTP_200_OK)
        self.assertFalse(unfollowed.data["is_following"])
        self.assertEqual(unfollowed.data["followers_count"], 0)

    def test_user_cannot_follow_self(self):
        me = self.register(self.payload)
        self.authenticate(me.data["access"])
        response = self.client.post(
            reverse("person-follow", kwargs={"username": "omar"}),
            {},
            format="json",
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_post_creation_feed_following_and_delete(self):
        me = self.register(self.payload)
        maya = self.register(
            {
                "email": "maya@example.com",
                "password": "StrongNovaPass2026!",
                "username": "maya",
                "name": "Maya Noor",
            }
        )
        self.assertEqual(me.status_code, status.HTTP_201_CREATED)
        self.assertEqual(maya.status_code, status.HTTP_201_CREATED)

        posts_url = reverse("posts")
        feed_url = reverse("feed")

        self.authenticate(me.data["access"])
        mine = self.client.post(
            posts_url,
            {"caption": "First Nova moment", "image": self.image("mine.png")},
            format="multipart",
        )
        self.assertEqual(mine.status_code, status.HTTP_201_CREATED)
        self.assertEqual(mine.data["caption"], "First Nova moment")
        self.assertTrue(mine.data["is_mine"])
        self.assertEqual(mine.data["likes_count"], 0)
        self.assertEqual(mine.data["comments_count"], 0)
        self.assertFalse(mine.data["is_liked"])
        mine_id = mine.data["id"]

        me_after_post = self.client.get(self.me_url)
        self.assertEqual(me_after_post.data["posts_count"], 1)

        self.authenticate(maya.data["access"])
        maya_post = self.client.post(
            posts_url,
            {"caption": "Maya's moment", "image": self.image("maya.png")},
            format="multipart",
        )
        self.assertEqual(maya_post.status_code, status.HTTP_201_CREATED)
        self.assertTrue(maya_post.data["is_mine"])

        self.authenticate(me.data["access"])
        feed_before_follow = self.client.get(feed_url)
        self.assertEqual(feed_before_follow.status_code, status.HTTP_200_OK)
        self.assertEqual(len(feed_before_follow.data["results"]), 1)
        self.assertEqual(feed_before_follow.data["results"][0]["id"], mine_id)
        self.assertIsNone(feed_before_follow.data["next_cursor"])

        self.client.post(
            reverse("person-follow", kwargs={"username": "maya"}),
            {},
            format="json",
        )
        feed_after_follow = self.client.get(feed_url)
        self.assertEqual(len(feed_after_follow.data["results"]), 2)
        usernames = {post["author"]["username"] for post in feed_after_follow.data["results"]}
        self.assertEqual(usernames, {"omar", "maya"})

        maya_detail = self.client.get(
            reverse("person-detail", kwargs={"username": "maya"})
        )
        self.assertEqual(maya_detail.data["posts_count"], 1)

        delete_response = self.client.delete(
            reverse("post-detail", kwargs={"post_id": mine_id})
        )
        self.assertEqual(delete_response.status_code, status.HTTP_204_NO_CONTENT)

        feed_after_delete = self.client.get(feed_url)
        self.assertEqual(len(feed_after_delete.data["results"]), 1)
        self.assertEqual(feed_after_delete.data["results"][0]["author"]["username"], "maya")

    def test_post_likes_and_comments_are_persistent_and_idempotent(self):
        me = self.register(self.payload)
        maya = self.register(
            {
                "email": "maya@example.com",
                "password": "StrongNovaPass2026!",
                "username": "maya",
                "name": "Maya Noor",
            }
        )

        self.authenticate(maya.data["access"])
        maya_post = self.client.post(
            reverse("posts"),
            {"caption": "Talk to me", "image": self.image("maya-talk.png")},
            format="multipart",
        )
        post_id = maya_post.data["id"]

        self.authenticate(me.data["access"])
        self.client.post(
            reverse("person-follow", kwargs={"username": "maya"}),
            {},
            format="json",
        )

        like_url = reverse("post-like", kwargs={"post_id": post_id})
        liked = self.client.post(like_url, {}, format="json")
        self.assertEqual(liked.status_code, status.HTTP_200_OK)
        self.assertTrue(liked.data["is_liked"])
        self.assertEqual(liked.data["likes_count"], 1)

        liked_again = self.client.post(like_url, {}, format="json")
        self.assertEqual(liked_again.data["likes_count"], 1)

        comments_url = reverse("post-comments", kwargs={"post_id": post_id})
        added = self.client.post(comments_url, {"body": "This feels alive."}, format="json")
        self.assertEqual(added.status_code, status.HTTP_201_CREATED)
        self.assertEqual(added.data["comment"]["body"], "This feels alive.")
        self.assertTrue(added.data["comment"]["is_mine"])
        self.assertEqual(added.data["post"]["comments_count"], 1)
        comment_id = added.data["comment"]["id"]

        comments = self.client.get(comments_url)
        self.assertEqual(comments.status_code, status.HTTP_200_OK)
        self.assertEqual(len(comments.data["results"]), 1)
        self.assertEqual(comments.data["results"][0]["author"]["username"], "omar")

        blank = self.client.post(comments_url, {"body": "   "}, format="json")
        self.assertEqual(blank.status_code, status.HTTP_400_BAD_REQUEST)

        unliked = self.client.delete(like_url)
        self.assertEqual(unliked.status_code, status.HTTP_200_OK)
        self.assertFalse(unliked.data["is_liked"])
        self.assertEqual(unliked.data["likes_count"], 0)

        deleted_comment = self.client.delete(
            reverse("comment-detail", kwargs={"comment_id": comment_id})
        )
        self.assertEqual(deleted_comment.status_code, status.HTTP_200_OK)
        self.assertEqual(deleted_comment.data["post"]["comments_count"], 0)

        feed = self.client.get(reverse("feed"))
        target = next(post for post in feed.data["results"] if post["id"] == post_id)
        self.assertEqual(target["likes_count"], 0)
        self.assertEqual(target["comments_count"], 0)
        self.assertFalse(target["is_liked"])

    def test_profile_gallery_is_visible_before_following(self):
        me = self.register(self.payload)
        maya = self.register(
            {
                "email": "maya@example.com",
                "password": "StrongNovaPass2026!",
                "username": "maya",
                "name": "Maya Noor",
            }
        )

        self.authenticate(maya.data["access"])
        maya_post = self.client.post(
            reverse("posts"),
            {"caption": "Public profile moment", "image": self.image("maya-profile.png")},
            format="multipart",
        )
        post_id = maya_post.data["id"]

        self.authenticate(me.data["access"])
        gallery = self.client.get(reverse("person-posts", kwargs={"username": "maya"}))
        self.assertEqual(gallery.status_code, status.HTTP_200_OK)
        self.assertEqual(len(gallery.data["results"]), 1)
        self.assertEqual(gallery.data["results"][0]["id"], post_id)
        self.assertFalse(gallery.data["results"][0]["is_mine"])

        detail = self.client.get(reverse("post-detail", kwargs={"post_id": post_id}))
        self.assertEqual(detail.status_code, status.HTTP_200_OK)
        self.assertEqual(detail.data["author"]["username"], "maya")

        liked = self.client.post(
            reverse("post-like", kwargs={"post_id": post_id}),
            {},
            format="json",
        )
        self.assertEqual(liked.status_code, status.HTTP_200_OK)
        self.assertTrue(liked.data["is_liked"])

        commented = self.client.post(
            reverse("post-comments", kwargs={"post_id": post_id}),
            {"body": "Saw this from your profile."},
            format="json",
        )
        self.assertEqual(commented.status_code, status.HTTP_201_CREATED)
        self.assertEqual(commented.data["post"]["comments_count"], 1)

    def test_feed_cursor_pagination_has_no_duplicates(self):
        me = self.register(self.payload)
        self.assertEqual(me.status_code, status.HTTP_201_CREATED)
        user = get_user_model().objects.get(email=self.payload["email"])

        Post.objects.bulk_create(
            [
                Post(
                    author=user,
                    image=f"posts/test-{index}.png",
                    caption=f"Moment {index}",
                )
                for index in range(45)
            ]
        )

        self.authenticate(me.data["access"])
        feed_url = reverse("feed")

        first = self.client.get(feed_url)
        self.assertEqual(first.status_code, status.HTTP_200_OK)
        self.assertEqual(len(first.data["results"]), 20)
        self.assertIsNotNone(first.data["next_cursor"])

        second = self.client.get(feed_url, {"cursor": first.data["next_cursor"]})
        self.assertEqual(second.status_code, status.HTTP_200_OK)
        self.assertEqual(len(second.data["results"]), 20)
        self.assertIsNotNone(second.data["next_cursor"])

        third = self.client.get(feed_url, {"cursor": second.data["next_cursor"]})
        self.assertEqual(third.status_code, status.HTTP_200_OK)
        self.assertEqual(len(third.data["results"]), 5)
        self.assertIsNone(third.data["next_cursor"])

        ids = [
            post["id"]
            for page in (first, second, third)
            for post in page.data["results"]
        ]
        self.assertEqual(len(ids), 45)
        self.assertEqual(len(set(ids)), 45)

        invalid = self.client.get(feed_url, {"cursor": "not-a-number"})
        self.assertEqual(invalid.status_code, status.HTTP_400_BAD_REQUEST)
