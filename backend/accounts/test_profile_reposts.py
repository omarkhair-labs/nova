from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Follow, Post, User, UserBlock
from .privacy_models import AccountPrivacy
from .sharing_models import Repost


class ProfileRepostsTests(APITestCase):
    def setUp(self):
        self.viewer = self.user("viewer")
        self.profile = self.user("profile")
        self.author = self.user("author")
        self.other_author = self.user("otherauthor")

    def user(self, username):
        return User.objects.create_user(
            email=f"{username}@example.com",
            username=username,
            name=username.title(),
            password="StrongNovaPass2026!",
        )

    def post_for(self, author, marker):
        return Post.objects.create(
            author=author,
            image=f"posts/test/{marker}.jpg",
            caption=marker,
        )

    def auth(self, user=None):
        self.client.force_authenticate(user=user or self.viewer)

    def endpoint(self):
        return reverse("person-posts", kwargs={"username": self.profile.username})

    def test_profile_reposts_are_returned_in_repost_order(self):
        older_post = self.post_for(self.author, "older")
        newer_post = self.post_for(self.other_author, "newer")
        Repost.objects.create(user=self.profile, post=older_post)
        Repost.objects.create(user=self.profile, post=newer_post)

        self.auth()
        response = self.client.get(self.endpoint(), {"kind": "reposts"})

        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertEqual(
            [item["id"] for item in response.data["results"]],
            [newer_post.id, older_post.id],
        )

    def test_private_profile_reposts_follow_profile_visibility(self):
        AccountPrivacy.objects.create(user=self.profile, is_private=True)
        reposted = self.post_for(self.author, "private-profile")
        Repost.objects.create(user=self.profile, post=reposted)

        self.auth()
        hidden = self.client.get(self.endpoint(), {"kind": "reposts"})
        self.assertEqual(hidden.status_code, status.HTTP_403_FORBIDDEN)

        Follow.objects.create(follower=self.viewer, following=self.profile)
        visible = self.client.get(self.endpoint(), {"kind": "reposts"})
        self.assertEqual(visible.status_code, status.HTTP_200_OK, visible.data)
        self.assertEqual([item["id"] for item in visible.data["results"]], [reposted.id])

    def test_reposts_do_not_leak_blocked_author_content(self):
        blocked_post = self.post_for(self.author, "blocked")
        visible_post = self.post_for(self.other_author, "visible")
        Repost.objects.create(user=self.profile, post=blocked_post)
        Repost.objects.create(user=self.profile, post=visible_post)
        UserBlock.objects.create(blocker=self.viewer, blocked=self.author)

        self.auth()
        response = self.client.get(self.endpoint(), {"kind": "reposts"})

        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertEqual([item["id"] for item in response.data["results"]], [visible_post.id])

    def test_invalid_profile_content_kind_is_rejected(self):
        self.auth()
        response = self.client.get(self.endpoint(), {"kind": "likes"})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
