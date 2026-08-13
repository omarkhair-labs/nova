from django.core.files.uploadedfile import SimpleUploadedFile
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Follow, User
from .privacy_models import AccountPrivacy
from .reels_models import Reel, ReelRepost


class ProfileReelRepostsV4Tests(APITestCase):
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

    def reel(self, author, suffix):
        return Reel.objects.create(
            author=author,
            video=SimpleUploadedFile(
                f"{suffix}.mp4",
                b"nova-reel-test",
                content_type="video/mp4",
            ),
            caption=f"Reel {suffix}",
        )

    def url(self):
        return reverse("profile-reels", kwargs={"username": self.owner.username})

    def auth(self):
        self.client.force_authenticate(user=self.viewer)

    def test_reposted_source_returns_reels_reposted_by_profile_owner(self):
        authored = self.reel(self.owner, "authored")
        reposted = self.reel(self.author, "reposted")
        ReelRepost.objects.create(user=self.owner, reel=reposted)

        self.auth()
        response = self.client.get(self.url(), {"source": "reposted"})

        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertEqual(response.data["source"], "reposted")
        self.assertEqual([item["id"] for item in response.data["results"]], [reposted.pk])
        self.assertNotIn(authored.pk, [item["id"] for item in response.data["results"]])
        self.assertEqual(response.data["results"][0]["reposted_by"]["username"], self.owner.username)

    def test_authored_source_does_not_mix_in_reposted_reels(self):
        authored = self.reel(self.owner, "owner-authored")
        reposted = self.reel(self.author, "other-authored")
        ReelRepost.objects.create(user=self.owner, reel=reposted)

        self.auth()
        response = self.client.get(self.url())

        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertEqual(response.data["source"], "authored")
        self.assertEqual([item["id"] for item in response.data["results"]], [authored.pk])

    def test_private_profile_reel_reposts_require_approved_follow(self):
        reposted = self.reel(self.author, "private-repost")
        ReelRepost.objects.create(user=self.owner, reel=reposted)
        AccountPrivacy.objects.create(user=self.owner, is_private=True)

        self.auth()
        denied = self.client.get(self.url(), {"source": "reposted"})
        self.assertEqual(denied.status_code, status.HTTP_403_FORBIDDEN)

        Follow.objects.create(follower=self.viewer, following=self.owner)
        allowed = self.client.get(self.url(), {"source": "reposted"})
        self.assertEqual(allowed.status_code, status.HTTP_200_OK, allowed.data)
        self.assertEqual([item["id"] for item in allowed.data["results"]], [reposted.pk])

    def test_reposted_source_cursor_uses_repost_id(self):
        first = self.reel(self.author, "cursor-first")
        second = self.reel(self.author, "cursor-second")
        first_repost = ReelRepost.objects.create(user=self.owner, reel=first)
        second_repost = ReelRepost.objects.create(user=self.owner, reel=second)

        self.auth()
        response = self.client.get(
            self.url(),
            {"source": "reposted", "cursor": str(second_repost.pk)},
        )

        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.assertEqual([item["id"] for item in response.data["results"]], [first.pk])
        self.assertLess(first_repost.pk, second_repost.pk)
