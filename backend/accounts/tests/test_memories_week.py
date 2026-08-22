from datetime import datetime, timedelta, timezone as dt_timezone
from unittest.mock import patch

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from ..messaging_models import GroupMembership
from ..models import Conversation, Post, User, UserBlock
from ..pulse_models import Pulse
from ..room_models import RoomItem


class WeeklyMemoryTests(APITestCase):
    def setUp(self):
        self.viewer = self.user("viewer")
        self.friend = self.user("friend")
        self.other = self.user("other")
        self.room = self.group("Our week", self.viewer, self.friend)
        self.other_room = self.group("Not mine", self.other, self.friend)
        self.client.force_authenticate(user=self.viewer)
        self.now = datetime(2026, 8, 17, 12, 0, tzinfo=dt_timezone.utc)  # Monday

    def user(self, stem):
        return User.objects.create_user(
            email=f"{stem}-memories@example.com",
            username=f"{stem}_memories",
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

    def pulse(self, author, when, note="Pulse"):
        pulse = Pulse.objects.create(
            author=author,
            media_type=Pulse.MediaType.TEXT,
            audience=Pulse.Audience.FOLLOWERS,
            note=note,
        )
        Pulse.objects.filter(pk=pulse.pk).update(created_at=when)
        pulse.refresh_from_db()
        return pulse

    def post(self, author, when, caption="Post"):
        post = Post.objects.create(
            author=author,
            image="posts/memory-test.jpg",
            caption=caption,
        )
        Post.objects.filter(pk=post.pk).update(created_at=when)
        post.refresh_from_db()
        return post

    def room_item(self, conversation, creator, when, body="Room thing"):
        item = RoomItem.objects.create(
            conversation=conversation,
            created_by=creator,
            kind=RoomItem.Kind.NOTE,
            body=body,
        )
        RoomItem.objects.filter(pk=item.pk).update(created_at=when)
        item.refresh_from_db()
        return item

    def get_memory(self, **params):
        with patch("accounts.memories.views.timezone.now", return_value=self.now):
            return self.client.get(reverse("memory-week"), params or {"utc_offset_minutes": 0})

    def test_latest_completed_week_combines_own_content_and_shared_room_history(self):
        pulse = self.pulse(self.viewer, datetime(2026, 8, 11, 21, 0, tzinfo=dt_timezone.utc))
        post = self.post(self.viewer, datetime(2026, 8, 13, 15, 0, tzinfo=dt_timezone.utc))
        room_item = self.room_item(
            self.room,
            self.friend,
            datetime(2026, 8, 15, 22, 0, tzinfo=dt_timezone.utc),
        )
        self.pulse(self.viewer, self.now - timedelta(hours=1), note="Current week must wait")

        response = self.get_memory(utc_offset_minutes=0)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["stats"]["pulses"], 1)
        self.assertEqual(response.data["stats"]["posts"], 1)
        self.assertEqual(response.data["stats"]["room_items"], 1)
        self.assertEqual(response.data["stats"]["rooms"], 1)
        self.assertEqual(response.data["stats"]["people"], 1)
        self.assertEqual(response.data["stats"]["highlights"], 3)
        self.assertEqual(
            [(row["source"], row["id"]) for row in response.data["highlights"]],
            [("pulse", pulse.pk), ("post", post.pk), ("room_item", room_item.pk)],
        )
        self.assertEqual(response.data["people"][0]["person"]["id"], self.friend.pk)
        self.assertEqual(response.data["rooms"][0]["room"]["id"], self.room.pk)

    def test_room_membership_and_block_policy_prevent_memory_leaks(self):
        self.room_item(
            self.other_room,
            self.friend,
            datetime(2026, 8, 12, 20, 0, tzinfo=dt_timezone.utc),
            body="Not a member",
        )
        self.room_item(
            self.room,
            self.friend,
            datetime(2026, 8, 13, 20, 0, tzinfo=dt_timezone.utc),
            body="Blocked",
        )
        UserBlock.objects.create(blocker=self.viewer, blocked=self.friend)

        response = self.get_memory(utc_offset_minutes=0)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["stats"]["room_items"], 0)
        self.assertEqual(response.data["rooms"], [])
        self.assertEqual(response.data["people"], [])
        self.assertEqual(response.data["highlights"], [])

    def test_weeks_ago_reads_an_older_completed_week(self):
        older = self.pulse(
            self.viewer,
            datetime(2026, 8, 5, 19, 0, tzinfo=dt_timezone.utc),
            note="Older week",
        )
        self.pulse(
            self.viewer,
            datetime(2026, 8, 12, 19, 0, tzinfo=dt_timezone.utc),
            note="Latest completed week",
        )

        response = self.get_memory(utc_offset_minutes=0, weeks_ago=1)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["weeks_ago"], 1)
        self.assertEqual(response.data["stats"]["pulses"], 1)
        self.assertEqual(response.data["highlights"][0]["id"], older.pk)

    def test_after_midnight_activity_belongs_to_the_previous_night(self):
        self.pulse(
            self.viewer,
            datetime(2026, 8, 12, 22, 30, tzinfo=dt_timezone.utc),
            note="Late",
        )
        self.room_item(
            self.room,
            self.viewer,
            datetime(2026, 8, 13, 2, 0, tzinfo=dt_timezone.utc),
            body="Still same night",
        )
        self.post(
            self.viewer,
            datetime(2026, 8, 14, 20, 0, tzinfo=dt_timezone.utc),
            caption="Different night",
        )

        response = self.get_memory(utc_offset_minutes=0)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["stats"]["nights"], 2)

    def test_local_offset_moves_calendar_week_boundary(self):
        # Sunday 22:30 UTC is already Monday 01:30 in UTC+3 and belongs to the current week,
        # therefore it must not appear in the just-completed local week.
        self.pulse(
            self.viewer,
            datetime(2026, 8, 16, 22, 30, tzinfo=dt_timezone.utc),
            note="Local Monday",
        )

        response = self.get_memory(utc_offset_minutes=180)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["stats"]["pulses"], 0)

    def test_invalid_offset_and_week_index_are_rejected(self):
        invalid_offset = self.get_memory(utc_offset_minutes=2000)
        invalid_week = self.get_memory(utc_offset_minutes=0, weeks_ago=99)

        self.assertEqual(invalid_offset.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(invalid_week.status_code, status.HTTP_400_BAD_REQUEST)
