from datetime import UTC, datetime, timedelta
from unittest.mock import patch

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from ..models import Follow, User, UserBlock
from ..privacy_models import CloseFriend
from ..pulse_models import Pulse
from ..tonight.views import tonight_window


class TonightApiTests(APITestCase):
    def setUp(self):
        self.viewer = self.user("viewer")
        self.friend = self.user("friend")
        Follow.objects.create(follower=self.viewer, following=self.friend)
        self.url = reverse("tonight")

    def user(self, stem):
        return User.objects.create_user(
            email=f"{stem}@tonight.example.com",
            username=f"{stem}_tonight",
            password="StrongNovaPass2026!",
            name=stem.title(),
        )

    def pulse(self, author, note, audience="followers", expires_at=None):
        return Pulse.objects.create(
            author=author,
            media_type=Pulse.MediaType.TEXT,
            audience=audience,
            note=note,
            expires_at=expires_at,
        )

    def authenticate(self):
        self.client.force_authenticate(user=self.viewer)

    def test_tonight_requires_authentication(self):
        response = self.client.get(self.url, {"utc_offset_minutes": 180})
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_invalid_utc_offset_is_rejected(self):
        self.authenticate()
        invalid_text = self.client.get(self.url, {"utc_offset_minutes": "cairo"})
        invalid_range = self.client.get(self.url, {"utc_offset_minutes": 1000})
        self.assertEqual(invalid_text.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(invalid_range.status_code, status.HTTP_400_BAD_REQUEST)

    def test_daytime_surface_is_inactive_and_does_not_return_people(self):
        now = datetime(2026, 8, 22, 11, 0, tzinfo=UTC)  # 14:00 at +03:00.
        with patch("accounts.tonight.views.timezone.now", return_value=now):
            self.pulse(self.friend, "Day pulse", expires_at=now + timedelta(hours=6))
            self.authenticate()
            response = self.client.get(self.url, {"utc_offset_minutes": 180})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["is_tonight"])
        self.assertEqual(response.data["local_hour"], 14)
        self.assertEqual(response.data["people"], [])
        self.assertEqual(response.data["people_count"], 0)

    def test_evening_groups_visible_pulses_by_person_and_tracks_self_separately(self):
        now = datetime(2026, 8, 22, 19, 0, tzinfo=UTC)  # 22:00 at +03:00.
        with patch("accounts.tonight.views.timezone.now", return_value=now):
            self.pulse(self.friend, "First", expires_at=now + timedelta(hours=5))
            latest = self.pulse(self.friend, "Second", expires_at=now + timedelta(hours=5))
            self.pulse(self.viewer, "Mine", expires_at=now + timedelta(hours=5))
            self.authenticate()
            response = self.client.get(self.url, {"utc_offset_minutes": 180})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data["is_tonight"])
        self.assertEqual(response.data["people_count"], 1)
        self.assertEqual(response.data["moments_count"], 2)
        self.assertEqual(response.data["my_moments_count"], 1)
        self.assertEqual(response.data["people"][0]["person"]["username"], self.friend.username)
        self.assertEqual(response.data["people"][0]["moments_count"], 2)
        self.assertEqual(response.data["people"][0]["latest_pulse"]["id"], latest.pk)

    def test_close_friends_and_block_policy_are_inherited_from_pulse_visibility(self):
        close_friend = self.user("close_friend")
        blocked = self.user("blocked")
        Follow.objects.create(follower=self.viewer, following=close_friend)
        Follow.objects.create(follower=self.viewer, following=blocked)
        UserBlock.objects.create(blocker=self.viewer, blocked=blocked)
        now = datetime(2026, 8, 22, 20, 0, tzinfo=UTC)

        with patch("accounts.tonight.views.timezone.now", return_value=now):
            self.pulse(
                close_friend,
                "Inner circle",
                audience=Pulse.Audience.CLOSE_FRIENDS,
                expires_at=now + timedelta(hours=4),
            )
            self.pulse(blocked, "Blocked", expires_at=now + timedelta(hours=4))
            self.authenticate()
            hidden = self.client.get(self.url, {"utc_offset_minutes": 180})

            CloseFriend.objects.create(owner=close_friend, member=self.viewer)
            visible = self.client.get(self.url, {"utc_offset_minutes": 180})

        hidden_usernames = {row["person"]["username"] for row in hidden.data["people"]}
        visible_usernames = {row["person"]["username"] for row in visible.data["people"]}
        self.assertNotIn(close_friend.username, hidden_usernames)
        self.assertNotIn(blocked.username, hidden_usernames)
        self.assertIn(close_friend.username, visible_usernames)
        self.assertNotIn(blocked.username, visible_usernames)

    def test_moment_before_local_night_window_is_not_counted(self):
        now = datetime(2026, 8, 22, 20, 0, tzinfo=UTC)  # 23:00 local, start is 15:00 UTC.
        with patch("accounts.tonight.views.timezone.now", return_value=now):
            old = self.pulse(self.friend, "Too early", expires_at=now + timedelta(hours=2))
            Pulse.objects.filter(pk=old.pk).update(
                created_at=datetime(2026, 8, 22, 14, 59, tzinfo=UTC)
            )
            self.authenticate()
            response = self.client.get(self.url, {"utc_offset_minutes": 180})

        self.assertTrue(response.data["is_tonight"])
        self.assertEqual(response.data["people"], [])

    def test_early_morning_belongs_to_previous_evening_window(self):
        now = datetime(2026, 8, 22, 0, 30, tzinfo=UTC)  # 03:30 local.
        window = tonight_window(now, 180)

        self.assertTrue(window["is_tonight"])
        self.assertEqual(window["starts_at"], datetime(2026, 8, 21, 15, 0, tzinfo=UTC))
        self.assertEqual(window["ends_at"], datetime(2026, 8, 22, 3, 0, tzinfo=UTC))
