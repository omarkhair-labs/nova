import tempfile
import shutil

from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Follow, User
from .privacy_models import AccountPrivacy
from .reels_models import Reel


class ProfileReelsTests(APITestCase):
    @classmethod
    def setUpClass(cls):
        cls._media_dir = tempfile.mkdtemp(prefix="nova-profile-reels-")
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
            email="me-profile-reels@example.com",
            username="meprofilereels",
            password="StrongNovaPass2026!",
            name="Me",
        )
        self.maya = User.objects.create_user(
            email="maya-profile-reels@example.com",
            username="mayaprofilereels",
            password="StrongNovaPass2026!",
            name="Maya",
        )
        self.lina = User.objects.create_user(
            email="lina-profile-reels@example.com",
            username="linaprofilereels",
            password="StrongNovaPass2026!",
            name="Lina",
        )
        self.client.force_authenticate(user=self.me)

    def create_reel(self, author, caption):
        return Reel.objects.create(
            author=author,
            video=SimpleUploadedFile(
                f"{author.username}-{caption}.mp4",
                b"nova-profile-reel-video",
                content_type="video/mp4",
            ),
            caption=caption,
        )

    def profile_url(self, username):
        return reverse("profile-reels", kwargs={"username": username})

    def test_profile_feed_only_contains_requested_author(self):
        first = self.create_reel(self.maya, "first")
        second = self.create_reel(self.maya, "second")
        self.create_reel(self.lina, "not maya")

        response = self.client.get(self.profile_url(self.maya.username))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["username"], self.maya.username)
        self.assertEqual(
            [item["id"] for item in response.data["results"]],
            [second.pk, first.pk],
        )
        self.assertTrue(all(item["author"]["username"] == self.maya.username for item in response.data["results"]))

    def test_private_profile_reels_require_follow_relationship(self):
        reel = self.create_reel(self.maya, "private")
        AccountPrivacy.objects.create(user=self.maya, is_private=True)

        hidden = self.client.get(self.profile_url(self.maya.username))
        self.assertEqual(hidden.status_code, status.HTTP_200_OK)
        self.assertEqual(hidden.data["results"], [])

        Follow.objects.create(follower=self.me, following=self.maya)
        visible = self.client.get(self.profile_url(self.maya.username))
        self.assertEqual([item["id"] for item in visible.data["results"]], [reel.pk])

    def test_profile_reels_are_cursor_paginated(self):
        created = [self.create_reel(self.maya, f"reel-{index}") for index in range(13)]

        first_page = self.client.get(self.profile_url(self.maya.username))
        self.assertEqual(first_page.status_code, status.HTTP_200_OK)
        self.assertEqual(len(first_page.data["results"]), 12)
        self.assertIsNotNone(first_page.data["next_cursor"])
        self.assertEqual(first_page.data["results"][0]["id"], created[-1].pk)

        second_page = self.client.get(
            self.profile_url(self.maya.username),
            {"cursor": first_page.data["next_cursor"]},
        )
        self.assertEqual(second_page.status_code, status.HTTP_200_OK)
        self.assertEqual(len(second_page.data["results"]), 1)
        self.assertEqual(second_page.data["results"][0]["id"], created[0].pk)
        self.assertIsNone(second_page.data["next_cursor"])

    def test_owner_can_always_load_own_profile_reels(self):
        mine = self.create_reel(self.me, "mine")
        AccountPrivacy.objects.create(user=self.me, is_private=True)

        response = self.client.get(self.profile_url(self.me.username))

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual([item["id"] for item in response.data["results"]], [mine.pk])
