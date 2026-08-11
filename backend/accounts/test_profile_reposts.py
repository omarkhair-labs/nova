from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Follow, Post, User
from .privacy_models import AccountPrivacy
from .sharing_models import Repost


class ProfileRepostsTests(APITestCase):
    def setUp(self):
        self.viewer = self.user("viewer")
        self.owner = self.user("owner")
        self.author = self.user("author")

    def user(self, username):
        return User.objects.create_user(
            email=f"{username}@example.com",
            username=username,
            name=username.title(),
            password="StrongNovaPass2026!",
        )

    def post(self, author, suffix):
        return Post.objects.create(
            author=author,
            image=f"posts/tests/{suffix}.jpg",
            caption=f"Post {suffix}",
        )

    def auth(self, user):
        self.client.force_authenticate(user=user)

    def reposts_url(self, username=None):
        return reverse(
            "person-reposts",
            kwargs={"username": username or self.owner.username},
        )

    def test_profile_reposts_are_newest_first_and_use_post_serializer(self):
        first = self.post(self.author, "first")
        second = self.post(self.owner, "second")
        Repost.objects.create(user=self.owner, post=first)
        Repost.objects.create(user=self.owner, post=second)

        self.auth(self.viewer)
        response = self.client.get(self.reposts_url())

        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertEqual(
            [item["id"] for item in response.data["results"]],
            [second.pk, first.pk],
        )
        self.assertIn("author", response.data["results"][0])
        self.assertIn("image_url", response.data["results"][0])

    def test_private_profile_reposts_require_approved_follow(self):
        post = self.post(self.author, "private-owner")
        Repost.objects.create(user=self.owner, post=post)
        AccountPrivacy.objects.create(user=self.owner, is_private=True)

        self.auth(self.viewer)
        denied = self.client.get(self.reposts_url())
        self.assertEqual(denied.status_code, status.HTTP_403_FORBIDDEN)

        Follow.objects.create(follower=self.viewer, following=self.owner)
        allowed = self.client.get(self.reposts_url())
        self.assertEqual(allowed.status_code, status.HTTP_200_OK, allowed.data)
        self.assertEqual([item["id"] for item in allowed.data["results"]], [post.pk])

    def test_repost_does_not_bypass_original_author_privacy(self):
        private_post = self.post(self.author, "private-source")
        Repost.objects.create(user=self.owner, post=private_post)
        AccountPrivacy.objects.create(user=self.author, is_private=True)

        self.auth(self.viewer)
        hidden = self.client.get(self.reposts_url())
        self.assertEqual(hidden.status_code, status.HTTP_200_OK, hidden.data)
        self.assertEqual(hidden.data["results"], [])

        Follow.objects.create(follower=self.viewer, following=self.author)
        visible = self.client.get(self.reposts_url())
        self.assertEqual(visible.status_code, status.HTTP_200_OK, visible.data)
        self.assertEqual([item["id"] for item in visible.data["results"]], [private_post.pk])

    def test_profile_reposts_use_repost_cursor_not_post_id(self):
        posts = [self.post(self.author, f"cursor-{index}") for index in range(3)]
        reposts = [Repost.objects.create(user=self.owner, post=post) for post in posts]

        self.auth(self.viewer)
        response = self.client.get(self.reposts_url(), {"cursor": str(reposts[-1].pk)})

        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertEqual(
            [item["id"] for item in response.data["results"]],
            [posts[1].pk, posts[0].pk],
        )

        invalid = self.client.get(self.reposts_url(), {"cursor": "bad"})
        self.assertEqual(invalid.status_code, status.HTTP_400_BAD_REQUEST)
