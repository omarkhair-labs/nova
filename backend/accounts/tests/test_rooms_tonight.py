from datetime import datetime, timedelta, timezone as dt_timezone
from unittest.mock import patch

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from ..messaging_models import GroupMembership
from ..models import Conversation, User, UserBlock
from ..room_models import RoomItem


class RoomTonightTests(APITestCase):
    def setUp(self):
        self.viewer = self.user("viewer")
        self.friend = self.user("friend")
        self.other = self.user("other")
        self.room = self.group("Night room", self.viewer, self.friend)
        self.other_room = self.group("Other room", self.other, self.friend)
        self.client.force_authenticate(user=self.viewer)

    def user(self, stem):
        return User.objects.create_user(
            email=f"{stem}-tonight@rooms.example.com",
            username=f"{stem}_room_tonight",
            password="StrongNovaPass2026!",
            name=stem.title(),
        )

    def group(self, title, owner, *members):
        conversation = Conversation.objects.create(
            kind=Conversation.Kind.GROUP,
            title=title,
            created_by=owner,
        )
        GroupMembership.objects.create(
            conversation=conversation,
            user=owner,
            role=GroupMembership.Role.OWNER,
        )
        for member in members:
            GroupMembership.objects.create(
                conversation=conversation,
                user=member,
                role=GroupMembership.Role.MEMBER,
            )
        return conversation

    def room_item(self, conversation, creator, when, body="Tonight"):
        item = RoomItem.objects.create(
            conversation=conversation,
            created_by=creator,
            kind=RoomItem.Kind.NOTE,
            body=body,
        )
        RoomItem.objects.filter(pk=item.pk).update(created_at=when)
        item.refresh_from_db()
        return item

    def test_daytime_returns_inactive_empty_room_surface(self):
        now = datetime(2026, 8, 22, 12, 0, tzinfo=dt_timezone.utc)
        self.room_item(self.room, self.friend, now - timedelta(minutes=10))

        with patch("accounts.rooms.views.timezone.now", return_value=now):
            response = self.client.get(reverse("rooms-tonight"), {"utc_offset_minutes": 0})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["is_tonight"])
        self.assertEqual(response.data["rooms_count"], 0)
        self.assertEqual(response.data["rooms"], [])

    def test_evening_groups_visible_activity_by_room_and_keeps_latest_item(self):
        now = datetime(2026, 8, 22, 22, 0, tzinfo=dt_timezone.utc)
        older = self.room_item(self.room, self.viewer, now - timedelta(hours=2), body="Mine")
        latest = self.room_item(self.room, self.friend, now - timedelta(minutes=20), body="Latest")
        self.room_item(self.other_room, self.friend, now - timedelta(minutes=5), body="Not my room")

        with patch("accounts.rooms.views.timezone.now", return_value=now):
            response = self.client.get(reverse("rooms-tonight"), {"utc_offset_minutes": 0})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data["is_tonight"])
        self.assertEqual(response.data["rooms_count"], 1)
        self.assertEqual(response.data["moments_count"], 2)
        row = response.data["rooms"][0]
        self.assertEqual(row["conversation"]["id"], self.room.pk)
        self.assertEqual(row["moments_count"], 2)
        self.assertEqual(row["my_moments_count"], 1)
        self.assertEqual(row["latest_item"]["id"], latest.pk)
        self.assertNotEqual(row["latest_item"]["id"], older.pk)

    def test_blocked_creator_activity_does_not_make_room_look_alive(self):
        now = datetime(2026, 8, 22, 23, 0, tzinfo=dt_timezone.utc)
        self.room_item(self.room, self.friend, now - timedelta(minutes=10))
        UserBlock.objects.create(blocker=self.viewer, blocked=self.friend)

        with patch("accounts.rooms.views.timezone.now", return_value=now):
            response = self.client.get(reverse("rooms-tonight"), {"utc_offset_minutes": 0})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["rooms_count"], 0)
        self.assertEqual(response.data["moments_count"], 0)

    def test_pre_window_room_activity_is_not_live_tonight(self):
        now = datetime(2026, 8, 22, 21, 0, tzinfo=dt_timezone.utc)
        self.room_item(self.room, self.friend, now.replace(hour=17, minute=59))

        with patch("accounts.rooms.views.timezone.now", return_value=now):
            response = self.client.get(reverse("rooms-tonight"), {"utc_offset_minutes": 0})

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["rooms_count"], 0)

    def test_invalid_offset_is_rejected(self):
        response = self.client.get(reverse("rooms-tonight"), {"utc_offset_minutes": 2000})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
