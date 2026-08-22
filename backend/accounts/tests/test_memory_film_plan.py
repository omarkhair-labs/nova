from datetime import datetime, timedelta, timezone as dt_timezone
from unittest.mock import patch

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from ..messaging_models import GroupMembership
from ..models import Conversation, Post, User, UserBlock
from ..pulse_models import Pulse
from ..room_models import RoomItem


class MemoryFilmPlanTests(APITestCase):
    def setUp(self):
        self.viewer = self.user("viewer")
        self.friend = self.user("friend")
        self.room = self.group("Film room", self.viewer, self.friend)
        self.client.force_authenticate(user=self.viewer)
        self.now = datetime(2026, 8, 17, 12, 0, tzinfo=dt_timezone.utc)

    def user(self, stem):
        return User.objects.create_user(
            email=f"{stem}-film@example.com",
            username=f"{stem}_memory_film",
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

    def image_pulse(self, when, note="Pulse image"):
        pulse = Pulse.objects.create(
            author=self.viewer,
            media="pulses/memory-film.jpg",
            media_type=Pulse.MediaType.IMAGE,
            audience=Pulse.Audience.FOLLOWERS,
            note=note,
        )
        Pulse.objects.filter(pk=pulse.pk).update(created_at=when)
        pulse.refresh_from_db()
        return pulse

    def video_pulse(self, when, note="Pulse video"):
        pulse = Pulse.objects.create(
            author=self.viewer,
            media="pulses/memory-film.mp4",
            media_type=Pulse.MediaType.VIDEO,
            audience=Pulse.Audience.FOLLOWERS,
            note=note,
        )
        Pulse.objects.filter(pk=pulse.pk).update(created_at=when)
        pulse.refresh_from_db()
        return pulse

    def post(self, when, caption="Post image"):
        post = Post.objects.create(
            author=self.viewer,
            image="posts/memory-film.jpg",
            caption=caption,
        )
        Post.objects.filter(pk=post.pk).update(created_at=when)
        post.refresh_from_db()
        return post

    def room_photo(self, when, creator=None, body="Room photo"):
        item = RoomItem.objects.create(
            conversation=self.room,
            created_by=creator or self.friend,
            kind=RoomItem.Kind.PHOTO,
            body=body,
            media="rooms/items/memory-film.jpg",
        )
        RoomItem.objects.filter(pk=item.pk).update(created_at=when)
        item.refresh_from_db()
        return item

    def room_note(self, when, creator=None, body="Room note"):
        item = RoomItem.objects.create(
            conversation=self.room,
            created_by=creator or self.friend,
            kind=RoomItem.Kind.NOTE,
            body=body,
        )
        RoomItem.objects.filter(pk=item.pk).update(created_at=when)
        item.refresh_from_db()
        return item

    def get_plan(self, **params):
        with patch("accounts.memories.film_views.timezone.now", return_value=self.now):
            return self.client.get(
                reverse("memory-film-plan"),
                params or {"utc_offset_minutes": 0},
            )

    def test_text_only_week_is_valid_but_not_film_ready(self):
        self.room_note(datetime(2026, 8, 12, 20, 0, tzinfo=dt_timezone.utc))

        response = self.get_plan(utc_offset_minutes=0)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["film_ready"])
        self.assertEqual(response.data["scenes"], [])
        self.assertEqual(response.data["total_duration_ms"], 0)

    def test_media_scenes_are_selected_then_returned_chronologically(self):
        older = self.post(datetime(2026, 8, 11, 12, 0, tzinfo=dt_timezone.utc))
        video = self.video_pulse(datetime(2026, 8, 13, 22, 0, tzinfo=dt_timezone.utc))
        latest = self.room_photo(datetime(2026, 8, 15, 19, 0, tzinfo=dt_timezone.utc))

        response = self.get_plan(utc_offset_minutes=0)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data["film_ready"])
        self.assertEqual(
            [(scene["source"], scene["source_id"]) for scene in response.data["scenes"]],
            [("post", older.pk), ("pulse", video.pk), ("room_item", latest.pk)],
        )
        self.assertEqual(response.data["scenes"][1]["duration_ms"], 5000)
        self.assertEqual(response.data["scenes"][0]["duration_ms"], 3000)
        self.assertEqual(response.data["cover_media_url"], response.data["scenes"][0]["media_url"])

    def test_plan_stays_bounded_to_twelve_scenes_and_target_runtime(self):
        start = datetime(2026, 8, 10, 9, 0, tzinfo=dt_timezone.utc)
        for index in range(20):
            self.image_pulse(start + timedelta(hours=index * 3), note=f"Moment {index}")

        response = self.get_plan(utc_offset_minutes=0)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertLessEqual(len(response.data["scenes"]), 12)
        self.assertLessEqual(response.data["total_duration_ms"], 45_000)

    def test_blocked_room_creator_cannot_enter_film_plan(self):
        self.room_photo(
            datetime(2026, 8, 14, 20, 0, tzinfo=dt_timezone.utc),
            creator=self.friend,
        )
        UserBlock.objects.create(blocker=self.viewer, blocked=self.friend)

        response = self.get_plan(utc_offset_minutes=0)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertFalse(response.data["film_ready"])
        self.assertEqual(response.data["scenes"], [])

    def test_night_heavy_week_selects_after_dark_mood(self):
        self.video_pulse(datetime(2026, 8, 10, 22, 0, tzinfo=dt_timezone.utc))
        self.image_pulse(datetime(2026, 8, 12, 22, 0, tzinfo=dt_timezone.utc))
        self.room_photo(datetime(2026, 8, 14, 22, 0, tzinfo=dt_timezone.utc))

        response = self.get_plan(utc_offset_minutes=0)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["mood"], "after_dark")

    def test_invalid_window_parameters_are_rejected(self):
        invalid_offset = self.get_plan(utc_offset_minutes=2000)
        invalid_week = self.get_plan(utc_offset_minutes=0, weeks_ago=99)

        self.assertEqual(invalid_offset.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(invalid_week.status_code, status.HTTP_400_BAD_REQUEST)
